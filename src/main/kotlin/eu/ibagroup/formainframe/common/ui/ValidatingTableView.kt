package eu.ibagroup.formainframe.common.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.table.TableView
import eu.ibagroup.formainframe.utils.castOrNull
import java.awt.Dimension
import javax.swing.DefaultCellEditor
import javax.swing.JComponent
import javax.swing.table.TableCellRenderer

class ValidatingTableView<Item>(
  model: ValidatingListTableModel<Item>,
  val disposable: Disposable
) : TableView<Item>(model) {

  override fun getCellRenderer(row: Int, column: Int): TableCellRenderer? {
    return super.getCellRenderer(row, column)?.apply {
        val editor = getCellEditor(row, column)
        if (editor is DefaultCellEditor) {
          preferredSize = with(preferredSize) {
            Dimension(this.width, this.height.coerceAtLeast(editor.component.preferredSize.height))
          }
        }
      }
  }

  @Suppress("UNCHECKED_CAST")
  override fun getListTableModel(): ValidatingListTableModel<Item> {
    return super.getModel() as ValidatingListTableModel<Item>
  }

  private val validationInfos
    get() = listTableModel.validationInfos

  fun <Component : JComponent> getValidationCallback(): ValidationInfoBuilder.(Component) -> ValidationInfo? {
    return { component ->
      val validationInfoComponentPair = validationInfos
        .asMap
        .entries
        .minByOrNull { if (it.value.warning) 1 else 0 }
      if (validationInfoComponentPair != null) {
        val validationInfo = validationInfoComponentPair.value
        val cell = validationInfoComponentPair.key
        editCellAt(cell.first, cell.second)
        if (validationInfo.warning) {
          ValidationInfoBuilder(validationInfo.component ?: component).warning(validationInfo.message)
        } else {
          ValidationInfoBuilder(validationInfo.component ?: component).error(validationInfo.message)
        }
      } else null
    }
  }

  fun getCellValidator(column: Int): ValidatingColumnInfo<Item>? {
    return listTableModel.columnInfos[convertColumnIndexToModel(column)].castOrNull()
  }

}