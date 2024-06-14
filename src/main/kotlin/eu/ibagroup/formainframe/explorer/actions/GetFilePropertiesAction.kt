/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.runBackgroundableTask
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.operations.UssChangeModeOperation
import eu.ibagroup.formainframe.dataops.operations.UssChangeModeParams
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.*
import org.zowe.kotlinsdk.ChangeMode

/**
 * Action for displaying properties of files on UI in dialog by clicking item in explorer context menu.
 * @author Valiantsin Krus.
 */
class GetFilePropertiesAction : AnAction() {

  /** Shows dialog with properties depending on type of the file selected by user. */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val node = view.mySelectedNodesData.getOrNull(0)?.node ?: return
    if (node is ExplorerUnitTreeNodeBase<ConnectionConfig, *, out ExplorerUnit<ConnectionConfig>>) {
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
            // TODO: need to think whether this sync is necessary
            // synchronize charset from file attributes with charset from file properties
            // if (attributes.charset != virtualFile.charset) {
            //   attributes.charset = virtualFile.charset
            //   updateFileTag(attributes)
            // }
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
                    }.onFailure { t ->
                      initFileMode?.owner?.let{attributes.fileMode.owner = it}
                      initFileMode?.group?.let{attributes.fileMode.group = it}
                      initFileMode?.all?.let{attributes.fileMode.all = it}
                      view.explorer.reportThrowable(t, e.project)
                    }
                    node.parent?.cleanCacheIfPossible(cleanBatchedQuery = false)
                  }
                }
              }
              val charset = attributes.charset
              if (!virtualFile.isDirectory && oldCharset != charset) {
                val changed = changeFileEncodingAction(e.project, virtualFile, attributes, charset)
                if (!changed) {
                  attributes.charset = oldCharset
                }
              }
            } else {
              attributes.charset = oldCharset
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
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val node = selected.getOrNull(0)?.node
    e.presentation.isVisible = selected.size == 1
      && (node is UssFileNode || node is FileLikeDatasetNode || node is LibraryNode || node is UssDirNode)

    // Mark the migrated dataset properties unavailable for clicking
    if (node != null && (node is FileLikeDatasetNode || node is LibraryNode)) {
      val dataOpsManager = node.explorer.componentManager.service<DataOpsManager>()
      val datasetAttributes = node.virtualFile?.let { dataOpsManager.tryToGetAttributes(it) }
      if (datasetAttributes is RemoteDatasetAttributes && datasetAttributes.isMigrated) {
        e.presentation.isEnabled = false
      }
    }
  }
}
