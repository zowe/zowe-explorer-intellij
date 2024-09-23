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

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.IconUtil
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.fetch.UssQuery
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.utils.clearAndMergeWith
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * Add a slash to the end of the USS path if it is needed
 * @param ussPath the path to modify
 */
private fun withSlashIfNeeded(ussPath: UssPath): String {
  return if (ussPath.path == "/") {
    ussPath.path
  } else {
    ussPath.path + "/"
  }
}

/** USS directory representation in the explorer tree */
class UssDirNode(
  ussPath: UssPath,
  project: Project,
  parent: ExplorerTreeNode<ConnectionConfig, *>,
  workingSet: FilesWorkingSet,
  treeStructure: ExplorerTreeStructureBase,
  private var vFile: MFVirtualFile? = null,
  private val isRootNode: Boolean = false,
  override val currentSortQueryKeysList: List<SortQueryKeys> = mutableListOf(
    SortQueryKeys.FILE_MODIFICATION_DATE,
    SortQueryKeys.ASCENDING
  ),
  override val sortedNodes: List<AbstractTreeNode<*>> = mutableListOf()
) : RemoteMFFileFetchNode<ConnectionConfig, UssPath, UssQuery, FilesWorkingSet>(
  ussPath, project, parent, workingSet, treeStructure
), UssNode, RefreshableNode {

  override fun init() {}

  init {
    super.init()
  }

  val isUssMask = vFile == null

  override val query: RemoteQuery<ConnectionConfig, UssQuery, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig
      return if (connectionConfig != null) {
        UnitRemoteQueryImpl(UssQuery(value.path), connectionConfig)
      } else null
    }

  private val attributesService
    get() = DataOpsManager.getService().getAttributesService<RemoteUssAttributes, MFVirtualFile>()

  /** Transform the collection of mainframe virtual files to the list of USS children nodes */
  override fun Collection<MFVirtualFile>.toChildrenNodes(): List<AbstractTreeNode<*>> {
    return find { attributesService.getAttributes(it)?.path == value.path }
      ?.also { vFile = it }
      ?.children
      ?.map {
        if (it.isDirectory) {
          UssDirNode(
            UssPath(withSlashIfNeeded(value) + it.name),
            notNullProject,
            this@UssDirNode,
            unit,
            treeStructure,
            it
          )
        } else {
          UssFileNode(it, notNullProject, this@UssDirNode, unit, treeStructure)
        }
      }?.let { sortChildrenNodes(it, currentSortQueryKeysList) } ?: listOf()
  }

  override val requestClass = UssQuery::class.java

  override fun makeFetchTaskTitle(query: RemoteQuery<ConnectionConfig, UssQuery, Unit>): String {
    return "Fetching USS listings for ${query.request.path}"
  }

  /**
   * Update the USS node icon with the appropriate one
   * @param presentation the node presentation to set a new icon
   */
  override fun update(presentation: PresentationData) {
    val icon = when {
      isRootNode -> {
        AllIcons.Nodes.Module
      }

      vFile != null -> {
        IconUtil.getIcon(vFile!!, 0, project)
      }

      else -> {
        AllIcons.Nodes.Folder
      }
    }
    val text = when {
      isRootNode -> {
        value.path
      }

      vFile != null -> {
        vFile!!.presentableName
      }

      else -> {
        value.path.split("/").last()
      }
    }
    presentation.setIcon(icon)
    if (vFile != null) {
      updateNodeTitleUsingCutBuffer(text, presentation)
    } else {
      presentation.addText(text, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
    if (isRootNode) {
      updateRefreshDateAndTime(presentation)
    }
  }

  override fun getVirtualFile(): MFVirtualFile? {
    return vFile
  }

  override fun <Node : AbstractTreeNode<*>> sortChildrenNodes(
    childrenNodes: List<Node>,
    sortKeys: List<SortQueryKeys>
  ): List<Node> {
    if (sortKeys.contains(SortQueryKeys.FILE_NAME)) {
      if (sortKeys.contains(SortQueryKeys.ASCENDING)) {
        return childrenNodes.sortedBy {
          when (it) {
            is UssDirNode -> it.vFile?.filenameInternal
            is UssFileNode -> it.virtualFile.filenameInternal
            else -> null
          }
        }.also {
          sortedNodes.clearAndMergeWith(it)
        }
      } else {
        return childrenNodes.sortedByDescending {
          when (it) {
            is UssDirNode -> it.vFile?.filenameInternal
            is UssFileNode -> it.virtualFile.filenameInternal
            else -> null
          }
        }.also {
          sortedNodes.clearAndMergeWith(it)
        }
      }
    } else if (sortKeys.contains(SortQueryKeys.FILE_TYPE)) {
      val listToReturn = mutableListOf<Node>()
      val dirs = mutableListOf<Node>()
      val files = mutableListOf<Node>()
      val sortedDirs: List<Node>
      val sortedFiles: List<Node>
      childrenNodes.forEach {
        when (it) {
          is UssDirNode -> dirs.add(it)
          is UssFileNode -> files.add(it)
        }
      }
      return if (sortKeys.contains(SortQueryKeys.ASCENDING)) {
        sortedDirs = dirs.sortedBy { (it as UssDirNode).vFile?.filenameInternal }
        sortedFiles = files.sortedBy { (it as UssFileNode).virtualFile.filenameInternal }
        listToReturn.addAll(sortedDirs)
        listToReturn.addAll(sortedFiles)
        listToReturn.also {
          sortedNodes.clearAndMergeWith(it)
        }
      } else {
        sortedDirs = dirs.sortedByDescending { (it as UssDirNode).vFile?.filenameInternal }
        sortedFiles = files.sortedByDescending { (it as UssFileNode).virtualFile.filenameInternal }
        listToReturn.addAll(sortedDirs)
        listToReturn.addAll(sortedFiles)
        listToReturn.also {
          sortedNodes.clearAndMergeWith(it)
        }
      }
    } else if (sortKeys.contains(SortQueryKeys.FILE_MODIFICATION_DATE)) {
      return if (sortKeys.contains(SortQueryKeys.ASCENDING)) {
        childrenNodes.sortedBy {
          modificationTimeSelector().invoke(it)
        }.also {
          sortedNodes.clearAndMergeWith(it)
        }
      } else {
        childrenNodes.sortedByDescending {
          modificationTimeSelector().invoke(it)
        }.also {
          sortedNodes.clearAndMergeWith(it)
        }
      }
    } else {
      return childrenNodes
    }
  }

  /**
   * Selector which extracts modification time for the every child node
   * @return String representation of the modification time of the virtual file
   */
  private fun modificationTimeSelector(): (AbstractTreeNode<*>) -> String? {
    return {
      when (it) {
        is UssDirNode -> {
          it.virtualFile?.let { vFile -> attributesService.getAttributes(vFile) }?.modificationTime
        }

        is UssFileNode -> {
          attributesService.getAttributes(it.virtualFile)?.modificationTime
        }

        else -> null
      }
    }
  }
}
