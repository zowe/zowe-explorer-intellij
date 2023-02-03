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
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.fetch.UssQuery
import eu.ibagroup.formainframe.dataops.getAttributesService
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

  val isConfigUssPath = vFile == null

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
      } ?: listOf()
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

}
