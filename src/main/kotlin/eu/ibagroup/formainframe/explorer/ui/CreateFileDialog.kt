package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import com.intellij.openapi.ui.ValidationInfo
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.r2z.CreateUssFile
import eu.ibagroup.r2z.FileMode
import eu.ibagroup.r2z.FileModeValue
import eu.ibagroup.r2z.FileType
import javax.swing.JComponent

class CreateFileDialog(project: Project?, override var state: CreateFileState, filePath: String) :
  DialogWrapper(project), StatefulComponent<CreateFileState> {

  private val forbiddenSymbols = "/"
  private val warningSymbols = "^[^>|:& ]*$"
  private val forbiddenSymbolsList = "/"
  private val warningSymbolsList = ">, |, :, &"

  override fun createCenterPanel(): JComponent {

    val modelTemplate = CollectionComboBoxModel(
      listOf(
        FileModeValue.NONE,
        FileModeValue.READ,
        FileModeValue.WRITE,
        FileModeValue.READ_WRITE,
        FileModeValue.EXECUTE,
        FileModeValue.READ_EXECUTE,
        FileModeValue.READ_WRITE,
        FileModeValue.READ_WRITE_EXECUTE
      )
    )

    fun Int.toFileModeValue() : FileModeValue {
      return when (this) {
        0 -> FileModeValue.NONE
        1 -> FileModeValue.EXECUTE
        2 -> FileModeValue.WRITE
        3 -> FileModeValue.WRITE_EXECUTE
        4 -> FileModeValue.READ
        5 -> FileModeValue.READ_EXECUTE
        6 -> FileModeValue.READ_WRITE
        7 -> FileModeValue.READ_WRITE_EXECUTE
        else -> FileModeValue.NONE
      }
    }


    return panel {
      row {
        label("Name")
        textField(state::fileName).withValidationOnInput {
          val name = it.text
          return@withValidationOnInput if (name.length > 255) {
            ValidationInfo("File name exceed 255 symbols", it)
          } else if (name.contains(forbiddenSymbols)) {
            ValidationInfo("Forbidden character from $forbiddenSymbolsList was used", it)
          }
//          else if (!name.matches(Regex(warningSymbols))) {
//            ValidationInfo("Special character from $warningSymbolsList was used",
//              it).apply {
//                asWarning()
//                isOKActionEnabled = true
//            }
//          }
          else {
            null
          }
        }.withValidationOnApply {
          if (it.text.isBlank()) {
            ValidationInfo("Name cannot be blank", it)
          } else {
            null
          }
        }
      }
      row {
        label("Owner")
        comboBox(
          model = modelTemplate,
          modelBinding = PropertyBinding(
            get = { state.parameters.mode.owner.toFileModeValue() },
            set = {
              state.parameters.mode.owner = it?.mode ?: 0
            }
          )
        )
      }
      row {
        label("Group")
        comboBox(
          model = modelTemplate,
          modelBinding = PropertyBinding(
            get = { state.parameters.mode.group.toFileModeValue() },
            set = {
              state.parameters.mode.group = it?.mode ?: 0
            }
          )
        )
      }
      row {
        label("All")
        comboBox(
          model = modelTemplate,
          modelBinding = PropertyBinding(
            get = { state.parameters.mode.all.toFileModeValue() },
            set = {
              state.parameters.mode.all = it?.mode ?: 0
            }
          )
        )
      }
    }

  }

  init {
    val type = if (state.parameters.type == FileType.DIR) "Directory" else "File"
    title = "Create $type under $filePath"
    init()
  }

}


class CreateFileState(override var mode: DialogMode = DialogMode.CREATE, val parameters: CreateUssFile) : DialogState {

  var fileName: String = ""

  var path: String = ""

}

val emptyFileState: CreateFileState
  get() = CreateFileState(
    parameters = CreateUssFile(
      type = FileType.FILE,
      mode = FileMode(6, 6, 6)
    )
  )

val emptyDirState: CreateFileState
  get() = CreateFileState(
    parameters = CreateUssFile(
      type = FileType.DIR,
      mode = FileMode(7, 7, 7)
    )
  )