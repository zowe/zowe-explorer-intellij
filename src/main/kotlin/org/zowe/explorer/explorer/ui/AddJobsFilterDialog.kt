/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.zowe.explorer.common.ui.StatefulComponent
import org.zowe.explorer.config.ws.JobFilterStateWithWS
import org.zowe.explorer.utils.validateJobFilter
import javax.swing.JComponent

class AddJobsFilterDialog(
  project: Project?,
  override var state: JobFilterStateWithWS
) : DialogWrapper(project), StatefulComponent<JobFilterStateWithWS> {

  init {
    title = "Create Jobs Filter"
    init()
  }

  override fun createCenterPanel(): JComponent {
    lateinit var prefixField: JBTextField
    lateinit var ownerField: JBTextField
    lateinit var jobIdField: JBTextField
    lateinit var dialogPanel: DialogPanel
    val sameWidthGroup = "ADD_JOB_FILTER_DIALOG_LABELS_WIDTH_GROUP"

    class ValidatePrefix(
      var componentsToIsJobId: List<Pair<JBTextField, Boolean>>
    ) : DialogValidation {
      override fun validate(): ValidationInfo? {
        dialogPanel.validateAll()
        var validationInfo: ValidationInfo? = null
        componentsToIsJobId.forEach { (component, isJobId) ->
          validationInfo = validateJobFilter(prefixField.text, ownerField.text, jobIdField.text, state.ws.masks, component, isJobId)
          if (validationInfo != null) return validationInfo
        }
        return null
      }
    }

    dialogPanel = panel {
      row {
        label("JES working set: ")
          .widthGroup(sameWidthGroup)
        label(state.ws.name)
      }
      row {
        label("Prefix: ")
          .widthGroup(sameWidthGroup)
        textField()
          .bindText(state::prefix)
          .also { prefixField = it.component }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Owner: ")
          .widthGroup(sameWidthGroup)
        textField()
          .bindText(state::owner)
          .also { ownerField = it.component }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Job ID: ")
          .widthGroup(sameWidthGroup)
        textField()
          .bindText(state::jobId)
          .also { jobIdField = it.component }
          .horizontalAlign(HorizontalAlign.FILL)
      }
    }
      .apply{
        validationsOnInput=mapOf(
          prefixField to listOf(ValidatePrefix(listOf(prefixField to false, ownerField to false, jobIdField to true))),
          ownerField to listOf(ValidatePrefix(listOf(ownerField to false, prefixField to false, jobIdField to true))),
          jobIdField to listOf(ValidatePrefix(listOf(jobIdField to true, prefixField to false, ownerField to false)))
        )
      }
    return dialogPanel
  }
}
