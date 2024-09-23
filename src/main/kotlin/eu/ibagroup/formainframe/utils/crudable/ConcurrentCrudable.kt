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

import eu.ibagroup.formainframe.utils.optionalLock
import java.util.*
import java.util.stream.Stream

/**
 * Class that represents working with the concurrent CRUDable object. Wraps the crudable object with thread safe locks for each operation
 * @param wrappingCrudable the crudable object that is being wrapped
 * @param locksManager the lock's manager to acquire lock for each operation
 */
class ConcurrentCrudable(
  val wrappingCrudable: Crudable,
  private val locksManager: LocksManager
) : Crudable {

  /**
   * Add a row of the row class to the crudable
   * @param rowClass the row class to add the row by
   * @param row the row to add
   */
  override fun <E : Any> add(rowClass: Class<out E>, row: E): Optional<E> {
    return locksManager.getLockForAdding(rowClass).optionalLock {
      wrappingCrudable.add(rowClass, row)
    }
  }

  /**
   * Get all rows of the row class
   * @param rowClass the row class to get all rows by
   */
  override fun <E : Any> getAll(rowClass: Class<out E>): Stream<E> {
    return locksManager.getLockForGettingAll(rowClass).optionalLock {
      wrappingCrudable.getAll(rowClass)
    }
  }

  /**
   * Update the row of the row class. It gets the row to update by the unique column value of the row updates parameter
   * @param rowClass the row class
   * @param row the row updates with the unique column to search for the old row by
   */
  override fun <E : Any> update(rowClass: Class<out E>, row: E): Optional<E> {
    return locksManager.getLockForDeleting(rowClass).optionalLock {
      wrappingCrudable.update(rowClass, row)
    }
  }

  /**
   * Delete the row by the row class and the row with the unique column value
   * @param rowClass the row class to get rows by
   * @param row the row to get the unique column value to search for the row to delete
   */
  override fun <E : Any> delete(rowClass: Class<out E>, row: E): Optional<E> {
    return locksManager.getLockForAdding(rowClass).optionalLock {
      wrappingCrudable.delete(rowClass, row)
    }
  }

  /**
   * Get the next unique value by the crudable unique value provider
   * @param rowClass the row class to get the unique value with
   */
  override fun <E : Any?, V : Any?> nextUniqueValue(rowClass: Class<out E>): V {
    return locksManager.getLockForNextUniqueValue(rowClass).optionalLock {
      wrappingCrudable.nextUniqueValue(rowClass)
    }
  }

}
