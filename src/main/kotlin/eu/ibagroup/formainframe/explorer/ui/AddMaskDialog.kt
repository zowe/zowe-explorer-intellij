package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.utils.validation.validateDatasetMask
import eu.ibagroup.formainframe.utils.validation.validateForBlank
import eu.ibagroup.formainframe.utils.validation.validateUssMask
import eu.ibagroup.formainframe.utils.validation.validateWorkingSetMaskName
import javax.swing.JComponent

class AddMaskDialog(project: Project?, override var state: MaskState) : DialogWrapper(project),
  StatefulComponent<MaskState> {

  init {
    title = "Create Mask"
    init()
  }

  override fun createCenterPanel(): JComponent {

    return panel {
      row {
        label("Working Set: ")
        label(state.ws.name)
      }
      row {
        label("File System: ")
        ComboBox(CollectionComboBoxModel(listOf(MaskState.ZOS, MaskState.USS))).apply {
          addActionListener { state.type = this.selectedItem as String }
        }()
      }
      row {
        label("Mask: ")

        textField(state::mask).withValidationOnInput {
          validateWorkingSetMaskName(it, state.ws)
        }.withValidationOnApply {
          validateForBlank(it.text, it) ?: if (state.type == MaskState.ZOS)
            validateDatasetMask(it.text, component)
          else
            validateUssMask(it.text, it)

        }

      }
    }
  }


}

class MaskState(
  var ws: WorkingSet,
  var mask: String = "",
  var type: String = "z/OS",
  var isSingle: Boolean = false,
){
  companion object {
    const val ZOS = "z/OS"
    const val USS = "USS"
  }
}