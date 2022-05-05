package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.utils.validateDatasetMask
import eu.ibagroup.formainframe.utils.validateForBlank
import eu.ibagroup.formainframe.utils.validateUssMask
import eu.ibagroup.formainframe.utils.validateWorkingSetMaskName
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent

class AddMaskDialog(project: Project?, override var state: MaskState) : DialogWrapper(project),
  StatefulComponent<MaskState> {

  init {
    title = "Create Mask"
    init()
  }

  override fun createCenterPanel(): JComponent {

    return panel {
      lateinit var comboBox: CellBuilder<ComboBox<String>>
      var maskTypeIsSelected = false
      row {
        label("Working Set: ")
        label(state.ws.name)
      }
      row {
        label("File System: ")
        comboBox = ComboBox(CollectionComboBoxModel(listOf(MaskState.ZOS, MaskState.USS))).apply {
          addActionListener {
            state.type = selectedItem as String
          }
          addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
              maskTypeIsSelected = true
            }
          })
        }()
      }
      row {
        label("Mask: ")

        textField(state::mask).withValidationOnInput {
          if (!maskTypeIsSelected) {
            if (it.text.contains("/")) {
              comboBox.component.item = MaskState.USS
            } else {
              comboBox.component.item = MaskState.ZOS
            }
          }
          validateWorkingSetMaskName(it, state.ws)
        }.withValidationOnApply {
          validateForBlank(it.text, it) ?: if (state.type == MaskState.ZOS)
            validateDatasetMask(it.text, component)
          else
            validateUssMask(it.text, it)
        }.apply {
          focused()
        }

      }
    }
  }


}

class MaskState(
  var ws: FilesWorkingSet,
  var mask: String = "",
  var type: String = "z/OS",
  var isSingle: Boolean = false,
){
  companion object {
    const val ZOS = "z/OS"
    const val USS = "USS"
  }
}
