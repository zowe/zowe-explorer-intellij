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
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.getAncestorNodes
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import kotlin.concurrent.withLock

val ACTION_TOOLBAR = DataKey.create<ActionToolbar>("actionToolbar")

fun interface CutBufferListener {

  companion object {
    @JvmField
    val CUT_BUFFER_CHANGES = Topic.create("cutBufferChanges", CutBufferListener::class.java)
  }

  fun onUpdate(previousBufferState: List<VirtualFile>, currentBufferState: List<VirtualFile>)
}

/** Base implementation of explorer content provider */
abstract class ExplorerContentProviderBase<Connection: ConnectionConfigBase, E : Explorer<Connection, *>>(
  val contextMenu: ActionGroup = ActionManager.getInstance().getAction("eu.ibagroup.formainframe.actions.ContextMenuGroup") as ActionGroup
) : ExplorerContentProvider<Connection, E> {

  abstract val actionGroup: ActionGroup
  abstract val place: String

  /**
   * Should build action toolbar that will be located at above the explorer view.
   * @param target target component ([SimpleToolWindowPanel] by default) inside which the toolbar will be located.
   * @return action toolbar instance.
   */
  open fun buildActionToolbar(target: JComponent?): ActionToolbar {
    return ActionManager.getInstance().createActionToolbar(place, actionGroup, true).apply {
      targetComponent = target
    }
  }

  /**
   * Build the explorer content vertical panel
   * @param parentDisposable the parent disposable to register the component to be disposed
   * @param project the project where to build the panel
   */
  override fun buildExplorerContent(parentDisposable: Disposable, project: Project): JComponent {
    return object : SimpleToolWindowPanel(true, true), Disposable {

      private val actionToolbar: ActionToolbar

      private var builtContent: JComponent? = null

      init {
        Disposer.register(parentDisposable, this)
        actionToolbar = buildActionToolbar(this)
        toolbar = actionToolbar.component
        setContent(buildContent(this, project).also { builtContent = it })
      }

      override fun getData(dataId: String): Any? {
        val view = getExplorerView(project)
        return when {
          EXPLORER_VIEW.`is`(dataId) -> view
          ACTION_TOOLBAR.`is`(dataId) -> actionToolbar
          else -> null
        }
      }

      override fun dispose() {
        if (builtContent is Disposable) {
          (builtContent as Disposable).dispose()
        }
        builtContent = null
      }
    }
  }

  /**
   * Check is file in the cut buffer
   * @param virtualFile the file to check
   */
  override fun isFileInCutBuffer(virtualFile: VirtualFile): Boolean {
    return lock.withLock {
      filesToCut.distinct().contains(virtualFile)
    }
  }

  abstract fun buildContent(parentDisposable: Disposable, project: Project): JComponent

  /**
   * Get selected explorer view.
   * @param project the project that the view is in.
   */
  abstract fun getExplorerView(project: Project): ExplorerTreeView<*, *, *>?

  protected val lock = ReentrantLock()

  @Volatile
  protected var filesToCut = listOf<VirtualFile>()


}
