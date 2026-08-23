package app.semblance.engine.model

data class ThumbFrame(
    val profileId: Int,
    val jpeg: ByteArray,
    val ts: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ThumbFrame
        if (profileId != other.profileId) return false
        if (!jpeg.contentEquals(other.jpeg)) return false
        if (ts != other.ts) return false
        return true
    }

    override fun hashCode(): Int {
        var result = profileId
        result = 31 * result + jpeg.contentHashCode()
        result = 31 * result + ts.hashCode()
        return result
    }
}
