package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.explorer.globalExplorer
import eu.ibagroup.formainframe.utils.sendTopic
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import kotlin.concurrent.withLock

class GlobalExplorerContent : ExplorerContent(
  ActionManager.getInstance().getAction("eu.ibagroup.formainframe.actions.ActionBarGroup") as ActionGroup,
  "File Explorer"
) {

  override fun isFileInCutBuffer(virtualFile: VirtualFile): Boolean {
    return lock.withLock { filesToCut.contains(virtualFile) }
  }

  override fun buildContent(parentDisposable: Disposable, project: Project): JComponent {
    return GlobalFileExplorerView(globalExplorer, project, parentDisposable) {
      lock.withLock {
        val previousState = filesToCut.toList()
        filesToCut = it
        sendTopic(CUT_BUFFER_CHANGES, globalExplorer.componentManager)
          .onUpdate(previousState, it)
      }
    }
  }

  private val lock = ReentrantLock()

  @Volatile
  private var filesToCut = listOf<VirtualFile>()

  override val displayName = "File Explorer"

  override val isLockable = true
}