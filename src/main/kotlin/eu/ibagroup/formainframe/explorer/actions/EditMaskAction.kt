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

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.MaskState
import eu.ibagroup.formainframe.config.ws.MaskStateWithWS
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.AddOrEditMaskDialog
import eu.ibagroup.formainframe.explorer.ui.DSMaskNode
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.explorer.ui.UssDirNode
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.utils.MaskType
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey

/**
 * Class which represents an "Edit" action.
 * The action is shown and triggered only on [DSMaskNode] and [UssDirNode] (a USS mask) node types
 */
class EditMaskAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Edit changed dataset mask. Will remove the mask from dataset masks list and add it to USS paths list
   * if the mask type changed
   * @param initialName the initial mask name
   * @param wsConfToUpdate the working set config the mask belongs to, that should be updated
   * @param changedMaskState the changed mask state with new parameters inside
   * @return [FilesWorkingSetConfig]
   */
  private fun editChangedDSMask(
    initialName: String,
    wsConfToUpdate: FilesWorkingSetConfig,
    changedMaskState: MaskStateWithWS
  ): FilesWorkingSetConfig {
    val maskToChange = wsConfToUpdate.dsMasks.filter { it.mask == initialName }[0]
    if (changedMaskState.type == MaskType.USS) {
      wsConfToUpdate.dsMasks.remove(maskToChange)
      wsConfToUpdate.ussPaths.add(UssPath(changedMaskState.mask))
    } else {
      wsConfToUpdate.dsMasks.filter { it.mask == initialName }[0].mask = changedMaskState.mask.uppercase()
    }
    return wsConfToUpdate
  }

  /**
   * Edit changed USS path. Will remove the mask from USS paths list and add it to dataset masks list
   * if the mask type changed
   * @param initialName the initial mask name
   * @param wsConfToUpdate the working set config the mask belongs to, that should be updated
   * @param changedMaskState the changed mask state with new parameters inside
   * @return [FilesWorkingSetConfig]
   */
  private fun editChangedUssPath(
    initialName: String,
    wsConfToUpdate: FilesWorkingSetConfig,
    changedMaskState: MaskStateWithWS
  ): FilesWorkingSetConfig {
    val maskToChange = wsConfToUpdate.ussPaths.filter { it.path == initialName }[0]
    if (changedMaskState.type == MaskType.ZOS) {
      wsConfToUpdate.ussPaths.remove(maskToChange)
      wsConfToUpdate.dsMasks.add(DSMask(changedMaskState.mask.uppercase(), mutableListOf(), ""))
    } else {
      wsConfToUpdate.ussPaths.filter { it.path == initialName }[0].path = changedMaskState.mask
    }
    return wsConfToUpdate
  }

  /**
   * Run "Edit" action on the selected element.
   * Runs only on [DSMaskNode] and [UssDirNode] (a USS mask) node types
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val selectedNodeData = view.mySelectedNodesData[0]
    val node = selectedNodeData.node

    if (node is DSMaskNode || (node is UssDirNode && node.isUssMask)) {
      val parentWS = node.parent?.value as FilesWorkingSet
      var wsConfToUpdate = ConfigService.getService().crudable
        .getByUniqueKey<FilesWorkingSetConfig>(parentWS.uuid)
        ?.clone()
      if (wsConfToUpdate != null) {
        val initialState =
          if (node is DSMaskNode) MaskState(mask = node.value.mask, type = MaskType.ZOS)
          else MaskState(mask = (node as UssDirNode).value.path, type = MaskType.USS)
        val initialStateWithWS = MaskStateWithWS(initialState, parentWS)
        val dialog = AddOrEditMaskDialog(e.project, "Edit Mask", parentWS.connectionConfig, initialStateWithWS)
        if (dialog.showAndGet()) {
          val changedMaskState = dialog.state
          wsConfToUpdate =
            if (node is DSMaskNode) editChangedDSMask(initialState.mask, wsConfToUpdate, changedMaskState)
            else editChangedUssPath(initialState.mask, wsConfToUpdate, changedMaskState)

          ConfigService.getService().crudable.update(wsConfToUpdate)
        }
      }
    }
  }

  /**
   * Make "Edit" action visible if a context menu is called.
   * The "Edit" action will be visible if:
   * 1. A context menu is triggered in a [FileExplorerView]
   * 2. Selected only one node
   * 3. The node is a [DSMaskNode], [UssDirNode] (a USS mask)
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodesData = view.mySelectedNodesData
    if (selectedNodesData.size != 1) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val node = selectedNodesData[0].node
    if (node !is DSMaskNode && (node !is UssDirNode || !node.isUssMask)) {
      e.presentation.isEnabledAndVisible = false
      return
    }
  }

  /**
   * Determines if an action is dumb aware
   */
  override fun isDumbAware(): Boolean {
    return true
  }
}
