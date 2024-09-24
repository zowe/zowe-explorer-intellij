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

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.runBackgroundableTask
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.content.synchronizer.DocumentedSyncProvider
import org.zowe.explorer.dataops.content.synchronizer.SaveStrategy
import org.zowe.explorer.dataops.operations.jobs.BasicGetJclRecordsParams
import org.zowe.explorer.dataops.operations.jobs.GetJclRecordsOperation
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.explorer.ui.JobNode
import org.zowe.explorer.explorer.ui.getExplorerView
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.utils.runWriteActionInEdtAndWait
import org.zowe.explorer.vfs.MFVirtualFile

/**
 * Action to edit job JCL through the editor in JES explorer
 */
class EditJclAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Receives the source JCL for job
   * Creates a virtual file with the resulting contents
   * Opens the file in editor
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: return
    val selected = view.mySelectedNodesData.getOrNull(0)
    val node = selected?.node
    val project = e.project
    if (node is JobNode) {
      val connectionConfig = node.unit.connectionConfig
      if (connectionConfig != null) {
        val attributes = selected.attributes as RemoteJobAttributes
        val dataOpsManager = DataOpsManager.getService()
        runBackgroundableTask(
          title = "Get JCL records for job ${attributes.jobInfo.jobName}",
          project = project,
          cancellable = true
        ) {
          runCatching {
            val jclContentBytes = dataOpsManager.performOperation(
              operation = GetJclRecordsOperation(
                request = BasicGetJclRecordsParams(
                  jobName = attributes.jobInfo.jobName,
                  jobId = attributes.jobInfo.jobId,
                ),
                connectionConfig = connectionConfig
              ),
              progressIndicator = it
            )
            val virtualFile = node.virtualFile
            if (virtualFile != null) {
              var wasCreatedBefore = true
              var cachedFile = virtualFile.findChild(attributes.name)
              if (cachedFile == null) {
                val createdFile = virtualFile.createChildData(null, attributes.name) as MFVirtualFile
                cachedFile = createdFile
                wasCreatedBefore = false
              }
              val descriptor = e.project?.let { pr -> OpenFileDescriptor(pr, cachedFile) }
              descriptor?.let {
                val syncProvider =
                  DocumentedSyncProvider(file = cachedFile, saveStrategy = SaveStrategy.default(e.project))
                runWriteActionInEdtAndWait {
                  if (!wasCreatedBefore) {
                    syncProvider.putInitialContent(jclContentBytes)
                  } else {
                    val currentContent = syncProvider.retrieveCurrentContent()
                    if (!(currentContent contentEquals jclContentBytes)) {
                      syncProvider.loadNewContent(jclContentBytes)
                    }
                  }
                  it.navigate(true)
                }
              }
            }
          }.onFailure {
            NotificationsService.getService().notifyError(it, project)
          }
        }
      }
    }
  }

  /**
   * Makes action visible only for job context menu in JES explorer
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val node = selected.getOrNull(0)?.node
    e.presentation.isVisible = selected.size == 1 && node is JobNode
  }
}
