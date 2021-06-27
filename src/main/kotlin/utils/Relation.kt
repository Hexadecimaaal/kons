package utils

typealias Relation<T> = Map<T, Lazy<Set<T>>>

fun <T> lazySetOf(vararg x : T) = lazyOf(setOf(*x))

fun <T> Relation<T>.connect(from : T, to : T) : Relation<T> =
  this + Pair(from, lazyOf((this[from]?.value ?: emptySet()) + to))

fun <T> Relation<T>.connectAll(from : T, to : Set<T>) : Relation<T> =
  this + Pair(from, lazyOf((this[from]?.value ?: emptySet()) + to))

fun <T> Relation<T>.union(that : Relation<T>) : Relation<T> = this.toList().fold(that) { result, (v, d) ->
  result.connectAll(v, d.value)
}

fun <T> Relation<T>.converse() : Relation<T> =
  this.map { (v, d) -> d.value.map { Pair(it, v) } }.flatten().fold(mapOf()) { result, (v, d) ->
    result.connect(v, d)
  }

fun <T> Relation<T>.reflexiveClosure() : Relation<T> = this.mapValues { (v, d) -> lazy { d.value + v } }

fun <T> Relation<T>.symmetricClosure() : Relation<T> = this.union(this.converse())

fun <T> Relation<T>.transitiveClosure() : Relation<T> = this.mapValues { (v, _) ->
  fun go(done : Set<T>, todo : Set<T>) : Set<T> = if(todo.isEmpty()) done
  else go(
    done + todo, todo.map { this[it]?.value ?: emptySet() }.reduce(Set<T>::plus) - done - todo
  )
  lazy { go(emptySet(), this[v]?.value ?: emptySet()) }
}


