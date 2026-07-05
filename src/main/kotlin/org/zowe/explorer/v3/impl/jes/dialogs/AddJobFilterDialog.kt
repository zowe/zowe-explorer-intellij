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
 * Dialog for adding a JES job filter to an existing `jes_ij` profile.
 * A filter is built in one of two mutually-exclusive ways, mirroring the z/OSMF
 * list-jobs constraint: either by owner and/or prefix, or by a job ID — the job ID
 * cannot be combined with an owner or a prefix. At least one field must be provided
 * @param project the current project
 * @param configType the active config type
 * @param profileName the name of the JES profile to add the filter to
 */
class AddJobFilterDialog(
  private val project: Project,
  private val configType: ConfigType,
  private val profileName: String
) : DialogWrapper(project) {

  private val configService = ZoweConfigService.getService()

  private lateinit var ownerField: JTextField
  private lateinit var prefixField: JTextField
  private lateinit var idField: JTextField

  init {
    title = "Add Job Filter"
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
          .applyToComponent { ownerField = this }
          .validationOnInput { crossFieldValidation() }
          .validationOnApply { crossFieldValidation() }
      }
      row("Prefix:") {
        textField()
          .applyToComponent { prefixField = this }
          .validationOnInput { crossFieldValidation() }
          .validationOnApply { crossFieldValidation() }
      }
      row("Job ID:") {
        textField()
          .applyToComponent { idField = this }
          .validationOnInput { crossFieldValidation() }
          .validationOnApply { crossFieldValidation() }
      }
    }
  }

  override fun doOKAction() {
    try {
      AddJobFilterHandler(project.basePath, project)
        .addEntry(
          configType,
          profileName,
          ownerField.text.trim(),
          prefixField.text.trim(),
          idField.text.trim()
        )
      super.doOKAction()
    } catch (e: Exception) {
      Messages.showErrorDialog(project, e.message ?: "Unknown error", "Failed to Add Job Filter")
    }
  }
}