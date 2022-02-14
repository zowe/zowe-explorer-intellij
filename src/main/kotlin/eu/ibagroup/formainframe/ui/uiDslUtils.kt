package eu.ibagroup.formainframe.ui

import com.intellij.ui.layout.*
import java.awt.Dimension
import javax.swing.JLabel

fun LayoutBuilder.flowableRow(init: InnerCell.() -> Unit): Row {
  return row { cell { init() } }
}

fun Cell.labelOfWidth(text: String, width: Int): CellBuilder<JLabel> {
  return label(text).apply {
    component.preferredSize = Dimension(width, component.height)
  }
}
