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

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.util.IconUtil
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.dataops.*
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.fetch.UssQuery
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.utils.service
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
  private val isRootNode: Boolean = false
) : RemoteMFFileFetchNode<ConnectionConfig, UssPath, UssQuery, FilesWorkingSet>(
  ussPath, project, parent, workingSet, treeStructure
), UssNode, RefreshableNode {

  override fun init() {}

  init {
    super.init()
  }

  /** Field which holds and identifies the current sort keys for particular Node, be default Nodes are sorted by Data in Ascending order */
  var currentSortQueryKeysList = mutableListOf(SortQueryKeys.DATE, SortQueryKeys.ASCENDING)

  /** Stores the sorted nodes for particular Node */
  private var sortedNodes: List<AbstractTreeNode<*>> = mutableListOf()

  val isUssMask = vFile == null

  override val query: RemoteQuery<ConnectionConfig, UssQuery, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig
      return if (connectionConfig != null) {
        UnitRemoteQueryImpl(UssQuery(value.path), connectionConfig)
      } else null
    }

  private val attributesService
    get() = explorer.componentManager.service<DataOpsManager>()
      .getAttributesService<RemoteUssAttributes, MFVirtualFile>()

  /** Transform the collection of mainframe virtual files to the list of USS children nodes */
  override fun Collection<MFVirtualFile>.toChildrenNodes(): List<AbstractTreeNode<*>> {
    return find { attributesService.getAttributes(it)?.path == value.path }
      ?.also {
        vFile = it
        treeStructure.registerNode(this@UssDirNode)
      }
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
      }?.sortChildrenNodes(currentSortQueryKeysList) ?: listOf()
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
      updateMainTitleUsingCutBuffer(text, presentation)
    } else {
      presentation.presentableText = text
    }
  }

  override fun getVirtualFile(): MFVirtualFile? {
    return vFile
  }

  /**
   * Method sorts the children nodes regarding the sort keys are currently enabled
   * @param sortKeys - Sort keys to check
   * @return list of sorted nodes
   */
  override fun List<AbstractTreeNode<*>>.sortChildrenNodes(sortKeys: List<SortQueryKeys>): List<AbstractTreeNode<*>> {
    if (sortKeys.contains(SortQueryKeys.NAME)) {
      if (sortKeys.contains(SortQueryKeys.ASCENDING)) {
        return this.sortedBy {
          when (it) {
            is UssDirNode -> it.vFile?.filenameInternal
            is UssFileNode -> it.virtualFile.filenameInternal
            else -> null
          }
        }.also { sortedNodes = it }
      } else {
        return this.sortedByDescending {
          when (it) {
            is UssDirNode -> it.vFile?.filenameInternal
            is UssFileNode -> it.virtualFile.filenameInternal
            else -> null
          }
        }.also { sortedNodes = it }
      }
    } else if (sortKeys.contains(SortQueryKeys.TYPE)) {
      val listToReturn = mutableListOf<AbstractTreeNode<*>>()
      val dirs = mutableListOf<UssDirNode>()
      val files = mutableListOf<UssFileNode>()
      val sortedDirs: List<UssDirNode>
      val sortedFiles: List<UssFileNode>
      this.forEach {
        when (it) {
          is UssDirNode -> dirs.add(it)
          is UssFileNode -> files.add(it)
        }
      }
      return if (sortKeys.contains(SortQueryKeys.ASCENDING)) {
        sortedDirs = dirs.sortedBy { it.vFile?.filenameInternal }
        sortedFiles = files.sortedBy { it.virtualFile.filenameInternal }
        listToReturn.addAll(sortedDirs)
        listToReturn.addAll(sortedFiles)
        listToReturn.also { sortedNodes = it }
      } else {
        sortedDirs = dirs.sortedByDescending { it.vFile?.filenameInternal }
        sortedFiles = files.sortedByDescending { it.virtualFile.filenameInternal }
        listToReturn.addAll(sortedDirs)
        listToReturn.addAll(sortedFiles)
        listToReturn.also { sortedNodes = it }
      }
    } else if (sortKeys.contains(SortQueryKeys.DATE)) {

      fun select(node: AbstractTreeNode<*>) : String? {
        return when (node) {
          is UssDirNode -> {
            node.virtualFile?.let { vFile -> attributesService.getAttributes(vFile) }?.modificationTime
          }
          is UssFileNode -> {
            attributesService.getAttributes(node.virtualFile)?.modificationTime
          }
          else -> null
        }
      }

      return if (sortKeys.contains(SortQueryKeys.ASCENDING)) {
        this.sortedBy {
          return@sortedBy select(it)
        }.also { sortedNodes = it }
      } else {
        this.sortedByDescending {
          return@sortedByDescending select(it)
        }.also { sortedNodes = it }
      }
    } else {
      return this
    }
  }

  fun sortChildrenForTestInternal(sortKeys: List<SortQueryKeys>): (List<AbstractTreeNode<*>>) -> List<AbstractTreeNode<*>> = { it.sortChildrenNodes(sortKeys) }
}

fun sortChildrenForTest(listToSort: List<AbstractTreeNode<*>>, sortKeys: List<SortQueryKeys>): UssDirNode.() -> List<AbstractTreeNode<*>>
    = { sortChildrenForTestInternal(sortKeys).invoke(listToSort) }
