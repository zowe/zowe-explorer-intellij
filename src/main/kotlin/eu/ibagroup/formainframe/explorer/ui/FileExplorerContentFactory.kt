package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import eu.ibagroup.formainframe.explorer.globalExplorer
import javax.swing.JComponent

class FileExplorerContentFactory : ExplorerContentFactoryBase(
  ActionManager.getInstance().getAction("eu.ibagroup.formainframe.actions.ActionBarGroup") as ActionGroup,
  "File Explorer"
) {

  override fun buildContent(parentDisposable: Disposable): JComponent {
    return FileExplorerContent(globalExplorer, parentDisposable)
  }

  override val displayName = "File Explorer"

  override val isLockable = true
}