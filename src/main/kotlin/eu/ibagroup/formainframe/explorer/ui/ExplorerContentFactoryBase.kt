package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import javax.swing.JComponent

abstract class ExplorerContentFactoryBase(
  actionGroup: ActionGroup,
  place: String
) : ExplorerContentFactory {

  private val actionToolbar = ActionManager.getInstance().createActionToolbar(place, actionGroup, true)

  protected abstract fun buildContent(parentDisposable: Disposable, project: Project): JComponent

  override fun buildComponent(parentDisposable: Disposable, project: Project): JComponent {
    return object : SimpleToolWindowPanel(true, true), Disposable {

      private var builtContent: JComponent? = null

      init {
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

}