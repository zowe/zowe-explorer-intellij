package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.toBinding
import com.intellij.ui.layout.withTextBinding
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.find
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.runIfTrue
import javax.swing.JComponent
import javax.swing.JPasswordField
import kotlin.streams.toList

class ConnectionDialog(
  private val crudable: Crudable,
  override var state: ConnectionDialogState = ConnectionDialogState()
) : DialogWrapper(false), StatefulComponent<ConnectionDialogState> {

  private val initialState = state.clone()


  init {
    setResizable(false)
  }

  private val availableUrlsComboBoxModel = CollectionComboBoxModel(
    crudable.getAll<UrlConnection>().toList(),
    state.urlConnectionUuid.isNotBlank().runIfTrue {
      crudable.getByUniqueKey(state.urlConnectionUuid)
    }
  )

  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label("Connection name")
        textField(state::connectionName)
          .focused()
          .withValidationOnInput {
            val initialNameIsNotBlank = initialState.connectionName.isNotBlank()
            val initialNameIsTheSameAsModified = initialState.connectionName != state.connectionName
            val connectionWithSameNameAlreadyExists = crudable.find<ConnectionConfig> { connectionConfig -> connectionConfig.name == it.text }.count() > 0
            (connectionWithSameNameAlreadyExists && (initialNameIsNotBlank && initialNameIsTheSameAsModified || !initialNameIsNotBlank))
              .runIfTrue {
                this.error("Please, provide unique Connection Name. ${it.text} is already in use")
              }
          }
          .withValidationOnApply {
            it.text.isBlank().runIfTrue { this.error("Please, provide non-blank Connection Name") }
          }
      }
      row {
        label("Connection URL")
        textField(state::connectionUrl)
//        comboBox(
//          model = availableUrlsComboBoxModel,
//          prop = state::zosmfUrlConnection,
//          renderer = SimpleListCellRenderer.create("") { it?.url }
//        ).applyToComponent {
//          isEditable = true
//          AutoCompleteDecorator.decorate(this, object : ObjectToStringConverter() {
//            override fun getPreferredStringForItem(p0: Any?): String {
//              return p0?.castOrNull<ZOSMFUrlConnection>()?.url ?: ""
//            }
//          })
//          selectedItem = state.zosmfUrlConnection
//        }.withErrorOnApplyIf("Please, provide non-blank URL") { it.editor.item?.castOrNull<ZOSMFUrlConnection>()?.url?.isBlank() ?: false }
//          .withValidationOnInput { (it.editor.item?.castOrNull<ZOSMFUrlConnection>()?.url == "popa").runIfTrue { this.warning("popa") } }
      }
      row {
        label("Username")
        textField(state::username)
      }
      row {
        label("Password")
        JPasswordField(state.password)().withTextBinding(state::password.toBinding())
      }
      row {
        checkBox("Accept self-signed SSL certificates", state::isAllowSsl)
          .withLargeLeftGap()
      }
    }.withMinimumWidth(500)
  }

  init {
    title = "Add Connection"
    init()
  }

}