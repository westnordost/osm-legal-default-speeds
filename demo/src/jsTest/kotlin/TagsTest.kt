import kotlin.test.*

class TagsTest {

    @Test fun parseInvalid() {
        val tags = mapOf<String, String>()
        assertEquals(tags, "a".toTags())
        assertEquals(tags, "a=".toTags())
        assertEquals(tags, "a =  ".toTags())
        assertEquals(tags, "=b".toTags())
        assertEquals(tags, " =   b".toTags())
        assertEquals(tags, "=".toTags())
        assertEquals(tags, "  =   ".toTags())
    }

    @Test fun parseTag() {
        val tags = mapOf("a" to "b")
        assertEquals(tags, "a=b".toTags())
        assertEquals(tags, "a =b".toTags())
        assertEquals(tags, "a= b".toTags())
        assertEquals(tags, "a = b".toTags())
        assertEquals(tags, "  a = b  ".toTags())
    }

    @Test fun handleMultipleEquals() {
        assertEquals(mapOf("a" to "=b"), "a==b".toTags())
        assertEquals(mapOf("a" to "= b"), "a == b".toTags())
    }

    @Test fun parseTags() {
        val tags = mapOf("a" to "b", "c" to "d")
        assertEquals(tags, "a=b\nc=d".toTags())
        assertEquals(tags, "a=b\r\nc=d".toTags())
        assertEquals(tags, "a=b\rc=d".toTags())
        assertEquals(tags, "a=b\r\r\n\n\rc=d".toTags())

        assertEquals(tags, "a=b\ninvalid\n=\nc=d".toTags())
    }

    @Test fun writeTags() {
        val tags = mapOf("a" to "b", "c" to "d")
        assertEquals("a=b\nc=d", tags.toTagString())
    }
}