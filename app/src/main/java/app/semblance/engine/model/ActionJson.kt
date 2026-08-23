package app.semblance.engine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ActionJson {
    @Serializable
    @SerialName("tap")
    data class Tap(
        val x: Float? = null,
        val y: Float? = null,
        val el: Int? = null,
        val intent: String? = null
    ) : ActionJson()

    @Serializable
    @SerialName("swipe")
    data class Swipe(
        val dir: String, // "up" | "down" | "left" | "right"
        val dist: Float = 300f,
        val curve: String = "bezier"
    ) : ActionJson()

    @Serializable
    @SerialName("type_text")
    data class TypeText(
        val el: Int? = null,
        val text: String
    ) : ActionJson()

    @Serializable
    @SerialName("key")
    data class Key(
        val code: Int
    ) : ActionJson()

    @Serializable
    @SerialName("wait")
    data class Wait(
        val s: Float
    ) : ActionJson()

    @Serializable
    @SerialName("navigate")
    data class Navigate(
        val url: String
    ) : ActionJson()

    @Serializable
    @SerialName("back")
    data object Back : ActionJson()

    @Serializable
    @SerialName("volume")
    data class Volume(
        val dir: String // "up" | "down" | "mute"
    ) : ActionJson()

    @Serializable
    @SerialName("maximize")
    data object Maximize : ActionJson()

    @Serializable
    @SerialName("minimize")
    data object Minimize : ActionJson()
}
