package eu.ibagroup.formainframe.config.ws.ui.files

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ComboBoxCellEditor
import eu.ibagroup.formainframe.common.ui.*
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsDialog
import eu.ibagroup.formainframe.config.ws.ui.WorkingSetDialogState
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.validateDatasetMask
import eu.ibagroup.formainframe.utils.validateForBlank
import eu.ibagroup.formainframe.utils.validateUssMask
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.TableCellEditor

class WorkingSetDialog(
  crudable: Crudable,
  state: WorkingSetDialogState
) : AbstractWsDialog<FilesWorkingSetConfig, WorkingSetDialogState.TableRow, WorkingSetDialogState>(crudable, WorkingSetDialogState::class.java, state) {
  override val wsConfigClass = FilesWorkingSetConfig::class.java

  override val masksTable = ValidatingTableView(
    ValidatingListTableModel(MaskColumn, TypeColumn).apply {
      items = state.maskRow
    },
    disposable
  ).apply { rowHeight = DEFAULT_ROW_HEIGHT }

  override val tableTitle = "DS Masks included in Working Set"

  override val wsNameLabel = "Working Set Name"

  override fun init() {
    title = when (state.mode) {
      DialogMode.CREATE -> "Add Working Set"
      else -> "Edit Working Set"
    }
    super.init()
  }

  init {
    init()
  }

  override fun emptyTableRow(): WorkingSetDialogState.TableRow = WorkingSetDialogState.TableRow()

  override fun validateOnApply(validationBuilder: ValidationInfoBuilder, component: JComponent): ValidationInfo? {
    return when {
      masksTable.listTableModel.validationInfos.asMap.isNotEmpty() -> {
        ValidationInfo("Fix errors in the table and try again", component)
      }
      masksTable.listTableModel.rowCount == 0 -> {
        validationBuilder.warning("You are going to create a Working Set that doesn't fetch anything")
      }
      hasDuplicatesInTable(masksTable.items) -> {
        ValidationInfo("You cannot add several identical masks to table")
      }
      else -> null
    }
  }

  private fun hasDuplicatesInTable(tableElements: List<WorkingSetDialogState.TableRow>): Boolean {
    return tableElements.size != tableElements.map { it.mask }.distinct().size
  }

  object MaskColumn : ValidatingColumnInfo<WorkingSetDialogState.TableRow>("Mask") {

    override fun valueOf(item: WorkingSetDialogState.TableRow): String {
      return item.mask
    }

    override fun setValue(item: WorkingSetDialogState.TableRow, value: String) {
      val editedCaseValue = if (item.type == "z/OS") value.toUpperCase() else value
      item.mask = if (editedCaseValue.length > 1 && editedCaseValue.endsWith("/")) editedCaseValue.substringBeforeLast("/") else editedCaseValue
    }

    override fun isCellEditable(item: WorkingSetDialogState.TableRow?): Boolean {
      return true
    }

    override fun validateOnInput(
      oldItem: WorkingSetDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      if (newValue.contains("/")) {
        oldItem.type = "USS"
      }
      return null
    }


    override fun validateEntered(item: WorkingSetDialogState.TableRow, component: JComponent): ValidationInfo? {
      return validateForBlank(item.mask, component) ?: if (item.type == "z/OS") validateDatasetMask(
        item.mask,
        component
      ) else validateUssMask(
        item.mask, component
      )
    }

  }

  object TypeColumn : ColumnInfo<WorkingSetDialogState.TableRow, String>("Type") {

    override fun getEditor(item: WorkingSetDialogState.TableRow): TableCellEditor {
      ComboBoxCellEditorImpl.clickCountToStart = 1
      return ComboBoxCellEditorImpl
    }

    override fun setValue(item: WorkingSetDialogState.TableRow, value: String) {
      item.type = value
    }

    override fun isCellEditable(item: WorkingSetDialogState.TableRow): Boolean {
      return true
    }

    override fun valueOf(item: WorkingSetDialogState.TableRow): String {
      return item.type
    }

    override fun getWidth(table: JTable): Int {
      return 100
    }

  }

  object ComboBoxCellEditorImpl : ComboBoxCellEditor() {
    override fun getComboBoxItems(): MutableList<String> {
      return with(WorkingSetDialogState.TableRow) { mutableListOf(ZOS, USS) }
    }
  }
}
