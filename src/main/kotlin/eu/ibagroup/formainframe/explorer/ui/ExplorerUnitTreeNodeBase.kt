package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings

abstract class ExplorerUnitTreeNodeBase<Value : Any, U : ExplorerUnit>(
  value: Value,
  project: Project,
  parent: ExplorerTreeNodeBase<*>,
  val unit: U,
  explorerViewSettings: ExplorerViewSettings
) : ExplorerTreeNodeBase<Value>(value, project, parent, unit.explorer, explorerViewSettings)