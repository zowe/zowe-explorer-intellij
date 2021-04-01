package eu.ibagroup.formainframe.config.ws.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import com.intellij.util.containers.isEmpty
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ComboBoxCellEditor
import eu.ibagroup.formainframe.common.ui.*
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.validation.validateDatasetMask
import eu.ibagroup.formainframe.utils.validation.validateForBlank
import eu.ibagroup.formainframe.utils.validation.validateUssMask
import eu.ibagroup.formainframe.utils.validation.validateWorkingSetName
import java.awt.Dimension
import java.util.*
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.TableCellEditor
import kotlin.streams.toList

class WorkingSetDialog(
  crudable: Crudable,
  override var state: WorkingSetDialogState
) : DialogWrapper(false), StatefulComponent<WorkingSetDialogState> {

  private val initialState = state.clone()

  private val connectionComboBoxModel = CollectionComboBoxModel(crudable.getAll<ConnectionConfig>().toList())

  private val dsMasksTable = ValidatingTableView(
    ValidatingListTableModel(MaskColumn, TypeColumn).apply {
      items = state.maskRow
    },
    disposable
  ).apply { rowHeight = DEFAULT_ROW_HEIGHT }

  private val panel by lazy {
    panel {
      row {
        label("Working Set name")
        textField(state::workingSetName)
          .withValidationOnInput {
            validateWorkingSetName(
              it, if (initialState.workingSetName.isNotBlank()) {
                initialState.workingSetName
              } else {
                null
              },
              crudable
            )
          }
          .withValidationOnApply {
            validateForBlank(it)
          }
      }
      row {
        label("Specify connection")
        comboBox(
          model = connectionComboBoxModel,
          modelBinding = PropertyBinding(
            get = {
              val connectionConfig = crudable.getByUniqueKey<ConnectionConfig>(state.connectionUuid)
              return@PropertyBinding if (connectionConfig != null) {
                connectionConfig
              } else if (!crudable.getAll<ConnectionConfig>().isEmpty()) {
                crudable.getAll<ConnectionConfig>().findAnyNullable()?.also {
                  state.connectionUuid = it.uuid
                }
              } else {
                null
              }
            },
            set = { config -> state.connectionUuid = config?.uuid ?: "" }
          ),
          renderer = SimpleListCellRenderer.create("") { it?.name }
        ).withValidationOnApply {
          if (it.selectedItem == null) {
            ValidationInfo("You must provide a connection", it)
          } else {
            null
          }
        }

      }
      row {
        toolbarTable("DS Mask included in Working Set", dsMasksTable, addDefaultActions = true) {
          addNewItemProducer { WorkingSetDialogState.TableRow() }
        }.withValidationOnApply {
          when {
            dsMasksTable.listTableModel.validationInfos.asMap.isNotEmpty() -> {
              ValidationInfo("Fix errors in the table and try again", it)
            }
            dsMasksTable.listTableModel.rowCount == 0 -> {
              this.warning("You are going to create a Working Set that doesn't fetch anything")
            }
            hasDuplicatesInTable(dsMasksTable.items) -> {
              ValidationInfo("You cannot add several identical masks to table")
            }
            else -> null
          }
        }.onApply {
          state.maskRow = dsMasksTable.items
        }
      }
    }.apply {
      minimumSize = Dimension(450, 500)
    }
  }

  private fun hasDuplicatesInTable(tableElements: List<WorkingSetDialogState.TableRow>): Boolean {
    
    return tableElements.size != tableElements.map {it.mask}.distinct().size
  }

  init {
    title = when (state.mode) {
      DialogMode.CREATE -> "Add Working Set"
      else -> "Edit Working Set"
    }
    init()
  }

  override fun createCenterPanel(): JComponent {
    return panel
  }

  object MaskColumn : ValidatingColumnInfo<WorkingSetDialogState.TableRow>("Mask") {

    override fun valueOf(item: WorkingSetDialogState.TableRow): String {
      return item.mask
    }

    override fun setValue(item: WorkingSetDialogState.TableRow, value: String) {
      item.mask = if (value.length > 1 && value.endsWith("/")) value.substringBeforeLast("/") else value
    }

    override fun isCellEditable(item: WorkingSetDialogState.TableRow?): Boolean {
      return true
    }

    override fun validateOnInput(
      oldItem: WorkingSetDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
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

  object IsSingleColumn : ColumnInfo<WorkingSetDialogState.TableRow, Boolean>("Is Single") {

    /*override fun getEditor(item: WorkingSetDialogState.TableRow?): TableCellEditor? {
        return BooleanTableCellEditor()
    }*/

    override fun getTooltipText(): String {
      return "Check it in case you want to obtain single dataset, not all matching to this ds level"
    }

    override fun getWidth(table: JTable?): Int {
      return 60
    }

    override fun getComparator(): Comparator<WorkingSetDialogState.TableRow> {
      return Comparator { o1, o2 -> o1.isSingle.compareTo(o2.isSingle) }
    }

    override fun getColumnClass(): Class<*> {
      return Boolean::class.java
    }

    override fun setValue(item: WorkingSetDialogState.TableRow, value: Boolean) {
      item.isSingle = value
    }


    override fun isCellEditable(item: WorkingSetDialogState.TableRow): Boolean {
      return with(WorkingSetDialogState.TableRow) { item.type == ZOS }
    }

    override fun valueOf(item: WorkingSetDialogState.TableRow): Boolean {
      return item.isSingle
    }

  }

  object ComboBoxCellEditorImpl : ComboBoxCellEditor() {
    override fun getComboBoxItems(): MutableList<String> {
      return with(WorkingSetDialogState.TableRow) { mutableListOf(ZOS, USS) }
    }
  }
}