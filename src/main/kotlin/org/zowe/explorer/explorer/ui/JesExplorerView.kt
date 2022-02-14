/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.config.ws.JobsWorkingSetConfig
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.GlobalJesWorkingSet

val JES_EXPLORER_VIEW = DataKey.create<JesExplorerView>("jesExplorerView")
const val JES_EXPLORER_CONTEXT_MENU = "Jes Explorer"

class JesExplorerView(
  explorer: Explorer<GlobalJesWorkingSet>,
  project: Project,
  parentDisposable: Disposable,
  contextMenu: ActionGroup,
  rootNodeProvider: (Explorer<*>, Project, ExplorerTreeStructureBase) -> ExplorerTreeNode<*>,
  cutProviderUpdater: (List<VirtualFile>) -> Unit
) : ExplorerTreeView<GlobalJesWorkingSet, JobsWorkingSetConfig>(
  explorer,
  project,
  parentDisposable,
  contextMenu,
  rootNodeProvider,
  cutProviderUpdater
) {


  override fun getData(dataId: String): Any? {
    return when {
      CommonDataKeys.NAVIGATABLE.`is`(dataId) -> if (mySelectedNodesData.isNotEmpty()) mySelectedNodesData[0].node else null
      CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId) -> mySelectedNodesData.map { it.node }.toTypedArray()
      JES_EXPLORER_VIEW.`is`(dataId) -> this
      else -> null
    }
  }

  override fun dispose() {

  }
}
