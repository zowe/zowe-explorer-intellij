/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.explorer.ui.ExplorerTreeView
import org.zowe.explorer.explorer.ui.JesExplorerRootNode
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.utils.sendTopic
import javax.swing.JComponent
import kotlin.concurrent.withLock


/** Factory to register [JesExplorerContentProvider] in Intellij IoC container. */
class JesExplorerContentProviderFactory : ExplorerContentProviderFactory<ConnectionConfig, JesExplorer>() {
  override fun buildComponent(): ExplorerContentProvider<ConnectionConfig, JesExplorer> = JesExplorerContentProvider()
}

/** Class to provide content for JES Explorer */
class JesExplorerContentProvider : ExplorerContentProviderBase<ConnectionConfig, JesExplorer>(
  contextMenu = ActionManager.getInstance().getAction("org.zowe.explorer.actions.JESContextMenuGroup") as ActionGroup
) {

  override val explorer: JesExplorer = UIComponentManager.INSTANCE.getExplorer(JesExplorer::class.java)
  override val displayName: String = "JES Explorer"
  override val isLockable: Boolean = false
  override val actionGroup: ActionGroup = ActionManager.getInstance().getAction("org.zowe.explorer.actions.JESActionBarGroup") as ActionGroup
  override val place: String = "JES Explorer"
  private val jesExplorerViews = mutableMapOf<Project, JesExplorerView>()

  override fun getExplorerView(project: Project): ExplorerTreeView<*, *, *>?  {
    return jesExplorerViews[project]
  }

  /**
   * Build the JES explorer content vertical panel
   * @param parentDisposable the parent disposable to register the component to be disposed
   * @param project the project where to build the panel
   */
  override fun buildContent(parentDisposable: Disposable, project: Project): JComponent {
    return JesExplorerView(
      explorer as Explorer<ConnectionConfig, JesWorkingSetImpl>,
      project,
      parentDisposable,
      contextMenu,
      { e, p, t ->
        JesExplorerRootNode(e, p, t)
      }) {
      lock.withLock {
        val previousState = filesToCut.toList()
        filesToCut = it
        sendTopic(CutBufferListener.CUT_BUFFER_CHANGES, explorer.componentManager)
          .onUpdate(previousState, it)
      }
    }.also {
      jesExplorerViews[project] = it
    }
  }
}
