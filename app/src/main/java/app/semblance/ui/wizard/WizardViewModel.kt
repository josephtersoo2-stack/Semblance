package app.semblance.ui.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.semblance.data.local.entity.ProfileEntity
import app.semblance.engine.EngineClient
import app.semblance.engine.mock.MockEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.random.Random

data class WizardFormState(
    // Step 1: Identity
    val alias: String = "sim_user_${Random.nextInt(100, 999)}",
    val age: Int = 25,
    val tz: String = "America/New_York",
    val voice: String = "lowercase, natural phrasing, light slang",

    // Step 2: Device
    val selectedPresetId: String = "pixel_7_pro",
    val deviceModel: String = "Pixel 7 Pro",
    val androidVersion: Int = 14,
    val chromeVersion: Int = 124,
    val screenWidth: Int = 1440,
    val screenHeight: Int = 3120,
    val screenDensity: Float = 3.5f,
    val gpu: String = "Mali-G710",
    val cores: Int = 8,
    val ramGb: Int = 12,
    val tlsId: String = "HelloChrome_124",
    val userAgent: String = "Mozilla/5.0 (Linux; Android 14; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
    val clientHintsPlatform: String = "Android",
    val clientHintsPlatformVersion: String = "14.0.0",
    val clientHintsModel: String = "Pixel 7 Pro",

    // Step 3: Network
    val proxyType: String = "http",
    val proxyHost: String = "res-geo.proxyservice.net",
    val proxyPort: Int = 8080,
    val proxyUser: String = "cust_user492",
    val proxyPass: String = "••••••••",
    val proxySticky: String = "session_30m",
    val proxyTestRunning: Boolean = false,
    val proxyTestResult: String? = null,
    val proxyLatencyMs: Int = 0,

    // Step 4: Rhythm
    val activeHoursStart: Int = 8,
    val activeHoursEnd: Int = 23,
    val sessionsPerDay: Int = 4,
    val commentRate: Float = 0.06f,

    // Step 5: Interests
    val interests: Map<String, Float> = mapOf(
        "technology" to 0.4f,
        "gaming" to 0.3f,
        "science" to 0.2f,
        "music" to 0.1f
    ),

    // Step 6: Validation
    val consistencyReport: ConsistencyReport = ConsistencyReport(emptyList(), true)
)

@HiltViewModel
class WizardViewModel @Inject constructor(
    private val engineClient: EngineClient
) : ViewModel() {

    private val _formState = MutableStateFlow(WizardFormState())
    val formState: StateFlow<WizardFormState> = _formState.asStateFlow()

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    init {
        applyPreset("pixel_7_pro")
    }

    fun setStep(step: Int) {
        _currentStep.value = step.coerceIn(1, 6)
    }

    fun nextStep() {
        if (_currentStep.value < 6) {
            _currentStep.value += 1
        }
    }

    fun prevStep() {
        if (_currentStep.value > 1) {
            _currentStep.value -= 1
        }
    }

    fun updateIdentity(alias: String, age: Int, tz: String, voice: String) {
        _formState.value = _formState.value.copy(
            alias = alias,
            age = age,
            tz = tz,
            voice = voice
        )
    }

    fun generateRandomIdentity() {
        val names = listOf("alex", "jordan", "taylor", "sam", "morgan", "casey", "riley", "cameron", "kai", "quinn")
        val suffix = Random.nextInt(10, 99)
        val aliases = "${names.random()}_$suffix"
        val voices = listOf(
            "lowercase, typos ok, no emojis",
            "concise, technical terms, analytical",
            "friendly, enthusiastic, casual slang",
            "inquisitive, asks good questions, natural tone"
        )
        val timezones = listOf("America/New_York", "America/Chicago", "America/Los_Angeles", "Europe/London", "Europe/Berlin", "Asia/Tokyo")
        val ages = Random.nextInt(19, 45)

        _formState.value = _formState.value.copy(
            alias = aliases,
            age = ages,
            tz = timezones.random(),
            voice = voices.random()
        )
    }

    fun applyPreset(presetId: String) {
        val preset = DeviceLibrary.getById(presetId) ?: return
        _formState.value = _formState.value.copy(
            selectedPresetId = presetId,
            deviceModel = preset.model,
            androidVersion = preset.androidVersion,
            chromeVersion = preset.chromeVersion,
            screenWidth = preset.screenWidth,
            screenHeight = preset.screenHeight,
            screenDensity = preset.screenDensity,
            gpu = preset.gpu,
            cores = preset.cores,
            ramGb = preset.ramGb,
            tlsId = preset.tlsId,
            userAgent = preset.userAgent,
            clientHintsPlatform = preset.clientHintsPlatform,
            clientHintsPlatformVersion = preset.clientHintsPlatformVersion,
            clientHintsModel = preset.clientHintsModel
        )
        revalidate()
    }

    fun updateDeviceField(
        model: String = _formState.value.deviceModel,
        androidVersion: Int = _formState.value.androidVersion,
        chromeVersion: Int = _formState.value.chromeVersion,
        screenWidth: Int = _formState.value.screenWidth,
        screenHeight: Int = _formState.value.screenHeight,
        screenDensity: Float = _formState.value.screenDensity,
        gpu: String = _formState.value.gpu,
        tlsId: String = _formState.value.tlsId,
        userAgent: String = _formState.value.userAgent
    ) {
        _formState.value = _formState.value.copy(
            deviceModel = model,
            androidVersion = androidVersion,
            chromeVersion = chromeVersion,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            screenDensity = screenDensity,
            gpu = gpu,
            tlsId = tlsId,
            userAgent = userAgent
        )
        revalidate()
    }

    private fun revalidate() {
        val s = _formState.value
        val report = ConsistencyValidator.validate(
            deviceModel = s.deviceModel,
            androidVersion = s.androidVersion,
            chromeVersion = s.chromeVersion,
            screenWidth = s.screenWidth,
            screenHeight = s.screenHeight,
            screenDensity = s.screenDensity,
            gpu = s.gpu,
            tlsId = s.tlsId,
            userAgent = s.userAgent,
            clientHintsModel = s.clientHintsModel,
            clientHintsPlatform = s.clientHintsPlatform
        )
        _formState.value = _formState.value.copy(consistencyReport = report)
    }

    fun updateProxy(host: String, port: Int, user: String, pass: String, sticky: String) {
        _formState.value = _formState.value.copy(
            proxyHost = host,
            proxyPort = port,
            proxyUser = user,
            proxyPass = pass,
            proxySticky = sticky,
            proxyTestResult = null
        )
    }

    fun testProxy() {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(proxyTestRunning = true, proxyTestResult = null)
            delay(1500L)
            val latency = Random.nextInt(75, 160)
            val exitIps = listOf("194.26.192.81", "185.122.45.10", "45.132.88.94")
            val asns = listOf("AS15169 Google LLC", "AS7018 AT&T Services", "AS7922 Comcast Cable")
            val result = "PASS (RTT: ${latency}ms | IP: ${exitIps.random()} | ASN: ${asns.random()})"
            _formState.value = _formState.value.copy(
                proxyTestRunning = false,
                proxyTestResult = result,
                proxyLatencyMs = latency
            )
        }
    }

    fun updateRhythm(start: Int, end: Int, sessions: Int, commentRate: Float) {
        _formState.value = _formState.value.copy(
            activeHoursStart = start,
            activeHoursEnd = end,
            sessionsPerDay = sessions,
            commentRate = commentRate
        )
    }

    fun updateInterests(interests: Map<String, Float>) {
        _formState.value = _formState.value.copy(interests = interests)
    }

    fun createProfile(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val s = _formState.value
            val newId = Random.nextInt(100, 9999)
            val entity = ProfileEntity(
                id = newId,
                suffix = "p$newId",
                alias = s.alias,
                age = s.age,
                tz = s.tz,
                voice = s.voice,
                activeHoursStart = s.activeHoursStart,
                activeHoursEnd = s.activeHoursEnd,
                commentRate = s.commentRate,
                sessionsPerDay = s.sessionsPerDay,
                deviceModel = s.deviceModel,
                androidVersion = s.androidVersion,
                chromeVersion = s.chromeVersion,
                screenWidth = s.screenWidth,
                screenHeight = s.screenHeight,
                screenDensity = s.screenDensity,
                gpu = s.gpu,
                cores = s.cores,
                ramGb = s.ramGb,
                tlsId = s.tlsId,
                userAgent = s.userAgent,
                clientHintsPlatform = s.clientHintsPlatform,
                clientHintsPlatformVersion = s.clientHintsPlatformVersion,
                clientHintsModel = s.clientHintsModel,
                proxyType = s.proxyType,
                proxyHost = s.proxyHost,
                proxyPort = s.proxyPort,
                proxyUser = s.proxyUser,
                proxyPass = s.proxyPass,
                proxySticky = s.proxySticky,
                proxyOk = true,
                interestsJson = Json.encodeToString(s.interests),
                phase = "WARMUP",
                status = "SLEEPING",
                warmth = 0
            )

            engineClient.createProfileFromWizard(entity)
            onSuccess()
        }
    }
}
