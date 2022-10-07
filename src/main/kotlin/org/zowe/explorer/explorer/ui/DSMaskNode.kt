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

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.containers.toMutableSmartList
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.BatchedRemoteQuery
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.vfs.MFVirtualFile
import icons.ForMainframeIcons

/**
 * File Explorer dataset mask representation.
 */
class DSMaskNode(
  dsMask: DSMask,
  project: Project,
  parent: ExplorerTreeNode<*>,
  workingSet: FilesWorkingSet,
  treeStructure: ExplorerTreeStructureBase
) : RemoteMFFileFetchNode<DSMask, DSMask, FilesWorkingSet>(
  dsMask, project, parent, workingSet, treeStructure
), MFNode, RefreshableNode {

  override fun update(presentation: PresentationData) {
    presentation.addText(value.mask, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    presentation.addText(" ${value.volser}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    presentation.setIcon(ForMainframeIcons.DatasetMask)
  }

  override val query: RemoteQuery<DSMask, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig
      return if (connectionConfig != null) {
        BatchedRemoteQuery(value, connectionConfig)
      } else null
    }

  /**
   * Returns map of children nodes (datasets and uss files).
   */
  override fun Collection<MFVirtualFile>.toChildrenNodes(): MutableList<AbstractTreeNode<*>> {
    return map {
      if (it.isDirectory) {
        LibraryNode(it, notNullProject, this@DSMaskNode, unit, treeStructure)
      } else {
        FileLikeDatasetNode(it, notNullProject, this@DSMaskNode, unit, treeStructure)
      }
    }.toMutableSmartList()
  }

  override val requestClass = DSMask::class.java

  /**
   * Makes and returns title for fetch task.
   */
  override fun makeFetchTaskTitle(query: RemoteQuery<DSMask, Unit>): String {
    return "Fetching listings for ${query.request.mask}"
  }

}
