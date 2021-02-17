package eu.ibagroup.formainframe.explorer.ui

import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings

abstract class ExplorerUnitTreeNodeBase<Value : Any, U : ExplorerUnit>(
  value: Value,
  val unit: U,
  explorerViewSettings: ExplorerViewSettings
) : ExplorerTreeNodeBase<Value>(value, unit.explorer, explorerViewSettings)