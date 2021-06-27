import arrow.core.Either.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail


val P = MTVar("P")
val Q = MTVar("Q")

val PQ = TArrow(P, Q)
val QP = TArrow(Q, P)


class TypeUnificationTest {

  val R = MTVar("R")
  val S = MTVar("S")
  val PQQP = TArrow(PQ, QP)
  val RS = TArrow(R, S)
  val RR = TArrow(R, R)

  @Test fun `panic when given recursive type unification`() {
    val result = runBlocking { PQ.unify(mapOf(), P) }
    assertEquals(Left(RecursiveMetaTypeError(P, PQ)), result)
  }

  @Test fun `more complex recursive unification 1`() {
    when (val result = runBlocking { PQQP.unify(mapOf(), RS) }) {
      is Left -> fail("unification failed with $result")
      is Right -> {
        assertEquals(PQ, result.value.first[R])
        assertEquals(QP, result.value.first[S])
      }
    }

  }

  @Test fun `more complex recursive unification 2`() {
    when(val result = runBlocking { PQQP.unify(mapOf(), RR) }) {
      is Left  -> assert(false)

      is Right -> {
        assertEquals(TArrow(P, P), result.value.first[R])
        assertEquals(TArrow(TArrow(P, P), TArrow(P, P)), result.value.second)
      }
    }
  }
}

class TypeInferenceTest {

  val a = Atom("cat")
  val indexed = Indexed("page", TNum)

  @Test fun `type-check simple tags`() {
    val result = runBlocking { a.check(mapOf(), TTag) }
    assertEquals(Right(Pair(mapOf(), TTag)), result)
  }

  @Test fun `fail when unresolvable`() {
    val result = runBlocking { a.check(mapOf(), PQ) }
    assertEquals(Left(TypeMismatchError(TTag, PQ)), result)
  }

  @Test fun `type-check indexed tags with meta types`() {
    val result = runBlocking { indexed.check(mapOf(), PQ) }
    assertEquals(
      Right(
        Pair(
          mapOf(
            P to TNum,
            Q to TTag
          ),
          TArrow(TNum, TTag)
        )
      ), result
    )
  }
}
