package eu.ibagroup.formainframe.config.jobs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import com.intellij.util.containers.isEmpty
import com.intellij.util.ui.ColumnInfo
import eu.ibagroup.formainframe.common.ui.*
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.ui.WorkingSetDialog
import eu.ibagroup.formainframe.config.ws.ui.WorkingSetDialogState
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.findAnyNullable
import javax.swing.JComponent
import kotlin.streams.toList

class JobsWsDialog(
  private val crudable: Crudable,
  override var state: JobsWsDialogState
): DialogWrapper(false), StatefulComponent<JobsWsDialogState> {

  private val initialState = state.clone()

  private val connectionComboBoxModel = CollectionComboBoxModel(crudable.getAll<ConnectionConfig>().toList())

  private val filtersTable = ValidatingTableView(
    ValidatingListTableModel(PrefixColumn, OwnerColumn, JobIdColumn).apply {
      items = state.maskRow
    },
    disposable
  ).apply { rowHeight = DEFAULT_ROW_HEIGHT }

  private val panel by lazy {
    panel {
      row {
        label("Jobs Working Set Name")
        textField(state::workingSetName)
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
        toolbarTable("DS Mask included in Working Set", filtersTable, addDefaultActions = true) {
          addNewItemProducer { JobsWsDialogState.TableRow() }
        }.withValidationOnApply {
          when {
            filtersTable.listTableModel.validationInfos.asMap.isNotEmpty() -> {
              ValidationInfo("Fix errors in the table and try again", it)
            }
            filtersTable.listTableModel.rowCount == 0 -> {
              this.warning("You are going to create a Working Set that doesn't fetch anything")
            }
//            hasDuplicatesInTable(dsMasksTable.items) -> {
//              ValidationInfo("You cannot add several identical masks to table")
//            }
            else -> null
          }
        }.onApply {
          state.maskRow = filtersTable.items
        }
      }
    }
  }
  init {
    title = when (state.mode) {
      DialogMode.CREATE -> "Add Working Set"
      else -> "Edit Working Set"
    }
    init()
  }

  override fun createCenterPanel(): JComponent? {
    return panel
  }

  object PrefixColumn : ValidatingColumnInfo<JobsWsDialogState.TableRow>("Prefix") {
    override fun valueOf(item: JobsWsDialogState.TableRow?): String? = item?.prefix
    override fun validateOnInput(
      oldItem: JobsWsDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      // TODO: "implement"
      return null
    }

    override fun validateEntered(item: JobsWsDialogState.TableRow, component: JComponent): ValidationInfo? {
      // TODO: "implement"
      return null
    }

    override fun isCellEditable(item: JobsWsDialogState.TableRow?): Boolean = true

    override fun setValue(item: JobsWsDialogState.TableRow, value: String) {
      item.prefix = value
    }
  }

  object OwnerColumn : ValidatingColumnInfo<JobsWsDialogState.TableRow>("Owner") {
    override fun valueOf(item: JobsWsDialogState.TableRow?): String? = item?.prefix
    override fun validateOnInput(
      oldItem: JobsWsDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      // TODO: "implement"
      return null
    }

    override fun validateEntered(item: JobsWsDialogState.TableRow, component: JComponent): ValidationInfo? {
      // TODO: "implement"
      return null
    }

    override fun isCellEditable(item: JobsWsDialogState.TableRow?): Boolean = true

    override fun setValue(item: JobsWsDialogState.TableRow, value: String) {
      item.owner = value
    }
  }

  object JobIdColumn : ValidatingColumnInfo<JobsWsDialogState.TableRow>("Job ID") {
    override fun valueOf(item: JobsWsDialogState.TableRow?): String? = item?.prefix
    override fun validateOnInput(
      oldItem: JobsWsDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      // TODO: "implement"
      return null
    }

    override fun validateEntered(item: JobsWsDialogState.TableRow, component: JComponent): ValidationInfo? {
      // TODO: "implement"
      return null
    }

    override fun isCellEditable(item: JobsWsDialogState.TableRow?): Boolean = true

    override fun setValue(item: JobsWsDialogState.TableRow, value: String) {
      item.jobId = value
    }
  }

}
