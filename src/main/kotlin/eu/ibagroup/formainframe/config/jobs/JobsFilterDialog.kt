/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.config.jobs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import com.intellij.util.containers.isEmpty
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.findAnyNullable
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JTextField
import kotlin.streams.toList

class JobsFilterDialog(
  private val crudable: Crudable,
  project: Project? = null,
  override var state: JobsFilter = JobsFilter()
) : DialogWrapper(project), StatefulComponent<JobsFilter> {

  private lateinit var ownerField: JTextField
  private lateinit var prefixField: JTextField
  private lateinit var jobIdField: JTextField

  private val connectionComboBoxModel = CollectionComboBoxModel(crudable.getAll<ConnectionConfig>().toList())

  override fun createCenterPanel(): JComponent = panel {
    row {
      label("Specify connection")
      comboBox(
        model = connectionComboBoxModel,
        modelBinding = PropertyBinding(
          get = {
            val connectionConfig = crudable.getByUniqueKey<ConnectionConfig>(state.connectionConfigUuid)
            return@PropertyBinding if (connectionConfig != null) {
              connectionConfig
            } else if (!crudable.getAll<ConnectionConfig>().isEmpty()) {
              crudable.getAll<ConnectionConfig>().findAnyNullable()?.also {
                state.connectionConfigUuid = it.uuid
              }
            } else {
              null
            }
          },
          set = { config -> state.connectionConfigUuid = config?.uuid ?: "" }
        ),
        renderer = SimpleListCellRenderer.create("") { it?.name }
      ).withValidationOnApply {
        if (it.selectedItem == null) {
          ValidationInfo("You must provide a connection", it)
        } else {
          null
        }
      }
    }
    row {
      label("Owner")
      textField(state::owner).also {
        ownerField = it.component
      }.also { ownerField = it.component }
    }
    row {
      label("Job Prefix")
      textField(state::prefix).also {
        prefixField = it.component
      }.also { prefixField = it.component }
    }
    row {
      label("Job ID")
      textField(state::jobId).also { jobIdField = it.component }.withValidationOnApply {
        if ((prefixField.text.isNotEmpty() || ownerField.text.isNotEmpty() ) && jobIdField.text.isNotEmpty()) {
          ValidationInfo("You must provide either an owner and a prefix or a job id.", jobIdField)
        } else {
          null
        }
      }
    }

  }.apply {
    minimumSize = Dimension(500, 150)
  }

  init {
    title = "Job Filter"
    init()
  }


}