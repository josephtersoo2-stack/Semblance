package app.semblance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: Int,
    val suffix: String,
    // Persona
    val alias: String,
    val age: Int,
    val tz: String,
    val voice: String,
    val activeHoursStart: Int = 8,
    val activeHoursEnd: Int = 23,
    val commentRate: Float = 0.05f,
    val sessionsPerDay: Int = 4,
    // Device
    val deviceModel: String,
    val androidVersion: Int = 14,
    val chromeVersion: Int = 124,
    val screenWidth: Int = 1080,
    val screenHeight: Int = 2400,
    val screenDensity: Float = 2.625f,
    val gpu: String = "Adreno 730",
    val cores: Int = 8,
    val ramGb: Int = 8,
    val tlsId: String = "HelloChrome_124",
    val userAgent: String = "",
    val clientHintsPlatform: String = "Android",
    val clientHintsPlatformVersion: String = "14.0.0",
    val clientHintsModel: String = "Pixel 6a",
    // Proxy
    val proxyType: String = "http",
    val proxyHost: String = "",
    val proxyPort: Int = 0,
    val proxyUser: String = "",
    val proxyPass: String = "",
    val proxySticky: String = "",
    val proxyOk: Boolean = true,
    // Interests (JSON string representation, e.g. {"gaming":0.4,"music":0.3})
    val interestsJson: String = "{}",
    // State
    val lastUrl: String? = null,
    val backstackBlob: ByteArray? = null,
    val agendaCursor: Int = 0,
    val nextWakeAt: Long = 0L,
    val phase: String = "WARMUP", // "WARMUP" | "ACTIVE" | "IDLE"
    val status: String = "SLEEPING", // "SLEEPING" | "WAKING" | "IDLE" | "BROWSING" | "WATCHING" | "TYPING" | "ERROR"
    // Stats
    val sessionsCount: Int = 0,
    val commentsCount: Int = 0,
    val watchMinutes: Int = 0,
    val warmth: Int = 0 // 0–100
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProfileEntity

        if (id != other.id) return false
        if (suffix != other.suffix) return false
        if (alias != other.alias) return false
        if (age != other.age) return false
        if (tz != other.tz) return false
        if (voice != other.voice) return false
        if (activeHoursStart != other.activeHoursStart) return false
        if (activeHoursEnd != other.activeHoursEnd) return false
        if (commentRate != other.commentRate) return false
        if (sessionsPerDay != other.sessionsPerDay) return false
        if (deviceModel != other.deviceModel) return false
        if (androidVersion != other.androidVersion) return false
        if (chromeVersion != other.chromeVersion) return false
        if (screenWidth != other.screenWidth) return false
        if (screenHeight != other.screenHeight) return false
        if (screenDensity != other.screenDensity) return false
        if (gpu != other.gpu) return false
        if (cores != other.cores) return false
        if (ramGb != other.ramGb) return false
        if (tlsId != other.tlsId) return false
        if (userAgent != other.userAgent) return false
        if (clientHintsPlatform != other.clientHintsPlatform) return false
        if (clientHintsPlatformVersion != other.clientHintsPlatformVersion) return false
        if (clientHintsModel != other.clientHintsModel) return false
        if (proxyType != other.proxyType) return false
        if (proxyHost != other.proxyHost) return false
        if (proxyPort != other.proxyPort) return false
        if (proxyUser != other.proxyUser) return false
        if (proxyPass != other.proxyPass) return false
        if (proxySticky != other.proxySticky) return false
        if (proxyOk != other.proxyOk) return false
        if (interestsJson != other.interestsJson) return false
        if (lastUrl != other.lastUrl) return false
        if (backstackBlob != null) {
            if (other.backstackBlob == null) return false
            if (!backstackBlob.contentEquals(other.backstackBlob)) return false
        } else if (other.backstackBlob != null) return false
        if (agendaCursor != other.agendaCursor) return false
        if (nextWakeAt != other.nextWakeAt) return false
        if (phase != other.phase) return false
        if (status != other.status) return false
        if (sessionsCount != other.sessionsCount) return false
        if (commentsCount != other.commentsCount) return false
        if (watchMinutes != other.watchMinutes) return false
        if (warmth != other.warmth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + suffix.hashCode()
        result = 31 * result + alias.hashCode()
        result = 31 * result + age
        result = 31 * result + tz.hashCode()
        result = 31 * result + voice.hashCode()
        result = 31 * result + activeHoursStart
        result = 31 * result + activeHoursEnd
        result = 31 * result + commentRate.hashCode()
        result = 31 * result + sessionsPerDay
        result = 31 * result + deviceModel.hashCode()
        result = 31 * result + androidVersion
        result = 31 * result + chromeVersion
        result = 31 * result + screenWidth
        result = 31 * result + screenHeight
        result = 31 * result + screenDensity.hashCode()
        result = 31 * result + gpu.hashCode()
        result = 31 * result + cores
        result = 31 * result + ramGb
        result = 31 * result + tlsId.hashCode()
        result = 31 * result + userAgent.hashCode()
        result = 31 * result + clientHintsPlatform.hashCode()
        result = 31 * result + clientHintsPlatformVersion.hashCode()
        result = 31 * result + clientHintsModel.hashCode()
        result = 31 * result + proxyType.hashCode()
        result = 31 * result + proxyHost.hashCode()
        result = 31 * result + proxyPort
        result = 31 * result + proxyUser.hashCode()
        result = 31 * result + proxyPass.hashCode()
        result = 31 * result + proxySticky.hashCode()
        result = 31 * result + proxyOk.hashCode()
        result = 31 * result + interestsJson.hashCode()
        result = 31 * result + (lastUrl?.hashCode() ?: 0)
        result = 31 * result + (backstackBlob?.contentHashCode() ?: 0)
        result = 31 * result + agendaCursor
        result = 31 * result + nextWakeAt.hashCode()
        result = 31 * result + phase.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + sessionsCount
        result = 31 * result + commentsCount
        result = 31 * result + watchMinutes
        result = 31 * result + warmth
        return result
    }
}
