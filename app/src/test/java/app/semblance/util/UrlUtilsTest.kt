package app.semblance.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun `normalizeUrl prepends https to scheme-less domain`() {
        val result = UrlUtils.normalizeUrl("example.com")
        assertEquals("https://example.com", result)
    }

    @Test
    fun `normalizeUrl trims leading and trailing whitespace`() {
        val result = UrlUtils.normalizeUrl("   wikipedia.org/wiki/Android   ")
        assertEquals("https://wikipedia.org/wiki/Android", result)
    }

    @Test
    fun `normalizeUrl preserves existing https scheme`() {
        val result = UrlUtils.normalizeUrl("https://news.ycombinator.com/item?id=123")
        assertEquals("https://news.ycombinator.com/item?id=123", result)
    }

    @Test
    fun `normalizeUrl preserves existing http scheme`() {
        val result = UrlUtils.normalizeUrl("http://localhost:8080/dashboard")
        assertEquals("http://localhost:8080/dashboard", result)
    }

    @Test
    fun `normalizeUrl returns null for blank or empty input`() {
        assertNull(UrlUtils.normalizeUrl(""))
        assertNull(UrlUtils.normalizeUrl("   "))
    }

    @Test
    fun `normalizeUrl rejects non-network schemes`() {
        assertNull(UrlUtils.normalizeUrl("ftp://ftp.example.com/file.zip"))
        assertNull(UrlUtils.normalizeUrl("javascript:void(0)"))
        assertNull(UrlUtils.normalizeUrl("file:///data/local/tmp"))
    }
}
