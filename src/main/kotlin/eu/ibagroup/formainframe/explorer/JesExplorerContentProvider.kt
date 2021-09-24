/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.explorer.ui.JesExplorerRootNode
import eu.ibagroup.formainframe.explorer.ui.JesExplorerView
import eu.ibagroup.formainframe.utils.sendTopic
import javax.swing.JComponent
import kotlin.concurrent.withLock


class JesExplorerContentProviderFactory : ExplorerContentProviderFactory<JesExplorer>() {
  override fun buildComponent(): ExplorerContentProvider<JesExplorer> = JesExplorerContentProvider()
}

class JesExplorerContentProvider : ExplorerContentProviderBase<JesExplorer>() {

  override val explorer: JesExplorer = UIComponentManager.INSTANCE.getExplorer(JesExplorer::class.java)
  override val displayName: String = "JES Explorer"
  override val isLockable: Boolean = false
  override val actionGroup: ActionGroup = ActionManager.getInstance().getAction("eu.ibagroup.formainframe.actions.ActionBarGroup") as ActionGroup
  override val place: String = "JES Explorer"

  override fun buildContent(parentDisposable: Disposable, project: Project): JComponent {
    return JesExplorerView(explorer as Explorer<GlobalJesWorkingSet>, project, parentDisposable, contextMenu, { e, p, t ->
      JesExplorerRootNode(e, p, t)
    }) {
      lock.withLock {
        val previousState = filesToCut.toList()
        filesToCut = it
        sendTopic(CutBufferListener.CUT_BUFFER_CHANGES, explorer.componentManager)
          .onUpdate(previousState, it)
      }
    }
  }
}
