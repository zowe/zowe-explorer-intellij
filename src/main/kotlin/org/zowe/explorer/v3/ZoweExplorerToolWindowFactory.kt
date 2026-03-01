/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService

// TODO: doc
class ZoweExplorerToolWindowFactory : ToolWindowFactory, DumbAware {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val contentFactory = ContentFactory.getInstance()

    val filesExplorerComponent = ExplorerTreeComponentService.getService().getFilesExplorerComponent(project)
    val filesExplorerContent = contentFactory.createContent(
      filesExplorerComponent.initExplorerTreeComponent(),
      filesExplorerComponent.explorerName,
      filesExplorerComponent.isLockable
    )
    toolWindow.contentManager.addContent(filesExplorerContent)

    val jesExplorerComponent = ExplorerTreeComponentService.getService().getJesExplorerComponent(project)
    val jesExplorerContent = contentFactory.createContent(
      jesExplorerComponent.initExplorerTreeComponent(),
      jesExplorerComponent.explorerName,
      jesExplorerComponent.isLockable
    )
    toolWindow.contentManager.addContent(jesExplorerContent)

    val tsoSessionsComponent = ExplorerTreeComponentService.getService().getTsoSessionsComponent(project)
    val tsoSessionsContent = contentFactory.createContent(
      tsoSessionsComponent.initExplorerTreeComponent(),
      tsoSessionsComponent.explorerName,
      tsoSessionsComponent.isLockable
    )
    toolWindow.contentManager.addContent(tsoSessionsContent)
  }

  override fun init(toolWindow: ToolWindow) {
    // TODO: define what to do with it
//    subscribe(
//      project = toolWindow.project,
//      topic = AutoSyncFileListener.AUTO_SYNC_FILE,
//      handler = object : AutoSyncFileListener {
//        override fun sync(file: VirtualFile) {
//          val dataOpsManager = DataOpsManager.getService()
//          if (dataOpsManager.isSyncSupported(file)) {
//            val contentSynchronizer = dataOpsManager.getContentSynchronizer(file) ?: return
//            runBackgroundableTask("Synchronizing file ${file.name} with mainframe") { indicator ->
//              val syncProvider = DocumentedSyncProvider(file)
//              runInEdtAndWait { syncProvider.saveDocument() }
//              contentSynchronizer.synchronizeWithRemote(syncProvider, indicator)
//            }
//          }
//        }
//      })
  }
}
