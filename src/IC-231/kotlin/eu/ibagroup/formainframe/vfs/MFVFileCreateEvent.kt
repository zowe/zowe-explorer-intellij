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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem.Companion.MF_VFS_CHANGES_TOPIC

/**
 * MF virtual file create event. Carries info about the created MF virtual file.
 * Use it together with [MF_VFS_CHANGES_TOPIC]
 * @property requestor an instance to describe the event requester
 * @property parent the created MF virtual file's parent
 * @property childName the created file's name
 * @property isDirectory indicates whether the created MF virtual file is a directory
 * @property delegate the delegate to execute the necessary functions
 */
class MFVFileCreateEvent(
  requestor: Any?,
  private val parent: MFVirtualFile,
  private val childName: String,
  private val isDirectory: Boolean,
  private val delegate: MFVFileCreateEventDelegate
) : VFileEvent(requestor, false) {

  override fun equals(other: Any?): Boolean {
    var otherIsDirectory: Boolean? = null
    var otherChildName: String? = null
    var otherParent: MFVirtualFile? = null
    if (other is MFVFileCreateEvent) {
      otherIsDirectory = other.isDirectory
      otherChildName = other.childName
      otherParent = other.parent
    }
    return delegate.equals(
      this, isDirectory, childName, parent, other, otherIsDirectory, otherChildName, otherParent
    )
  }

  override fun hashCode(): Int {
    return delegate.hashCode(parent, isDirectory, childName)
  }

  override fun computePath(): String {
    return delegate.computePath(parent, childName)
  }

  override fun getFile(): VirtualFile {
    return delegate.getFile(parent, childName)
  }

  override fun getFileSystem(): VirtualFileSystem {
    return delegate.getFileSystem(parent)
  }

  override fun isValid(): Boolean {
    return delegate.isValid(parent, childName)
  }

}
