package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.explorer.JesWorkingSet
import eu.ibagroup.formainframe.utils.validateJobFilter
import eu.ibagroup.formainframe.utils.validateJobFilterOnInput
import javax.swing.JComponent

class AddJobsFilterDialog(
  project: Project?,
  override var state: JobsFilterState
) : DialogWrapper(project), StatefulComponent<JobsFilterState> {

  init {
    title = "Create Mask"
    init()
  }

  override fun createCenterPanel(): JComponent? {
    lateinit var prefixField: JBTextField
    lateinit var ownerField: JBTextField
    lateinit var jobIdField: JBTextField
    return panel {
      row {
        label("Jobs Working Set: ")
        label(state.ws.name)
      }
      row {
        label("Prefix: ")
        textField(state::prefix).also {
          prefixField = it.component
        }.withValidationOnApply {
          validateJobFilterOnInput(it) ?: validateJobFilter(it.text, ownerField.text, jobIdField.text, state.ws, it)
        }
      }
      row {
        label("Owner: ")
        textField(state::owner).also{
          ownerField = it.component
        }.withValidationOnApply {
          validateJobFilterOnInput(it) ?: validateJobFilter(prefixField.text, it.text, jobIdField.text, state.ws, it)
        }
      }
      row {
        label("Job ID: ")
        textField(state::jobId).also{
          jobIdField = it.component
        }.withValidationOnApply {
          validateJobFilterOnInput(it) ?: validateJobFilter(prefixField.text, ownerField.text, it.text, state.ws, it)
        }
      }
    }
  }
}

class JobsFilterState(
  var ws: JesWorkingSet,
  var prefix: String = "*",
  var owner: String = "*",
  var jobId: String = "",
) {

  fun toJobsFilter (): JobsFilter {
    val resultPrefix = prefix.ifEmpty { "*" }
    val resultOwner = owner.ifEmpty {
      CredentialService.instance.getUsernameByKey(ws.connectionConfig?.uuid ?: "") ?: ""
    }
    return JobsFilter(resultOwner, resultPrefix, jobId)
  }

}
