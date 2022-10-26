/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.JesWorkingSetImpl

/**
 * Data key for extracting current instance of JesExplorerView.
 * @see JesExplorerView
 */
val JES_EXPLORER_VIEW = DataKey.create<JesExplorerView>("jesExplorerView")

const val JES_EXPLORER_CONTEXT_MENU = "JES Explorer"

/**
 * JES Explorer tree view implementation.
 * @param explorer instance of units explorer (logical representation of explorer view data).
 * @param project current project.
 * @param parentDisposable parent disposable.
 * @param contextMenu action group for context menu (with items New, Delete, Refresh and etc.).
 * @param rootNodeProvider function to get root node of the tree.
 * @param cutProviderUpdater function that will be triggered after each cut action.
 *
 * @author Valiantsin Krus
 */
class JesExplorerView(
  explorer: Explorer<JesWorkingSetImpl>,
  project: Project,
  parentDisposable: Disposable,
  contextMenu: ActionGroup,
  rootNodeProvider: (Explorer<*>, Project, ExplorerTreeStructureBase) -> ExplorerTreeNode<*>,
  cutProviderUpdater: (List<VirtualFile>) -> Unit
) : ExplorerTreeView<JesWorkingSetImpl, JesWorkingSetConfig>(
  explorer,
  project,
  parentDisposable,
  contextMenu,
  rootNodeProvider,
  cutProviderUpdater
) {

  /**
   * Provides data in data context. Intellij understands the context
   * from which the action was triggered and some data can be extracted
   * in this action by data keys from this context.
   * @param dataId key of the data to extract. JES Explorer provides data for:
   *               1) NAVIGATABLE - first selected node if something is selected or null otherwise;
   *               2) NAVIGATABLE_ARRAY - array of selected nodes;
   *               3) JES_EXPLORER_VIEW - current instance of the JesExplorerView.
   * @return data corresponding to specified dataId or null if no data linked with passed dataId.
   */
  override fun getData(dataId: String): Any? {
    return when {
      CommonDataKeys.NAVIGATABLE.`is`(dataId) -> if (mySelectedNodesData.isNotEmpty()) mySelectedNodesData[0].node else null
      CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId) -> mySelectedNodesData.map { it.node }.toTypedArray()
      JES_EXPLORER_VIEW.`is`(dataId) -> this
      else -> null
    }
  }

  override fun dispose() {}
}
