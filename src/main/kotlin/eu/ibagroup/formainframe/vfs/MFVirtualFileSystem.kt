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

import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import eu.ibagroup.formainframe.dataops.DataOpsManager

class MFVirtualFileSystem : VirtualFileSystemModelWrapper<MFVirtualFile, MFVirtualFileSystemModel>(
  MFVirtualFile::class.java,
  MFVirtualFileSystemModel()
) {

  companion object {
    const val SEPARATOR = "/"
    const val PROTOCOL = "mf"
    const val ROOT_NAME = "For Mainframe"
    const val ROOT_ID = 0

    @JvmStatic
    val instance: MFVirtualFileSystem
      get() = VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as MFVirtualFileSystem

    @JvmStatic
    val model
      get() = instance.model
  }

  init {
    Disposer.register(service<DataOpsManager>(), this)
  }

  val root = model.root

  override fun isValidName(name: String) = name.isNotBlank()

}