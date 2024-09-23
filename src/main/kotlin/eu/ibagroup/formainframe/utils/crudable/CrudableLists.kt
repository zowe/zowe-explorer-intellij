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

import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.Crudable.Companion.getUniqueValueForRow
import java.util.*
import java.util.stream.Stream

/**
 * Class to provide crudable lists object with the function to accept the next UUID provider
 * @param getListByClass the callback to get a rows list by a row class
 */
class CrudableListsBuilder(
  private val getListByClass: (Class<*>) -> MutableList<*>?
) {
  fun withNextUuidProvider(nextUuidProvider: (Class<*>) -> Any): CrudableLists {
    return CrudableLists(getListByClass = getListByClass, nextUuidProvider = nextUuidProvider)
  }
}

/**
 * Class to represent the crudable object lists. Provides the functionality to work with such crudable objects
 * @param eventHandler the event handler to trigger corresponding events
 * @param addFilter the filter that is triggered before the "add" operation to check whether the row being added is unique
 * @param updateFilter the filter
 */
open class CrudableLists(
  var eventHandler: EventHandler? = null,
  var addFilter: AddFilter = object: AddFilter {},
  var updateFilter: UpdateFilter = object: UpdateFilter {},
  val nextUuidProvider: (Class<*>) -> Any,
  val getListByClass: (Class<*>) -> MutableList<*>?
) : Crudable {

  /**
   * Get the next unique value by the crudable unique value provider
   * @param rowClass the row class to get the unique value with
   */
  @Suppress("UNCHECKED_CAST")
  override fun <E : Any?, V : Any?> nextUniqueValue(rowClass: Class<out E>): V {
    return nextUuidProvider(rowClass) as V
  }

  /**
   * Get a rows list by the row class
   * @param rowClass the row class to get the rows list by
   */
  @Suppress("UNCHECKED_CAST")
  private fun <E : Any> classToList(rowClass: Class<out E>): MutableList<E>? {
    return getListByClass(rowClass) as MutableList<E>?
  }

  /**
   * Get all not null rows by the row class
   * @param rowClass the row class to get the rows by
   */
  override fun <E : Any> getAll(rowClass: Class<out E>): Stream<E> {
    return classToList(rowClass).streamOrEmpty().filterNotNull()
  }

  /**
   * Add the row by the row class. Skips the processing if the row already exists. Triggers onAdd event when the new row is added
   * @param rowClass the row class to check if the row has a unique value for the column with the "unique" attribute, that is different from the other rows for the class
   * @param row the row class to check if the row is unique
   */
  override fun <E : Any> add(rowClass: Class<out E>, row: E): Optional<E> {
    return runIfTrue(addFilter(rowClass, row)) {
      getUniqueValueForRow(row)
        .nullable
        ?.let { field ->
          classToList(rowClass)
            ?.let { list ->
              val alreadyExists = list.any { o -> getUniqueValueForRow(o).nullable?.equals(field) ?: false }
              runIfTrue(!alreadyExists && list.add(row)) {
                eventHandler?.onAdd(rowClass, row)
                row
              }
            }
        }
    }
      .optional
  }

  /**
   * Update the row of the row class. Performs an update when the row is present in the crudable. Triggers onUpdate when the new row is updated
   * @param rowClass the row class to get rows by. Then the rows will be checked for the row existence
   * @param row the row updates with the value to get the row from the rows
   */
  override fun <E : Any> update(rowClass: Class<out E>, row: E): Optional<E> {
    return runIfPresent(getUniqueValueForRow(row)) { field ->
      classToList(rowClass)
        ?.let { list ->
          list
            .indexOf { e -> getUniqueValueForRow(e).nullable?.equals(field) ?: false }
            ?.let { index ->
              val old = list[index]
              runIfTrue(updateFilter(rowClass, old, row)) {
                if (old != row) {
                  list[index] = row
                  eventHandler?.onUpdate(rowClass, old, row)
                }
                old
              }
            }
        }
    }
      .optional
  }

  /**
   * Remove the row from the crudable. Triggers onDelete event when the row is deleted
   * @param rowClass the row class to get rows by
   * @param row the row to search by the unique value for the row to delete
   */
  override fun <E : Any> delete(rowClass: Class<out E>, row: E): Optional<E> {
    return classToList(rowClass)
      ?.let main@{ list ->
        list
          .remove(row)
          .runIfTrue {
            eventHandler?.onDelete(rowClass, row)
            row
          }
      }
      .optional
  }

}
