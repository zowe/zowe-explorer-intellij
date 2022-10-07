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
import com.intellij.util.IconUtil
import org.zowe.explorer.config.ws.UssPath
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.fetch.UssQuery
import org.zowe.explorer.dataops.getAttributesService
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.utils.service
import org.zowe.explorer.vfs.MFVirtualFile

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
  parent: ExplorerTreeNode<*>,
  workingSet: FilesWorkingSet,
  treeStructure: ExplorerTreeStructureBase,
  private var vFile: MFVirtualFile? = null,
  private val isRootNode: Boolean = false
) : RemoteMFFileFetchNode<UssPath, UssQuery, FilesWorkingSet>(
  ussPath, project, parent, workingSet, treeStructure
), UssNode, RefreshableNode {

  override fun init() {}

  init {
    super.init()
  }

  val isConfigUssPath = vFile == null

  override val query: RemoteQuery<UssQuery, Unit>?
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

  override fun makeFetchTaskTitle(query: RemoteQuery<UssQuery, Unit>): String {
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
