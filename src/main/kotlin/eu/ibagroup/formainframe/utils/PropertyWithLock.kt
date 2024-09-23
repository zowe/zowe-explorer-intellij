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

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Class to specify property with lock handling.
 * This lock is intended to be as the read and write lock at the same time on each operation
 */
class PropertyWithLock<V>(
  private var value: V,
  private val lock: Lock
) : ReadWriteProperty<Any?, V> {

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
    lock.withLock { this.value = value }
  }

  override fun getValue(thisRef: Any?, property: KProperty<*>) = lock.withLock {
    this.value
  }
}

/** Class to specify property with RW lock handling */
class PropertyWithRWLock<V>(
  private var value: V,
  private val lock: ReadWriteLock
) : ReadWriteProperty<Any?, V> {

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
    lock.write { this.value = value }
  }

  override fun getValue(thisRef: Any?, property: KProperty<*>) = lock.read {
    this.value
  }
}

/** Make a property with a generic lock handling */
fun <V> locked(value: V, lock: Lock = ReentrantLock()) = PropertyWithLock(value, lock)

/** Make a property with a read/write lock handling */
fun <V> rwLocked(value: V, lock: ReadWriteLock = ReentrantReadWriteLock()) = PropertyWithRWLock(value, lock)
