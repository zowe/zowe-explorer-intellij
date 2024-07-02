/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.util.FileContentUtilCore
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem.Companion.MF_VFS_CHANGES_TOPIC

/**
 * MF virtual file property change event. Carries info about a property of a MF virtual file being changed.
 * Use it together with [MF_VFS_CHANGES_TOPIC]
 * @property requestor an instance to describe the event requester
 * @property file the MF virtual file to change property of
 * @property propName the name of the property to change
 * @property oldValue the old value of the property being changed
 * @property newValue the possible new value of the property being changed
 */
class MFVFilePropertyChangeEvent(
  requestor: Any?,
  private val file: MFVirtualFile,
  private val propName: MFVirtualFile.PropName,
  private val oldValue: Any,
  private val newValue: Any
) : VFileEvent(requestor, false) {

  init {
    checkPropValid(requestor, propName, oldValue, newValue)
  }

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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this.javaClass != other.javaClass) return false

    val event = other as MFVFilePropertyChangeEvent
    return file == event.file
      && newValue == event.newValue
      && oldValue == event.oldValue
      && propName == event.propName
  }

  override fun hashCode(): Int {
    var result = file.hashCode();
    result = 31 * result + propName.hashCode()
    result = 31 * result + oldValue.hashCode()
    result = 31 * result + newValue.hashCode()
    return result
  }

  override fun computePath(): String {
    return file.path
  }

  override fun getFile(): VirtualFile {
    return file
  }

  override fun getFileSystem(): VirtualFileSystem {
    return file.fileSystem
  }

  override fun isValid(): Boolean {
    return file.isValid
  }

}
