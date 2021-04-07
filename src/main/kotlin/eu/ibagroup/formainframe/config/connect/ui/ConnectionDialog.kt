package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.toBinding
import com.intellij.ui.layout.withTextBinding
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.StatefulDialog
import eu.ibagroup.formainframe.common.ui.showUntilDone
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.InfoOperation
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.runTask
import eu.ibagroup.formainframe.utils.validation.*
import java.awt.Component
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField

class ConnectionDialog(
  private val crudable: Crudable,
  override var state: ConnectionDialogState = ConnectionDialogState(),
  project: Project? = null
) : StatefulDialog<ConnectionDialogState>(project) {

  companion object {
    @JvmStatic
    fun showAndTestConnection(
      crudable: Crudable,
      parentComponent: Component? = null,
      project: Project? = null,
      initialState: ConnectionDialogState
    ): ConnectionDialogState? {
      return showUntilDone(
        initialState = initialState,
        factory = { ConnectionDialog(crudable, initialState, project) },
        test = { state ->
          val throwable = runTask(title = "Testing Connection to ${state.urlConnection.url}", project = project) {
            return@runTask try {
              service<DataOpsManager>().performOperation(InfoOperation(state.urlConnection.url, state.isAllowSsl), it)
              null
            } catch (t: Throwable) {
              t
            }
          }
          if (throwable != null) {
            val confirmMessage = "Do you want to add it anyway?"
            val tMessage = throwable.message
            val message = if (tMessage != null) {
              "$tMessage\n\n$confirmMessage"
            } else {
              confirmMessage
            }
            val addAnyway = MessageDialogBuilder
              .yesNo(
                title = "Error Creating Connection",
                message = message
              ).icon(AllIcons.General.ErrorDialog)
              .run {
                if (parentComponent != null) {
                  ask(parentComponent)
                } else {
                  ask(project)
                }
              }
            addAnyway
          } else {
            true
          }
        }
      )
    }
  }

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
            it.text = it.text.trim()
            validateForBlank(it)
          }
      }
      row {
        label("Connection URL")
        textField(state::connectionUrl).withValidationOnApply {
          it.text = it.text.trim()
          validateForBlank(it) ?: validateZosmfUrl(it)
        }.also { urlTextField = it.component }
      }
      row {
        label("Username")
        textField(state::username).withValidationOnInput {
          validateUsername(it)
        }.withValidationOnApply {
          it.text = it.text.trim()
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
    title = when (state.mode) {
      DialogMode.READ -> "Connection Properties"
      DialogMode.DELETE -> "Delete Connection"
      DialogMode.UPDATE -> "Edit Connection"
      DialogMode.CREATE -> "Add Connection"
    }
  }

}