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

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.SettingsProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.ui.SimpleTextAttributes
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.dataops.content.synchronizer.SaveStrategy
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.UIComponentManager
import eu.ibagroup.formainframe.utils.isBeingEditingNow
import eu.ibagroup.formainframe.utils.runWriteActionInEdtAndWait
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import javax.swing.tree.TreePath

/** Base class to implement the basic interactions with an explorer node */
abstract class ExplorerTreeNode<Connection : ConnectionConfigBase, Value : Any>(
  value: Value,
  project: Project,
  val parent: ExplorerTreeNode<Connection, *>?,
  val explorer: Explorer<Connection, *>,
  protected val treeStructure: ExplorerTreeStructureBase
) : AbstractTreeNode<Value>(project, value), SettingsProvider {

  var navigating: Boolean = false

  open fun init() {
    treeStructure.registerNode(this)
  }

  init {
    @Suppress("LeakingThis")
    init()
  }

  private val contentProvider = UIComponentManager.INSTANCE.getExplorerContentProvider(explorer::class.java)

  private val descriptor: OpenFileDescriptor?
    get() {
      return OpenFileDescriptor(notNullProject, virtualFile ?: return null)
    }

  public override fun getVirtualFile(): MFVirtualFile? {
    return null
  }

  val notNullProject = project

  override fun getSettings(): ViewSettings {
    return treeStructure
  }

  /**
   * Set the node's title as a regular colored text. If the node's file is in cut buffer,
   * the text's color will be gray
   * @param text the text to set for the node
   * @param presentationData the node to set the colored text to
   */
  protected fun updateNodeTitleUsingCutBuffer(text: String, presentationData: PresentationData) {
    val file = virtualFile ?: return
    val textAttributes = if (contentProvider?.isFileInCutBuffer(file) == true) {
      SimpleTextAttributes.GRAYED_ATTRIBUTES
    } else {
      SimpleTextAttributes.REGULAR_ATTRIBUTES
    }
    presentationData.addText(text, textAttributes)
  }

  override fun navigate(requestFocus: Boolean) {
    val file = virtualFile ?: return
    descriptor?.let { fileDescriptor ->
      if (!file.isDirectory) {
        val dataOpsManager = explorer.componentManager.service<DataOpsManager>()
        val contentSynchronizer = dataOpsManager.getContentSynchronizer(file) ?: return
        val doSync = file.isReadable || showYesNoDialog(
          title = "File ${file.name} is not readable",
          message = "Do you want to try open it anyway?",
          project = project,
          icon = AllIcons.General.WarningDialog
        )
        runBackgroundableTask("Navigating to ${file.name}") { indicator ->
          if (doSync) {
            val onThrowableHandler: (Throwable) -> Unit = {
              if (it.message?.contains("Client is not authorized for file access") == true) {
                invokeLater {
                  Messages.showDialog(
                    project,
                    "You do not have permissions to read this file",
                    "Error While Opening File ${file.name}",
                    arrayOf("Ok"),
                    0,
                    AllIcons.General.ErrorDialog
                  )
                }
              } else {
                DocumentedSyncProvider.defaultOnThrowableHandler(file, it)
              }
            }
            val onSyncSuccessHandler: () -> Unit = {
              dataOpsManager.tryToGetAttributes(file)?.let { attributes ->
                service<AnalyticsService>().trackAnalyticsEvent(FileEvent(attributes, FileAction.OPEN))
              }
              runWriteActionInEdtAndWait {
                fileDescriptor.navigate(requestFocus)
              }
            }
            val syncProvider =
              DocumentedSyncProvider(
                file = file,
                saveStrategy = SaveStrategy.syncOnOpen(project),
                onThrowableHandler = onThrowableHandler,
                onSyncSuccessHandler = onSyncSuccessHandler
              )
            if (!file.isBeingEditingNow()) {
              this.navigating = true
              this.update()
              runCatching {
                contentSynchronizer.synchronizeWithRemote(syncProvider, indicator)
              }.also {
                this.navigating = false
                this.update()
              }
            } else {
              runCatching {
                invokeLater {
                  FileEditorManager.getInstance(project).openFile(file, true)
                }
              }
            }
          }
        }
      }
    }
  }

  override fun canNavigate(): Boolean {
    return descriptor?.canNavigate() ?: super.canNavigate()
  }

  override fun canNavigateToSource(): Boolean {
    return descriptor?.canNavigateToSource() ?: super.canNavigateToSource()
  }

  private val pathList: List<ExplorerTreeNode<Connection, *>>
    get() = if (parent != null) {
      parent.pathList + this
    } else {
      listOf(this)
    }

  val path: TreePath
    get() = TreePath(pathList.toTypedArray())
}
