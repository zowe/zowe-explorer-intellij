package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.r2z.CreateUssFile
import eu.ibagroup.r2z.FileMode
import eu.ibagroup.r2z.FileModeValue
import eu.ibagroup.r2z.FileType
import javax.swing.JComponent

class CreateFileDialog(project: Project?, override var state: CreateFileState) :
  DialogWrapper(project), StatefulComponent<CreateFileState> {
  override fun createCenterPanel(): JComponent {

    return panel {
      row {
        label("Name")
        textField(state::fileName)
      }
      row {
        label("Owner")
        comboBox(
          model = CollectionComboBoxModel(
            listOf(
              FileModeValue.NONE,
              FileModeValue.WRITE
            )
          ),
          modelBinding = PropertyBinding(
            get = {state.parameters.mode},
            set = { v ->
              val value = v as FileModeValue
              state.parameters.mode.owner = value.mode
            }
          )
        )
      }
    }
  }

  init {
    val type = if(state.parameters.type == FileType.DIR)  "Directory" else  "File"
    title = "Create $type"
    init()
  }

}



class CreateFileState(override var mode: DialogMode = DialogMode.CREATE, val parameters: CreateUssFile) : DialogState {

  var fileName: String = ""



}

val emptyFileState: CreateFileState
  get() = CreateFileState(parameters = CreateUssFile(
    type = FileType.FILE,
    mode = FileMode(6,6,6)
  ))

val emptyDirState: CreateFileState
  get() = CreateFileState(parameters = CreateUssFile(
    type = FileType.DIR,
    mode = FileMode(6,6,6)
  ))