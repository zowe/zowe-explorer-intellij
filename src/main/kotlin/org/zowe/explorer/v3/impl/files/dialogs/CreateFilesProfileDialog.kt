/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

/**
 * Dialog for creating a new files profile inside an `explorer_ij` profile
 * in the target `zowe.config.json`.
 *
 * @param project the current project (used to resolve local config path)
 * @param configType the config type determining which zowe.config.json to write to
 */
class CreateFilesProfileDialog(
  private val project: Project,
  private val configType: ConfigType
) : DialogWrapper(project) {

  private val state = CreateFilesProfileDialogState()
  private val configService = ZoweConfigService.getService()
  private val handler = CreateFilesProfileHandler(configService, project.basePath, project)

  init {
    title = "Create Files Profile"
    init()
  }

  override fun createCenterPanel(): JComponent {
    val connectionProfiles = handler.findConnectionProfiles(configType)

    if (connectionProfiles.defaultProfile != null && connectionProfiles.profiles.contains(connectionProfiles.defaultProfile)) {
      state.connectionProfile = connectionProfiles.defaultProfile
    } else if (connectionProfiles.profiles.isNotEmpty()) {
      state.connectionProfile = connectionProfiles.profiles.first()
    }

    return panel {
      row("Config type:") {
        label(configType.displayName)
        label(configService.configPathDescription(configType)).applyToComponent {
          foreground = UIUtil.getContextHelpForeground()
        }
      }
      row("Profile name:") {
        textField()
          .bindText(state::profileName)
          .focused()
          .validationOnApply {
            if (it.text.isBlank()) ValidationInfo("Profile name must not be empty", it)
            else null
          }
          .validationOnInput {
            val resolvedName = handler.resolveProfileName(it.text, configType)
            if (it.text.isNotBlank() && resolvedName != it.text) {
              warning("Profile with the same name already exists. It will be saved as '$resolvedName'")
            } else null
          }
      }
      row("Connection profile:") {
        cell(ComboBox(DefaultComboBoxModel(connectionProfiles.profiles.toTypedArray())).apply {
          isSwingPopup = false
          renderer = SimpleListCellRenderer.create("") { profile ->
            if (profile == connectionProfiles.defaultProfile) "$profile (default)" else profile ?: ""
          }
          selectedItem = state.connectionProfile
          addActionListener {
            state.connectionProfile = selectedItem as? String ?: ""
          }
        })
      }
    }
  }

  override fun doValidate(): ValidationInfo? {
    val configFile = configService.resolveConfigFile(configType, project.basePath)
    if (!configFile.exists()) {
      val typeName = configType.displayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
      return ValidationInfo("$typeName Zowe Config does not exist under '${configService.configPathDescription(configType)}' path")
    }
    return super.doValidate()
  }

  override fun doOKAction() {
    try {
      applyFields()
      handler.generate(state, configType)
      super.doOKAction()
    } catch (e: Exception) {
      Messages.showErrorDialog(project, e.message ?: "Unknown error", "Failed to Create Files Profile")
    }
  }
}