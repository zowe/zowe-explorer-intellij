package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.SettingsProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.AcceptancePolicy
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile

abstract class ExplorerTreeNodeBase<Value : Any>(
  value: Value,
  project: Project,
  val explorer: Explorer,
  protected val viewSettings: ExplorerViewSettings
) : AbstractTreeNode<Value>(project, value), SettingsProvider {

  protected open val openFileDescriptor: OpenFileDescriptor?
    get() = virtualFile?.let {
      if (!it.isDirectory) {
        service<DataOpsManager>(explorer.componentManager).syncContentIfNeeded(
          file = it,
          acceptancePolicy = AcceptancePolicy.FORCE_REWRITE,
          saveStrategy = { _, _, _ -> true }
        )
        OpenFileDescriptor(notNullProject, it)
      } else null
    }

  public override fun getVirtualFile(): MFVirtualFile? {
    return null
  }

  val notNullProject = project

  override fun getSettings(): ViewSettings {
    return viewSettings
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
}