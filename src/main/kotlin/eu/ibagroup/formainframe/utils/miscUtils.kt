package eu.ibagroup.formainframe.utils

import com.google.gson.Gson
import com.intellij.util.containers.toArray
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.concurrent.withLock
import kotlin.streams.toList

fun <E> Stream<E>.toMutableList(): MutableList<E> {
  return this.toList().toMutableList()
}

inline fun <reified T> Any?.castOrNull(): T? = (this is T).runIfTrue { this as T }

val <E> Optional<out E>.nullable: E?
  inline get() = this.orElse(null)

inline fun <T, R> Optional<out T>.runIfPresent(block: (T) -> R): R? {
  return this.isPresent.runIfTrue {
    block(this.get())
  }
}

@JvmName("runIfPresent1")
inline fun <T, R> runIfPresent(optional: Optional<out T>, block: (T) -> R): R? {
  return optional.runIfPresent(block)
}

val <E> E?.optional: Optional<E>
  inline get() = Optional.ofNullable(this)

inline fun <E : Any?> List<E>.indexOf(predicate: (E) -> Boolean): Int? {
  for (i in this.indices) {
    if (predicate(this[i])) {
      return i
    }
  }
  return null
}

inline fun <T> lock(vararg locks: Lock?, block: () -> T): T {
  locks.forEach { it?.lock() }
  return try {
    block()
  } finally {
    locks.forEach { it?.unlock() }
  }
}

inline fun <T> ReadWriteLock.read(block: () -> T): T {
  return readLock().withLock(block)
}

inline fun <T> ReadWriteLock.write(block: () -> T): T {
  return writeLock().withLock(block)
}

inline fun <T> Lock?.optionalLock(block: () -> T) : T {
  return if (this != null) {
    withLock(block)
  } else {
    block()
  }
}

val gson by lazy { Gson() }

inline fun <reified T : Any> T.clone() = clone(T::class.java)

fun <T : Any> T.clone(clazz: Class<out T>): T {
  return with(gson) {
    fromJson(toJson(this@clone), clazz)
  }
}

inline fun <T> Boolean?.runIfTrue(block: () -> T): T? {
  return if (this == true) {
    block()
  } else null
}

@JvmName("runIfTrue1")
inline fun <T> runIfTrue(aBoolean: Boolean, block: () -> T): T? {
  return aBoolean.runIfTrue(block)
}

fun <T> Collection<T>?.streamOrEmpty(): Stream<T> {
  return this?.stream() ?: Stream.empty()
}

inline fun <reified T> Class<*>.isThe(): Boolean {
  return this == T::class.java
}

inline fun <reified T> Stream<T>.findAnyNullable(): T? {
  return this.findAny().nullable
}

fun <T> Stream<T>.filterNotNull(): Stream<T> {
  return filter(Objects::nonNull)
}

fun <T, R> Stream<T>.mapNotNull(mapper: (T) -> R): Stream<R> {
  return map(mapper).filterNotNull()
}

infix fun <T> Collection<T>.isTheSameAs(other: Collection<T>): Boolean {
  return this.size == other.size && (this.isEmpty() || this.containsAll(other))
}

infix fun <T> Collection<T>.isNotTheSameAs(other: Collection<T>): Boolean {
  return !(this isTheSameAs other)
}

fun <T> Collection<T>.withoutElementsOf(other: Collection<T>): Collection<T> {
  return this.filter { thisElement ->
    other.find { otherElement -> otherElement == thisElement } == null
  }
}

fun <T> Iterator<T>.stream(): Stream<T> {
  return StreamSupport.stream(Iterable { this }.spliterator(), false)
}

inline fun <reified T> Collection<T>.asArray() = toArray(arrayOf())

fun String.nullIfBlank() = (isNotBlank()).runIfTrue { this }

fun <E : Any> E.asMutableList() = mutableListOf(this)

fun <R> List<R>.mergeWith(another: List<R>): MutableList<R> {
  return this.plus(another).toSet().toMutableList()
}

val UNIT_CLASS = Unit::class.java

inline fun <reified T, reified V> T.applyIfNotNull(v: V?, block: T.(V) -> T): T {
  return run {
    v?.let { block(this, it) } ?: this
  }
}