/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.config.ws.JobFilterStateWithMultipleWS
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.utils.validateJobFilter
import javax.swing.JComponent

// TODO: JobsFilterDialogAbstraction
/** Edit jobs filter action dialog. Provides dialog view to change job filter values */
class EditJobsFilterDialog(
  project: Project?,
  override var state: JobFilterStateWithMultipleWS
) : DialogWrapper(project), StatefulComponent<JobFilterStateWithMultipleWS> {

  init {
    title = "Edit Jobs Filter"
    init()
  }

  override fun createCenterPanel(): JComponent {
    lateinit var prefixField: JBTextField
    lateinit var ownerField: JBTextField
    lateinit var jobIdField: JBTextField
    val initJobFilter = JobsFilter(state.owner, state.prefix, state.jobId)

    return panel {
      val sameWidthGroup = "EDIT_JOBS_FILTER_DIALOG_LABELS_WIDTH_GROUP"

      row {
        label("JES working set: ")
          .widthGroup(sameWidthGroup)
        label(state.selectedWS.name)
      }
      row {
        label("Prefix: ")
          .widthGroup(sameWidthGroup)
        textField()
          .bindText(state::prefix)
          .also { prefixField = it.component }
          .validationOnApply {
            validateJobFilter(initJobFilter, it.text, ownerField.text, jobIdField.text, state.selectedWS.masks, it, false)
          }
          .align(AlignX.FILL)
      }
      row {
        label("Owner: ")
          .widthGroup(sameWidthGroup)
        textField()
          .bindText(state::owner)
          .also { ownerField = it.component }
          .validationOnApply {
            validateJobFilter(initJobFilter, prefixField.text, it.text, jobIdField.text, state.selectedWS.masks, it, false)
          }
          .align(AlignX.FILL)
      }
      row {
        label("Job ID: ")
          .widthGroup(sameWidthGroup)
        textField()
          .bindText(state::jobId)
          .also { jobIdField = it.component }
          .validationOnApply {
            validateJobFilter(initJobFilter, prefixField.text, ownerField.text, it.text, state.selectedWS.masks, it, true)
          }
          .align(AlignX.FILL)
      }
    }
  }

}
