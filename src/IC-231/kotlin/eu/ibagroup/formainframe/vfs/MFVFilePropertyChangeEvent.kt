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

package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem.Companion.MF_VFS_CHANGES_TOPIC

/**
 * MF virtual file property change event. Carries info about a property of a MF virtual file being changed.
 * Use it together with [MF_VFS_CHANGES_TOPIC]
 * @property requestor an instance to describe the event requester
 * @property file the MF virtual file to change property of
 * @property propName the name of the property to change
 * @property oldValue the old value of the property being changed
 * @property newValue the possible new value of the property being changed
 * @property delegate the delegate to execute the necessary functions
 */
class MFVFilePropertyChangeEvent(
  requestor: Any?,
  private val file: MFVirtualFile,
  private val propName: MFVirtualFile.PropName,
  private val oldValue: Any,
  private val newValue: Any,
  private val delegate: MFVFilePropertyChangeEventDelegate
) : VFileEvent(requestor, false) {

  init {
    delegate.checkPropValid(requestor, propName, oldValue, newValue)
  }

  override fun equals(other: Any?): Boolean {
    var otherFile: MFVirtualFile? = null
    var otherPropName: MFVirtualFile.PropName? = null
    var otherOldValue: Any? = null
    var otherNewValue: Any? = null
    if (other is MFVFilePropertyChangeEvent) {
      otherFile = other.file
      otherPropName = other.propName
      otherOldValue = other.oldValue
      otherNewValue = other.newValue
    }
    return delegate.equals(
      this, file, propName, oldValue, newValue, other, otherFile, otherPropName, otherOldValue, otherNewValue
    )
  }

  override fun hashCode(): Int {
    return delegate.hashCode(propName, oldValue, newValue, file)
  }

  override fun computePath(): String {
    return delegate.computePath(file)
  }

  override fun getFile(): VirtualFile {
    return delegate.getFile(file)
  }

  override fun getFileSystem(): VirtualFileSystem {
    return delegate.getFileSystem(file)
  }

  override fun isValid(): Boolean {
    return delegate.isValid(file)
  }

}
