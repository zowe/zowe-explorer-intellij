package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.r2z.FileModeValue
import javax.swing.JComponent

class UssFilePropertiesDialog(project: Project?, override var state: UssFileState) : DialogWrapper(project),
  StatefulComponent<UssFileState> {

  var fileTypeName: String = "File"

  init {

    if (state.ussAttributes.isDirectory)
      fileTypeName = "Directory"
    title = "$fileTypeName Properties"
    init()
  }

  override fun createCenterPanel(): JComponent {
    val tabbedPanel = JBTabbedPane()

    tabbedPanel.add("General", panel {

      row {
        label("$fileTypeName name: ")
        JBTextField(state.ussAttributes.name).apply { isEditable = false }()
      }
      row {
        label("Location: ")
        JBTextField(state.ussAttributes.parentDirPath).apply { isEditable = false }()
      }
      row {
        label("Path: ")
        JBTextField(state.ussAttributes.path).apply { isEditable = false }()
      }
      row {
        label("$fileTypeName size: ")
        JBTextField("${state.ussAttributes.length} bytes").apply { isEditable = false }()

      }
      row {
        label("Last modified: ")
        JBTextField(state.ussAttributes.modificationTime ?: "").apply {
          isEditable = false
        }()
      }
      if (state.ussAttributes.isSymlink) {
        row {
          label("Symlink to: ")
          JBTextField(state.ussAttributes.symlinkTarget ?: "").apply { isEditable = false }()
        }
      }
    })

    tabbedPanel.add("Permissions", panel {
      row {
        label("Owner: ")
        JBTextField(state.ussAttributes.owner ?: "").apply { isEditable = false }()
      }
      row {
        label("Group: ")
        JBTextField(state.ussAttributes.groupId ?: "").apply { isEditable = false }()
      }
      row {
        label("The numeric group ID (GID): ")
        JBTextField(state.ussAttributes.gid?.toString() ?: "").apply { isEditable = false }()
      }
      row {
        label("Owner permissions: ")
        JBTextField(state.ussAttributes.fileMode?.owner?.toFileModeValue().toString()).apply { isEditable = false }()

      }
      row {
        label("Group permissions: ")
        JBTextField(state.ussAttributes.fileMode?.group?.toFileModeValue().toString()).apply { isEditable = false }()
      }
      row {
        label("Permissions for all users: ")
        JBTextField(state.ussAttributes.fileMode?.all?.toFileModeValue().toString()).apply { isEditable = false }()
      }
    })
    return tabbedPanel
  }


}


class UssFileState(var ussAttributes: RemoteUssAttributes, override var mode: DialogMode = DialogMode.READ) :
  DialogState

//class MemberState(var member: Member, override var mode: DialogMode = DialogMode.CREATE) : PropertiesState(mode)

fun Int.toFileModeValue(): FileModeValue {
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