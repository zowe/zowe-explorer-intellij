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
 * Dialog for editing an existing dataset mask or USS filter in a files profile.
 * The type combo box auto-detects the entry type based on the value input:
 * if the value starts with `/`, the type switches to USS Filter automatically.
 * Once the user manually selects a type, auto-detection stops
 * @param project the current project
 * @param configType the active config type
 * @param profileName the name of the files profile containing the entry
 * @param initialType the current type of the entry being edited
 * @param initialValue the current value of the mask or filter
 */
class EditMaskOrFilterDialog(
  private val project: Project,
  private val configType: ConfigType,
  private val profileName: String,
  private val initialType: EntryType,
  private val initialValue: String
) : DialogWrapper(project) {

  private val configService = ZoweConfigService.getService()

  private var selectedType = initialType
  private var isTypeSelectedManually = false
  private var isTypeSelectedAutomatically = false
  private lateinit var valueField: JTextField
  private lateinit var typeComboBox: ComboBox<EntryType>

  init {
    title = "Edit Mask or Filter"
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
          typeComboBox = this
          selectedItem = selectedType
          addActionListener {
            if (!isTypeSelectedAutomatically) {
              isTypeSelectedManually = true
            } else {
              isTypeSelectedAutomatically = false
            }
            selectedType = selectedItem as EntryType
          }
        })
      }
      row("Value:") {
        textField()
          .focused()
          .applyToComponent {
            valueField = this
            text = initialValue
          }
          .validationOnInput {
            if (!isTypeSelectedManually) {
              selectedType = if (it.text.contains("/")) {
                EntryType.USS_FILTER
              } else {
                EntryType.DS_MASK
              }
              isTypeSelectedAutomatically = true
              typeComboBox.selectedItem = selectedType
            }
            null
          }
          .validationOnApply {
            if (it.text.isBlank()) ValidationInfo("Value must not be empty", it)
            else null
          }
      }
    }
  }

  override fun doOKAction() {
    try {
      val newValue = valueField.text.trim()
      val actualType = typeComboBox.selectedItem as EntryType
      EditMaskOrFilterHandler(project.basePath, project)
        .editEntry(configType, profileName, initialType, initialValue, actualType, newValue)
      super.doOKAction()
    } catch (e: Exception) {
      Messages.showErrorDialog(project, e.message ?: "Unknown error", "Failed to Edit Entry")
    }
  }
}