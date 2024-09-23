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

package eu.ibagroup.formainframe.utils.crudable

import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.ActionType
import eu.ibagroup.formainframe.analytics.events.ConnectionEvent
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.utils.crudable.Utils.castToColumnOrNull
import eu.ibagroup.formainframe.utils.nullable
import java.lang.reflect.Field
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

inline fun <reified E : Any> Crudable.find(noinline predicate: (E) -> Boolean): Stream<E> {
  return this.find(E::class.java, predicate)
}

inline fun <reified E : Any> Crudable.getAll(): Stream<E> {
  return this.getAll(E::class.java)
}

inline fun <reified E : Any> Crudable.getByColumnLambda(row: E, crossinline lambdaGetter: (E) -> Any): Stream<E> {
  return this.find { lambdaGetter(it) == lambdaGetter(row) }
}

inline fun <reified E : Any> Crudable.getByUniqueKey(uniqueKey: Any): E? {
  return this.getByUniqueKey(E::class.java, uniqueKey).nullable
}

inline fun <reified E : Any, reified V : Any> Crudable.nextUniqueValue(): V {
  return this.nextUniqueValue(E::class.java)
}

interface AddFilter {
  operator fun <T : Any> invoke(clazz: Class<out T>, addingRow: T): Boolean = true
}

interface UpdateFilter {
  operator fun <T : Any> invoke(clazz: Class<out T>, currentRow: T, updatingRow: T): Boolean = true
}

/**
 * Interface that describes the basic interaction with CRUDable object
 */
interface Crudable {
  fun <E : Any> add(rowClass: Class<out E>, row: E): Optional<E>
  fun <E : Any> add(row: E): Optional<E>? {
    return add(row.javaClass, row)
  }

  /**
   * Get all rows of the row class from crudable
   * @param rowClass the row class to get all rows by
   */
  fun <E : Any> getAll(rowClass: Class<out E?>): Stream<E>
  fun <E : Any> update(rowClass: Class<out E>, row: E): Optional<E>
  fun <E : Any> update(row: E): Optional<E>? {
    return update(row.javaClass, row)
  }

  fun <E : Any> addOrUpdate(rowClass: Class<out E>, row: E): Optional<E> {
    var optional = add(rowClass, row)
    if (optional.isEmpty) {
      optional = update(rowClass, row)
    }
    return optional
  }

  fun <E : Any> addOrUpdate(row: E): Optional<E> {
    return addOrUpdate(row.javaClass, row)
  }

  fun <E : Any> delete(rowClass: Class<out E>, row: E): Optional<E>
  fun <E : Any> delete(row: E): Optional<E> {
    return delete(row.javaClass, row)
  }

  fun <E : Any?, V : Any?> nextUniqueValue(rowClass: Class<out E?>): V

  /**
   * Replace the rows of the class with the new provided rows
   *
   * @param rowClass row class to replace rows
   * @param rows     new rows to replace the old ones
   */
  fun <E : Any> replaceGracefully(rowClass: Class<out E?>, rows: Stream<out E?>) {
    val current = this.getAll(rowClass).collect(Collectors.toList())
    val newRows = rows.toList().filterNotNull()
    applyMergedCollections(rowClass, mergeCollections(current, newRows))
  }

  /**
   * Perform actions on the row class with the new rows
   *
   * @param rowClass          row class to perform actions on
   * @param mergedCollections merged row collections to perform actions by
   */
  fun <E : Any> applyMergedCollections(
    rowClass: Class<out E>,
    mergedCollections: MergedCollections<out E>
  ) {
    mergedCollections.toDelete.forEach { e: E -> delete(rowClass, e) }
    mergedCollections.toUpdate.forEach { e: E -> update(rowClass, e) }
    mergedCollections.toAdd.forEach { e: E ->
      add(rowClass, e)
      if (e is ConnectionConfig) {
        AnalyticsService.getService().trackAnalyticsEvent(ConnectionEvent(ActionType.CREATE))
      }
    }
  }

  fun <E : Any> find(
    rowClass: Class<out E?>,
    predicate: Predicate<in E>
  ): Stream<E> {
    return getAll(rowClass).filter(predicate) as Stream<E>
  }

  /**
   * Get field by unique key
   *
   * @param rowClass  row class to get fields by
   * @param uniqueKey unique key to get field by
   * @return the field or nothing
   */
  fun <E : Any, U> getByUniqueKey(
    rowClass: Class<out E?>,
    uniqueKey: U
  ): Optional<E> {
    val uniqueField = Arrays.stream(Utils.getFieldsDeeply(rowClass))
      .filter(Utils.uniqueFieldPredicate)
      .findAny()
      .orElse(null)
    return if (uniqueField != null) {
      find(rowClass) { e: E ->
        try {
          uniqueField.isAccessible = true
          return@find uniqueField[e] == uniqueKey
        } catch (illegalAccessException: IllegalAccessException) {
          return@find false
        }
      }.findAny() as Optional<E>
    } else {
      Optional.empty<E>()
    }
  }

  fun <E : Any, U> deleteByUniqueKey(
    rowClass: Class<out E?>,
    uniqueKey: U
  ): Optional<out E> {
    val optional: Optional<out E> = getByUniqueKey(rowClass, uniqueKey)
    return if (optional.isPresent) {
      delete(optional.get())
    } else {
      Optional.empty()
    }
  }

  fun <E : Any, F : Any> getByForeignKey(row: E, foreignRowClass: Class<out F>): Optional<F>? {
    return Utils.getByForeignKeyInternal(this, row, null, foreignRowClass)
  }

  companion object {
    /**
     * Merge the old and the new row collections, so they will be removed, updated or added respectively
     *
     * @param oldCollection the old rows collection
     * @param newCollection the new rows collection
     */
    fun <E : Any> mergeCollections(
      oldCollection: Collection<E>,
      newCollection: Collection<E>
    ): MergedCollections<E> {
      val oldCollectionWithUniqueValues = Utils.mapWithUniqueValues(oldCollection)
      val newCollectionWithUniqueValues = Utils.mapWithUniqueValues(newCollection)
      val toDelete = oldCollectionWithUniqueValues.stream()
        .filter { (oldOptional): Pair<Optional<*>, E> ->
          newCollectionWithUniqueValues.stream()
            .noneMatch { (newOptional): Pair<Optional<*>, E> ->
              if (oldOptional.isPresent && newOptional.isPresent) {
                return@noneMatch oldOptional.get() == newOptional.get()
              }
              false
            }
        }
        .map { pair: Pair<*, E> -> pair.second }
        .collect(Collectors.toList())
      val toAdd = newCollectionWithUniqueValues.stream()
        .filter { (newOptional): Pair<Optional<*>, E> ->
          oldCollectionWithUniqueValues.stream()
            .noneMatch { (oldOptional): Pair<Optional<*>, E> ->
              if (oldOptional.isPresent && newOptional.isPresent) {
                return@noneMatch oldOptional.get() == newOptional.get()
              }
              false
            }
        }
        .map { pair: Pair<*, E> -> pair.second }
        .collect(Collectors.toList())
      val toUpdate = newCollectionWithUniqueValues.stream()
        .filter { (newOptional, second): Pair<Optional<*>, E> ->
          oldCollectionWithUniqueValues.stream()
            .anyMatch { (oldOptional, second1): Pair<Optional<*>, E> ->
              if (oldOptional.isPresent && newOptional.isPresent) {
                return@anyMatch oldOptional.get() == newOptional.get() && second != second1
              }
              false
            }
        }
        .map { pair: Pair<*, E> -> pair.second }
        .collect(Collectors.toList())
      return MergedCollections(
        toAdd, toUpdate, toDelete
      )
    }

    /**
     * Get column value with column "unique" attribute set to "true"
     * @param row the row to get the unique column value from
     */
    fun <E : Any> getUniqueValueForRow(row: E): Optional<*> {
      return Arrays.stream(Utils.getFieldsDeeply(row.javaClass))
        .filter { field: Field ->
          field.isAccessible = true
          field.castToColumnOrNull()?.unique ?: false
        }
        .findAny()
        .map { field: Field ->
          field.isAccessible = true
          try {
            return@map field[row]
          } catch (e: IllegalAccessException) {
            return@map null
          }
        }
    }
  }
}
