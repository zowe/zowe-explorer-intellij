/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.utils

import com.google.gson.Gson
import com.intellij.util.containers.minimalElements
import com.intellij.util.containers.toArray
import eu.ibagroup.formainframe.config.ConfigDeclaration
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys
import eu.ibagroup.formainframe.dataops.sort.orderingSortKeys
import eu.ibagroup.formainframe.dataops.sort.typedSortKeys
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeView
import eu.ibagroup.formainframe.explorer.ui.ExplorerUnitTreeNodeBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/**
 * Finds class loader for specified class and tries to load the class.
 * @param className name of the class to load.
 * @return desired class instance, that is controlled by 1 of available class loaders
 *         or null if desired class is not found or something went wrong.
 */
fun loadConfigClass(className: String): Class<*>? {
  val configClassLoaders = ConfigDeclaration.EP.extensionList.map { it.javaClass.classLoader }
  return configClassLoaders.firstNotNullOfOrNull { classLoader ->
    runCatching { classLoader.loadClass(className) }.getOrNull()
  }
}

/** Transform the stream to the mutable list */
fun <E> Stream<E>.toMutableList(): MutableList<E> {
  return this.toList().toMutableList()
}

/** Transform the value to the specified class or return null if the cast is not possible */
inline fun <reified T> Any?.castOrNull(): T? = (this is T).runIfTrue { this as T }

/** Transform the value to the specified class or return null if the cast is not possible */
@Suppress("UNCHECKED_CAST")
fun <T> Any?.castOrNull(clazz: Class<T>): T? =
  if (this != null && clazz.isAssignableFrom(this::class.java)) this as T else null

inline fun <reified T> Any?.`is`(): Boolean = this.`is`(T::class.java)

fun <T> Any?.`is`(clazz: Class<T>): Boolean = this != null && clazz.isAssignableFrom(this::class.java)

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

val <E : Any> E?.optional: Optional<E>
  inline get() = Optional.ofNullable(this)

/**
 * Get index of an element that matches the specified predicate
 * @param predicate the predicate to search an index of the element
 */
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

inline fun <T> Lock?.optionalLock(block: () -> T): T {
  return if (this != null) {
    withLock(block)
  } else {
    block()
  }
}

val gson by lazy { Gson() }

inline fun <reified T : Any> T.clone() = clone(T::class.java)

/**
 * Clone the object deeply
 * @param clazz the class to cast the object to after the clone operation
 */
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

fun <T> Class<*>.isThe(clazz: Class<out T>): Boolean {
  return this == clazz
}

inline fun <reified T> Class<*>.isThe(): Boolean {
  return this.isThe(T::class.java)
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

fun <T> Iterator<T>.stream(): Stream<T> {
  return StreamSupport.stream(Iterable { this }.spliterator(), false)
}

inline fun <reified T> Collection<T>.asArray() = toArray(arrayOf())

fun String.nullIfBlank() = (isNotBlank()).runIfTrue { this }

fun <E : Any> E.asMutableList() = mutableListOf(this)

fun <R> List<R>.mergeWith(another: List<R>): MutableList<R> {
  return this.plus(another).toSet().toMutableList()
}

/**
 * Function clears the input list and adds another list elements to the end of this list
 * @receiver any kind of MutableList
 * @param another
 */
fun <R> List<R>.clearAndMergeWith(another: List<R>) {
  (this as MutableList<R>).apply {
    clear()
    addAll(another)
  }
}

/**
 * Function clears the input list and adds the new sortKey to this list or does nothing if sortKey is null
 * @receiver Any kind of MutableList of the current sortKeys
 * @param toAdd
 */
fun List<SortQueryKeys>.clearOldKeysAndAddNew(toAdd: SortQueryKeys?) {
  if (toAdd != null) {
    if (typedSortKeys.contains(toAdd)) {
      (this as MutableList<SortQueryKeys>).apply {
        removeAll(typedSortKeys.toSet())
        add(toAdd)
      }
    } else {
      (this as MutableList<SortQueryKeys>).apply {
        removeAll(orderingSortKeys.toSet())
        add(toAdd)
      }
    }
  }
}

val UNIT_CLASS = Unit::class.java

inline fun <reified T, reified V> T.applyIfNotNull(v: V?, block: T.(V) -> T): T {
  return run {
    v?.let { block(this, it) } ?: this
  }
}

inline fun <K, V> Iterable<V>.associateListedBy(selector: (V) -> K): Map<K, List<V>> {
  val map = mutableMapOf<K, MutableList<V>>()
  for (v in this) {
    map.computeIfAbsent(selector(v)) { mutableListOf() }.add(v)
  }
  return map
}

fun <T> T.getParentsChain(parentGetter: T.() -> T?): List<T> {
  val chain = mutableListOf<T>()
  var current: T? = this
  while (current != null) {
    chain.add(current)
    current = current.parentGetter()
  }
  return chain
}

fun <T> T.getAncestorNodes(childrenGetter: T.() -> Iterable<T>): List<T> {
  val result = mutableListOf(this)
  val stack = mutableListOf(this)
  while (stack.isNotEmpty()) {
    val current = stack.removeLast()
    val children = current.childrenGetter()
    result.addAll(children)
    stack.addAll(children)
  }
  return result
}

/**
 * Get the list of minimal common parents of the two elements in the list
 * @param parentGetter the parent getter to get the parents chains of each component
 */
fun <T> Iterable<T>.getMinimalCommonParents(parentGetter: T.() -> T?): Collection<T> {
  val parentsCache = mutableMapOf<T, List<T>>()
  val comparisonCache = mutableMapOf<Pair<List<T>, List<T>>, Boolean>()
  return if (this is List<T>) {
    this
  } else {
    toList()
  }.minimalElements { o1, o2 ->
    val firstParents = parentsCache.computeIfAbsent(o1) { o1.getParentsChain(parentGetter) }
    val secondParents = parentsCache.computeIfAbsent(o2) { o2.getParentsChain(parentGetter) }
    val firstContainsSecond = comparisonCache.computeIfAbsent(Pair(firstParents, secondParents)) {
      firstParents.containsAll(secondParents)
    }
    val secondContainsFirst = comparisonCache.computeIfAbsent(Pair(secondParents, firstParents)) {
      secondParents.containsAll(firstParents)
    }
    when {
      firstContainsSecond && !secondContainsFirst -> 1
      secondContainsFirst && !firstContainsSecond -> -1
      else -> 0
    }
  }
}

/**
 * Make a debounce action thread and then run the block of the provided code
 * @param delayInterval the delay interval to run the block after
 * @param block the block of the code to run after the debounce action finished
 */
fun debounce(delayInterval: Long, block: () -> Unit): () -> Unit {
  var t: Thread? = null
  return {
    if (t?.isAlive == true) {
      t?.interrupt()
    }
    t = thread {
      runCatching {
        Thread.sleep(delayInterval)
        block()
      }
    }
  }
}

/**
 * Get all distinct working sets for the selected nodes.
 * In case the items belong to different working sets, it returns all the distinct working sets
 * @param view the view where the nodes are selected
 */
fun <U : WorkingSet<ConnectionConfig, *>> getSelectedNodesWorkingSets(view: ExplorerTreeView<*, *, *>): List<U> {
  return view.mySelectedNodesData
    .map { it.node }
    .filterIsInstance<ExplorerUnitTreeNodeBase<ConnectionConfig, *, U>>()
    .map { it.unit }
    .distinct()
}

/**
 * Replace repeated slashes at the end of line with one
 */
fun String.removeTrailingSlashes(): String {
  return this.replace(Regex("/+$"), "/")
}

/**
 * Utility function which transforms LocalDateTime timestamp to human-readable format (without nanos)
 *
 * @receiver LocalDateTime instance
 * @return String representation of LocalDateTime in human-readable format
 */
fun LocalDateTime.toHumanReadableFormat(): String {
  return "$dayOfMonth ${month.name} ${
    toLocalTime().truncatedTo(ChronoUnit.SECONDS).format(
      DateTimeFormatter.ISO_LOCAL_TIME
    )
  }"
}

const val UNKNOWN_PARAM_VALUE = "<Unknown>"

/**
 * Replace empty or null parameter value with <Unknown>
 */
fun getParamTextValueOrUnknown(param: Any?): String {
  return param?.toString()?.trim().orEmpty().ifEmpty { UNKNOWN_PARAM_VALUE }
}

// TODO: Remove when it becomes possible to mock class constructor with init section.
/** Wrapper for init() method. It is necessary only for test purposes for now. */
fun initialize(init: () -> Unit) {
  init()
}
