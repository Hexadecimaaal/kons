import org.junit.Test
import org.junit.Assert.*
import utils.lazySetOf
import utils.transitiveClosure

class RelationTest {

  @Test fun `test transitiveClosure`() {
    mapOf(
      1 to lazySetOf(2), 2 to lazySetOf(3), 3 to lazySetOf(4, 5, 6), 7 to lazySetOf(8)
    ).transitiveClosure().let {
      assertEquals(it[1]?.value, setOf(2, 3, 4, 5, 6))
      assertEquals(it[2]?.value, setOf(3, 4, 5, 6))
      assertEquals(it[3]?.value, setOf(4, 5, 6))
      assertEquals(it[7]?.value, setOf(8))
    }

    mapOf(
      1 to lazySetOf(2), 2 to lazySetOf(3), 3 to lazySetOf(1), 4 to lazySetOf(1), 5 to lazySetOf(4)
    ).transitiveClosure().let {
      assertEquals(it[1]?.value, setOf(1, 2, 3))
      assertEquals(it[4]?.value, setOf(1, 2, 3))
      assertEquals(it[5]?.value, setOf(1, 2, 3, 4))
    }
  }

}