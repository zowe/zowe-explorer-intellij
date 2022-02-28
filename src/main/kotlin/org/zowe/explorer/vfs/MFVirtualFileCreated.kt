/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.vfs

import com.intellij.openapi.util.io.FileAttributes
import java.util.*

class MFVirtualFileCreated(
  vFile: MFVirtualFile,
  parent: MFVirtualFile,
  attributes: FileAttributes,
  isFromRefresh: Boolean,
  requestor: Any? = null
) : MFVirtualFileEventBase(vFile, parent, attributes, isFromRefresh, requestor) {

  override fun isValid() = parent.children?.contains(vFile) ?: false

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    return true
  }

  override fun hashCode(): Int {
    return Objects.hash(vFile, parent, attributes, isFromRefresh, requestor)
  }


}
