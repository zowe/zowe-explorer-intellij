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

import org.zowe.explorer.utils.optionalLock
import java.util.*
import java.util.stream.Stream

class ConcurrentCrudable(
  val wrappingCrudable: Crudable,
  private val locksManager: LocksManager
) : Crudable by wrappingCrudable {

  override fun <E : Any> add(rowClass: Class<out E>, row: E): Optional<E> {
    return locksManager.getLockForAdding(rowClass).optionalLock {
      wrappingCrudable.add(rowClass, row)
    }
  }

  override fun <E : Any> getAll(rowClass: Class<out E>): Stream<E> {
    return locksManager.getLockForGettingAll(rowClass).optionalLock {
      wrappingCrudable.getAll(rowClass)
    }
  }

  override fun <E : Any> update(rowClass: Class<out E>, row: E): Optional<E> {
    return locksManager.getLockForDeleting(rowClass).optionalLock {
      wrappingCrudable.update(rowClass, row)
    }
  }

  override fun <E : Any> delete(rowClass: Class<out E>, row: E): Optional<E> {
    return locksManager.getLockForAdding(rowClass).optionalLock {
      wrappingCrudable.delete(rowClass, row)
    }
  }

  override fun <E : Any?, V : Any?> nextUniqueValue(rowClass: Class<out E>): V {
    return locksManager.getLockForNextUniqueValue(rowClass).optionalLock {
      wrappingCrudable.nextUniqueValue(rowClass)
    }
  }

}
