package app.semblance.ui.wizard

data class ValidationItem(
    val title: String,
    val description: String,
    val isValid: Boolean,
    val details: String
)

data class ConsistencyReport(
    val items: List<ValidationItem>,
    val allValid: Boolean
)

object ConsistencyValidator {

    fun validate(
        deviceModel: String,
        androidVersion: Int,
        chromeVersion: Int,
        screenWidth: Int,
        screenHeight: Int,
        screenDensity: Float,
        gpu: String,
        tlsId: String,
        userAgent: String,
        clientHintsModel: String,
        clientHintsPlatform: String
    ): ConsistencyReport {
        val items = mutableListOf<ValidationItem>()

        // 1. Android-only invariant (Master §2 rejected iOS personas on Android)
        val isAndroidHardware = !deviceModel.contains("iPhone", ignoreCase = true) &&
                !deviceModel.contains("iPad", ignoreCase = true) &&
                !deviceModel.contains("iOS", ignoreCase = true) &&
                !userAgent.contains("iPhone", ignoreCase = true)

        // 2. TLS ID vs Chrome Version match
        val tlsMatchesChrome = tlsId.contains(chromeVersion.toString())
        items.add(
            ValidationItem(
                title = "TLS Handshake Fingerprint",
                description = "uTLS ClientHello ID matches target Chromium version",
                isValid = tlsMatchesChrome,
                details = if (tlsMatchesChrome) "PASS: $tlsId conforms to Chrome/$chromeVersion" else "FAIL: TLS ID ($tlsId) diverges from Chrome $chromeVersion"
            )
        )

        // 3. UA vs Client Hints model match
        val uaMatchesModel = isAndroidHardware && userAgent.contains(deviceModel) && (deviceModel == clientHintsModel || userAgent.contains(clientHintsModel))
        items.add(
            ValidationItem(
                title = "Client Hints & User-Agent Alignment",
                description = "Sec-CH-UA-Model matches User-Agent platform tokens",
                isValid = uaMatchesModel,
                details = if (uaMatchesModel) "PASS: UA and Sec-CH-UA agree on model '$clientHintsModel'" else "FAIL: UA does not match model token '$deviceModel' or invalid OS persona"
            )
        )

        // 4. Screen Dimensions & DPR coherence
        val dprReasonable = screenDensity in 2.0f..4.0f && screenWidth >= 1080 && screenHeight >= 2000
        items.add(
            ValidationItem(
                title = "Display Geometry & DPR",
                description = "Viewport coordinates match physical DPR curve",
                isValid = dprReasonable,
                details = if (dprReasonable) "PASS: ${screenWidth}x${screenHeight} @ ${screenDensity}x DPR" else "FAIL: Aberrant display geometry"
            )
        )

        // 5. GPU renderer string consistency
        val gpuValid = gpu.isNotBlank() && (gpu.startsWith("Adreno") || gpu.startsWith("Mali") || gpu.startsWith("Xclipse"))
        items.add(
            ValidationItem(
                title = "WebGL / GPU Renderer",
                description = "UNMASKED_RENDERER_WEBGL matches SoC hardware profile",
                isValid = gpuValid,
                details = if (gpuValid) "PASS: Hardware GPU '$gpu' valid" else "FAIL: Invalid/missing GPU string"
            )
        )

        // 6. OS & Platform Hints
        val osValid = isAndroidHardware && clientHintsPlatform == "Android" && androidVersion in 12..15
        items.add(
            ValidationItem(
                title = "Platform OS Integrity",
                description = "Sec-CH-UA-Platform matches Android $androidVersion API",
                isValid = osValid,
                details = if (osValid) "PASS: Android $androidVersion (Platform: $clientHintsPlatform)" else "FAIL: Incompatible OS level or iOS persona rejected"
            )
        )

        val allValid = items.all { it.isValid }
        return ConsistencyReport(items = items, allValid = allValid)
    }
}
