package eu.ibagroup.formainframe.common.ui

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.util.ui.ColumnInfo
import javax.swing.JComponent
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

abstract class ValidatingColumnInfo<Item>(
    name: String
) : ColumnInfo<Item, String>(name) {

  open fun getValidatingCellRenderer(item: Item): ValidatingCellRenderer<Item> {
    return ValidatingCellRenderer()
  }

  open fun getValidatingCellEditor(item: Item): ValidatingCellEditor<Item> {
    return ValidatingCellEditor()
  }

  override fun getRenderer(item: Item): TableCellRenderer {
    return getValidatingCellRenderer(item)
  }

  override fun getEditor(item: Item): TableCellEditor {
    return getValidatingCellEditor(item)
  }

  abstract fun validateOnInput(oldItem: Item, newValue: String, component: JComponent): ValidationInfo?

  abstract fun validateEntered(item: Item, component: JComponent): ValidationInfo?

}