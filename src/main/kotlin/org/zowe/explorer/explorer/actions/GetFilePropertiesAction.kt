/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.content.synchronizer.*
import org.zowe.explorer.dataops.operations.UssChangeModeOperation
import org.zowe.explorer.dataops.operations.UssChangeModeParams
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.utils.clone
import org.zowe.explorer.utils.isBeingEditingNow
import org.zowe.explorer.utils.runWriteActionInEdtAndWait
import org.zowe.explorer.utils.updateFileTag
import org.zowe.kotlinsdk.ChangeMode

/**
 * Action for displaying properties of files on UI in dialog by clicking item in explorer context menu.
 * @author Valiantsin Krus.
 */
class GetFilePropertiesAction : AnAction() {

  /** Shows dialog with properties depending on type of the file selected by user. */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    val node = view.mySelectedNodesData.getOrNull(0)?.node
    if (node is ExplorerUnitTreeNodeBase<*, *>) {
      val virtualFile = node.virtualFile
      val connectionConfig = node.unit.connectionConfig ?: return
      if (virtualFile != null) {
        val dataOpsManager = node.explorer.componentManager.service<DataOpsManager>()
        when (val attributes = dataOpsManager.tryToGetAttributes(virtualFile)) {
          is RemoteDatasetAttributes -> {
            val dialog = DatasetPropertiesDialog(e.project, DatasetState(attributes))
            dialog.showAndGet()
          }

          is RemoteUssAttributes -> {
            val oldCharset = attributes.charset
            val initFileMode = attributes.fileMode?.clone()
            val dialog = UssFilePropertiesDialog(e.project, UssFileState(attributes, virtualFile.isBeingEditingNow()))
            if (dialog.showAndGet()) {
              if (attributes.fileMode?.owner != initFileMode?.owner || attributes.fileMode?.group != initFileMode?.group || attributes.fileMode?.all != initFileMode?.all) {
                runBackgroundableTask(
                  title = "Changing file mode on ${attributes.path}",
                  project = e.project,
                  cancellable = true
                ) {
                  if (attributes.fileMode != null) {
                    runCatching {
                      dataOpsManager.performOperation(
                        operation = UssChangeModeOperation(
                          request = UssChangeModeParams(ChangeMode(mode = attributes.fileMode), attributes.path),
                          connectionConfig = connectionConfig
                        ),
                        progressIndicator = it
                      )
                    }
                      .onSuccess {
                        node.parent?.cleanCacheIfPossible(cleanBatchedQuery = false)
                      }
                      .onFailure { t ->
                        view.explorer.reportThrowable(t, e.project)
                      }
                  }
                }
              }
              val newAttributes = dialog.state.ussAttributes
              if (!virtualFile.isDirectory && oldCharset != newAttributes.charset) {
                val contentEncodingMode = if (!virtualFile.isWritable) {
                  showReloadCancelDialog(virtualFile.name, newAttributes.charset.name(), e.project)
                } else {
                  showReloadConvertCancelDialog(virtualFile.name, newAttributes.charset.name(), e.project)
                }
                if (contentEncodingMode == null) {
                  attributes.charset = oldCharset
                } else {
                  val syncProvider = DocumentedSyncProvider(virtualFile)
                  updateFileTag(newAttributes)
                  if (contentEncodingMode == ContentEncodingMode.RELOAD) {
                    val contentSynchronizer =
                      service<DataOpsManager>().getContentSynchronizer(virtualFile)
                    runWriteActionInEdtAndWait {
                      syncProvider.saveDocument()
                      contentSynchronizer?.synchronizeWithRemote(syncProvider = syncProvider, forceReload = true)
                    }
                  }
                  changeFileEncodingTo(virtualFile, newAttributes.charset)
                }
              }
            }
          }

          is RemoteMemberAttributes -> {
            val dialog = MemberPropertiesDialog(e.project, MemberState(attributes))
            dialog.showAndGet()
          }
        }
      }
    }

  }

  /** Action is available in all time and not only after indexing process will finish. */
  override fun isDumbAware(): Boolean {
    return true
  }

  /** Shows action only for datasets (sequential and pds), for uss files and for uss directories. */
  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val node = selected.getOrNull(0)?.node
    e.presentation.isVisible = selected.size == 1
      && (node is UssFileNode || node is FileLikeDatasetNode || node is LibraryNode || node is UssDirNode)
  }
}
