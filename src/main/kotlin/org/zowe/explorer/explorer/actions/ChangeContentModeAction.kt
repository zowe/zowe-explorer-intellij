/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.synchronizer.FileAttributesChangeListener
import org.zowe.explorer.explorer.ui.FILE_EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.GlobalFileExplorerView
import org.zowe.explorer.utils.service
import eu.ibagroup.r2z.XIBMDataType

class ChangeContentModeAction : ToggleAction() {

  override fun isSelected(e: AnActionEvent): Boolean {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return false
    return getMappedNodes(view)
      .mapNotNull {
        view.explorer.componentManager.service<DataOpsManager>()
          .getAttributesService(it.first::class.java, it.second::class.java)
          .getAttributes(it.second)
      }
      .all {
        it.contentMode == XIBMDataType(XIBMDataType.Type.BINARY)
      }
  }

  private fun getMappedNodes(view: GlobalFileExplorerView): List<Pair<FileAttributes, VirtualFile>> {
    return view.mySelectedNodesData
      .mapNotNull {
        val file = it.file
        if (file != null) {
          val attributes = service<DataOpsManager>().tryToGetAttributes(file) as? RemoteDatasetAttributes
          val isMigrated = attributes?.isMigrated ?: false
          if (isMigrated) {
            return@mapNotNull null
          }
        }
        if (file?.isDirectory == true) {
          return@mapNotNull null
        }
        Pair(it.attributes ?: return@mapNotNull null, file ?: return@mapNotNull null)
      }
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    getMappedNodes(view)
      .forEach {
        view.explorer.componentManager.service<DataOpsManager>()
          .getAttributesService(it.first::class.java, it.second::class.java)
          .updateAttributes(it.first) {
            contentMode = if (state) { XIBMDataType(XIBMDataType.Type.BINARY) } else { XIBMDataType(XIBMDataType.Type.TEXT) }
          }
        ApplicationManager.getApplication()
          .messageBus
          .syncPublisher(FileAttributesChangeListener.TOPIC)
          .onFileAttributesChange(it.second)
      }
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isEnabledAndVisible = getMappedNodes(view).isNotEmpty()
  }


  override fun isDumbAware(): Boolean {
    return true
  }
}
