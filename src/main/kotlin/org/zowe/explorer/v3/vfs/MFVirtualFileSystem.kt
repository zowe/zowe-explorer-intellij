/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.vfs

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileSystem

// TODO: doc
class MFVirtualFileSystem : VirtualFileSystem() {
  companion object {
    const val PROTOCOL = "zowemf"
  }

  override fun getProtocol() = PROTOCOL

  override fun findFileByPath(path: String): MFVirtualFile? {
    TODO("Not yet implemented")
  }

  override fun refresh(asynchronous: Boolean) {
    TODO("Not yet implemented")
  }

  override fun refreshAndFindFileByPath(path: String): VirtualFile? {
    TODO("Not yet implemented")
  }

  override fun addVirtualFileListener(listener: VirtualFileListener) {
    TODO("Not yet implemented")
  }

  override fun removeVirtualFileListener(listener: VirtualFileListener) {
    TODO("Not yet implemented")
  }

  override fun deleteFile(requestor: Any?, vFile: VirtualFile) {
    TODO("Not yet implemented")
  }

  override fun moveFile(
    requestor: Any?,
    vFile: VirtualFile,
    newParent: VirtualFile
  ) {
    TODO("Not yet implemented")
  }

  override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {
    TODO("Not yet implemented")
  }

  override fun createChildFile(
    requestor: Any?,
    vDir: VirtualFile,
    fileName: String
  ): VirtualFile {
    TODO("Not yet implemented")
  }

  override fun createChildDirectory(
    requestor: Any?,
    vDir: VirtualFile,
    dirName: String
  ): VirtualFile {
    TODO("Not yet implemented")
  }

  override fun copyFile(
    requestor: Any?,
    virtualFile: VirtualFile,
    newParent: VirtualFile,
    copyName: String
  ): VirtualFile {
    TODO("Not yet implemented")
  }

  override fun isReadOnly(): Boolean {
    TODO("Not yet implemented")
  }
}