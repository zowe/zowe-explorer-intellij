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
import org.zowe.explorer.explorer.ui.JesExplorerRootNode
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.utils.sendTopic
import javax.swing.JComponent
import kotlin.concurrent.withLock


class JesExplorerContentProviderFactory : ExplorerContentProviderFactory<JesExplorer>() {
  override fun buildComponent(): ExplorerContentProvider<JesExplorer> = JesExplorerContentProvider()
}

class JesExplorerContentProvider : ExplorerContentProviderBase<JesExplorer>() {

  override val explorer: JesExplorer = UIComponentManager.INSTANCE.getExplorer(JesExplorer::class.java)
  override val displayName: String = "JES Explorer"
  override val isLockable: Boolean = false
  override val actionGroup: ActionGroup = ActionManager.getInstance().getAction("org.zowe.explorer.actions.JESActionBarGroup") as ActionGroup
  override val place: String = "JES Explorer"

  override fun buildContent(parentDisposable: Disposable, project: Project): JComponent {
    return JesExplorerView(explorer as Explorer<GlobalJesWorkingSet>, project, parentDisposable, contextMenu, { e, p, t ->
      JesExplorerRootNode(e, p, t)
    }) {
      lock.withLock {
        val previousState = filesToCut.toList()
        filesToCut = it
        sendTopic(CutBufferListener.CUT_BUFFER_CHANGES, explorer.componentManager)
          .onUpdate(previousState, it)
      }
    }
  }
}
