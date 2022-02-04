package eu.ibagroup.formainframe.config.jesrun.ui

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.jesrun.JobSubmitConfiguration
import eu.ibagroup.formainframe.utils.crudable.getAll
import javax.swing.*
import kotlin.streams.toList

class JobSubmitSettingsEditor
  : SettingsEditor<JobSubmitConfiguration>() {

  val connections by lazy { configCrudable.getAll<ConnectionConfig>().toList() }

  var connectionConfig: ConnectionConfig? = null
  lateinit var filePathLabel: JLabel

  override fun resetEditorFrom(s: JobSubmitConfiguration) {
    TODO("Not yet implemented")
  }

  override fun applyEditorTo(s: JobSubmitConfiguration) {
    TODO("Not yet implemented")
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
            label(connectionConfig?.name ?: connections[0].name).component
          }
        )
      }
      row {
        label("")
      }
    }
  }

}
