import arrow.core.Either
import arrow.core.computations.either

sealed interface Term {

  suspend fun check(context : Context, type : Type) : CheckingContext
  suspend fun infer(context : Context) : CheckingContext {
    val (ctx1, t) = context.fresh()
    return this.check(ctx1, t)
  }
}


// a tag with any wild name you want.
data class Atom(val name : String) : Term {

  //  f is an Atom (Tag)
  // --------------------
  //      f : TTag
  override suspend fun check(context : Context, type : Type) =
    type.unify(context, TTag)
}

// the indexed tag type is also created by the user and
// the parameter type is given at it's creation and
// monomorphic after; it's a opaque function anyways.
data class Indexed(val name : String, val paramType : Type) : Term {

  //  f is an Indexed with paramType A
  // ----------------------------------
  //           f : A -> TTag
  override suspend fun check(context : Context, type : Type) =
    type.unify(context, TArrow(paramType, TTag))
}

data class Num(val n : Int) : Term {

  // ------------------------------
  //  {.., -1, 0, 1, 2, ..} : TNum
  override suspend fun check(context : Context, type : Type) =
    type.unify(context, TNum)
}

data class App(val lhs : Term, val rhs : Term) : Term {

  //  lhs : A -> B     rhs: A
  // -------------------------
  //      (lhs rhs) : B
  override suspend fun check(context : Context, type : Type) = either<TypeError, Pair<Context, Type>> {
    val (ctx1, tParam) = rhs.infer(context).bind()
    lhs.check(ctx1, TArrow(tParam, type)).bind()
  }
}

object Cons : Term {

  // --------------------------
  //  cons : A -> B -> (A * B)
  override suspend fun check(context : Context, type : Type) : CheckingContext {
    val (ctx1, tFst) = context.fresh()
    val (ctx2, tSnd) = ctx1.fresh()
    return type.unify(ctx2, TArrow(tFst, TArrow(tSnd, TPair(tFst, tSnd))))
  }

}

object Inl : Term {

  // --------------------
  //  inl : A -> (A + B)
  override suspend fun check(context : Context, type : Type) : CheckingContext {
    val (ctx1, tLeft) = context.fresh()
    val (ctx2, tRight) = ctx1.fresh()
    return type.unify(ctx2, TArrow(tLeft, TEither(tLeft, tRight)))
  }
}

object Inr : Term {

  // --------------------
  //  inr : B -> (A + B)
  override suspend fun check(context : Context, type : Type) : CheckingContext {
    val (ctx1, tLeft) = context.fresh()
    val (ctx2, tRight) = ctx1.fresh()
    return type.unify(ctx2, TArrow(tRight, TEither(tLeft, tRight)))
  }
}

// these are mostly used for exact identity comparison (or substructure search) only;
// I haven't add any elimination rules because I don't want
// to have any operation semantics and normalizing going on (yet).
