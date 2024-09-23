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

import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.util.FileContentUtilCore
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem.Companion.MF_VFS_CHANGES_TOPIC

/**
 * MF virtual file property change event. Carries info about a property of a MF virtual file being changed.
 * Use it together with [MF_VFS_CHANGES_TOPIC]
 */
class MFVFilePropertyChangeEventDelegate {

  /**
   * Check is the provided property valid (property name exists, old and new values are not same)
   * @param requestor an instance to describe the event requester
   * @param propName the name of the changing property
   * @param oldValue the old value of the property
   * @param newValue the new value of the property
   */
  fun checkPropValid(requestor: Any?, propName: MFVirtualFile.PropName, oldValue: Any, newValue: Any) {
    if (propName == MFVirtualFile.PropName.IS_DIRECTORY) {
      if (Comparing.equal(oldValue, newValue) && FileContentUtilCore.FORCE_RELOAD_REQUESTOR != requestor) {
        throw IllegalArgumentException("Values must be different, got the same: $oldValue");
      }
      if (oldValue !is Boolean) throw IllegalArgumentException("oldWriteable must be boolean, got $oldValue");
      if (newValue !is Boolean) throw IllegalArgumentException("newWriteable must be boolean, got $newValue");
    } else {
      VFilePropertyChangeEvent.checkPropertyValuesCorrect(requestor, propName.toString(), oldValue, newValue)
    }
  }

  fun equals(
    origin: MFVFilePropertyChangeEvent,
    file: MFVirtualFile,
    propName: MFVirtualFile.PropName,
    oldValue: Any,
    newValue: Any,
    other: Any?,
    otherFile: MFVirtualFile?,
    otherPropName: MFVirtualFile.PropName?,
    otherOldValue: Any?,
    otherNewValue: Any?
  ): Boolean {
    if (origin === other) return true
    if (other == null || origin.javaClass != other.javaClass) return false
    return file == otherFile
      && newValue == otherNewValue
      && oldValue == otherOldValue
      && propName == otherPropName
  }

  fun hashCode(
    propName: MFVirtualFile.PropName,
    oldValue: Any,
    newValue: Any,
    file: MFVirtualFile
  ): Int {
    var result = file.hashCode()
    result = 31 * result + propName.hashCode()
    result = 31 * result + oldValue.hashCode()
    result = 31 * result + newValue.hashCode()
    return result
  }

  fun computePath(file: MFVirtualFile): String {
    return file.path
  }

  fun getFile(file: MFVirtualFile): VirtualFile {
    return file
  }

  fun getFileSystem(file: MFVirtualFile): VirtualFileSystem {
    return file.fileSystem
  }

  fun isValid(file: MFVirtualFile): Boolean {
    return file.isValid
  }

}
