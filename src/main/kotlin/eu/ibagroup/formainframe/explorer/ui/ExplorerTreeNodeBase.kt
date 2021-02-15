package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.SettingsProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.ProjectManager
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import org.snakeyaml.engine.v2.api.Load

abstract class ExplorerTreeNodeBase<Value : Any>(
  value: Value,
  protected val explorer: Explorer,
  protected val viewSettings: ExplorerViewSettings
) : AbstractTreeNode<Value>(explorer.project ?: ProjectManager.getInstance().defaultProject, value), SettingsProvider {

  override fun getSettings(): ViewSettings {
    return viewSettings
  }

}