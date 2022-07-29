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
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.utils.validateDatasetMask
import eu.ibagroup.formainframe.utils.validateForBlank
import eu.ibagroup.formainframe.utils.validateUssMask
import eu.ibagroup.formainframe.utils.validateWorkingSetMaskName
import java.awt.Dimension
import javax.swing.JComponent

class AddMaskDialog(project: Project?, override var state: MaskState) : DialogWrapper(project),
  StatefulComponent<MaskState> {

  init {
    title = "Create Mask"
    init()
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      lateinit var comboBox: Cell<ComboBox<String>>
      var isSelectedAutomatically = false
      var isManualSelectionPerformed = false
      val sameWidthGroup = "ADD_MASK_DIALOG_LABELS_WIDTH_GROUP"

      row {
        label("Working Set: ")
          .widthGroup(sameWidthGroup)
        label(state.ws.name)
      }
      row {
        label("File system: ")
          .widthGroup(sameWidthGroup)
        comboBox = comboBox(listOf(MaskState.ZOS, MaskState.USS))
          .bindItem(state::type.toNullableProperty())
          .applyToComponent {
            addActionListener {
              if (!isSelectedAutomatically) {
                isManualSelectionPerformed = true
              } else {
                isSelectedAutomatically = false
              }
              state.type = selectedItem as String
            }
          }
      }
      row {
        label("Mask: ")
          .widthGroup(sameWidthGroup)
        textField()
          .bindText(state::mask)
          .validationOnInput {
            if (!isManualSelectionPerformed) {
              if (it.text.contains("/")) {
                state.type = MaskState.USS
              } else {
                state.type = MaskState.ZOS
              }
              isSelectedAutomatically = true
              comboBox.component.item = state.type
            }
            validateWorkingSetMaskName(it, state.ws)
          }
          .validationOnApply {
            validateForBlank(it.text, it)
              ?: if (state.type == MaskState.ZOS)
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

class MaskState(
  var ws: FilesWorkingSet,
  var mask: String = "",
  var type: String = "z/OS",
  var isSingle: Boolean = false,
) {
  companion object {
    const val ZOS = "z/OS"
    const val USS = "USS"
  }
}
