package eu.ibagroup.formainframe.config.ws.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.panel
import com.intellij.util.containers.isEmpty
import eu.ibagroup.formainframe.common.ui.*
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import java.awt.Dimension
import javax.swing.JComponent
import kotlin.streams.toList

abstract class AbstractWsDialog<WSConfig, TableRow, WSDState : AbstractWsDialogState<WSConfig, TableRow>>(
  crudable: Crudable,
  wsdStateClass: Class<out WSDState>,
  override var state: WSDState,
  var initialState: WSDState = state.clone(wsdStateClass)
) : DialogWrapper(false), StatefulComponent<WSDState> {


  private val connectionComboBoxModel = CollectionComboBoxModel(crudable.getAll<ConnectionConfig>().toList())

  abstract val tableTitle: String

  abstract val masksTable: ValidatingTableView<TableRow>

  abstract fun emptyTableRow(): TableRow

  abstract fun validateOnApply(validationBuilder: ValidationInfoBuilder, component: JComponent): ValidationInfo?

  open fun onWSApplyed(state: WSDState): WSDState = state

  private val panel by lazy {
    panel {
      row {
        label("Working Set name")
        textField(getter = { state.workingSetName }, setter = { state.workingSetName = it })
          .withValidationOnInput {
            validateWorkingSetName(
              it, initialState.workingSetName.ifBlank {
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
              return@PropertyBinding crudable.getByUniqueKey<ConnectionConfig>(state.connectionUuid)
                ?: if (!crudable.getAll<ConnectionConfig>().isEmpty()) {
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
        toolbarTable(tableTitle, masksTable, addDefaultActions = true) {
          addNewItemProducer { emptyTableRow() }
        }.withValidationOnApply {
          validateOnApply(this, it)
        }.onApply {
          state.maskRow = masksTable.items
          state = onWSApplyed(state)
        }
      }
    }.apply {
      minimumSize = Dimension(450, 500)
    }
  }

  override fun init() {
    title = when (state.mode) {
      DialogMode.CREATE -> "Add Working Set"
      else -> "Edit Working Set"
    }
    super.init()
  }

  override fun createCenterPanel(): JComponent {
    return panel
  }
}
