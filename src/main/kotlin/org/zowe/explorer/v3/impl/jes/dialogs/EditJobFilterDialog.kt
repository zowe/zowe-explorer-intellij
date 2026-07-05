/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.jes.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import javax.swing.JComponent
import javax.swing.JTextField

/**
 * Dialog for editing an existing JES job filter in a `jes_ij` profile.
 * The fields are pre-filled with the current filter values.
 * Validation uses IntelliJ Kotlin UI DSL `validationOnInput` to react
 * immediately to every keystroke across all three fields
 * @param project the current project
 * @param configType the active config type
 * @param profileName the name of the JES profile containing the filter
 * @param currentOwner the current owner value of the filter
 * @param currentPrefix the current prefix value of the filter
 * @param currentId the current job ID value of the filter
 */
class EditJobFilterDialog(
  private val project: Project,
  private val configType: ConfigType,
  private val profileName: String,
  private val currentOwner: String,
  private val currentPrefix: String,
  private val currentId: String
) : DialogWrapper(project) {

  private val configService = ZoweConfigService.getService()

  private lateinit var ownerField: JTextField
  private lateinit var prefixField: JTextField
  private lateinit var idField: JTextField

  init {
    title = "Edit Job Filter"
    init()
  }

  private fun crossFieldValidation(): ValidationInfo? {
    if (!::ownerField.isInitialized) return null
    val hasOwnerOrPrefix = ownerField.text.isNotBlank() || prefixField.text.isNotBlank()
    val hasId = idField.text.isNotBlank()
    return when {
      !hasOwnerOrPrefix && !hasId ->
        ValidationInfo("Provide either an owner/prefix or a job ID", ownerField)
      hasId && hasOwnerOrPrefix ->
        ValidationInfo("A job ID cannot be combined with an owner or a prefix", idField)
      else -> null
    }
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
        label(profileName)
      }
      row("Owner:") {
        textField()
          .focused()
          .applyToComponent {
            ownerField = this
            text = currentOwner
          }
          .validationOnInput { crossFieldValidation() }
          .validationOnApply { crossFieldValidation() }
      }
      row("Prefix:") {
        textField()
          .applyToComponent {
            prefixField = this
            text = currentPrefix
          }
          .validationOnInput { crossFieldValidation() }
          .validationOnApply { crossFieldValidation() }
      }
      row("Job ID:") {
        textField()
          .applyToComponent {
            idField = this
            text = currentId
          }
          .validationOnInput { crossFieldValidation() }
          .validationOnApply { crossFieldValidation() }
      }
    }
  }

  override fun doOKAction() {
    try {
      EditJobFilterHandler(project.basePath, project)
        .editEntry(
          configType,
          profileName,
          currentOwner,
          currentPrefix,
          currentId,
          ownerField.text.trim(),
          prefixField.text.trim(),
          idField.text.trim()
        )
      super.doOKAction()
    } catch (e: Exception) {
      Messages.showErrorDialog(project, e.message ?: "Unknown error", "Failed to Edit Job Filter")
    }
  }
}