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
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.config.ws.JobFilterStateWithMultipleWS
import eu.ibagroup.formainframe.explorer.JesWorkingSet
import eu.ibagroup.formainframe.utils.validateJobFilter
import javax.swing.JComponent

class AddJobsFilterDialog(
  project: Project?,
  override var state: JobFilterStateWithMultipleWS
) : DialogWrapper(project), StatefulComponent<JobFilterStateWithMultipleWS> {

  companion object {

    // TODO: Remove when it becomes possible to mock class constructor with init section.
    /** Wrapper for init() method. It is necessary only for test purposes for now. */
    private fun initialize(init: () -> Unit) {
      init()
    }
  }

  private val wsSize = state.wsList.size
  private val wsComboBoxModel by lazy { CollectionComboBoxModel(state.wsList) }
  private lateinit var jesWSComboBox: ComboBox<JesWorkingSet>

  init {
    title = "Create Jobs Filter"
    initialize { init() }
  }

  override fun createCenterPanel(): JComponent {
    lateinit var prefixField: JBTextField
    lateinit var ownerField: JBTextField
    lateinit var jobIdField: JBTextField
    val sameWidthGroup = "ADD_JOB_FILTER_DIALOG_LABELS_WIDTH_GROUP"

    return panel {
      row {
        label("JES working set: ")
        if (wsSize > 1) {
          comboBox(wsComboBoxModel, SimpleListCellRenderer.create("") { it?.name })
            .bindItem(state::selectedWS.toNullableProperty())
            .also { jesWSComboBox = it.component }
            .widthGroup(sameWidthGroup)
        } else {
          label(state.selectedWS.name)
        }
      }
      row {
        label("Prefix: ")
          .widthGroup(sameWidthGroup)
        textField()
          .bindText(state::prefix)
          .also { prefixField = it.component }
          .validationOnApply {
            validateJobFilter(it.text, ownerField.text, jobIdField.text, if (wsSize > 1) (jesWSComboBox.selectedItem as JesWorkingSet).masks else state.selectedWS.masks, it, false)
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
            validateJobFilter(prefixField.text, it.text, jobIdField.text, if (wsSize > 1) (jesWSComboBox.selectedItem as JesWorkingSet).masks else state.selectedWS.masks, it, false)
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
            validateJobFilter(prefixField.text, ownerField.text, it.text, if (wsSize > 1) (jesWSComboBox.selectedItem as JesWorkingSet).masks else state.selectedWS.masks, it, true)
          }
          .align(AlignX.FILL)
      }
    }
  }
}
