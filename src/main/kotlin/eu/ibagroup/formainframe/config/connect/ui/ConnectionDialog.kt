package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.toBinding
import com.intellij.ui.layout.withTextBinding
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.validation.*
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField

class ConnectionDialog(
  private val crudable: Crudable,
  override var state: ConnectionDialogState = ConnectionDialogState(),
  project: Project? = ProjectManager.getInstance().defaultProject
) : DialogWrapper(project), StatefulComponent<ConnectionDialogState> {

  private val initialState = state.clone()

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
            validateConnectionName(
              it,
              if (initialState.connectionName.isNotBlank()) initialState.connectionName else null,
              crudable
            )
          }
          .withValidationOnApply {
            validateForBlank(it)
          }
      }
      row {
        label("Connection URL")
        textField(state::connectionUrl)
          .withValidationOnApply {
            validateForBlank(it) ?: validateZosmfUrl(it)
          }
          .also { urlTextField = it.component }
      }
      row {
        label("Username")
        textField(state::username).withValidationOnInput {
          validateUsername(it)
        }.withValidationOnApply {
          validateForBlank(it)
        }
      }
      row {
        label("Password")
        JPasswordField(state.password)().withTextBinding(state::password.toBinding()).withValidationOnInput {
          validatePassword(it)
        }.withValidationOnApply {
          validateForBlank(it)
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