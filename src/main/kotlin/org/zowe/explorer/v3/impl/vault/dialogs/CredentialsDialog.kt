/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.vault.dialogs

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import java.awt.Dimension
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JToggleButton

/**
 * Dialog for creating or editing a single secure credential field of a profile
 * in the Zowe Team Config. In edit mode, pre-fills the value from the OS secure store
 * and shows the credential name as a read-only label.
 * In create mode, allows selecting the credential name via a combo box.
 *
 * @param project the current project
 * @param configType the config type determining which zowe.config.json to use
 * @param profilePath the dot-separated profile path to create or edit a credential for
 * @param editFieldName the credential field name to edit, or null for create mode
 */
class CredentialsDialog(
  private val project: Project,
  private val configType: ConfigType,
  private val profilePath: String,
  private val editFieldName: String? = null
) : DialogWrapper(project) {

  private val configService = ZoweConfigService.getService()
  private val isEditMode = editFieldName != null
  private var selectedFieldName = editFieldName ?: ""
  private var fieldValue = ""

  companion object {
    val KNOWN_SECURE_FIELDS = listOf("user", "password", "keyPassphrase", "token", "tokenValue")
  }

  private val existingSecureFields: List<String> by lazy {
    configService.readAllProfiles(configType, project.basePath)
      .find { it.profilePath == profilePath }
      ?.secureFields ?: emptyList()
  }

  init {
    title = if (isEditMode) "Edit Credential" else "Create Credential"
    if (isEditMode && editFieldName != null) {
      fieldValue = configService.readSecureField(configType, project.basePath, profilePath, editFieldName) ?: ""
    }
    init()
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row("Config:") {
        label(configType.displayName)
        label(configService.configPathDescription(configType)).applyToComponent {
          foreground = UIUtil.getContextHelpForeground()
        }
      }
      row("Profile:") {
        label(profilePath)
      }
      row("Credential:") {
        if (isEditMode) {
          label(editFieldName!!)
        } else {
          val availableFields = KNOWN_SECURE_FIELDS.filter { it !in existingSecureFields }.toTypedArray()
          cell(ComboBox(DefaultComboBoxModel(availableFields)).apply {
            isSwingPopup = false
            if (availableFields.isNotEmpty()) {
              selectedItem = availableFields[0]
              selectedFieldName = availableFields[0]
            }
            addActionListener {
              selectedFieldName = selectedItem as? String ?: ""
            }
          })
        }
      }
      row("Value:") {
        lateinit var pwdField: JBPasswordField
        passwordField()
          .bindText(::fieldValue)
          .resizableColumn()
          .align(AlignX.FILL)
          .applyToComponent { pwdField = this }
        cell(createPasswordToggleButton(pwdField))
        cell()
      }
    }
  }

  /**
   * Creates a toggle button that switches the value field between hidden and visible modes.
   */
  private fun createPasswordToggleButton(passwordField: JBPasswordField): JToggleButton {
    val showHideButtonSize = Dimension(22, 22)
    return JToggleButton(AllIcons.Actions.Show)
      .apply {
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusPainted = false
        preferredSize = showHideButtonSize
        minimumSize = showHideButtonSize
        maximumSize = showHideButtonSize
        toolTipText = "Show value"
        addActionListener {
          if (isSelected) {
            passwordField.echoChar = 0.toChar()
            icon = ZoweExplorerIcons.hidePassword
            toolTipText = "Hide value"
          } else {
            passwordField.echoChar = '•'
            icon = AllIcons.Actions.Show
            toolTipText = "Show value"
          }
        }
      }
  }

  override fun doValidate(): ValidationInfo? {
    if (!isEditMode && selectedFieldName.isBlank()) {
      return ValidationInfo("Please select a credential field")
    }
    val configFile = configService.resolveConfigFile(configType, project.basePath)
    if (!configFile.exists()) {
      val typeName = configType.displayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
      return ValidationInfo("$typeName Zowe Config does not exist under '${configService.configPathDescription(configType)}' path")
    }
    return super.doValidate()
  }

  override fun doOKAction() {
    applyFields()
    val fieldName = if (isEditMode) editFieldName!! else selectedFieldName
    try {
      if (!isEditMode) {
        configService.addSecureFields(configType, project.basePath, project, profilePath, listOf(fieldName))
      }
      configService.saveSecureField(configType, project.basePath, profilePath, fieldName, fieldValue)
      super.doOKAction()
    } catch (e: Exception) {
      val action = if (isEditMode) "Save" else "Create"
      Messages.showErrorDialog(project, e.message ?: "Unknown error", "Failed to $action Credential")
    }
  }
}
