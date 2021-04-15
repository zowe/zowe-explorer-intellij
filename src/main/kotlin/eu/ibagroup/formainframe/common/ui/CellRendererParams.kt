package eu.ibagroup.formainframe.common.ui

import javax.swing.JTable

data class CellRendererParams(
  val table: JTable?,
  val value: Any?,
  val isSelected: Boolean,
  val hasFocus: Boolean,
  val row: Int,
  val column: Int
)
