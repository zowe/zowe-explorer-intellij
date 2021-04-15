package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.SettingsProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.showYesNoDialog
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.synchronizer.AcceptancePolicy
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.utils.component
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import javax.swing.tree.TreePath

@Suppress("LeakingThis")
abstract class ExplorerTreeNode<Value : Any>(
  value: Value,
  project: Project,
  val parent: ExplorerTreeNode<*>?,
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
        val contentSynchronizer = explorer.componentManager.service<DataOpsManager>()
          .getContentSynchronizer(file) ?: return
        if (!contentSynchronizer.isAlreadySynced(file)) {
          val doSync = file.isReadable || showYesNoDialog(
            title = "File ${file.name} is not readable",
            message = "Do you want to try open it anyway?",
            project = project,
            icon = AllIcons.General.WarningDialog
          )
          if (doSync) {
            runModalTask(
              title = "Fetching Content for ${file.name}",
              cancellable = true,
              project = project,
            ) { indicator ->
              contentSynchronizer.startSyncIfNeeded(
                file = file,
                project = notNullProject,
                acceptancePolicy = AcceptancePolicy.FORCE_REWRITE,
                saveStrategy = { f, lastSuccessfulState, remoteBytes ->
                  (lastSuccessfulState contentEquals remoteBytes).let decision@{ result ->
                    return@decision if (!result) {
                      invokeAndWaitIfNeeded {
                        showYesNoDialog(
                          title = "Remote Conflict in File ${f.name}",
                          message = "The file you are currently editing was changed on remote. Do you want to accept remote changes and discard local ones, or overwrite content on the mainframe by local version?",
                          noText = "Accept Remote",
                          yesText = "Overwrite Content on the Mainframe",
                          project = project,
                          icon = AllIcons.General.WarningDialog
                        )
                      }
                    } else {
                      true
                    }
                  }
                },
                removeSyncOnThrowable = { f, t ->
                  invokeLater {
                    notNullProject.component<FileEditorManager>().closeFile(f)
                    explorer.reportThrowable(t, project)
                  }
                  true
                },
                progressIndicator = indicator
              )
            }
          }
        }
        it.navigate(requestFocus)
      }
    }
  }

  override fun canNavigate(): Boolean {
    return descriptor?.canNavigate() ?: super.canNavigate()
  }

  override fun canNavigateToSource(): Boolean {
    return descriptor?.canNavigateToSource() ?: super.canNavigateToSource()
  }

  private val pathList: List<ExplorerTreeNode<*>>
    get() = if (parent != null) {
      parent.pathList + this
    } else {
      listOf(this)
    }

  val path: TreePath
    get() = TreePath(pathList.toTypedArray())

}