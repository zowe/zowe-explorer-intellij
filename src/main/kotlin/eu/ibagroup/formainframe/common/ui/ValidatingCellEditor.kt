package eu.ibagroup.formainframe.common.ui

import com.intellij.ui.components.JBTextField
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.runIfTrue
import java.awt.Component
import javax.swing.DefaultCellEditor
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.JTextField

class ValidatingCellEditor<Item> : DefaultCellEditor(JBTextField()) {

  @Suppress("UNCHECKED_CAST")
  override fun getTableCellEditorComponent(
    table: JTable?,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component? {
    return super.getTableCellEditorComponent(table, value, isSelected, row, column)?.apply component@{
      if (table is ValidatingTableView<*> && this is JComponent) {
        table.castOrNull<ValidatingTableView<Item>>()?.apply {
          getCellValidator(column)?.let { validator ->
            ComponentValidatorBuilder(this@component, table.disposable)
              .setup {
                withValidator { ->
                  with(table as ValidatingTableView<Item>) {
                    (editingRow in 0 until columnCount).runIfTrue {
                      getRow(editingRow)?.let { value ->
                        validator.validateOnInput(
                          value,
                          this@ValidatingCellEditor.component.castOrNull<JTextField>()!!.text,
                          component
                        )
                      }
                    }.also {
                      listTableModel.validationInfos[editingRow, editingColumn] = it
                    }
                  }
                }
              }.finish { }
          }
        }
      }
    }
  }
}