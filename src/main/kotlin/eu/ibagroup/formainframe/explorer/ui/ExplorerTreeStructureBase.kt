package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.util.treeView.AbstractTreeStructureBase
import com.intellij.openapi.project.ProjectManager
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings

abstract class ExplorerTreeStructureBase(
  protected val explorer: Explorer
  ) : AbstractTreeStructureBase(explorer.project ?: ProjectManager.getInstance().defaultProject), ExplorerViewSettings