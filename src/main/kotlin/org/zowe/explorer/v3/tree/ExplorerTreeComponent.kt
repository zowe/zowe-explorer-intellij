/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.tree

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.jetbrains.concurrency.Promise
import org.zowe.explorer.utils.subscribe
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigChangeListener
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import javax.swing.JComponent
import javax.swing.tree.TreePath

// TODO: doc
abstract class ExplorerTreeComponent(private val project: Project) : Disposable {
  private var docListenerDisposable: Disposable = Disposer.newDisposable(this, "docListener")

  protected abstract val explorerTreeStructure: ExplorerTreeStructure
  protected val explorerStructureTreeModel by lazy { StructureTreeModel(explorerTreeStructure, this) }
  protected val explorerAsyncTreeModel by lazy { AsyncTreeModel(explorerStructureTreeModel, false, this) }
  protected abstract val explorerTreeView: ExplorerTreeView
  val explorerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  abstract val explorerName: String
  val isLockable = true

  val selectedNodes: List<ExplorerTreeNode>
    get() {
      return explorerTreeView.selectedNodes
    }

  /**
   * Finishes initialization of the component after it is fully constructed.
   *
   * This work is intentionally kept out of the constructor: it accesses
   * [explorerTreeStructure], which is an abstract member initialized by subclass
   * property initializers. Those initializers run *after* the base class constructor,
   * so touching [explorerTreeStructure] from an `init` block would read it as `null`.
   *
   * Must be called exactly once, right after the instance is created
   */
  fun postConstruct() {
    explorerTreeStructure.addEntriesFromConfig()

    subscribe(
      ZoweConfigService.CONFIG_CHANGED_TOPIC,
      ZoweConfigChangeListener { syncProfiles() },
      this
    )

    attachDocumentListener()
  }

  fun invalidateNode(node: ExplorerTreeNode, withChildren: Boolean = true): Promise<TreePath> {
    return explorerStructureTreeModel.invalidate(node, withChildren)
  }

  fun initExplorerTreeComponent(): JComponent {
    return explorerTreeView.initExplorerTreeView()
  }

  /**
   * Attaches a [DocumentListener] to the active config file's [com.intellij.openapi.editor.Document].
   * Disposes the previous listener before re-attaching so the tree tracks
   * the correct config file after a config type change
   */
  protected fun attachDocumentListener() {
    Disposer.dispose(docListenerDisposable)
    docListenerDisposable = Disposer.newDisposable(this, "docListener")
    val configService = ZoweConfigService.getService()
    val configType = configService.getSelectedConfigType(project)
    val configFile = configService.resolveConfigFile(configType, project.basePath)
    val vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(configFile.absolutePath) ?: return
    val document = runReadAction { FileDocumentManager.getInstance().getDocument(vf) } ?: return
    document.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        syncProfiles()
      }
    }, docListenerDisposable)
  }

  private fun syncProfiles() {
    explorerTreeStructure.syncEntriesWithConfig()
    invalidateNode(explorerTreeStructure.rootElement)
    attachDocumentListener()
  }

  override fun dispose() {
    explorerScope.cancel()
    explorerTreeView.dispose()
    explorerAsyncTreeModel.dispose()
    explorerStructureTreeModel.dispose()
  }
}
