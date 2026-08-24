package app.semblance.util

object UrlUtils {
    /**
     * Normalizes a raw URL string by trimming whitespace and ensuring a valid scheme (defaults to https://).
     * Returns null if the URL is blank or not a valid network URL (http:// or https://).
     */
    fun normalizeUrl(raw: String): String? {
        val t = raw.trim()
        if (t.isEmpty()) return null

        val colonIdx = t.indexOf(':')
        if (colonIdx != -1) {
            val scheme = t.substring(0, colonIdx).lowercase()
            if (scheme != "http" && scheme != "https") {
                return null
            }
            val afterScheme = t.substring(colonIdx + 1)
            if (!afterScheme.startsWith("//") || afterScheme.length <= 2) {
                return null
            }
            return t
        }

        return "https://$t"
    }
}
