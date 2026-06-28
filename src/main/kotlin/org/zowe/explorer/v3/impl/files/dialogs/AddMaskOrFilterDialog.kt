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
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JTextField

/**
 * Dialog for adding a dataset mask or USS filter to an existing files profile.
 * @param project the current project
 * @param configType the active config type
 * @param profileName the name of the files profile to add the entry to
 */
class AddMaskOrFilterDialog(
  private val project: Project,
  private val configType: ConfigType,
  private val profileName: String
) : DialogWrapper(project) {

  private val configService = ZoweConfigService.getService()

  private var selectedType = EntryType.DS_MASK
  private var entryValue = ""
  private lateinit var valueField: JTextField

  enum class EntryType(val displayName: String) {
    DS_MASK("Data Set Mask"),
    USS_FILTER("USS Filter");

    override fun toString() = displayName
  }

  init {
    title = "Add Mask or Filter"
    init()
  }

  override fun createCenterPanel(): JComponent {
    val typeComboModel = DefaultComboBoxModel(EntryType.entries.toTypedArray())
    return panel {
      row("Config:") {
        label(configType.displayName)
        label(configService.configPathDescription(configType)).applyToComponent {
          foreground = UIUtil.getContextHelpForeground()
        }
      }
      row("Profile:") {
        label(profileName)
      }
      row("Type:") {
        cell(ComboBox(typeComboModel).apply {
          selectedItem = selectedType
          addActionListener {
            selectedType = selectedItem as EntryType
            updateValueFieldLabel()
          }
        })
      }
      row("Value:") {
        textField()
          .focused()
          .applyToComponent {
            valueField = this
          }
          .validationOnApply {
            if (it.text.isBlank()) ValidationInfo("Value must not be empty", it)
            else null
          }
      }
    }
  }

  private fun updateValueFieldLabel() {
    valueField.toolTipText = when (selectedType) {
      EntryType.DS_MASK -> "e.g. USER.**"
      EntryType.USS_FILTER -> "e.g. /u/USER"
    }
  }

  override fun doOKAction() {
    try {
      entryValue = valueField.text.trim()
      AddMaskOrFilterHandler(configService, project.basePath, project)
        .addEntry(configType, profileName, selectedType, entryValue)
      super.doOKAction()
    } catch (e: Exception) {
      Messages.showErrorDialog(project, e.message ?: "Unknown error", "Failed to Add Entry")
    }
  }
}
