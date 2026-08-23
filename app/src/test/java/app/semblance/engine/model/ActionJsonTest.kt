package app.semblance.engine.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionJsonTest {

    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    @Test
    fun testTapActionSerialization() {
        val tap = ActionJson.Tap(x = 512f, y = 830f, el = 3, intent = "subscribe_btn")
        val serialized = json.encodeToString<ActionJson>(tap)
        assertTrue(serialized.contains(""""type":"tap""""))
        val deserialized = json.decodeFromString<ActionJson>(serialized)
        assertEquals(tap, deserialized)
    }

    @Test
    fun testSwipeActionSerialization() {
        val swipe = ActionJson.Swipe(dir = "up", dist = 450f, curve = "bezier")
        val serialized = json.encodeToString<ActionJson>(swipe)
        assertTrue(serialized.contains(""""type":"swipe""""))
        val deserialized = json.decodeFromString<ActionJson>(serialized)
        assertEquals(swipe, deserialized)
    }

    @Test
    fun testTypeTextActionSerialization() {
        val typeText = ActionJson.TypeText(el = 1, text = "custom query text")
        val serialized = json.encodeToString<ActionJson>(typeText)
        assertTrue(serialized.contains(""""type":"type_text""""))
        val deserialized = json.decodeFromString<ActionJson>(serialized)
        assertEquals(typeText, deserialized)
    }

    @Test
    fun testNavigateActionSerialization() {
        val nav = ActionJson.Navigate(url = "https://youtube.com/watch?v=sample")
        val serialized = json.encodeToString<ActionJson>(nav)
        assertTrue(serialized.contains(""""type":"navigate""""))
        val deserialized = json.decodeFromString<ActionJson>(serialized)
        assertEquals(nav, deserialized)
    }

    @Test
    fun testBackAndVolumeActions() {
        val back = ActionJson.Back
        val backSerialized = json.encodeToString<ActionJson>(back)
        val backDeserialized = json.decodeFromString<ActionJson>(backSerialized)
        assertEquals(back, backDeserialized)

        val volume = ActionJson.Volume(dir = "mute")
        val volumeSerialized = json.encodeToString<ActionJson>(volume)
        val volumeDeserialized = json.decodeFromString<ActionJson>(volumeSerialized)
        assertEquals(volume, volumeDeserialized)
    }
}
