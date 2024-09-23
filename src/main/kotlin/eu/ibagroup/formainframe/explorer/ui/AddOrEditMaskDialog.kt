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
import com.intellij.ui.dsl.builder.*
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.getUsername
import eu.ibagroup.formainframe.config.ws.MaskStateWithWS
import eu.ibagroup.formainframe.utils.MaskType
import eu.ibagroup.formainframe.utils.validateDatasetMask
import eu.ibagroup.formainframe.utils.validateForBlank
import eu.ibagroup.formainframe.utils.validateUssMask
import eu.ibagroup.formainframe.utils.validateWorkingSetMaskName
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JTextField

/**
 * Dialog to add a new or edit an existing mask
 */
class AddOrEditMaskDialog(
  project: Project?,
  dialogTitle: String,
  config: ConnectionConfig?,
  override var state: MaskStateWithWS
) :
  DialogWrapper(project), StatefulComponent<MaskStateWithWS> {

  private lateinit var maskField: JTextField
  private val HLQ = if (config != null) getUsername(config) else null

  companion object {

    // TODO: Remove when it becomes possible to mock class constructor with init section.
    /** Wrapper for init() method. It is necessary only for test purposes for now. */
    private fun initialize(init: () -> Unit) {
      init()
    }
  }

  init {
    title = dialogTitle
    initialize { init() }
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
          .also {
            maskField = it.component
            if (maskField.text == "") {
              maskField.text = if (HLQ != null) "${HLQ}.*" else ""
            }
          }
          .validationOnInput {
            if (!state.isTypeSelectedManually) {
              state.type = if (it.text.contains("/")) MaskType.USS else MaskType.ZOS
              state.isTypeSelectedAutomatically = true
              comboBox.component.item = state.type.stringType
            }
            null
          }
          .validationOnApply {
            var validationResult = validateForBlank(it.text, it) ?: validateWorkingSetMaskName(it, state)
            if (validationResult == null) {
              validationResult = if (state.type == MaskType.ZOS)
                validateDatasetMask(it.text, component)
              else
                validateUssMask(it.text, it)
            }
            validationResult
          }
          .apply { focused() }
          .apply {
            component.minimumSize = Dimension(10, component.height)
          }
          .align(AlignX.FILL)
      }
    }
  }


}
