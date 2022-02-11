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
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.utils.validateForBlank
import javax.swing.JComponent
import javax.swing.JTextField

class RenameDialog(project: Project?,
                   type: String,
                   override var state: String
) : DialogWrapper(project),
  StatefulComponent<String> {

  private var validationOnInput : (JTextField) -> ValidationInfo? = {null}
  private var validationOnApply : (JTextField) -> ValidationInfo? = {null}
  private var validationForBlankOnApply : (JTextField) -> ValidationInfo? = {null}

  fun withValidationOnApply(func: (JTextField) -> ValidationInfo?) : RenameDialog {
    validationOnApply = func
    return this
  }

  fun withValidationOnInput(func: (JTextField) -> ValidationInfo?) : RenameDialog {
    validationOnInput = func
    return this
  }

  fun withValidationForBlankOnApply(): RenameDialog {
    validationForBlankOnApply = { validateForBlank(it) }
    return this
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label("New name")
        textField(this@RenameDialog::state).withValidationOnInput {
          validationOnInput(it)
        }.withValidationOnApply {
          validationForBlankOnApply(it) ?: validationOnApply(it)
        }.apply {
          focused()
        }
      }
    }
  }

  init {
    title = "Rename $type"
    init()
  }


}
