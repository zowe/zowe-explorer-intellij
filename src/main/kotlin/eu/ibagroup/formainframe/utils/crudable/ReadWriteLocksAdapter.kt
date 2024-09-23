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

import java.util.concurrent.locks.Lock

/** Abstract class that conforms all the lock to the read/write locks */
abstract class ReadWriteLocksAdapter : LocksManager {

  abstract fun <E : Any> getReadLock(rowClass: Class<out E>): Lock?

  abstract fun <E : Any> getWriteLock(rowClass: Class<out E>): Lock?

  override fun <E : Any> getLockForAdding(rowClass: Class<out E>): Lock? {
    return getWriteLock(rowClass)
  }

  override fun <E : Any> getLockForGettingAll(rowClass: Class<out E>): Lock? {
    return getReadLock(rowClass)
  }

  override fun <E : Any> getLockForUpdating(rowClass: Class<out E>): Lock? {
    return getWriteLock(rowClass)
  }

  override fun <E : Any> getLockForDeleting(rowClass: Class<out E>): Lock? {
    return getWriteLock(rowClass)
  }

  override fun <E : Any> getLockForNextUniqueValue(rowClass: Class<out E>): Lock? {
    return getWriteLock(rowClass)
  }

}
