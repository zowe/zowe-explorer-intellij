/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */
package eu.ibagroup.formainframe.utils

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.showYesNoDialog
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.JesWorkingSetImpl
import eu.ibagroup.formainframe.explorer.ui.*

/**
 * Function is used to perform units deletion based on current selection
 * Function accepts only:
 *    Files Working Set nodes
 *    Dataset Mask nodes
 *    Uss Directory Root nodes (isUssMask = true)
 *    JES Working Set nodes
 *    JES Filter nodes
 * @param project
 * @param fileExplorerView
 * @param jesExplorerView
 * @receiver List of filtered nodes by type, e.g. List<DSMaskNode> dsMasksToDelete
 */
fun <T : ExplorerTreeNode<*, *>> List<T>.performUnitsDeletionBasedOnSelection(
  project: Project?,
  fileExplorerView: FileExplorerView?,
  jesExplorerView: JesExplorerView?
) {
  var unitTypes = when (first()) {
    is FilesWorkingSetNode -> "File Working Set(s)"
    is DSMaskNode -> "Dataset Mask(s)"
    is UssDirNode -> "Uss Root Path(s)"
    is JesWsNode -> "JES Working Set(s)"
    else -> "Job Filter(s)"
  }
  // Change the header and body of the dialog in case both JesWsNode's and JesFilter's are present in receiver list
  unitTypes =
    if (this.filterIsInstance<JesWsNode>().isNotEmpty() && this.filterIsInstance<JesWsNode>().size != this.size)
      "Jes Working Set(s) and Jes Filter(s)"
    else unitTypes
  
  if (showYesNoDialog(
      title = "Confirm $unitTypes Deletion",
      message = "Do you want to delete selected $unitTypes from config? Note: all data under it(them) will be untouched",
      project = project,
      icon = AllIcons.General.QuestionDialog
    )
  ) {
    forEach {
      when (val node: T = it) {
        is FilesWorkingSetNode -> fileExplorerView?.explorer?.disposeUnit(node.unit as FilesWorkingSet)
        is DSMaskNode, is UssDirNode -> {
          (node as FileFetchNode<*,*,*,*,*,*>).cleanCache(
            recursively = true,
            cleanFetchProviderCache = true,
            cleanBatchedQuery = true,
            sendTopic = false
          )
        }
      }
      when (val node: T = it) {
        is DSMaskNode -> node.unit.removeMask(node.value)
        is UssDirNode -> node.unit.removeUssPath(node.value)
        is JesWsNode -> jesExplorerView?.explorer?.disposeUnit(node.unit as JesWorkingSetImpl)
        is JesFilterNode -> node.unit.removeFilter(node.value)
      }
    }
  }
}