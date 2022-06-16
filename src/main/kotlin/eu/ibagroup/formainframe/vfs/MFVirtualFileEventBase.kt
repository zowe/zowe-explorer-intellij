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

import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

abstract class MFVirtualFileEventBase(
  protected val vFile: MFVirtualFile,
  val parent: MFVirtualFile,
  val attributes: FileAttributes,
  isFromRefresh: Boolean,
  requestor: Any? = null
) : VFileEvent(requestor, isFromRefresh) {

  override fun computePath() = file.path

  override fun getFile() = vFile

  override fun getFileSystem() = file.fileSystem

}