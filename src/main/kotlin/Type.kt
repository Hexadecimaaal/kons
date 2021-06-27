import arrow.core.Either
import arrow.core.Either.*
import arrow.core.computations.either

sealed interface Type {

  suspend fun unify(context : Context, type : Type) : Either<TypeError, Pair<Context, Type>>

  fun freeOf(type : MTVar) : Boolean

  fun subst(variable : MTVar, type : Type) : Type

  fun subst(context : Context) : Type =
    // if the context contains a record points a `MTVar` to itself, this means it's allocated
    // (thus not `fresh()` will return it), but don't have a resolution yet.
    if(context.keys.all { this.freeOf(it) || it == context[it] }) this
    else (context.entries.fold(this) { acc, (v, t) ->
      if(v != t) acc.subst(v, t) else acc
    }.subst(context))
}

sealed interface TypeError
data class TypeMismatchError(val expected : Type, val actual : Type) : TypeError
data class RecursiveMetaTypeError(val variable : MTVar, val appearsIn : Type) : TypeError

typealias Context = Map<MTVar, Type>

fun Context.subst() : Context = this.mapValues { (_, v) -> v.subst(this) }

typealias CheckingContext = Either<TypeError, Pair<Context, Type>>

object TTag : Type {

  override suspend fun unify(context : Context, type : Type) = when(type) {
    is TTag  -> Right(Pair(context, TTag))
    is MTVar -> type.unify(context, this)
    else     -> Left(TypeMismatchError(type, this))
  }

  override fun freeOf(type : MTVar) = true
  override fun subst(variable : MTVar, type : Type) : Type = this

}

object TNum : Type {

  override suspend fun unify(context : Context, type : Type) = when(type) {
    is TNum  -> Right(Pair(context, TNum))
    is MTVar -> type.unify(context, this)
    else     -> Left(TypeMismatchError(type, this))
  }

  override fun freeOf(type : MTVar) = true
  override fun subst(variable : MTVar, type : Type) : Type = this
}

data class TEither(val left : Type, val right : Type) : Type {

  override suspend fun unify(context : Context, type : Type) = when(type) {
    is TArrow -> either<TypeError, Pair<Context, Type>> {
      val (ctx1, tLeft) = type.from.unify(context, left).bind()
      val (ctx2, tRight) = type.to.unify(ctx1, right).bind()
      Pair(ctx2, TArrow(tLeft.subst(ctx2), tRight.subst(ctx2)))
    }
    is MTVar  -> type.unify(context, this)
    else      -> Left(TypeMismatchError(type, this))
  }

  override fun subst(variable : MTVar, type : Type) = TEither(left.subst(variable, type), right.subst(variable, type))

  override fun freeOf(type : MTVar) = left.freeOf(type) && right.freeOf(type)

  override fun toString() = "($left + $right)"
}

data class TPair(val fst : Type, val snd : Type) : Type {

  override suspend fun unify(context : Context, type : Type) = when(type) {
    is TArrow -> either<TypeError, Pair<Context, Type>> {
      val (ctx1, tFst) = type.from.unify(context, fst).bind()
      val (ctx2, tSnd) = type.to.unify(ctx1, snd).bind()
      Pair(ctx2, TArrow(tFst.subst(ctx2), tSnd.subst(ctx2)))
    }
    is MTVar  -> type.unify(context, this)
    else      -> Left(TypeMismatchError(type, this))
  }

  override fun subst(variable : MTVar, type : Type) = TPair(fst.subst(variable, type), snd.subst(variable, type))

  override fun freeOf(type : MTVar) = fst.freeOf(type) && snd.freeOf(type)

  override fun toString() = "($fst * $snd)"
}

data class TArrow(val from : Type, val to : Type) : Type {

  override fun toString() = "($from -> $to)"

  override suspend fun unify(context : Context, type : Type) = when(type) {
    is TArrow -> either<TypeError, Pair<Context, Type>> {
      val (ctx1, tFrom) = type.from.unify(context, from).bind()
      val (ctx2, tTo) = type.to.unify(ctx1, to).bind()
      Pair(ctx2, TArrow(tFrom.subst(ctx2), tTo.subst(ctx2)))
    }
    is MTVar  -> type.unify(context, this)
    else      -> Left(TypeMismatchError(type, this))
  }

  override fun freeOf(type : MTVar) = from.freeOf(type) && to.freeOf(type)
  override fun subst(variable : MTVar, type : Type) : Type =
    TArrow(from.subst(variable, type), to.subst(variable, type))
}


data class MTVar(val name : String) : Type {

  override fun toString() = "$$name"

  override suspend fun unify(context : Context, type : Type) : CheckingContext {
    // strict eval will cause SO; this should only be evaluated
    // if this is not `solved`. I think the `lazy` delegate
    // is pretty neat but maybe this could be optimised more.
    val newContext by lazy { (context + (this to type.subst(context))).subst() }

    val solved = context.containsKey(this) && context[this] != this
    return if(solved)
      context[this]!!.unify(context, type) // i don't know why kotlin can't infer non-null on this
    else when(type) {
      is MTVar ->
        // `is this` also falls into this branch;
        // if we see a new unsolved `T ~ T` appear we should
        // also write it down (to prevent `fresh()` from returning it).
        //
        // this could not be a infinite type: mutual recursion is
        // transformed into direct recursion in the if clause above;
        // and the new resolution created in the context can only be
        // a `T := (MTVar)` that is either `T := U` or `T := T`.
        if(solved) this.unify(context, context[type]!!)
        else Right(Pair(newContext, type.subst(newContext)))
      else     ->
        if(!type.freeOf(this)) Left(RecursiveMetaTypeError(this, type))
        else Right(Pair(newContext, type.subst(newContext)))
    }
  }

  override fun freeOf(type : MTVar) = this != type
  override fun subst(variable : MTVar, type : Type) : Type = if(this == variable) type else this
}

fun Context.fresh() : Pair<Context, MTVar> {
  var i = 0;
  while(containsKey(MTVar("T$i"))) i++
  val v = MTVar("T$i")
  return Pair(this + (v to v), v)
}
