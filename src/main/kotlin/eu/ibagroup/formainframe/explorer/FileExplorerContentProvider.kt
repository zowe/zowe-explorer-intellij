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

package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeView
import eu.ibagroup.formainframe.explorer.ui.FileExplorerTreeNodeRoot
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.utils.sendTopic
import javax.swing.JComponent
import kotlin.concurrent.withLock

class FileExplorerContentProviderFactory : ExplorerContentProviderFactory<ConnectionConfig, FileExplorer>() {
  override fun buildComponent() = FileExplorerContentProvider.getInstance()
}

/** Class to provide content for File Explorer */
class FileExplorerContentProvider private constructor() :
  ExplorerContentProviderBase<ConnectionConfig, FileExplorer>() {

  override val explorer: FileExplorer = UIComponentManager.getService().getExplorer(FileExplorer::class.java)
  override val displayName: String = "File Explorer"
  override val isLockable: Boolean = false
  override val actionGroup: ActionGroup =
    ActionManager.getInstance().getAction("eu.ibagroup.formainframe.actions.FilesActionBarGroup") as ActionGroup
  override val place: String = "File Explorer"
  private val fileExplorerViews = mutableMapOf<Project, FileExplorerView>()

  companion object {
    private val fileExplorerContentProvider by lazy { FileExplorerContentProvider() }
    fun getInstance(): FileExplorerContentProvider {
      return fileExplorerContentProvider
    }
  }

  override fun getExplorerView(project: Project): ExplorerTreeView<*, *, *>? {
    return fileExplorerViews[project]
  }

  /**
   * Build the file explorer content vertical panel
   * @param parentDisposable the parent disposable to register the component to be disposed
   * @param project the project where to build the panel
   */
  @Suppress("UNCHECKED_CAST")
  override fun buildContent(parentDisposable: Disposable, project: Project): JComponent {
    return FileExplorerView(
      explorer as Explorer<ConnectionConfig, FilesWorkingSet>,
      project,
      parentDisposable,
      contextMenu,
      { e, p, t ->
        FileExplorerTreeNodeRoot(e, p, t)
      }) {
      lock.withLock {
        val previousState = filesToCut.toList()
        filesToCut = it
        sendTopic(CutBufferListener.CUT_BUFFER_CHANGES, explorer.componentManager)
          .onUpdate(previousState, it)
      }
    }.also {
      fileExplorerViews[project] = it
    }
  }

}
