/*
 * Copyright (c) 2024 IBA Group.
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

package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem.Companion.MF_VFS_CHANGES_TOPIC

/**
 * MF virtual file create event. Carries info about the created MF virtual file.
 * Use it together with [MF_VFS_CHANGES_TOPIC]
 */
class MFVFileCreateEventDelegate {

  private var createdFile: MFVirtualFile? = null

  fun equals(
    origin: MFVFileCreateEvent,
    isDirectory: Boolean,
    childName: String,
    parent: MFVirtualFile,
    other: Any?,
    otherIsDirectory: Boolean?,
    otherChildName: String?,
    otherParent: MFVirtualFile?
  ): Boolean {
    if (origin === other) return true
    if (other == null || origin.javaClass != other.javaClass) return false
    return isDirectory == otherIsDirectory
      && childName == otherChildName
      && parent == otherParent
  }

  fun hashCode(
    parent: VirtualFile,
    isDirectory: Boolean,
    childName: String
  ): Int {
    var result = parent.hashCode()
    result = 31 * result + (if (isDirectory) 1 else 0)
    result = 31 * result + childName.hashCode()
    return result
  }

  fun computePath(parent: MFVirtualFile, childName: String): String {
    val parentPath = parent.path
    return if (StringUtil.endsWithChar(parentPath, '/')) parentPath + childName else "$parentPath/$childName"
  }

  fun getFile(parent: MFVirtualFile, childName: String): VirtualFile {
    var createdFileNewValue = createdFile
    if (createdFile == null && parent.isValid()) {
      createdFile = parent.findChild(childName)
      createdFileNewValue = createdFile
    }
    return createdFileNewValue ?: throw Exception("Created file is not found")
  }

  fun getFileSystem(parent: MFVirtualFile): VirtualFileSystem {
    return parent.fileSystem
  }

  fun isValid(parent: MFVirtualFile, childName: String): Boolean {
    return parent.isValid() && parent.findChild(childName) == null
  }

}
