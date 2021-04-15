package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.explorer.ExplorerUnit

abstract class ExplorerUnitTreeNodeBase<Value : Any, U : ExplorerUnit>(
  value: Value,
  project: Project,
  parent: ExplorerTreeNode<*>,
  val unit: U,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerTreeNode<Value>(value, project, parent, unit.explorer, treeStructure)