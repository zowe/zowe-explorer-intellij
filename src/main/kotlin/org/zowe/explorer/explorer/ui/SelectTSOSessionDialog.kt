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
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.util.preferredWidth
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.utils.nullable
import org.zowe.explorer.utils.validateTsoSessionSelection
import org.zowe.explorer.utils.initialize
import java.awt.Dimension
import javax.swing.JComponent

/**
 * Dialog to select TSO session from the list of sessions from the crudable
 */
class SelectTSOSessionDialog(
  project: Project?,
  private val crudable: Crudable,
  override var state: SelectTSOSessionDialogState
): StatefulDialog<SelectTSOSessionDialogState>(project = project) {

  private val tsoSessionComboBoxModel = CollectionComboBoxModel(crudable.getAll<TSOSessionConfig>().toList())

  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label("Specify TSO Session")
        comboBox(
          model = tsoSessionComboBoxModel,
          renderer = SimpleListCellRenderer.create("") { it.name }
        )
          .bindItem(
            {
              crudable.getAll<TSOSessionConfig>().findFirst().nullable
                .also { state.tsoSessionConfig = it }
            },
            { state.tsoSessionConfig = it }
          )
          .validationOnApply { validateTsoSessionSelection(it, crudable) }
          .align(AlignX.FILL)
          .applyToComponent {
            preferredWidth = 200
          }
      }.resizableRow()
    }.apply {
      minimumSize = Dimension(320, 50)
    }
  }

  init {
    title = "Select TSO Session"
    initialize { init() }
  }
}

/**
 * Data class which represents state for the TSO session selection dialog
 */
data class SelectTSOSessionDialogState(
  var tsoSessionConfig: TSOSessionConfig?
)
