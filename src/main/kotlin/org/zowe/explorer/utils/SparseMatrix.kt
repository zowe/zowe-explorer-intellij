/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.utils

class SparseMatrix<T>(
  private val defaultValue: T? = null,
) : ObjectMatrix<T> {

  private val map = mutableMapOf<Pair<Int, Int>, T?>()

  override fun get(i: Int, j: Int): T? {
    return map[Pair(i, j)] ?: defaultValue
  }

  override fun set(i: Int, j: Int, value: T?) {
    map[Pair(i, j)] = value
  }

  fun removeByRow(i: Int) {
    map.filter { it.key.first == i }.forEach{ map.remove(it.key) }
  }

  @Suppress("UNCHECKED_CAST")
  val asMap: Map<Pair<Int, Int>, T>
    get() = map.filter { it.value != null } as Map<Pair<Int, Int>, T>

  fun clean() {
    asMap.forEach {
      map[it.key] = defaultValue
    }
  }

}
