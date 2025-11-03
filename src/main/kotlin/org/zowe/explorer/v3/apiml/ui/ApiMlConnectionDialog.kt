/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.v3.apiml.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.utils.validateForBlank
import org.zowe.explorer.utils.validateZosmfUrl
import org.zowe.explorer.v3.SupportedSchemes
import org.zowe.explorer.v3.state.config.getUrl
import org.zowe.explorer.v3.ui.validation.validateConnectionName
import javax.swing.JComponent

class ApiMlConnectionDialog(
  override var state: ApiMlConnectionDialogState,
  val project: Project? = null,
): StatefulDialog<ApiMlConnectionDialogState>(project) {

  init {
    init()
    title = when(state.mode) {
      DialogMode.CREATE -> "Add API ML Connection"
      DialogMode.UPDATE, DialogMode.READ, DialogMode.DELETE -> "Edit API ML Connection"
    }
  }

  override fun createCenterPanel(): JComponent {
    val sameWidthLabelsGroup = "CONNECTION_DIALOG_LABELS_WIDTH_GROUP"

    return panel {
      row {
        label("Connection name")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::name)
          .validationOnApply {
            it.text = it.text.trim()
            validateForBlank(it)
              ?: validateConnectionName(it, state.name.ifBlank { null }, state.configType)
          }
          .focused()
          .align(AlignX.FILL)
      }
//      row {
//        label("Scheme")
//          .widthGroup(sameWidthLabelsGroup)
//        comboBox(
//          listOf(SupportedSchemes.HTTP, SupportedSchemes.HTTPS)
//        )
//          .bindItem(state::scheme.toNullableProperty())
//          .align(AlignX.FILL)
//      }
//      row {
//        label("Host")
//          .widthGroup(sameWidthLabelsGroup)
//        textField()
//          .bindText(state::host)
//          .validationOnApply { null }
//          .align(AlignX.FILL)
//      }
//      row {
//        label("Port")
//          .widthGroup(sameWidthLabelsGroup)
//        intTextField()
//          .bindIntText(state::port)
//          .validationOnApply { null }
//          .align(AlignX.FILL)
//      }
      row {
        label("URL")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(
            { getUrl(state.apiMlConnectionConfig) },
            {
              runCatching {
                val httpUrl = it.toHttpUrl()
                state.scheme = SupportedSchemes.invoke(httpUrl.scheme)
                state.host = httpUrl.host
                state.port = httpUrl.port
              }
            }
          )
          .validationOnApply {
            validateZosmfUrl(it)
          }
          .align(AlignX.FILL)
      }
      row {
        label("Base path")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::basePath)
          .validationOnApply { null }
          .align(AlignX.FILL)
      }
      row {
        label("Username")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::username)
          .validationOnApply {
            it.text = it.text.trim()
            validateForBlank(it)
          }
          .onApply {
            state.username = state.username.uppercase()
          }
          .align(AlignX.FILL)
      }
      row {
        label("Password")
          .widthGroup(sameWidthLabelsGroup)
        passwordField()
          .bindText(
            { String(state.password) },
            { state.password = it.toCharArray() }
          )
          .validationOnApply {
            validateForBlank(it)
          }
          .align(AlignX.FILL)
      }
      indent {
        row {
          checkBox("Reject unauthorized certificates")
            .bindSelected(state::rejectUnauthorized)
            .applyToComponent {
              if (isSelected) {
                //showSelfSignedUsageWarningDialog(this)
              }
            }
          contextHelp(
            "Select this checkbox if your organization uses self-signed certificates (not recommended)."
              .trimMargin()
          )
        }
      }
      collapsibleGroup("Advanced Parameters", false) {
        row {
          label("Gateway path")
            .widthGroup(sameWidthLabelsGroup)
          textField()
            .bindText(state::gatewayPath)
            .validationOnApply { null }
            .align(AlignX.FILL)
        }
      }
    }
  }

}