/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.profiles

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import org.zowe.explorer.v3.dialogs.LazyDialog
import org.zowe.explorer.v3.impl.resolveUniqueName
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ConnectionProfiles
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

abstract class ProfileDialog(
  private val project: Project,
  private val configType: ConfigType,
  private val customTitle: String,
  profileName: String
) : LazyDialog<ProfileDialogState>(project) {
  private val zoweConfigService: ZoweConfigService = ZoweConfigService.getService()

  override var state: ProfileDialogState = ProfileDialogState(profileName, "")

  /**
   * Initializes [state] with connection profile selection.
   * Subclasses may override to provide custom initialization (e.g. reading current values from config)
   */
  protected abstract fun initState(connectionProfiles: ConnectionProfiles)

  protected abstract fun doOKActionCallback()

  override fun produceDialog(): DialogWrapper {
    return object : DialogWrapper(project) {
      init {
        title = customTitle
        init()
      }

      override fun createCenterPanel(): JComponent {
        val connectionProfiles = zoweConfigService.findConnectionProfiles(configType, project.basePath)

        if (connectionProfiles.defaultProfile != null && connectionProfiles.profiles.contains(connectionProfiles.defaultProfile)) {
          state.connectionProfile = connectionProfiles.defaultProfile
        } else if (connectionProfiles.profiles.isNotEmpty()) {
          state.connectionProfile = connectionProfiles.profiles.first()
        }

        return panel {
          row("Config type:") {
            label(configType.displayName)
            label(zoweConfigService.configPathDescription(configType)).applyToComponent {
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
              .validationOnInput { input ->
                zoweConfigService.getExplorerProfiles(configType, project.basePath)
                  ?.let { explorerNestedProfiles ->
                    val resolvedName = resolveUniqueName(input.text, explorerNestedProfiles.keySet())
                    if (input.text.isNotBlank() && resolvedName != input.text) {
                      warning("Profile with the same name already exists. It will be saved as '$resolvedName'")
                    } else null
                  }
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
        val configFile = zoweConfigService.resolveConfigFile(configType, project.basePath)
        if (!configFile.exists()) {
          val typeName = configType.displayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
          return ValidationInfo("$typeName Zowe Config does not exist under '${zoweConfigService.configPathDescription(configType)}' path")
        }
        return super.doValidate()
      }

      override fun doOKAction() {
        try {
          applyFields()
          doOKActionCallback()
          super.doOKAction()
        } catch (e: Exception) {
          Messages.showErrorDialog(project, e.message ?: "Unknown error", "Failed to $customTitle")
        }
      }
    }
  }
}
