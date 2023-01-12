/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.config.ws.JobFilterStateWithWS
import eu.ibagroup.formainframe.utils.validateJobFilter
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
    val sameWidthGroup = "ADD_JOB_FILTER_DIALOG_LABELS_WIDTH_GROUP"

    return panel {
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
          .validationOnApply {
            validateJobFilter(it.text, ownerField.text, jobIdField.text, state.ws.masks, it, false)
          }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Owner: ")
          .widthGroup(sameWidthGroup)
        textField()
          .bindText(state::owner)
          .also { ownerField = it.component }
          .validationOnApply {
            validateJobFilter(prefixField.text, it.text, jobIdField.text, state.ws.masks, it, false)
          }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Job ID: ")
          .widthGroup(sameWidthGroup)
        textField()
          .bindText(state::jobId)
          .also { jobIdField = it.component }
          .validationOnApply {
            validateJobFilter(prefixField.text, ownerField.text, it.text, state.ws.masks, it, true)
          }
          .horizontalAlign(HorizontalAlign.FILL)
      }
    }
  }
}
