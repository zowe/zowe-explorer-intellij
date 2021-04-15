package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import javax.swing.JComponent

fun interface CutBufferListener {
  fun onUpdate(previousBufferState: List<VirtualFile>, currentBufferState: List<VirtualFile>)
}

abstract class ExplorerContent(
  actionGroup: ActionGroup,
  place: String
) {

  companion object {
    @JvmField
    val CUT_BUFFER_CHANGES = Topic.create("cutBufferChanges", CutBufferListener::class.java)
  }

  private val actionToolbar = ActionManager.getInstance().createActionToolbar(place, actionGroup, true)

  abstract fun isFileInCutBuffer(virtualFile: VirtualFile): Boolean

  protected abstract fun buildContent(parentDisposable: Disposable, project: Project): JComponent

  fun buildComponent(parentDisposable: Disposable, project: Project): JComponent {
    return object : SimpleToolWindowPanel(true, true), Disposable {

      private var builtContent: JComponent? = null

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

  abstract val displayName: String

  abstract val isLockable: Boolean

}

