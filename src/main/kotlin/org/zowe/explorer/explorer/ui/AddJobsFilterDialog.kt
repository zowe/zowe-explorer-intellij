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
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import org.zowe.explorer.common.ui.StatefulComponent
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.explorer.JesWorkingSet
import org.zowe.explorer.utils.validateJobFilter
import javax.swing.JComponent

class AddJobsFilterDialog(
  project: Project?,
  override var state: JobsFilterState
) : DialogWrapper(project), StatefulComponent<JobsFilterState> {

  init {
    title = "Create Jobs Filter"
    init()
  }

  override fun createCenterPanel(): JComponent? {
    lateinit var prefixField: JBTextField
    lateinit var ownerField: JBTextField
    lateinit var jobIdField: JBTextField
    return panel {
      row {
        label("Jobs Working Set: ")
        label(state.ws.name)
      }
      row {
        label("Prefix: ")
        textField(state::prefix).also {
          prefixField = it.component
        }.withValidationOnApply {
          validateJobFilter(it.text, ownerField.text, jobIdField.text, state.ws, it, false)
        }
      }
      row {
        label("Owner: ")
        textField(state::owner).also{
          ownerField = it.component
        }.withValidationOnApply {
          validateJobFilter(prefixField.text, it.text, jobIdField.text, state.ws, it, false)
        }
      }
      row {
        label("Job ID: ")
        textField(state::jobId).also{
          jobIdField = it.component
        }.withValidationOnApply {
          validateJobFilter(prefixField.text, ownerField.text, it.text, state.ws, it, true)
        }
      }
    }
  }
}

class JobsFilterState(
  var ws: JesWorkingSet,
  var prefix: String = "*",
  var owner: String = "*",
  var jobId: String = "",
) {

  fun toJobsFilter (): JobsFilter {
    val resultOwner = owner.ifBlank { "" }
    val resultPrefix = prefix.ifBlank { "" }
    val resultJobId = jobId.ifBlank { "" }
    return JobsFilter(resultOwner, resultPrefix, resultJobId)
  }

}
