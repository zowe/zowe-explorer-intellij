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
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.config.ws.MaskStateWithWS
import eu.ibagroup.formainframe.utils.*
import java.awt.Dimension
import javax.swing.JComponent

class AddMaskDialog(project: Project?, override var state: MaskStateWithWS) : DialogWrapper(project),
  StatefulComponent<MaskStateWithWS> {

  init {
    title = "Create Mask"
    init()
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      lateinit var comboBox: Cell<ComboBox<String>>
      val sameWidthGroup = "ADD_MASK_DIALOG_LABELS_WIDTH_GROUP"

      row {
        label("Files working set: ")
          .widthGroup(sameWidthGroup)
        label(state.ws.name)
      }
      row {
        label("File system: ")
          .widthGroup(sameWidthGroup)
        comboBox = comboBox(listOf(MaskType.ZOS.stringType, MaskType.USS.stringType))
          .bindItem(
            { state.type.stringType },
            { selectedType ->
              state.type = selectedType?.let { selectedTypeStr ->
                MaskType.values().find { it.stringType == selectedTypeStr }
              } ?: state.type
            }
          )
          .applyToComponent {
            addActionListener {
              if (!state.isTypeSelectedAutomatically) {
                state.isTypeSelectedManually = true
              } else {
                state.isTypeSelectedAutomatically = false
              }
              state.type = MaskType.values().find { it.stringType == selectedItem as String } ?: state.type
            }
          }
      }
      row {
        label("Mask: ")
          .widthGroup(sameWidthGroup)
        textField()
          .bindText(state::mask)
          .validationOnInput {
            if (!state.isTypeSelectedManually) {
              state.type = if (it.text.contains("/")) MaskType.USS else MaskType.ZOS
              state.isTypeSelectedAutomatically = true
              comboBox.component.item = state.type.stringType
            }
            null
          }
          .validationOnApply {
            validateForBlank(it.text, it)
              ?: validateWorkingSetMaskName(it, state.ws) ?: if (state.type == MaskType.ZOS)
                validateDatasetMask(it.text, component)
              else
                validateUssMask(it.text, it)
          }
          .apply { focused() }
          .apply {
            component.minimumSize = Dimension(10, component.height)
          }
          .horizontalAlign(HorizontalAlign.FILL)
      }
    }
  }


}
