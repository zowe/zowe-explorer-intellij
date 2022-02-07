package eu.ibagroup.formainframe.config.jesrun.ui

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.migLayout.gapToBoundSize
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.flowableRow
import eu.ibagroup.formainframe.common.ui.labelOfWidth
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.jesrun.JobSubmitConfiguration
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.TextField
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxRenderer
import kotlin.streams.toList

class JobSubmitSettingsEditor
  : SettingsEditor<JobSubmitConfiguration>() {

  val connections by lazy { configCrudable.getAll<ConnectionConfig>().toList() }

  private val LABEL_WIDTH = 100

  lateinit var filePathLabel: JLabel
  lateinit var memberRow: Row

  var connectionsCombobox = ComboBox(connections.toTypedArray()).apply {
    renderer = ListCellRenderer { _, value, _, _, _ -> JBLabel(value?.name ?: "") }
  }
  var fileTypeCombobox = ComboBox(MfFileType.values())
  var filePathField = JBTextField("")
  var memberNameField = JBTextField("")

  override fun resetEditorFrom(s: JobSubmitConfiguration) {
    fileTypeCombobox.model.selectedItem = s.jobSubmitFileType
    filePathField.text = s.jobSubmitFilePath
    memberNameField.text = s.jobSubmitMemberName ?: ""
    var selectedConnection = configCrudable.getByUniqueKey<ConnectionConfig>(s.jobSubmitConnectionId)
    if (selectedConnection == null && connections.isNotEmpty()) {
      selectedConnection = connections[0]
    }
    connectionsCombobox.model.selectedItem = selectedConnection
  }

  override fun applyEditorTo(s: JobSubmitConfiguration) {
    var connection: ConnectionConfig? = connectionsCombobox.selectedItem as ConnectionConfig?
    if (connection == null && connections.isNotEmpty()) {
      connection = connections[0]
    }
    s.jobSubmitConnectionId = connection?.uuid ?: ""
    s.jobSubmitFileType = fileTypeCombobox.item
    s.jobSubmitFilePath = filePathField.text
    s.jobSubmitMemberName = memberNameField.text
  }

  override fun createEditor(): JComponent {
    return panel {
      flowableRow {
        labelOfWidth("Connection:", LABEL_WIDTH)
        if (connections.isEmpty()) {
          label("No connections available")
            .apply {
              component.foreground = Color.RED
            }
        } else {
          connectionsCombobox()
        }
      }
      flowableRow {
        labelOfWidth("File type:", LABEL_WIDTH)
        fileTypeCombobox.apply {
          var prevType = selectedItem
          addActionListener {
            if (prevType != selectedItem) {
              filePathField.text = ""
            }
            prevType = selectedItem
            when (selectedItem as MfFileType) {
              MfFileType.MEMBER -> {
                filePathLabel.text = "Member dataset:"
                memberRow.visible = true
              }
              MfFileType.DATASET -> {
                filePathLabel.text = "Dataset name:"
                memberRow.visible = false
              }
              else -> {
                filePathLabel.text = "Path to file:"
                memberRow.visible = false
              }
            }
          }
        }()
      }
      flowableRow {
        labelOfWidth("Member dataset:", LABEL_WIDTH)
          .also { filePathLabel = it.component }
        filePathField().withValidationOnInput {
          if (fileTypeCombobox.selectedItem == MfFileType.MEMBER ||
              fileTypeCombobox.selectedItem == MfFileType.DATASET) {
            validateDatasetNameOnInput(filePathField)
          } else validateUssMask(filePathField.text, filePathField)
        }
      }
      flowableRow {
        labelOfWidth("Member name:", LABEL_WIDTH)
        memberNameField().withValidationOnInput {
          if (fileTypeCombobox.selectedItem == MfFileType.MEMBER) {
            validateMemberName(memberNameField)
          } else null
        }
      }.also {
        memberRow = it
      }
    }
  }
}
