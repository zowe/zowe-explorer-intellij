/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package org.zowe.explorer.config.connect.ui.zosmf

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.*
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.find
import org.zowe.explorer.utils.removeTrailingSlashes
import org.zowe.explorer.utils.validateConnectionName
import org.zowe.explorer.utils.validateForBlank
import org.zowe.explorer.utils.validateZosmfUrl
import org.zowe.explorer.zowe.service.ZoweConfigServiceImpl
import org.zowe.explorer.zowe.service.ZoweConfigType
import java.awt.Component
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPasswordField

/** Dialog to add a new zowe config file */
class ZoweTeamConfigDialog(
  crudable: Crudable,
  state: ConnectionDialogState = ConnectionDialogState(),
  project: Project? = null
) : CommonConnectionDialog(crudable, state, project) {

  /**
   * Private field
   * In case of DialogMode.UPDATE takes the last successful state from crudable, takes default state otherwise
   */
  private val lastSuccessfulState: ConnectionDialogState =
    if (state.mode == DialogMode.UPDATE) crudable.find<ConnectionConfig> { it.uuid == state.connectionUuid }
      .findAny()
      .orElseGet { state.connectionConfig }
      .toDialogState(crudable) else ConnectionDialogState()

  companion object {
    //Call showAndTestConnectionCommon for current class
    @JvmStatic
    fun showAndTestConnection(
      crudable: Crudable,
      parentComponent: Component? = null,
      project: Project? = null,
      initialState: ConnectionDialogState
    ): ConnectionDialogState? {
      val connectionDialog =
        ZoweTeamConfigDialog(crudable, initialState, project)
      return connectionDialog.showAndTestConnectionCommon(crudable, parentComponent, project, initialState)
    }
  }

  private val initialState = state.clone()


  private lateinit var globalConfigCheckbox: JCheckBox

  init {
    isResizable = false
  }

  /** Create object to use in abstract class */
  override fun createConnectionDialog(
    crudable: Crudable,
    state: ConnectionDialogState,
    project: Project?
  ): CommonConnectionDialog {
    return ZoweTeamConfigDialog(crudable, state, project)
  }

  /** Create dialog with the fields */
  override fun createCenterPanel(): JComponent {
    val sameWidthLabelsGroup = "CONNECTION_DIALOG_LABELS_WIDTH_GROUP"

    state.zoweConfigPath = ZoweConfigServiceImpl.getZoweConfigLocation(project, ZoweConfigType.LOCAL)
    val connectionName = ZoweConfigServiceImpl.getZoweConnectionName(project, ZoweConfigType.LOCAL)
    return panel {
      row {
        label("Connection name")
          .widthGroup(sameWidthLabelsGroup)
        if (state.zoweConfigPath == null) {
          textField()
            .bindText(state::connectionName)
            .enabled(false)
            .text(connectionName)
            .validationOnApply {
              it.text = it.text.trim()
              validateForBlank(it) ?: validateConnectionName(
                it,
                initialState.connectionName.ifBlank { null },
                crudable
              )
            }
            .focused()
            .align(AlignX.FILL)
        } else {
          textField()
            .bindText(state::connectionName)
            .enabled(false)
            .text(connectionName)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
      }
      row {
        label("Connection URL: ")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::connectionUrl)
          .validationOnApply {
            it.text = it.text.trim().removeTrailingSlashes()
            validateForBlank(it) ?: validateZosmfUrl(it)
          }
          .also { urlTextField = it.component }
          .align(AlignX.FILL)
      }
      row {
        label("Username")
          .widthGroup(sameWidthLabelsGroup)
        (
            cell(JPasswordField())
            )
          .bindText(state::username)
          .validationOnApply {
            validateForBlank(String(it.password).trim(), it)
          }
          .onApply {
            state.username = state.username.trim().uppercase()
          }
          .align(AlignX.FILL)
      }
      row {
        label("Password: ")
          .widthGroup(sameWidthLabelsGroup)
        cell(JPasswordField())
          .bindText(state::password)
          .validationOnApply { validateForBlank(it) }
          .align(AlignX.FILL)
      }
      indent {
        row {
          checkBox("Accept self-signed SSL certificates")
            .bindSelected(state::isAllowSsl)
            .also { sslCheckbox = it.component }
        }
        row {
          checkBox("Create global Zowe Team Configuration file")
            .enabled(false)
        }
      }
    }
      .withMinimumWidth(500)
  }

  init {
    init()
    title = "Add Zowe Team Configuration file"
  }
}
