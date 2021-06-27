import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class StorageTest {

  val jsonFile = File(this::class.java.getResource("test.json")?.file!!)

  @Test fun `test json parse`() {
    val attrs = parseAttrs(jsonFile)
    assertEquals(attrs.comment, "I fapped to this")
    assertEquals(attrs.tags, listOf("cat", "cute"))
  }
}