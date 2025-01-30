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

package eu.ibagroup.formainframe.config.connect.ui

/**
 * Data class which represents state for change password dialog
 */
data class ChangePasswordDialogState(
  var username: String = "",
  var oldPassword: CharArray = charArrayOf(),
  var newPassword: CharArray = charArrayOf(),
  var confirmPassword: CharArray = charArrayOf()
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ChangePasswordDialogState) return false

    if (username != other.username) return false
    if (!oldPassword.contentEquals(other.oldPassword)) return false
    if (!newPassword.contentEquals(other.newPassword)) return false
    if (!confirmPassword.contentEquals(other.confirmPassword)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = username.hashCode()
    result = 31 * result + oldPassword.contentHashCode()
    result = 31 * result + newPassword.contentHashCode()
    result = 31 * result + confirmPassword.contentHashCode()
    return result
  }
}
