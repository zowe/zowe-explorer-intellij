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

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.BatchedRemoteQuery
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.fetch.LibraryQuery
import org.zowe.explorer.dataops.getAttributesService
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.utils.service
import org.zowe.explorer.vfs.MFVirtualFile
import icons.ForMainframeIcons

/**
 * TODO: merge as LibraryNode and FileLikeDatasetNode function
 * Get VOLSER for file if it is applicable for the file type
 * @param dataOpsManager the data ops manager to get file attributes through
 * @param file the virtual file to get attributes for
 * @return the VOLSER or null if it is not a dataset (if it is a member)
 */
fun getVolserIfPresent(dataOpsManager: DataOpsManager, file: MFVirtualFile): String? {
  val attributesService = dataOpsManager.getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
  return attributesService.getAttributes(file)?.volser?.let { " $it" }
}

/** Dataset node presentation implementation */
class LibraryNode(
  library: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNode<ConnectionConfig, *>,
  workingSet: FilesWorkingSet,
  treeStructure: ExplorerTreeStructureBase
) : RemoteMFFileFetchNode<ConnectionConfig, MFVirtualFile, LibraryQuery, FilesWorkingSet>(
  library, project, parent, workingSet, treeStructure
), RefreshableNode {

  override val query: RemoteQuery<ConnectionConfig, LibraryQuery, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig

      return if (connectionConfig != null) {
        BatchedRemoteQuery(LibraryQuery(value), connectionConfig)
      } else null
    }

  override fun Collection<MFVirtualFile>.toChildrenNodes(): List<AbstractTreeNode<*>> {
    return map { FileLikeDatasetNode(it, notNullProject, this@LibraryNode, unit, treeStructure) }
  }

  override val requestClass = LibraryQuery::class.java

  override fun update(presentation: PresentationData) {
    presentation.setIcon(if (value.isDirectory) ForMainframeIcons.DatasetMask else AllIcons.FileTypes.Any_type)
    updateNodeTitleUsingCutBuffer(value.presentableName, presentation)
    val dataOpsManager = explorer.componentManager.service<DataOpsManager>()
    getVolserIfPresent(dataOpsManager, value)
      ?.let { presentation.addText(it, SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES) }
  }

  override fun getVirtualFile(): MFVirtualFile {
    return value
  }

  override fun makeFetchTaskTitle(query: RemoteQuery<ConnectionConfig, LibraryQuery, Unit>): String {
    return "Fetching members for ${query.request.library.name}"
  }
}
