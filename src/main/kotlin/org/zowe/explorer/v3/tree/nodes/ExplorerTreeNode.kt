/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.tree.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import kotlinx.coroutines.runBlocking
import org.zowe.explorer.v3.impl.connection.ConnectionProfileRelated
import org.zowe.explorer.v3.impl.connection.ZoweConnectionService

/**
 * Base tree node for all explorer trees (Files, JES, TSO, Vault).
 * Supports navigating to [Navigable] node descriptors on double-click or Enter
 * by fetching mainframe content and opening it as a read-only file in the editor
 */
open class ExplorerTreeNode(
  var nodeDescriptor: ExplorerTreeNodeDescriptor,
  project: Project,
  parent: ExplorerTreeNode? = null
) : AbstractTreeNode<ExplorerTreeNodeDescriptor>(project, nodeDescriptor) {
  override fun isAlwaysLeaf(): Boolean {
    return nodeDescriptor.isLeaf
  }

  override fun isAlwaysExpand(): Boolean {
    return nodeDescriptor.hasExpandChevron
  }

  override fun getName(): String {
    return nodeDescriptor.displayName
  }

  override fun getChildren(): Collection<AbstractTreeNode<*>?> {
    return nodeDescriptor.getNodeChildren(this)
  }

  override fun update(presentation: PresentationData) {
    nodeDescriptor.updatePresentation(presentation)
  }

  override fun canNavigate(): Boolean {
    return nodeDescriptor is Navigable
  }

  override fun canNavigateToSource(): Boolean {
    return canNavigate()
  }

  override fun navigate(requestFocus: Boolean) {
    val navigable = nodeDescriptor as? Navigable ?: return
    val connectionRelated = nodeDescriptor as? ConnectionProfileRelated ?: return
    val proj = project ?: return
    val fileName = navigable.getFileName()

    val existingFile = FileEditorManager.getInstance(proj).openFiles.firstOrNull { it.name == fileName }
    if (existingFile != null) {
      FileEditorManager.getInstance(proj).openFile(existingFile, requestFocus)
      return
    }

    nodeDescriptor.isBusy = true
    nodeDescriptor.invalidateAssociatedNodes()

    ProgressManager.getInstance().run(object : Task.Backgroundable(proj, "Fetching $fileName...") {
      override fun run(indicator: ProgressIndicator) {
        try {
          val connectionManager = ZoweConnectionService.getService().getZoweConnectionManager(proj)
          val connection = connectionManager.produceHttpConnection(connectionRelated.connectionProfile, shouldOverrideWithEnv = true)
          val content = runBlocking {
            navigable.fetchContent(connection)
          }
          val virtualFile = LightVirtualFile(fileName, content)
          virtualFile.isWritable = false
          ApplicationManager.getApplication().invokeLater {
            nodeDescriptor.isBusy = false
            nodeDescriptor.invalidateAssociatedNodes()
            FileEditorManager.getInstance(proj).openFile(virtualFile, requestFocus)
          }
        } catch (e: Exception) {
          ApplicationManager.getApplication().invokeLater {
            nodeDescriptor.isBusy = false
            nodeDescriptor.invalidateAssociatedNodes()
          }
          throw e
        }
      }
    })
  }

  init {
    this.parent = parent
    nodeDescriptor.associateNode(this)
  }
}