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

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.wm.IdeFocusManager
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.WorkingSetConfig
import org.zowe.explorer.config.ws.ui.AbstractWsDialog
import org.zowe.explorer.explorer.WorkingSet
import org.zowe.explorer.explorer.ui.ExplorerTreeView
import org.zowe.explorer.explorer.ui.WorkingSetNode
import org.zowe.explorer.explorer.ui.getExplorerView
import org.zowe.explorer.utils.clone
import org.zowe.explorer.utils.crudable.Crudable
import kotlin.jvm.optionals.getOrNull

/** Base abstract class for the Edit Working set action */
abstract class EditWorkingSetAction<
  WSExplorerView : ExplorerTreeView<ConnectionConfig, *, *>,
  WSNode : WorkingSetNode<ConnectionConfig, *>,
  WSConfig : WorkingSetConfig
> : DumbAwareAction() {

  abstract val explorerViewClass: Class<WSExplorerView>
  abstract val workingSetNodeClass: Class<WSNode>
  abstract val workingSetConfigClass: Class<WSConfig>

  abstract fun dialogCreator(
    crudable: Crudable,
    workingSetConfig: WSConfig
  ): AbstractWsDialog<ConnectionConfig, WSConfig, *, *>

  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  /** Show the Edit Working set dialog for the appropriate selected working set node */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView(explorerViewClass) ?: return
    val node = view.mySelectedNodesData[0].node
    when {
      node.javaClass == workingSetNodeClass -> {
        val workingSetConfig =
          ConfigService.getService().crudable
            .getByUniqueKey(workingSetConfigClass, (node.value as WorkingSet<*, *>).uuid)
            .getOrNull()
            ?.clone(workingSetConfigClass)
            ?: return
        service<IdeFocusManager>()
          .runOnOwnContext(DataContext.EMPTY_CONTEXT) {
            dialogCreator(ConfigService.getService().crudable, workingSetConfig)
              .apply {
                if (showAndGet()) {
                  val dialogState = state
                  ConfigService.getService().crudable.update(dialogState.workingSetConfig)
                }
              }
          }
      }

      else -> return
    }
  }

  /**
   * Show the action in context menu only if it is:
   * 1. the appropriate explorer view class
   * 2. there is a single selected working set node of the appropriate explorer view type
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView(explorerViewClass) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = selected.size == 1 && (selected[0].node::class.java == workingSetNodeClass)
  }

}
