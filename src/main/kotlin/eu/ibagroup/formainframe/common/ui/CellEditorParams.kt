package eu.ibagroup.formainframe.common.ui

import javax.swing.JTable

data class CellEditorParams(
  val table: JTable?,
  val value: Any?,
  val isSelected: Boolean,
  val row: Int,
  val column: Int
)
