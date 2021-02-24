package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import javax.swing.JComponent

class AddMemberDialog(project: Project?, override var state: AddMemberState) : DialogWrapper(project),
  StatefulComponent<AddMemberState> {

  private val firstLetterRegex = Regex("[A-Z@\$#a-z]")
  private val memberRegex = Regex("[A-Z@\$#a-z]{1}[A-Z@#\$a-z0-9]{0,7}")

  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label("Member name")
        textField(state::memberName).withValidationOnInput {
          if (it.text.isNotEmpty() && !it.text[0].toString().matches(firstLetterRegex)) {
            ValidationInfo("Member name should start with A-Z a-z or national characters", it)
          } else if (!it.text.matches(memberRegex)) {
            ValidationInfo("Member name should contain only A-Z a-z 0-9 or national characters", it)
          } else {
            null
          }
        }
      }
    }
  }

  init {
    title = "Create Member"
    init()
  }
}

class AddMemberState(override var mode: DialogMode = DialogMode.CREATE, val datasetName: String) : DialogState {

  var memberName = ""
}