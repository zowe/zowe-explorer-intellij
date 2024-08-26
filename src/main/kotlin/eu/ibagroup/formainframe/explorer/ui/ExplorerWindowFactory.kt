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

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.synchronizer.AutoSyncFileListener
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.explorer.UIComponentManager
import eu.ibagroup.formainframe.utils.runInEdtAndWait
import eu.ibagroup.formainframe.utils.subscribe

/** Explorer window. This is the main class to represent the plugin */
class ExplorerWindowFactory : ToolWindowFactory, DumbAware {

  override fun isApplicable(project: Project): Boolean {
    return true
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val contentFactory = ContentFactory.getInstance()
    service<UIComponentManager>().getExplorerContentProviders()
      .forEach {
        val content = contentFactory
          .createContent(it.buildExplorerContent(toolWindow.disposable, project), it.displayName, it.isLockable)
        toolWindow.contentManager.addContent(content)
    }
  }

  override fun init(toolWindow: ToolWindow) {
    subscribe(
      project = toolWindow.project,
      topic = AutoSyncFileListener.AUTO_SYNC_FILE,
      handler = object : AutoSyncFileListener {
        override fun sync(file: VirtualFile) {
          val dataOpsManager = service<DataOpsManager>()
          if (dataOpsManager.isSyncSupported(file)) {
            val contentSynchronizer = dataOpsManager.getContentSynchronizer(file) ?: return
            runBackgroundableTask("Synchronizing file ${file.name} with mainframe") { indicator ->
              val syncProvider = DocumentedSyncProvider(file)
              runInEdtAndWait { syncProvider.saveDocument() }
              contentSynchronizer.synchronizeWithRemote(syncProvider, indicator)
            }
          }
        }
      })
  }

  override fun shouldBeAvailable(project: Project): Boolean {
    return true
  }
}
