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
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Class that implements abstract read/write lock adapter class.
 * Triggers reentrant read/write lock on read or write lock
 */
class SimpleReadWriteAdapter : ReadWriteLocksAdapter() {

  private val lock = ReentrantReadWriteLock()

  override fun <E : Any> getReadLock(rowClass: Class<out E>): Lock? {
    return lock.readLock()
  }

  override fun <E : Any> getWriteLock(rowClass: Class<out E>): Lock? {
    return lock.writeLock()
  }

}
