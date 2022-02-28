/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.utils.crudable

import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.crudable.annotations.Column
import org.zowe.explorer.utils.nullable
import java.util.stream.Stream
import kotlin.reflect.KProperty

inline fun <reified E> Crudable.deleteByUniqueKey(uniqueKey: Any): E? {
  return this.deleteByUniqueKey(E::class.java, uniqueKey).nullable
}

inline fun <reified E> Crudable.deleteIf(noinline predicate: (E) -> Boolean): Stream<E> {
  return this.deleteIf(E::class.java, predicate)
}

inline fun <reified E> Crudable.find(noinline predicate: (E) -> Boolean): Stream<E> {
  return this.find(E::class.java, predicate)
}

inline fun <reified E> Crudable.getAll(): Stream<E> {
  return this.getAll(E::class.java)
}

inline fun <reified E, V : Any> Crudable.getByColumnValue(columnName: String, columnValue: V): Stream<E> {
  return this.getByColumnValue(E::class.java, columnName, columnValue)
}

inline fun <reified E, V : Any> Crudable.getByColumnValue(columnProperty: KProperty<V>, columnValue: V): Stream<E> {
  val columnName = columnProperty.annotations
    .find { it == Column::class.java }
    ?.castOrNull<Column>()?.name
    .let {
      if (!it.isNullOrBlank()) {
        it
      } else null
    } ?: columnProperty.name
  return this.getByColumnValue(E::class.java, columnName, columnValue)
}

inline fun <reified E> Crudable.getByColumnLambda(row: E, crossinline lambdaGetter: (E) -> Any): Stream<E> {
  return this.find { lambdaGetter(it) == lambdaGetter(row) }
}

inline fun <reified E> Crudable.getByUniqueKey(uniqueKey: Any): E? {
  return this.getByUniqueKey(E::class.java, uniqueKey).nullable
}

fun getUniqueValue(row: Any): Any? {
  return Crudable.getUniqueValueForRow(row).nullable
}

inline fun <reified E> Crudable.isColumnPresented(row: Any, crossinline lambdaGetter: (E) -> Any): Boolean {
  return row.castOrNull<E>()?.let { castedRow ->
    this.getByColumnLambda(castedRow) { lambdaGetter(it) == lambdaGetter(castedRow) }.findAny().isPresent
  } ?: false
}

inline fun <reified E, reified V : Any> Crudable.nextUniqueValue(): V {
  return this.nextUniqueValue<E, V>(E::class.java)
}

inline fun <reified E : Any, reified F : Any> Crudable.getByForeignKey(row: E): F? {
  return this.getByForeignKey(row, F::class.java).nullable
}

inline fun <reified E : Any> Crudable.applyMergedCollections(mergedCollections: MergedCollections<E>) {
  this.applyMergedCollections(E::class.java, mergedCollections)
}

inline fun <reified E : Any, reified F : Any> Crudable.getByForeignKeyDeeply(row: E): F? {
  return this.getByForeignKeyDeeply<E, F>(row, F::class.java).nullable
}
