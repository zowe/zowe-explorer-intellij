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

<<<<<<<< HEAD:src/main/kotlin/org/zowe/explorer/utils/Child.kt
interface Child {

  val parent: Child?

========
/** Enum class that represents the file mask types */
enum class MaskType(val stringType: String) {
  ZOS("z/OS"), USS("USS")
>>>>>>>> release/v0.7.0:src/main/kotlin/org/zowe/explorer/utils/MaskType.kt
}
