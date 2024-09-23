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

package eu.ibagroup.formainframe.config.ws

import java.util.*

/** Class that represent a dataset mask */
class DSMask {

  var mask = ""

  var excludes: MutableList<String> = ArrayList()

  var volser = ""

  private val isSingle = false

  constructor()

  constructor(mask: String, excludes: MutableList<String>) {
    this.mask = mask
    this.excludes = excludes
  }

  constructor(mask: String, excludes: MutableList<String>, volser: String) {
    this.mask = mask
    this.excludes = excludes
    this.volser = volser
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val dsMask = other as DSMask
    return isSingle == dsMask.isSingle && mask == dsMask.mask && excludes == dsMask.excludes && volser == dsMask.volser
  }

  override fun hashCode(): Int {
    return Objects.hash(mask, excludes, volser, isSingle)
  }

  override fun toString(): String {
    return "DSMask(mask='$mask', excludes=$excludes, volser='$volser', isSingle=$isSingle)"
  }

}
