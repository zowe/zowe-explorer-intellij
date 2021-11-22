/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.explorer.ui.FileExplorerTreeNodeRoot
import eu.ibagroup.formainframe.explorer.ui.GlobalFileExplorerView
import eu.ibagroup.formainframe.utils.sendTopic
import javax.swing.JComponent
import kotlin.concurrent.withLock

class FileExplorerContentProviderFactory : ExplorerContentProviderFactory<GlobalExplorer>() {
  override fun buildComponent(): ExplorerContentProvider<GlobalExplorer> = FileExplorerContentProvider()
}

class FileExplorerContentProvider : ExplorerContentProviderBase<GlobalExplorer>() {

  override val explorer: GlobalExplorer = UIComponentManager.INSTANCE.getExplorer(GlobalExplorer::class.java)
  override val displayName: String = "File Explorer"
  override val isLockable: Boolean = false
  override val actionGroup: ActionGroup =
    ActionManager.getInstance().getAction("eu.ibagroup.formainframe.actions.FilesActionBarGroup") as ActionGroup
  override val place: String = "File Explorer"

  @Suppress("UNCHECKED_CAST")
  override fun buildContent(parentDisposable: Disposable, project: Project): JComponent {
    return GlobalFileExplorerView(explorer as Explorer<FilesWorkingSet>, project, parentDisposable, contextMenu, { e, p, t ->
      FileExplorerTreeNodeRoot(e, p, t)
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
