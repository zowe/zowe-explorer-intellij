package eu.ibagroup.formainframe.config.jesrun.ui

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.jesrun.JobSubmitConfiguration
import eu.ibagroup.formainframe.utils.MfFileType
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import java.awt.Color
import java.awt.Dimension
import javax.swing.*
import kotlin.streams.toList

class JobSubmitSettingsEditor
  : SettingsEditor<JobSubmitConfiguration>() {

  val connections by lazy { configCrudable.getAll<ConnectionConfig>().toList() }

  var connectionConfig: ConnectionConfig? = if (connections.isEmpty()) null else connections[0]
  lateinit var filePathLabel: JLabel



  var fileType = MfFileType.MEMBER
  var filePath = ""
  var memberName = ""

  override fun resetEditorFrom(s: JobSubmitConfiguration) {
    fileType = s.jobSubmitFileType
    filePath = s.jobSubmitFilePath
    memberName = s.jobSubmitMemberName ?: ""
    connectionConfig = configCrudable.getByUniqueKey(s.jobSubmitConnectionId)
  }

  override fun applyEditorTo(s: JobSubmitConfiguration) {
    s.jobSubmitConnectionId = connectionConfig?.uuid ?: connections[0]?.uuid ?: ""

  }

  override fun createEditor(): JComponent {
    return panel {
      row {
        label("connection:")
        comboBox(
          DefaultComboBoxModel(connections.toTypedArray()),
          getter = { connectionConfig },
          setter = { connectionConfig = it },
          ListCellRenderer { _, connectionConfig, _, _, _ ->
            if (connectionConfig != null) {
              label(connectionConfig.name ?: "No connections available").component
            } else if (connections.isNotEmpty()) {
              label(connections[0].name).component
            } else {
              label("No connections available").component.apply {
                foreground = Color.RED
                size = Dimension(150, height)
              }
            }
          }
        )
      }
      row {
        label("File type")
        comboBox(
          DefaultComboBoxModel(MfFileType.values()),
          getter = { fileType },
          setter = { fileType = it ?: MfFileType.MEMBER }
        )
      }
      row {
        label("Member Dataset: ")
          .also { filePathLabel = it.component }
        textField(::filePath)
      }
      row {
        label("Member name: ")
        textField(::memberName)
      }
    }
  }

}
