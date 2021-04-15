package eu.ibagroup.formainframe.common.ui

import eu.ibagroup.formainframe.utils.castOrNull
import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

class ValidatingCellRenderer<Item> : DefaultTableCellRenderer() {

  @Suppress("UNCHECKED_CAST")
  override fun getTableCellRendererComponent(
    table: JTable?,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    if (table is ValidatingTableView<*>) {
      table.getCellValidator(column)
        ?.castOrNull<ValidatingColumnInfo<Item>>()
        ?.let { validator ->
          ComponentValidatorBuilder(this, table.disposable)
            .setup {
              withValidator { ->
                table.listTableModel.getItem(row).let { item ->
                  validator.validateEntered(item as Item, this@ValidatingCellRenderer)
                }.apply {
                  icon = getIcon()
                  table.listTableModel.validationInfos[row, column] = this
                  putClientProperty(TOOL_TIP_TEXT_KEY, this?.message)
                }
              }
            }.finish {}.apply {
              revalidate()
            }
        }
    } else {
      icon = null
    }
    return this
  }

}
