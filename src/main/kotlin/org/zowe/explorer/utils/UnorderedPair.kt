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

class UnorderedPair<T>(val first: T, val second: T) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is UnorderedPair<*>) return false



    return true
  }

  override fun hashCode(): Int {
    return first.hashCode() + second.hashCode()
  }

}
