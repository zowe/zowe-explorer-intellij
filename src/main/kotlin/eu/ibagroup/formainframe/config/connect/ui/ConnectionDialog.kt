package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.toBinding
import com.intellij.ui.layout.withTextBinding
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.find
import eu.ibagroup.formainframe.utils.runIfTrue
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField

class ConnectionDialog(
  private val crudable: Crudable,
  override var state: ConnectionDialogState = ConnectionDialogState(),
  private val project: Project? = ProjectManager.getInstance().defaultProject
) : DialogWrapper(project), StatefulComponent<ConnectionDialogState> {

  private val initialState = state.clone()

  private val urlRegex = Regex("^(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")

  private lateinit var urlTextField: JTextField

  private lateinit var sslCheckbox: JCheckBox

  init {
    setResizable(false)
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label("Connection name")
        textField(state::connectionName)
          .focused()
          .withValidationOnInput {
            val initialNameIsNotBlank = initialState.connectionName.isNotBlank()
            val initialNameIsTheSameAsModified = initialState.connectionName != state.connectionName
            val connectionWithSameNameAlreadyExists =
              crudable.find<ConnectionConfig> { connectionConfig -> connectionConfig.name == it.text }.count() > 0
            (connectionWithSameNameAlreadyExists && (initialNameIsNotBlank && initialNameIsTheSameAsModified || !initialNameIsNotBlank))
              .runIfTrue {
                this.error("Please, provide unique Connection Name. ${it.text} is already in use")
              }

          }
          .withValidationOnApply {
            if (it.text.isBlank()) {
              ValidationInfo("Please, provide non-blank Connection Name", it)
            } else {
              null
            }
          }
      }
      row {
        label("Connection URL")
        textField(state::connectionUrl)
          .withValidationOnApply {
            if (!it.text.matches(urlRegex)) {
              ValidationInfo("Please provide a valid URL to zOSMF. Example: https://myhost.com:10443", it)
            } else {
              null
            }
          }
          .also { urlTextField = it.component }
      }
      row {
        label("Username")
        textField(state::username).withValidationOnApply {
          when {
            it.text.isBlank() -> {
              ValidationInfo("Username must not be blank", it)
            }
            it.text.length > 8 -> {
              ValidationInfo("Username must not exceed 8 characters", it)
            }
            else -> {
              null
            }
          }
        }
      }
      row {
        label("Password")
        JPasswordField(state.password)().withTextBinding(state::password.toBinding()).withValidationOnApply {
          when {
            it.text.isBlank() -> {
              ValidationInfo("Password must not be blank", it)
            }
            it.text.length > 8 -> {
              ValidationInfo("Password must not exceed 8 characters", it)
            }
            else -> {
              null
            }
          }
        }
      }
      row {
        checkBox("Accept self-signed SSL certificates", state::isAllowSsl)
          .withLargeLeftGap().also { sslCheckbox = it.component }
      }
    }.withMinimumWidth(500)
  }

  init {
    init()
    title = when (state.mode){
      DialogMode.READ -> "Connection Properties"
      DialogMode.DELETE -> "Delete Connection"
      DialogMode.UPDATE -> "Edit Connection"
      DialogMode.CREATE -> "Add Connection"
    }
  }

}