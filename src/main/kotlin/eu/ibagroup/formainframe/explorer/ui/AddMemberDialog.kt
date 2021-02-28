package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.dataops.operations.MemberAllocationParams
import eu.ibagroup.formainframe.utils.validation.validateForBlank
import eu.ibagroup.formainframe.utils.validation.validateMemberName
import javax.swing.JComponent

class AddMemberDialog(project: Project?, override var state: MemberAllocationParams) : DialogWrapper(project),
  StatefulComponent<MemberAllocationParams> {

  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label("Member name")
        textField(state::memberName).withValidationOnInput {
          validateMemberName(it)
        }.withValidationOnApply {
          validateForBlank(it)
        }
      }
    }
  }

  init {
    title = "Create Member"
    init()
  }

}