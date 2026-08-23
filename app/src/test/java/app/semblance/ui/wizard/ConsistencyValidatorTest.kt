package app.semblance.ui.wizard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsistencyValidatorTest {

    @Test
    fun testValidPresetPassesAllChecks() {
        val preset = DeviceLibrary.presets.first()
        val report = ConsistencyValidator.validate(
            deviceModel = preset.model,
            androidVersion = preset.androidVersion,
            chromeVersion = preset.chromeVersion,
            screenWidth = preset.screenWidth,
            screenHeight = preset.screenHeight,
            screenDensity = preset.screenDensity,
            gpu = preset.gpu,
            tlsId = preset.tlsId,
            userAgent = preset.userAgent,
            clientHintsModel = preset.clientHintsModel,
            clientHintsPlatform = preset.clientHintsPlatform
        )

        assertTrue("Expected all checks to pass for preset ${preset.name}", report.allValid)
        assertTrue(report.items.all { it.isValid })
    }

    @Test
    fun testAllDeviceLibraryPresetsAreInternallyConsistent() {
        assertTrue("Device library should contain at least 6 presets", DeviceLibrary.presets.size >= 6)

        for (preset in DeviceLibrary.presets) {
            val report = ConsistencyValidator.validate(
                deviceModel = preset.model,
                androidVersion = preset.androidVersion,
                chromeVersion = preset.chromeVersion,
                screenWidth = preset.screenWidth,
                screenHeight = preset.screenHeight,
                screenDensity = preset.screenDensity,
                gpu = preset.gpu,
                tlsId = preset.tlsId,
                userAgent = preset.userAgent,
                clientHintsModel = preset.clientHintsModel,
                clientHintsPlatform = preset.clientHintsPlatform
            )
            assertTrue("Preset '${preset.name}' failed consistency validation", report.allValid)
        }
    }

    @Test
    fun testTlsChromeMismatchFails() {
        val preset = DeviceLibrary.presets.first()
        val report = ConsistencyValidator.validate(
            deviceModel = preset.model,
            androidVersion = preset.androidVersion,
            chromeVersion = 124,
            screenWidth = preset.screenWidth,
            screenHeight = preset.screenHeight,
            screenDensity = preset.screenDensity,
            gpu = preset.gpu,
            tlsId = "HelloChrome_118", // Mismatched TLS ID
            userAgent = preset.userAgent,
            clientHintsModel = preset.clientHintsModel,
            clientHintsPlatform = preset.clientHintsPlatform
        )

        assertFalse("Mismatched TLS ID should fail validation", report.allValid)
        val tlsItem = report.items.find { it.title.contains("TLS") }
        assertNotNull(tlsItem)
        assertFalse(tlsItem!!.isValid)
    }

    @Test
    fun testUserAgentModelMismatchFails() {
        val preset = DeviceLibrary.presets.first()
        val report = ConsistencyValidator.validate(
            deviceModel = "iPhone 14 Pro", // Invariant violation
            androidVersion = preset.androidVersion,
            chromeVersion = preset.chromeVersion,
            screenWidth = preset.screenWidth,
            screenHeight = preset.screenHeight,
            screenDensity = preset.screenDensity,
            gpu = preset.gpu,
            tlsId = preset.tlsId,
            userAgent = preset.userAgent,
            clientHintsModel = preset.clientHintsModel,
            clientHintsPlatform = preset.clientHintsPlatform
        )

        assertFalse("Mismatched device model should fail validation", report.allValid)
    }
}
