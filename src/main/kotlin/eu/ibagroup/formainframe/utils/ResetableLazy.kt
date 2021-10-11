/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.utils

import java.util.*
import kotlin.reflect.KProperty

class LazyManager {
  private val managedDelegates = LinkedList<Resettable>()

  internal fun register(managed: Resettable) {
    synchronized(managedDelegates) {
      managedDelegates.add(managed)
    }
  }

  fun reset() {
    synchronized(managedDelegates) {
      managedDelegates.forEach { it.reset() }
      managedDelegates.clear()
    }
  }
}

interface Resettable {
  fun reset()
}

class ResettableLazy<E>(private val manager: LazyManager, val init: () -> E) : Resettable {
  @Volatile
  var lazyHolder = makeInitBlock()

  operator fun getValue(thisRef: Any?, property: KProperty<*>): E {
    return lazyHolder.value
  }

  override fun reset() {
    lazyHolder = makeInitBlock()
  }

  private fun makeInitBlock(): Lazy<E> {
    return lazy {
      manager.register(this)
      init()
    }
  }
}

fun <E> resettableLazy(manager: LazyManager, init: () -> E): ResettableLazy<E> {
  return ResettableLazy(manager, init)
}