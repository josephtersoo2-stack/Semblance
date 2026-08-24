package app.semblance.engine.mock

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import app.semblance.data.datastore.SettingsDataStore
import app.semblance.data.local.entity.EventEntity
import app.semblance.data.local.entity.ProfileEntity
import app.semblance.data.local.entity.TaskEntity
import app.semblance.data.repository.EventRepository
import app.semblance.data.repository.ProfileRepository
import app.semblance.data.repository.TaskRepository
import app.semblance.engine.EngineClient
import app.semblance.engine.model.ActionJson
import app.semblance.engine.model.AgentEvent
import app.semblance.engine.model.ProfileStatus
import app.semblance.engine.model.ProfileUiState
import app.semblance.engine.model.ThumbFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MockEngine @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val taskRepository: TaskRepository,
    private val eventRepository: EventRepository,
    private val settingsDataStore: SettingsDataStore
) : EngineClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _thumbs = MutableSharedFlow<ThumbFrame>(replay = 1, extraBufferCapacity = 64)
    override val thumbs: Flow<ThumbFrame> = _thumbs.asSharedFlow()

    private val _events = MutableSharedFlow<AgentEvent>(replay = 20, extraBufferCapacity = 128)
    override val events: Flow<AgentEvent> = _events.asSharedFlow()

    private val _customViewEvents = MutableSharedFlow<Pair<Int, Boolean>>(replay = 1, extraBufferCapacity = 64)
    override val customViewEvents: Flow<Pair<Int, Boolean>> = _customViewEvents.asSharedFlow()

    // Live active profile IDs currently open in the worker pool
    private val liveProfileIds = MutableStateFlow<Set<Int>>(setOf(1, 2, 3, 5, 8))

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
            seedProfilesIfEmpty()
            startThumbnailGenerator()
            startStatusTransitionEngine()
            startPeriodicTelemetryEngine()
        }
    }

    private suspend fun seedProfilesIfEmpty() {
        if (profileRepository.getCount() == 0) {
            val seeded = listOf(
                ProfileEntity(
                    id = 1, suffix = "p1", alias = "alex_prime", age = 26, tz = "America/New_York",
                    voice = "casual tech enthusiast, short sentences", activeHoursStart = 9, activeHoursEnd = 23,
                    commentRate = 0.08f, sessionsPerDay = 5, deviceModel = "Pixel 7 Pro",
                    androidVersion = 14, chromeVersion = 124, screenWidth = 1440, screenHeight = 3120,
                    screenDensity = 3.5f, gpu = "Mali-G710", cores = 8, ramGb = 12, tlsId = "HelloChrome_124",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "Pixel 7 Pro",
                    proxyType = "http", proxyHost = "res-us.proxy-hub.io", proxyPort = 8001, proxyOk = true,
                    interestsJson = """{"technology":0.45,"ai_research":0.3,"cybersec":0.25}""",
                    lastUrl = "https://news.ycombinator.com", status = "WATCHING", warmth = 82, phase = "ACTIVE"
                ),
                ProfileEntity(
                    id = 2, suffix = "p2", alias = "sarah_travels", age = 29, tz = "America/Los_Angeles",
                    voice = "enthusiastic traveler, friendly lowercase", activeHoursStart = 7, activeHoursEnd = 22,
                    commentRate = 0.12f, sessionsPerDay = 4, deviceModel = "Galaxy S23",
                    androidVersion = 14, chromeVersion = 123, screenWidth = 1080, screenHeight = 2340,
                    screenDensity = 3.0f, gpu = "Adreno 740", cores = 8, ramGb = 8, tlsId = "HelloChrome_123",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; SM-S911B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "SM-S911B",
                    proxyType = "http", proxyHost = "res-west.brightdata-net.com", proxyPort = 8002, proxyOk = true,
                    interestsJson = """{"travel":0.5,"japan":0.3,"photography":0.2}""",
                    lastUrl = "https://reddit.com/r/travel", status = "BROWSING", warmth = 68, phase = "ACTIVE"
                ),
                ProfileEntity(
                    id = 3, suffix = "p3", alias = "marcus_dev", age = 22, tz = "Europe/London",
                    voice = "analytical, concise, code references", activeHoursStart = 10, activeHoursEnd = 2,
                    commentRate = 0.04f, sessionsPerDay = 6, deviceModel = "Pixel 6a",
                    androidVersion = 14, chromeVersion = 124, screenWidth = 1080, screenHeight = 2400,
                    screenDensity = 2.625f, gpu = "Mali-G78", cores = 8, ramGb = 6, tlsId = "HelloChrome_124",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 6a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "Pixel 6a",
                    proxyType = "http", proxyHost = "res-uk.smartproxy.io", proxyPort = 8003, proxyOk = true,
                    interestsJson = """{"kotlin":0.4,"rust":0.35,"open_source":0.25}""",
                    lastUrl = "https://github.com/trending", status = "TYPING", warmth = 91, phase = "ACTIVE"
                ),
                ProfileEntity(
                    id = 4, suffix = "p4", alias = "elena_vibe", age = 24, tz = "America/Chicago",
                    voice = "artistic, expressive, lowercase", activeHoursStart = 12, activeHoursEnd = 1,
                    commentRate = 0.15f, sessionsPerDay = 3, deviceModel = "OnePlus 11",
                    androidVersion = 13, chromeVersion = 122, screenWidth = 1440, screenHeight = 3216,
                    screenDensity = 3.5f, gpu = "Adreno 740", cores = 8, ramGb = 16, tlsId = "HelloChrome_122",
                    userAgent = "Mozilla/5.0 (Linux; Android 13; CPH2449) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "13.0.0", clientHintsModel = "CPH2449",
                    proxyType = "http", proxyHost = "res-midwest.oxylabs.io", proxyPort = 8004, proxyOk = true,
                    interestsJson = """{"design":0.4,"indie_music":0.4,"architecture":0.2}""",
                    lastUrl = "https://dribbble.com", status = "IDLE", warmth = 44, phase = "WARMUP"
                ),
                ProfileEntity(
                    id = 5, suffix = "p5", alias = "jake_gamer", age = 20, tz = "America/New_York",
                    voice = "gaming slang, fast reactions, hype", activeHoursStart = 15, activeHoursEnd = 4,
                    commentRate = 0.20f, sessionsPerDay = 5, deviceModel = "Xiaomi 13",
                    androidVersion = 14, chromeVersion = 124, screenWidth = 1080, screenHeight = 2400,
                    screenDensity = 3.0f, gpu = "Adreno 740", cores = 8, ramGb = 12, tlsId = "HelloChrome_124",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; 2211133C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "2211133C",
                    proxyType = "http", proxyHost = "res-us.soax.com", proxyPort = 8005, proxyOk = true,
                    interestsJson = """{"valorant":0.5,"twitch":0.3,"hardware":0.2}""",
                    lastUrl = "https://twitch.tv/directory", status = "WATCHING", warmth = 78, phase = "ACTIVE"
                ),
                ProfileEntity(
                    id = 6, suffix = "p6", alias = "chloe_foodie", age = 31, tz = "Europe/Paris",
                    voice = "culinary enthusiast, thoughtful descriptions", activeHoursStart = 8, activeHoursEnd = 22,
                    commentRate = 0.09f, sessionsPerDay = 3, deviceModel = "Pixel 8",
                    androidVersion = 14, chromeVersion = 124, screenWidth = 1080, screenHeight = 2400,
                    screenDensity = 3.0f, gpu = "Mali-G715", cores = 8, ramGb = 8, tlsId = "HelloChrome_124",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "Pixel 8",
                    proxyType = "http", proxyHost = "res-fr.iproyal.com", proxyPort = 8006, proxyOk = true,
                    interestsJson = """{"pastry":0.4,"french_cuisine":0.4,"wine":0.2}""",
                    lastUrl = "https://seriouseats.com", status = "SLEEPING", warmth = 32, phase = "WARMUP"
                ),
                ProfileEntity(
                    id = 7, suffix = "p7", alias = "kev_19", age = 19, tz = "America/Chicago",
                    voice = "lowercase, typos ok, no emojis", activeHoursStart = 16, activeHoursEnd = 1,
                    commentRate = 0.05f, sessionsPerDay = 4, deviceModel = "Pixel 6a",
                    androidVersion = 14, chromeVersion = 124, screenWidth = 1080, screenHeight = 2400,
                    screenDensity = 2.625f, gpu = "Adreno 730", cores = 8, ramGb = 8, tlsId = "HelloChrome_124",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 6a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "Pixel 6a",
                    proxyType = "http", proxyHost = "res-central.stormproxies.com", proxyPort = 8007, proxyOk = true,
                    interestsJson = """{"off_road_trucks":0.4,"gaming":0.3,"music":0.2}""",
                    lastUrl = "https://youtube.com/results?search_query=ford+raptor+baja", status = "WAKING", warmth = 12, phase = "WARMUP"
                ),
                ProfileEntity(
                    id = 8, suffix = "p8", alias = "nova_stream", age = 27, tz = "America/Denver",
                    voice = "science geek, curious questions", activeHoursStart = 8, activeHoursEnd = 23,
                    commentRate = 0.07f, sessionsPerDay = 4, deviceModel = "Galaxy S22",
                    androidVersion = 14, chromeVersion = 123, screenWidth = 1080, screenHeight = 2340,
                    screenDensity = 3.0f, gpu = "Xclipse 920", cores = 8, ramGb = 8, tlsId = "HelloChrome_123",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "SM-S901B",
                    proxyType = "http", proxyHost = "res-co.proxyscrape.com", proxyPort = 8008, proxyOk = true,
                    interestsJson = """{"astronomy":0.5,"james_webb":0.3,"quantum":0.2}""",
                    lastUrl = "https://wikipedia.org/wiki/James_Webb_Space_Telescope", status = "BROWSING", warmth = 55, phase = "ACTIVE"
                )
            )
            profileRepository.saveAll(seeded)
        }
    }

    private fun startThumbnailGenerator() {
        scope.launch {
            while (true) {
                delay(1000L) // 1Hz thumbnail loop
                val liveIds = liveProfileIds.value
                for (id in liveIds) {
                    val profile = profileRepository.getProfileSync(id) ?: continue
                    val jpegBytes = generatePlaceholderJpeg(profile)
                    _thumbs.emit(
                        ThumbFrame(
                            profileId = id,
                            jpeg = jpegBytes,
                            ts = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    private fun generatePlaceholderJpeg(profile: ProfileEntity): ByteArray {
        try {
            val width = 240
            val height = 360
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Generate theme color based on profile ID
            val baseHue = (profile.id * 47f) % 360f
            val bgColor = Color.HSVToColor(floatArrayOf(baseHue, 0.65f, 0.18f))
            canvas.drawColor(bgColor)

            // Draw grid lines for console visual
            val linePaint = Paint().apply {
                color = Color.argb(40, 255, 255, 255)
                strokeWidth = 1f
            }
            for (y in 0 until height step 40) {
                canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), linePaint)
            }
            for (x in 0 until width step 40) {
                canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), linePaint)
            }

            // Header status bar
            val barPaint = Paint().apply {
                color = Color.argb(160, 10, 14, 20)
            }
            canvas.drawRect(Rect(0, 0, width, 44), barPaint)

            // Text paints
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 16f
                isAntiAlias = true
                isFakeBoldText = true
            }

            val subPaint = Paint().apply {
                color = Color.argb(200, 0, 230, 118)
                textSize = 12f
                isAntiAlias = true
            }

            val metaPaint = Paint().apply {
                color = Color.argb(180, 155, 167, 185)
                textSize = 11f
                isAntiAlias = true
            }

            canvas.drawText("@${profile.alias}", 12f, 28f, textPaint)
            canvas.drawText(profile.status, 12f, 75f, subPaint)

            val host = profile.lastUrl?.removePrefix("https://")?.removePrefix("http://")?.substringBefore("/") ?: "idle"
            canvas.drawText("host: $host", 12f, 100f, metaPaint)
            canvas.drawText("warmth: ${profile.warmth}% [${profile.phase}]", 12f, 120f, metaPaint)
            canvas.drawText("device: ${profile.deviceModel}", 12f, 140f, metaPaint)

            // Mock UI frame viewport box in middle
            val boxPaint = Paint().apply {
                color = Color.argb(80, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(Rect(20, 160, width - 20, height - 30), boxPaint)
            canvas.drawText("VIEWPORT: ${profile.status}", 28f, 185f, metaPaint)
            canvas.drawText("FPS: 30 | RES: ${profile.screenWidth}x${profile.screenHeight}", 28f, 205f, metaPaint)

            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
            bitmap.recycle()
            return out.toByteArray()
        } catch (e: Throwable) {
            // Fallback for JVM unit tests without android.graphics.Bitmap implementation
            return "MOCK_JPEG_FOR_P${profile.id}".toByteArray()
        }
    }

    private fun startStatusTransitionEngine() {
        scope.launch {
            while (true) {
                // Randomized log-normal timer between 10s and 30s
                val delayMs = (10000L + Random.nextLong(20000L))
                delay(delayMs)

                val liveIds = liveProfileIds.value.toList()
                if (liveIds.isNotEmpty()) {
                    val pickId = liveIds.random()
                    val profile = profileRepository.getProfileSync(pickId) ?: continue

                    val nextStatus = when (profile.status) {
                        "IDLE" -> "BROWSING"
                        "BROWSING" -> if (Random.nextFloat() < 0.6f) "WATCHING" else "TYPING"
                        "WATCHING" -> if (Random.nextFloat() < 0.5f) "TYPING" else "IDLE"
                        "TYPING" -> "IDLE"
                        "WAKING" -> "IDLE"
                        "SLEEPING" -> "WAKING"
                        else -> "IDLE"
                    }

                    val updated = profile.copy(
                        status = nextStatus,
                        warmth = (profile.warmth + if (nextStatus == "WATCHING" || nextStatus == "TYPING") 1 else 0).coerceAtMost(100)
                    )
                    profileRepository.updateProfile(updated)

                    // Emit corresponding agent event
                    emitStatusEvent(profile.id, profile.alias, nextStatus)
                }
            }
        }
    }

    private fun startPeriodicTelemetryEngine() {
        scope.launch {
            val hosts = listOf("youtube.com", "reddit.com", "github.com", "twitter.com", "wikipedia.org", "news.ycombinator.com")
            while (true) {
                delay(4000L + Random.nextLong(6000L))
                val liveIds = liveProfileIds.value.toList()
                if (liveIds.isNotEmpty()) {
                    val id = liveIds.random()
                    val kind = listOf("llm", "motor", "nav", "mitm", "sys").random()
                    val text = when (kind) {
                        "llm" -> listOf(
                            "Evaluating viewport candidate elements [conf=0.96, target=video_card_3]",
                            "Strategic query refinement: adding category nuance to search string",
                            "Persona voice check: verified natural typo variance σ=0.04",
                            "Dwell timer resolved: waiting 3.8s before engagement trigger"
                        ).random()
                        "motor" -> listOf(
                            "tap(x=482, y=915) hold=84ms pressure=0.91 jitter=2.4px",
                            "swipe(dir=up, dist=420px, bezier_t=410ms, smoothstep=true)",
                            "type_text(chars=14, wpm=52, error_rate=0.03, KeyCharacterMap=active)",
                            "micro_scroll(dy=24px, natural_overshoot=3px)"
                        ).random()
                        "nav" -> listOf(
                            "Navigating to https://${hosts.random()}/watch?v=${UUID.randomUUID().toString().take(11)}",
                            "SPA route transition detected: pushState url_updated",
                            "History stack snapshot preserved: depth=4",
                            "Page render idle reached in 340ms"
                        ).random()
                        "mitm" -> listOf(
                            "JA4 TLS fingerprint verified (match=100%, cipher_order=strict)",
                            "Sec-CH-UA client hints injected for target model",
                            "domain_visited ${hosts.random()} -> TLS ALPN h2 established",
                            "QUIC/UDP 443 outbound leak filter: PASS"
                        ).random()
                        else -> listOf(
                            "Circadian heartbeat: entropy=0.91, simulated_batt=82%",
                            "Worker process heartbeat OK (memory=128MB, cpu=2.1%)",
                            "Storage budget sync: cache=14.2MB / 500MB limit",
                            "Proxy health check: RTT=112ms, exit_ip=194.26.192.*"
                        ).random()
                    }

                    val event = AgentEvent(profileId = id, ts = System.currentTimeMillis(), kind = kind, text = text)
                    _events.emit(event)
                    eventRepository.recordEvent(
                        EventEntity(profileId = id, ts = event.ts, kind = kind, text = text)
                    )
                }
            }
        }
    }

    private suspend fun emitStatusEvent(profileId: Int, alias: String, status: String) {
        val eventText = when (status) {
            "BROWSING" -> "Agent started autonomous exploration loop on active agenda"
            "WATCHING" -> "Agent entered video dwell state: engaging with main content"
            "TYPING" -> "Agent formulating persona-coherent commentary / search query"
            "IDLE" -> "Reflex idle loop active; circadian cooldown running"
            "WAKING" -> "Worker process spawned; restoring suffix storage & session cookies"
            "SLEEPING" -> "Circadian rest phase triggered; worker idling"
            else -> "Status shifted to $status"
        }
        val event = AgentEvent(profileId = profileId, ts = System.currentTimeMillis(), kind = "sys", text = eventText)
        _events.emit(event)
        eventRepository.recordEvent(
            EventEntity(profileId = profileId, ts = event.ts, kind = "sys", text = eventText)
        )
    }

    override suspend fun open(id: Int) {
        liveProfileIds.value = liveProfileIds.value + id
        val profile = profileRepository.getProfileSync(id)
        if (profile != null) {
            profileRepository.updateProfile(profile.copy(status = "IDLE"))
            emitStatusEvent(id, profile.alias, "IDLE")
        }
    }

    override suspend fun close(id: Int, save: Boolean) {
        liveProfileIds.value = liveProfileIds.value - id
        val profile = profileRepository.getProfileSync(id)
        if (profile != null) {
            profileRepository.updateProfile(profile.copy(status = "SLEEPING"))
            emitStatusEvent(id, profile.alias, "SLEEPING")
        }
    }

    override suspend fun maximize(id: Int) {
        val profile = profileRepository.getProfileSync(id)
        val event = AgentEvent(profileId = id, ts = System.currentTimeMillis(), kind = "motor", text = "Maximized profile surface (unmuted audio)")
        _events.emit(event)
        eventRepository.recordEvent(EventEntity(profileId = id, ts = event.ts, kind = "motor", text = event.text))
    }

    override suspend fun minimize(id: Int) {
        val event = AgentEvent(profileId = id, ts = System.currentTimeMillis(), kind = "sys", text = "Minimized profile to background (muted audio)")
        _events.emit(event)
        eventRepository.recordEvent(EventEntity(profileId = id, ts = event.ts, kind = "sys", text = event.text))
    }

    override suspend fun simulateAppSwitch(id: Int, durationMs: Long) {
        val event = AgentEvent(profileId = id, ts = System.currentTimeMillis(), kind = "sys", text = "Simulating app-switch (visibilitychange=hidden for ${durationMs}ms)")
        _events.emit(event)
        eventRepository.recordEvent(EventEntity(profileId = id, ts = event.ts, kind = "sys", text = event.text))
    }

    override suspend fun wakeNow(id: Int) {
        open(id)
        val profile = profileRepository.getProfileSync(id)
        if (profile != null) {
            val updated = profile.copy(status = "IDLE", nextWakeAt = 0L)
            profileRepository.updateProfile(updated)
            val event = AgentEvent(profileId = id, ts = System.currentTimeMillis(), kind = "sys", text = "Manual WakeNow triggered by operator")
            _events.emit(event)
            eventRepository.recordEvent(EventEntity(profileId = id, ts = event.ts, kind = "sys", text = event.text))
        }
    }

    override suspend fun sleepNow(id: Int) {
        val profile = profileRepository.getProfileSync(id)
        if (profile != null) {
            val updated = profile.copy(status = "SLEEPING", nextWakeAt = System.currentTimeMillis() + 7200000L)
            profileRepository.updateProfile(updated)
            liveProfileIds.value = liveProfileIds.value - id
            val event = AgentEvent(profileId = id, ts = System.currentTimeMillis(), kind = "sys", text = "Manual SleepNow triggered by operator")
            _events.emit(event)
            eventRepository.recordEvent(EventEntity(profileId = id, ts = event.ts, kind = "sys", text = event.text))
        }
    }

    override suspend fun snapshot(id: Int): String {
        val profile = profileRepository.getProfileSync(id)
        return """
            {
              "url": "${profile?.lastUrl ?: "https://google.com"}",
              "scrollY": 420,
              "visibility": "visible",
              "video": {"playing": true, "t": 48.2, "dur": 320.0},
              "els": [
                {"i": 0, "tag": "input", "text": "Search", "x": 120, "y": 80},
                {"i": 1, "tag": "button", "text": "Subscribe", "x": 840, "y": 520},
                {"i": 2, "tag": "div", "text": "Top Comment by user9", "x": 60, "y": 920}
              ]
            }
        """.trimIndent()
    }

    override suspend fun action(id: Int, action: ActionJson) {
        val eventKind = when (action) {
            is ActionJson.Tap, is ActionJson.Swipe, is ActionJson.TypeText, is ActionJson.Key, is ActionJson.Volume -> "motor"
            is ActionJson.Navigate, is ActionJson.Back -> "nav"
            else -> "sys"
        }
        val event = AgentEvent(
            profileId = id,
            ts = System.currentTimeMillis(),
            kind = eventKind,
            text = "Executed action: ${action::class.simpleName} ($action)"
        )
        _events.emit(event)
        eventRepository.recordEvent(
            EventEntity(profileId = id, ts = event.ts, kind = eventKind, text = event.text)
        )
    }

    override suspend fun sendInstruction(targets: List<Int>, text: String, runAt: Long?) {
        val taskId = UUID.randomUUID().toString()
        val initialTraces = listOf(
            "[QUEUED] Instruction received: '$text'",
            "[TARGETS] Assigned to profiles: ${targets.joinToString()}",
            "[SCHEDULER] Dispatch scheduled for ${if (runAt != null && runAt > 0) "future timestamp $runAt" else "IMMEDIATE execution"}"
        )

        val taskEntity = TaskEntity(
            id = taskId,
            targetProfilesJson = Json.encodeToString(targets),
            instruction = text,
            status = "queued",
            createdAt = System.currentTimeMillis(),
            runAt = runAt,
            completedAt = null,
            traceLogJson = Json.encodeToString(initialTraces)
        )
        taskRepository.insertTask(taskEntity)

        // Spawn mock execution lifecycle: queued -> running -> done over 10-25s
        scope.launch {
            val traces = initialTraces.toMutableList()

            delay(3000L)
            traces.add("[STRATEGIC] Tactical agenda formulated: 4 micro-action blocks")
            traces.add("[LLM] Decomposing goals: navigate -> locate -> dwell -> engage")
            taskRepository.updateStatusAndTrace(taskId, "running", Json.encodeToString(traces), null)

            for (targetId in targets) {
                _events.emit(AgentEvent(targetId, System.currentTimeMillis(), "llm", "Instruction intake: '$text'"))
            }

            delay(4000L)
            traces.add("[MOTOR] Simulating human typing & search navigation")
            traces.add("[PERCEPTION] Scanning 32 interactive elements in viewport")
            traces.add("[MOTOR] Bezier swipe sequence applied (hold=78ms, σ=2.9px)")
            taskRepository.updateStatusAndTrace(taskId, "running", Json.encodeToString(traces), null)

            delay(5000L)
            traces.add("[MITM] Verified proxy upstream connection (RTT 98ms)")
            traces.add("[PERCEPTION] Target element reached; verified landing page 200 OK")
            traces.add("[MOTOR] Dwell routine active (watching simulation in progress)")
            taskRepository.updateStatusAndTrace(taskId, "running", Json.encodeToString(traces), null)

            delay(4000L)
            traces.add("[VERIFY] Engagement verified; zero detection signals triggered")
            traces.add("[DONE] Task execution complete across all target profiles")
            val completedTime = System.currentTimeMillis()
            taskRepository.updateStatusAndTrace(taskId, "done", Json.encodeToString(traces), completedTime)

            for (targetId in targets) {
                _events.emit(AgentEvent(targetId, System.currentTimeMillis(), "sys", "Task $taskId complete: SUCCESS"))
            }
        }
    }

    override suspend fun runQa(id: Int) {
        scope.launch {
            val profile = profileRepository.getProfileSync(id) ?: return@launch
            val prefix = "[QA PROFILE #$id @${profile.alias}]"

            _events.emit(AgentEvent(id, System.currentTimeMillis(), "sys", "$prefix Starting automated fingerprint QA suite..."))
            delay(800L)
            _events.emit(AgentEvent(id, System.currentTimeMillis(), "mitm", "$prefix JA3/JA4 TLS handshake: PASS (${profile.tlsId})"))
            delay(800L)
            _events.emit(AgentEvent(id, System.currentTimeMillis(), "mitm", "$prefix HTTP/2 SETTINGS frame order: PASS (Chromium 124 exact match)"))
            delay(800L)
            _events.emit(AgentEvent(id, System.currentTimeMillis(), "nav", "$prefix Client Hints consistency (Model=${profile.deviceModel}, Platform=${profile.clientHintsPlatform}): PASS"))
            delay(800L)
            _events.emit(AgentEvent(id, System.currentTimeMillis(), "sys", "$prefix WebRTC leak test: PASS (no STUN/TURN binding leaks)"))
            delay(800L)
            _events.emit(AgentEvent(id, System.currentTimeMillis(), "sys", "$prefix Detection QA Suite Result: 100% HEALTHY"))
        }
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
        liveProfileIds.value = liveProfileIds.value - id
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
