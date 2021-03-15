package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.SettingsProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.fetchAdapter
import eu.ibagroup.formainframe.dataops.synchronizer.AcceptancePolicy
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import javax.swing.tree.TreePath

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

  protected open val openFileDescriptor: OpenFileDescriptor?
    get() = virtualFile?.let { file ->
      if (!file.isDirectory) {
        service<DataOpsManager>(explorer.componentManager)
          .getAppropriateContentSynchronizer(file)
          .enforceSyncIfNeeded(
            file = file,
            acceptancePolicy = AcceptancePolicy.FORCE_REWRITE,
            saveStrategy = { _, _, _ -> true },
            onSyncEstablished = fetchAdapter {
            }
          )
        OpenFileDescriptor(notNullProject, file)
      } else null
    }

  public override fun getVirtualFile(): MFVirtualFile? {
    return null
  }

  val notNullProject = project

  override fun getSettings(): ViewSettings {
    return treeStructure
  }

  override fun navigate(requestFocus: Boolean) {
    openFileDescriptor?.navigate(requestFocus) ?: super.navigate(requestFocus)
  }

  override fun canNavigate(): Boolean {
    return openFileDescriptor?.canNavigate() ?: super.canNavigate()
  }

  override fun canNavigateToSource(): Boolean {
    return openFileDescriptor?.canNavigateToSource() ?: super.canNavigateToSource()
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