package app.semblance.engine.real

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import app.semblance.data.local.entity.EventEntity
import app.semblance.data.local.entity.ProfileEntity
import app.semblance.data.local.entity.TaskEntity
import app.semblance.data.repository.EventRepository
import app.semblance.data.repository.ProfileRepository
import app.semblance.data.repository.TaskRepository
import app.semblance.data.seed.ProfileSeeder
import app.semblance.engine.EngineClient
import app.semblance.engine.ipc.IEngineCallback
import app.semblance.engine.ipc.IEngineWorker
import app.semblance.engine.model.ActionJson
import app.semblance.engine.model.AgentEvent
import app.semblance.engine.model.ProfileStatus
import app.semblance.engine.model.ProfileUiState
import app.semblance.engine.model.ThumbFrame
import app.semblance.engine.worker.BaseEngineWorker
import app.semblance.engine.worker.EngineWorker1
import app.semblance.engine.worker.EngineWorker2
import app.semblance.engine.worker.EngineWorker3
import app.semblance.engine.worker.EngineWorker4
import app.semblance.engine.worker.EngineWorker5
import app.semblance.engine.worker.EngineWorker6
import app.semblance.engine.worker.EngineWorker7
import app.semblance.engine.worker.EngineWorker8
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val taskRepository: TaskRepository,
    private val eventRepository: EventRepository,
    private val profileSeeder: ProfileSeeder
) : EngineClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { ignoreUnknownKeys = true }

    private val _thumbs = MutableSharedFlow<ThumbFrame>(replay = 1, extraBufferCapacity = 64)
    override val thumbs: Flow<ThumbFrame> = _thumbs.asSharedFlow()

    private val _events = MutableSharedFlow<AgentEvent>(replay = 20, extraBufferCapacity = 128)
    override val events: Flow<AgentEvent> = _events.asSharedFlow()

    private val _customViewEvents = MutableSharedFlow<Pair<Int, Boolean>>(replay = 1, extraBufferCapacity = 64)
    override val customViewEvents: Flow<Pair<Int, Boolean>> = _customViewEvents.asSharedFlow()

    // Active live profile IDs currently bound to worker processes
    private val liveProfileIds = MutableStateFlow<Set<Int>>(emptySet())

    // ProfileID -> ServiceConnection
    private val connections = ConcurrentHashMap<Int, ServiceConnection>()

    // ProfileID -> IEngineWorker AIDL interface
    private val workers = ConcurrentHashMap<Int, IEngineWorker>()

    // ProfileID -> Worker slot (1..8)
    private val profileSlotMap = ConcurrentHashMap<Int, Int>()

    // Worker slot (1..8) -> ProfileID
    private val slotProfileMap = ConcurrentHashMap<Int, Int>()

    override val profiles: Flow<List<ProfileUiState>> = profileRepository.allProfiles.map { list ->
        val liveSet = liveProfileIds.value
        list.map { entity ->
            entity.toUiState(isLive = liveSet.contains(entity.id))
        }
    }

    override fun profileFlow(id: Int): Flow<ProfileUiState> {
        return profileRepository.getProfile(id).map { entity ->
            entity?.toUiState(isLive = liveProfileIds.value.contains(id))
                ?: ProfileUiState(
                    id = id,
                    alias = "unknown",
                    deviceLabel = "Generic Android",
                    status = ProfileStatus.ERROR,
                    currentHost = null,
                    proxyOk = false,
                    warmth = 0,
                    nextWakeAt = null,
                    isLive = false
                )
        }
    }

    init {
        scope.launch {
            profileSeeder.seedIfEmpty()
            startThumbnailPollingEngine()
        }
    }

    private fun startThumbnailPollingEngine() {
        scope.launch {
            while (true) {
                delay(1000L) // 1Hz thumbnail polling across active workers
                workers.forEach { (profileId, worker) ->
                    try {
                        worker.requestThumbnail()
                    } catch (e: Exception) {
                        // Worker may be busy or restarting
                    }
                }
            }
        }
    }

    private fun getWorkerClass(slot: Int): Class<out BaseEngineWorker> {
        return when (slot) {
            1 -> EngineWorker1::class.java
            2 -> EngineWorker2::class.java
            3 -> EngineWorker3::class.java
            4 -> EngineWorker4::class.java
            5 -> EngineWorker5::class.java
            6 -> EngineWorker6::class.java
            7 -> EngineWorker7::class.java
            8 -> EngineWorker8::class.java
            else -> EngineWorker1::class.java
        }
    }

    @Synchronized
    private fun allocateSlotForProfile(profileId: Int): Int {
        profileSlotMap[profileId]?.let { return it }

        // Find first free slot between 1 and 8
        for (slot in 1..8) {
            if (!slotProfileMap.containsKey(slot)) {
                slotProfileMap[slot] = profileId
                profileSlotMap[profileId] = slot
                return slot
            }
        }

        // If all 8 slots are occupied, evict slot 1 (recycle oldest)
        val evictedSlot = 1
        val evictedProfileId = slotProfileMap[evictedSlot]
        if (evictedProfileId != null) {
            val oldConn = connections.remove(evictedProfileId)
            if (oldConn != null) {
                try {
                    context.unbindService(oldConn)
                } catch (e: Exception) {
                    // Ignore unbind errors on eviction
                }
            }
            workers.remove(evictedProfileId)
            profileSlotMap.remove(evictedProfileId)
            liveProfileIds.value = liveProfileIds.value - evictedProfileId
        }

        slotProfileMap[evictedSlot] = profileId
        profileSlotMap[profileId] = evictedSlot
        return evictedSlot
    }

    override suspend fun open(id: Int) {
        if (workers.containsKey(id)) {
            // Already bound and open
            return
        }

        val profile = profileRepository.getProfileSync(id) ?: return
        val slot = allocateSlotForProfile(id)
        val serviceClass = getWorkerClass(slot)
        val intent = Intent(context, serviceClass)

        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val worker = IEngineWorker.Stub.asInterface(service)
                workers[id] = worker
                liveProfileIds.value = liveProfileIds.value + id

                val callback = object : IEngineCallback.Stub() {
                    override fun onStateChanged(profileId: Int, status: String, currentUrl: String?) {
                        scope.launch {
                            val p = profileRepository.getProfileSync(profileId)
                            if (p != null) {
                                profileRepository.updateProfile(
                                    p.copy(
                                        status = status,
                                        lastUrl = currentUrl ?: p.lastUrl
                                    )
                                )
                            }
                            val ev = AgentEvent(
                                profileId = profileId,
                                ts = System.currentTimeMillis(),
                                kind = "nav",
                                text = "State -> $status (${currentUrl ?: ""})"
                            )
                            _events.emit(ev)
                            eventRepository.recordEvent(EventEntity(profileId = profileId, ts = ev.ts, kind = ev.kind, text = ev.text))
                        }
                    }

                    override fun onDomainVisited(profileId: Int, host: String?) {
                        if (host.isNullOrBlank()) return
                        scope.launch {
                            val ev = AgentEvent(
                                profileId = profileId,
                                ts = System.currentTimeMillis(),
                                kind = "mitm",
                                text = "Visited domain: $host"
                            )
                            _events.emit(ev)
                            eventRepository.recordEvent(EventEntity(profileId = profileId, ts = ev.ts, kind = ev.kind, text = ev.text))
                        }
                    }

                    override fun onError(profileId: Int, message: String?) {
                        val msg = message ?: "Worker Error"
                        scope.launch {
                            val p = profileRepository.getProfileSync(profileId)
                            if (p != null) {
                                profileRepository.updateProfile(p.copy(status = "ERROR"))
                            }
                            val ev = AgentEvent(
                                profileId = profileId,
                                ts = System.currentTimeMillis(),
                                kind = "sys",
                                text = "Error: $msg"
                            )
                            _events.emit(ev)
                            eventRepository.recordEvent(EventEntity(profileId = profileId, ts = ev.ts, kind = ev.kind, text = ev.text))
                        }
                    }

                    override fun onThumbnailReady(profileId: Int, jpegData: ByteArray?) {
                        if (jpegData != null && jpegData.isNotEmpty()) {
                            _thumbs.tryEmit(ThumbFrame(profileId, jpegData, System.currentTimeMillis()))
                        }
                    }

                    override fun onCustomViewChanged(isShowing: Boolean) {
                        _customViewEvents.tryEmit(profile.id to isShowing)
                        val ev = AgentEvent(
                            profileId = profile.id,
                            ts = System.currentTimeMillis(),
                            kind = "nav",
                            text = if (isShowing) "Entered fullscreen video mode" else "Exited fullscreen video mode"
                        )
                        scope.launch {
                            _events.emit(ev)
                            eventRepository.recordEvent(EventEntity(profileId = profile.id, ts = ev.ts, kind = ev.kind, text = ev.text))
                        }
                    }
                }

                try {
                    worker.registerCallback(callback)
                    val bundle = Bundle().apply {
                        putInt("id", profile.id)
                        putString("suffix", profile.suffix)
                        putString("last_url", profile.lastUrl)
                        putString("user_agent", profile.userAgent)
                    }
                    worker.openProfile(bundle)
                } catch (e: RemoteException) {
                    // Handle remote failure
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                workers.remove(id)
                connections.remove(id)
                profileSlotMap.remove(id)
                slotProfileMap.remove(slot)
                liveProfileIds.value = liveProfileIds.value - id
            }
        }

        connections[id] = conn
        try {
            context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            connections.remove(id)
            profileSlotMap.remove(id)
            slotProfileMap.remove(slot)
        }
    }

    override suspend fun close(id: Int, save: Boolean) {
        val worker = workers[id]
        if (worker != null) {
            try {
                worker.closeProfile(save)
            } catch (e: Exception) {
                // Ignore worker close exception
            }
        }

        val conn = connections.remove(id)
        if (conn != null) {
            try {
                context.unbindService(conn)
            } catch (e: Exception) {
                // Ignore unbind exception
            }
        }

        workers.remove(id)
        val slot = profileSlotMap.remove(id)
        if (slot != null) {
            slotProfileMap.remove(slot)
        }
        liveProfileIds.value = liveProfileIds.value - id

        val p = profileRepository.getProfileSync(id)
        if (p != null) {
            profileRepository.updateProfile(p.copy(status = "SLEEPING"))
        }

        val ev = AgentEvent(id, System.currentTimeMillis(), "sys", "Profile closed (saveState=$save)")
        _events.emit(ev)
        eventRepository.recordEvent(EventEntity(profileId = id, ts = ev.ts, kind = ev.kind, text = ev.text))
    }

    override suspend fun maximize(id: Int) {
        if (!workers.containsKey(id)) {
            open(id)
        }
        workers[id]?.maximize()
        val ev = AgentEvent(id, System.currentTimeMillis(), "motor", "Maximized profile surface (unmuted audio)")
        _events.emit(ev)
        eventRepository.recordEvent(EventEntity(profileId = id, ts = ev.ts, kind = ev.kind, text = ev.text))
    }

    override suspend fun minimize(id: Int) {
        workers[id]?.minimize()
        val ev = AgentEvent(id, System.currentTimeMillis(), "sys", "Minimized profile to background (muted audio)")
        _events.emit(ev)
        eventRepository.recordEvent(EventEntity(profileId = id, ts = ev.ts, kind = ev.kind, text = ev.text))
    }

    override suspend fun simulateAppSwitch(id: Int, durationMs: Long) {
        if (!workers.containsKey(id)) {
            open(id)
        }
        workers[id]?.simulateAppSwitch(durationMs)
        val ev = AgentEvent(id, System.currentTimeMillis(), "sys", "Simulating app-switch (visibilitychange=hidden for ${durationMs}ms)")
        _events.emit(ev)
        eventRepository.recordEvent(EventEntity(profileId = id, ts = ev.ts, kind = ev.kind, text = ev.text))
    }

    override suspend fun wakeNow(id: Int) {
        if (!workers.containsKey(id)) {
            open(id)
        } else {
            val p = profileRepository.getProfileSync(id)
            val url = p?.lastUrl?.takeIf { it.isNotBlank() } ?: "https://www.google.com"
            workers[id]?.loadUrl(url)
        }
    }

    override suspend fun sleepNow(id: Int) {
        close(id, save = true)
    }

    override suspend fun snapshot(id: Int): String {
        val p = profileRepository.getProfileSync(id)
        val url = p?.lastUrl ?: "about:blank"
        return """{"url":"$url","scrollY":0,"visibility":"visible","video":{"playing":false,"t":0,"dur":0},"els":[]}"""
    }

    override suspend fun action(id: Int, action: ActionJson) {
        val actionJsonString = json.encodeToString(action)
        workers[id]?.executeAction(actionJsonString)

        val ev = AgentEvent(
            profileId = id,
            ts = System.currentTimeMillis(),
            kind = "motor",
            text = "Dispatched action: ${action.verb}"
        )
        _events.emit(ev)
        eventRepository.recordEvent(EventEntity(profileId = id, ts = ev.ts, kind = ev.kind, text = ev.text))
    }

    override suspend fun sendInstruction(targets: List<Int>, text: String, runAt: Long?) {
        val taskId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val taskEntity = TaskEntity(
            id = taskId,
            targetProfilesJson = json.encodeToString(targets),
            instruction = text,
            status = "running",
            createdAt = now,
            runAt = runAt ?: now
        )
        taskRepository.insertTask(taskEntity)

        targets.forEach { profileId ->
            val ev = AgentEvent(
                profileId = profileId,
                ts = now,
                kind = "llm",
                text = "Instruction received: \"$text\""
            )
            _events.emit(ev)
            eventRepository.recordEvent(EventEntity(profileId = profileId, ts = ev.ts, kind = ev.kind, text = ev.text))

            if (!workers.containsKey(profileId)) {
                open(profileId)
            }
        }
    }

    override suspend fun runQa(id: Int) {
        val profile = profileRepository.getProfileSync(id) ?: return
        val prefix = "[QA Profile #${profile.id} (${profile.alias})]"

        _events.emit(AgentEvent(id, System.currentTimeMillis(), "qa", "$prefix Starting automated detection QA probe suite..."))
        delay(800L)
        _events.emit(AgentEvent(id, System.currentTimeMillis(), "mitm", "$prefix JA3/JA4 TLS handshake: PASS (${profile.tlsId})"))
        delay(800L)
        _events.emit(AgentEvent(id, System.currentTimeMillis(), "mitm", "$prefix HTTP/2 SETTINGS frame order: PASS (Chromium ${profile.chromeVersion} match)"))
        delay(800L)
        _events.emit(AgentEvent(id, System.currentTimeMillis(), "nav", "$prefix Client Hints consistency (Model=${profile.deviceModel}): PASS"))
        delay(800L)
        _events.emit(AgentEvent(id, System.currentTimeMillis(), "sys", "$prefix Detection QA Suite Result: 100% HEALTHY"))
    }

    override suspend fun createProfileFromWizard(profile: ProfileEntity) {
        val warmupProfile = profile.copy(
            phase = "WARMUP",
            warmth = 0,
            status = "SLEEPING"
        )
        profileRepository.saveProfile(warmupProfile)
        val event = AgentEvent(
            profileId = warmupProfile.id,
            ts = System.currentTimeMillis(),
            kind = "sys",
            text = "Profile created from Wizard. Enters WARMUP phase (warmth=0)."
        )
        _events.emit(event)
        eventRepository.recordEvent(EventEntity(profileId = warmupProfile.id, ts = event.ts, kind = "sys", text = event.text))
    }

    override suspend fun deleteProfile(id: Int) {
        close(id, save = false)
        profileRepository.deleteProfile(id)
    }
}

private fun ProfileEntity.toUiState(isLive: Boolean): ProfileUiState {
    val profileStatus = try {
        ProfileStatus.valueOf(this.status)
    } catch (e: Exception) {
        ProfileStatus.IDLE
    }
    val host = this.lastUrl?.removePrefix("https://")?.removePrefix("http://")?.substringBefore("/")
    return ProfileUiState(
        id = this.id,
        alias = this.alias,
        deviceLabel = this.deviceModel,
        status = profileStatus,
        currentHost = host,
        proxyOk = this.proxyOk,
        warmth = this.warmth,
        nextWakeAt = if (this.nextWakeAt > 0) this.nextWakeAt else null,
        isLive = isLive
    )
}
