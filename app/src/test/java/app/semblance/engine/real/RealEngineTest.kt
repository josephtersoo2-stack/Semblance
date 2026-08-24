package app.semblance.engine.real

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import app.semblance.data.local.dao.EventDao
import app.semblance.data.local.dao.ProfileDao
import app.semblance.data.local.dao.TaskDao
import app.semblance.data.local.entity.EventEntity
import app.semblance.data.local.entity.ProfileEntity
import app.semblance.data.local.entity.TaskEntity
import app.semblance.data.repository.EventRepository
import app.semblance.data.repository.ProfileRepository
import app.semblance.data.repository.TaskRepository
import app.semblance.data.seed.ProfileSeeder
import app.semblance.engine.ipc.IEngineCallback
import app.semblance.engine.ipc.IEngineWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class RealEngineTest {

    private val profilesStore = mutableMapOf<Int, ProfileEntity>()
    private val tasksStore = mutableMapOf<String, TaskEntity>()
    private val eventsStore = mutableListOf<EventEntity>()

    private val profilesFlow = MutableStateFlow<List<ProfileEntity>>(emptyList())
    private val tasksFlow = MutableStateFlow<List<TaskEntity>>(emptyList())

    private lateinit var profileRepository: ProfileRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var eventRepository: EventRepository
    private lateinit var profileSeeder: ProfileSeeder
    private lateinit var mockContext: Context

    private val fakeProfileDao = object : ProfileDao {
        override fun getAllProfiles(): Flow<List<ProfileEntity>> = profilesFlow
        override fun getProfileById(id: Int): Flow<ProfileEntity?> = profilesFlow.map { it.find { p -> p.id == id } }
        override suspend fun getProfileByIdSync(id: Int): ProfileEntity? = profilesStore[id]
        override suspend fun getProfileCount(): Int = profilesStore.size
        override suspend fun insertOrUpdate(profile: ProfileEntity) {
            profilesStore[profile.id] = profile
            profilesFlow.value = profilesStore.values.toList()
        }
        override suspend fun insertAll(profiles: List<ProfileEntity>) {
            profiles.forEach { profilesStore[it.id] = it }
            profilesFlow.value = profilesStore.values.toList()
        }
        override suspend fun update(profile: ProfileEntity) {
            profilesStore[profile.id] = profile
            profilesFlow.value = profilesStore.values.toList()
        }
        override suspend fun delete(profile: ProfileEntity) {
            profilesStore.remove(profile.id)
            profilesFlow.value = profilesStore.values.toList()
        }
        override suspend fun deleteById(id: Int) {
            profilesStore.remove(id)
            profilesFlow.value = profilesStore.values.toList()
        }
    }

    private val fakeTaskDao = object : TaskDao {
        override fun getAllTasks(): Flow<List<TaskEntity>> = tasksFlow
        override fun getTaskById(id: String): Flow<TaskEntity?> = tasksFlow.map { it.find { t -> t.id == id } }
        override suspend fun getTaskByIdSync(id: String): TaskEntity? = tasksStore[id]
        override suspend fun insert(task: TaskEntity) {
            tasksStore[task.id] = task
            tasksFlow.value = tasksStore.values.toList()
        }
        override suspend fun update(task: TaskEntity) {
            tasksStore[task.id] = task
            tasksFlow.value = tasksStore.values.toList()
        }
        override suspend fun updateStatusAndTrace(id: String, status: String, traceLogJson: String, completedAt: Long?) {
            val existing = tasksStore[id]
            if (existing != null) {
                val updated = existing.copy(status = status, traceLogJson = traceLogJson, completedAt = completedAt)
                tasksStore[id] = updated
                tasksFlow.value = tasksStore.values.toList()
            }
        }
    }

    private val fakeEventDao = object : EventDao {
        override fun getRecentEvents(limit: Int): Flow<List<EventEntity>> = flowOf(eventsStore.takeLast(limit).reversed())
        override fun getEventsByProfile(profileId: Int, limit: Int): Flow<List<EventEntity>> =
            flowOf(eventsStore.filter { it.profileId == profileId }.takeLast(limit).reversed())
        override suspend fun insert(event: EventEntity) {
            eventsStore.add(event)
        }
        override suspend fun insertAll(events: List<EventEntity>) {
            eventsStore.addAll(events)
        }
        override suspend fun trimOlderThan(beforeTs: Long) {
            eventsStore.removeAll { it.ts < beforeTs }
        }
    }

    private lateinit var mockWorker: IEngineWorker
    private lateinit var mockBinder: IBinder

    @Before
    fun setup() {
        profilesStore.clear()
        tasksStore.clear()
        eventsStore.clear()
        profileRepository = ProfileRepository(fakeProfileDao)
        taskRepository = TaskRepository(fakeTaskDao)
        eventRepository = EventRepository(fakeEventDao)
        profileSeeder = ProfileSeeder(profileRepository)

        mockBinder = Proxy.newProxyInstance(
            IBinder::class.java.classLoader,
            arrayOf(IBinder::class.java)
        ) { _, method, _ ->
            if (method.name == "queryLocalInterface") {
                mockWorker
            } else {
                null
            }
        } as IBinder

        mockWorker = object : IEngineWorker {
            override fun openProfile(profileData: Bundle?) {}
            override fun closeProfile(saveState: Boolean) {}
            override fun loadUrl(url: String?) {}
            override fun requestThumbnail() {}
            override fun executeAction(actionJson: String?) {}
            override fun maximize() {}
            override fun minimize() {}
            override fun simulateAppSwitch(durationMs: Long) {}
            override fun registerCallback(cb: IEngineCallback?) {}
            override fun unregisterCallback(cb: IEngineCallback?) {}
            override fun asBinder(): IBinder = mockBinder
        }

        mockContext = object : ContextWrapper(null) {
            override fun bindService(service: Intent, conn: ServiceConnection, flags: Int): Boolean {
                conn.onServiceConnected(ComponentName("app.semblance", "EngineWorker1"), mockBinder)
                return true
            }
            override fun unbindService(conn: ServiceConnection) {
                conn.onServiceDisconnected(ComponentName("app.semblance", "EngineWorker1"))
            }
        }
    }

    @Test
    fun testRealEngineSeedsDatabaseAndExposesProfiles() = runTest {
        val engine = RealEngine(
            context = mockContext,
            profileRepository = profileRepository,
            taskRepository = taskRepository,
            eventRepository = eventRepository,
            profileSeeder = profileSeeder
        )

        var count = 0
        for (i in 1..30) {
            count = profileRepository.getCount()
            if (count == 8) break
            Thread.sleep(50)
        }
        assertEquals(8, count)

        var profileList = engine.profiles.first()
        for (i in 1..30) {
            profileList = engine.profiles.first()
            if (profileList.size == 8) break
            Thread.sleep(50)
        }
        assertEquals(8, profileList.size)
        assertTrue(profileList.any { it.alias == "alex_prime" })
        assertTrue(profileList.any { it.alias == "kev_19" })
    }

    @Test
    fun testRealEngineWizardCreationAndDeletion() = runTest {
        val engine = RealEngine(
            context = mockContext,
            profileRepository = profileRepository,
            taskRepository = taskRepository,
            eventRepository = eventRepository,
            profileSeeder = profileSeeder
        )

        for (i in 1..30) {
            if (profileRepository.getCount() == 8) break
            Thread.sleep(50)
        }

        val newProfile = ProfileEntity(
            id = 101,
            suffix = "p101",
            alias = "real_engine_user",
            age = 24,
            tz = "America/Chicago",
            voice = "casual",
            deviceModel = "Pixel 6a",
            phase = "ACTIVE",
            status = "WATCHING",
            warmth = 80
        )

        engine.createProfileFromWizard(newProfile)

        val persisted = profileRepository.getProfileSync(101)
        assertNotNull(persisted)
        assertEquals("WARMUP", persisted!!.phase)
        assertEquals(0, persisted.warmth)
        assertEquals("SLEEPING", persisted.status)

        engine.deleteProfile(101)
        val afterDelete = profileRepository.getProfileSync(101)
        assertEquals(null, afterDelete)
    }

    @Test
    fun testRealEngineMaximizeMinimizeAndSimulateAppSwitch() = runTest {
        val engine = RealEngine(
            context = mockContext,
            profileRepository = profileRepository,
            taskRepository = taskRepository,
            eventRepository = eventRepository,
            profileSeeder = profileSeeder
        )

        for (i in 1..30) {
            if (profileRepository.getCount() == 8) break
            Thread.sleep(50)
        }

        // Test maximize, minimize, and simulateAppSwitch event emissions
        engine.maximize(1)
        var recentEvents = eventRepository.getRecentEvents(5).first()
        assertTrue(recentEvents.any { it.kind == "motor" && it.text.contains("Maximized") })

        engine.minimize(1)
        recentEvents = eventRepository.getRecentEvents(5).first()
        assertTrue(recentEvents.any { it.kind == "sys" && it.text.contains("Minimized") })

        engine.simulateAppSwitch(1, 2000L)
        recentEvents = eventRepository.getRecentEvents(5).first()
        assertTrue(recentEvents.any { it.kind == "sys" && it.text.contains("Simulating app-switch") })
    }
}
