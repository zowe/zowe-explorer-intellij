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

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.BatchedRemoteQuery
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys
import eu.ibagroup.formainframe.dataops.sort.typedSortKeys
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.utils.clearAndMergeWith
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import icons.ForMainframeIcons

/**
 * File Explorer dataset mask representation.
 */
class DSMaskNode(
  dsMask: DSMask,
  project: Project,
  parent: ExplorerTreeNode<ConnectionConfig, *>,
  workingSet: FilesWorkingSet,
  treeStructure: ExplorerTreeStructureBase,
  override val currentSortQueryKeysList: List<SortQueryKeys> = mutableListOf(
    SortQueryKeys.DATASET_MODIFICATION_DATE,
    SortQueryKeys.ASCENDING
  ),
  override val sortedNodes: List<AbstractTreeNode<*>> = mutableListOf()
) : RemoteMFFileFetchNode<ConnectionConfig, DSMask, DSMask, FilesWorkingSet>(
  dsMask, project, parent, workingSet, treeStructure
), RefreshableNode, SortableNode {

  override fun update(presentation: PresentationData) {
    presentation.addText(value.mask, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    presentation.addText(" ${value.volser}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    presentation.setIcon(ForMainframeIcons.DatasetMask)
    updateRefreshDateAndTime(presentation)
  }

  override val query: RemoteQuery<ConnectionConfig, DSMask, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig
      return if (connectionConfig != null) {
        BatchedRemoteQuery(value, connectionConfig)
      } else null
    }

  /**
   * Prepare child node instance from the provided [MFVirtualFile].
   * Creates [LibraryNode] if the MF virtual file is a directory or [FileLikeDatasetNode] otherwise
   * @param file the MF virtual file instance
   * @return the created node
   */
  fun prepareChildNodeFromMFVirtualFile(file: MFVirtualFile): ExplorerTreeNode<*, *> {
    return if (file.isDirectory) {
      LibraryNode(file, notNullProject, this@DSMaskNode, unit, treeStructure)
    } else {
      FileLikeDatasetNode(file, notNullProject, this@DSMaskNode, unit, treeStructure)
    }
  }

  /**
   * Returns map of children nodes (datasets and uss files).
   */
  override fun Collection<MFVirtualFile>.toChildrenNodes(): List<AbstractTreeNode<*>> {
    return sortChildrenNodes(
      map { prepareChildNodeFromMFVirtualFile(it) },
      currentSortQueryKeysList
    )
  }

  override val requestClass = DSMask::class.java

  /**
   * Makes and returns title for fetch task.
   */
  override fun makeFetchTaskTitle(query: RemoteQuery<ConnectionConfig, DSMask, Unit>): String {
    return "Fetching listings for ${query.request.mask}"
  }

  override fun <Node : AbstractTreeNode<*>> sortChildrenNodes(
    childrenNodes: List<Node>,
    sortKeys: List<SortQueryKeys>
  ): List<Node> {
    val listToReturn = mutableListOf<Node>()
    val psFiles = childrenNodes.filter { it is FileLikeDatasetNode }
    val libraries = childrenNodes.filter { it is LibraryNode }
    val foundSortKey = sortKeys.firstOrNull { typedSortKeys.contains(it) }
    if (foundSortKey != null && foundSortKey == SortQueryKeys.DATASET_TYPE) {
      val sortedPSFiles = performDatasetsSorting(psFiles, this@DSMaskNode, SortQueryKeys.DATASET_NAME)
      val sortedLibraries = performDatasetsSorting(libraries, this@DSMaskNode, SortQueryKeys.DATASET_NAME)
      listToReturn.addAll(sortedLibraries)
      listToReturn.addAll(sortedPSFiles)
      also { sortedNodes.clearAndMergeWith(listToReturn) }
    } else if (foundSortKey != null) {
      listToReturn.clearAndMergeWith(performDatasetsSorting(childrenNodes, this@DSMaskNode, foundSortKey))
    } else {
      listToReturn.clearAndMergeWith(childrenNodes)
    }
    return listToReturn
  }

  /**
   * Function sorts the children nodes by specified sorting key
   * @param nodes
   * @param mask
   * @param sortKey
   * @return sorted nodes by specified key
   */
  private fun <Node : AbstractTreeNode<*>> performDatasetsSorting(
    nodes: List<Node>,
    mask: DSMaskNode,
    sortKey: SortQueryKeys
  ): List<Node> {
    val sortedNodesInternal: List<Node> = if (mask.currentSortQueryKeysList.contains(SortQueryKeys.ASCENDING)) {
      nodes.sortedBy {
        selector(sortKey).invoke(it)
      }
    } else {
      nodes.sortedByDescending {
        selector(sortKey).invoke(it)
      }
    }
    return sortedNodesInternal.also { sortedNodes.clearAndMergeWith(it) }
  }

  /**
   * Selector which extracts the dataset attributes by specified sort key
   * @param key - sort key
   * @return String representation of the extracted dataset info of the virtual file
   */
  private fun selector(key: SortQueryKeys): (AbstractTreeNode<*>) -> String? {
    return {
      val datasetAttributes = when (it) {
        is FileLikeDatasetNode -> DataOpsManager.getService()
          .tryToGetAttributes(it.virtualFile) as RemoteDatasetAttributes

        is LibraryNode -> DataOpsManager.getService().tryToGetAttributes(it.virtualFile) as RemoteDatasetAttributes
        else -> null
      }
      when (key) {
        SortQueryKeys.DATASET_NAME -> datasetAttributes?.datasetInfo?.name
        SortQueryKeys.DATASET_MODIFICATION_DATE -> datasetAttributes?.datasetInfo?.lastReferenceDate
        else -> null
      }
    }
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (query?.hashCode() ?: 0)
    result = 31 * result + requestClass.hashCode()
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as DSMaskNode

    if (query != other.query) return false
    if (requestClass != other.requestClass) return false

    return true
  }

}
