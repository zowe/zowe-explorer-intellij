package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.SettingsProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showYesNoDialog
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.fetchAdapter
import eu.ibagroup.formainframe.dataops.synchronizer.AcceptancePolicy
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.utils.runWriteActionOnWriteThread
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.util.concurrent.locks.ReentrantLock
import javax.swing.tree.TreePath
import kotlin.concurrent.withLock

@Suppress("LeakingThis")
abstract class ExplorerTreeNodeBase<Value : Any>(
  value: Value,
  project: Project,
  val parent: ExplorerTreeNodeBase<*>?,
  val explorer: Explorer,
  protected val treeStructure: ExplorerTreeStructureBase
) : AbstractTreeNode<Value>(project, value), SettingsProvider {

  init {
    treeStructure.registerNode(this)
  }

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

  override fun navigate(requestFocus: Boolean) {
    val file = virtualFile ?: return
    descriptor?.let {
      if (!file.isDirectory) {
        val contentSynchronizer = service<DataOpsManager>(explorer.componentManager)
          .getContentSynchronizer(file)
        if (!contentSynchronizer.isAlreadySynced(file)) {
          val doSync = file.isReadable || showYesNoDialog(
            title = "File ${file.name} is not readable",
            message = "Do you want to try open it anyway?",
            project = project,
            icon = AllIcons.General.WarningDialog
          )
          if (doSync) {
            var successful = false
            runModalTask(
              title = "Fetching Content for ${file.name}",
              cancellable = false,
              project = project,
            ) {
              val lock = ReentrantLock()
              val condition = lock.newCondition()
              val fsModel = file.fileSystem.model
              fsModel.blockIOStreams(file)
              contentSynchronizer.enforceSyncIfNeeded(
                file = file,
                acceptancePolicy = AcceptancePolicy.FORCE_REWRITE,
                saveStrategy = { f, lastSuccessfulState, remoteBytes ->
                  (lastSuccessfulState contentEquals remoteBytes)
                },
                onSyncEstablished = fetchAdapter {
                  onStart { lock.lock() }
                  onResult { r ->
                    fsModel.unblockIOStreams(file)
                    successful = r.isSuccess
                    condition.signalAll()
                    lock.unlock()
                  }
                }
              )
              lock.withLock { condition.await() }
            }
            if (successful) {
              it.navigate(requestFocus)
            } else {
              contentSynchronizer.removeSync(file)
            }
          }
        } else {
          it.navigate(requestFocus)
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

  private val pathList: List<ExplorerTreeNodeBase<*>>
    get() = if (parent != null) {
      parent.pathList + this
    } else {
      listOf(this)
    }

  val path: TreePath
    get() = TreePath(pathList.toTypedArray())

}