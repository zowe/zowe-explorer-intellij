/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops

import com.google.common.util.concurrent.AtomicDouble
import com.intellij.openapi.util.UserDataHolderBase

class OperationStatus : UserDataHolderBase() {

  @Volatile
  var stage = Stage.STARTUP

  private val atomicFraction = AtomicDouble(0.0)

  val fraction
    get() = atomicFraction.get().let { if (it > 1) 1.0 else it }

  fun incFraction(increment: Double): Double {
    if (increment < 0) {
      throw IllegalArgumentException("Increment must be a positive number")
    }
    return atomicFraction.addAndGet(increment)
  }

}

enum class Stage {
  STARTUP,
  BEFORE_EXECUTION,
  AFTER_EXECUTION,
  FINISHED,
}
