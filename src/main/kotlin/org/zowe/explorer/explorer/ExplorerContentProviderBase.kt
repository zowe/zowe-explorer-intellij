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
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import org.zowe.explorer.utils.getAncestorNodes
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import kotlin.concurrent.withLock

fun interface CutBufferListener {

  companion object {
    @JvmField
    val CUT_BUFFER_CHANGES = Topic.create("cutBufferChanges", CutBufferListener::class.java)
  }

  fun onUpdate(previousBufferState: List<VirtualFile>, currentBufferState: List<VirtualFile>)
}

abstract class ExplorerContentProviderBase<E : Explorer<*>> : ExplorerContentProvider<E>  {

  abstract val actionGroup: ActionGroup
  abstract val place: String
  val contextMenu: ActionGroup =
    ActionManager.getInstance().getAction("org.zowe.explorer.actions.ContextMenuGroup") as ActionGroup


  fun buildActionToolbar() = ActionManager.getInstance().createActionToolbar(place, actionGroup, true)

  override fun buildExplorerContent(parentDisposable: Disposable, project: Project): JComponent {
    return object : SimpleToolWindowPanel(true, true), Disposable {

      private var builtContent: JComponent? = null
      private val actionToolbar = buildActionToolbar()

      init {
        Disposer.register(parentDisposable, this)
        actionToolbar.let {
          it.setTargetComponent(this)
          toolbar = it.component
        }
        setContent(buildContent(this, project).also { builtContent = it })
      }

      override fun dispose() {
        if (builtContent is Disposable) {
          (builtContent as Disposable).dispose()
        }
        builtContent = null
      }
    }
  }

  override fun isFileInCutBuffer(virtualFile: VirtualFile): Boolean {
    return lock.withLock {
      filesToCut.map { it.getAncestorNodes() }.flatten().distinct().contains(virtualFile)
    }
  }

  abstract fun buildContent(parentDisposable: Disposable, project: Project) : JComponent

  protected val lock = ReentrantLock()

  @Volatile
  protected var filesToCut = listOf<VirtualFile>()



}
