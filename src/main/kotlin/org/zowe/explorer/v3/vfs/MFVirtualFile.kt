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
import com.intellij.openapi.vfs.VirtualFileSystem
import java.io.InputStream
import java.io.OutputStream

// TODO: doc
class MFVirtualFile : VirtualFile() {
  override fun getName(): String {
    TODO("Not yet implemented")
  }

  override fun getFileSystem(): VirtualFileSystem {
    TODO("Not yet implemented")
  }

  override fun getPath(): String {
    TODO("Not yet implemented")
  }

  override fun isWritable(): Boolean {
    TODO("Not yet implemented")
  }

  override fun isDirectory(): Boolean {
    TODO("Not yet implemented")
  }

  override fun isValid(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getParent(): VirtualFile? {
    TODO("Not yet implemented")
  }

  override fun getChildren(): Array<out VirtualFile?>? {
    TODO("Not yet implemented")
  }

  override fun getOutputStream(
    requestor: Any?,
    newModificationStamp: Long,
    newTimeStamp: Long
  ): OutputStream {
    TODO("Not yet implemented")
  }

  override fun contentsToByteArray(): ByteArray {
    TODO("Not yet implemented")
  }

  override fun getTimeStamp(): Long {
    TODO("Not yet implemented")
  }

  override fun getLength(): Long {
    TODO("Not yet implemented")
  }

  override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
    TODO("Not yet implemented")
  }

  override fun getInputStream(): InputStream {
    TODO("Not yet implemented")
  }
}