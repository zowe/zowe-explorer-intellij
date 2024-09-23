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
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.dataops.operations.MemberAllocationParams
import eu.ibagroup.formainframe.utils.validateForBlank
import eu.ibagroup.formainframe.utils.validateMemberName
import javax.swing.JComponent

/** Dialog to add dataset member */
class AddMemberDialog(project: Project?, override var state: MemberAllocationParams) : DialogWrapper(project),
  StatefulComponent<MemberAllocationParams> {

  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label("Member name: ")
        textField()
          .bindText(state::memberName)
          .validationOnApply { validateForBlank(it) ?: validateMemberName(it) }
          .apply { focused() }
          .align(AlignX.FILL)
      }
    }
  }

  init {
    title = "Create Member"
    init()
  }

}
