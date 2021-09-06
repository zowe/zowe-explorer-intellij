package eu.ibagroup.formainframe.config.ws.ui.jobs

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.ValidationInfoBuilder
import eu.ibagroup.formainframe.common.ui.*
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsDialog
import eu.ibagroup.formainframe.config.ws.ui.JobsWorkingSetDialogState
import eu.ibagroup.formainframe.utils.crudable.Crudable
import javax.swing.JComponent

class JobsWsDialog(
  crudable: Crudable,
  state: JobsWorkingSetDialogState
): AbstractWsDialog<JobsWorkingSetConfig, JobsWorkingSetDialogState.TableRow, JobsWorkingSetDialogState>(crudable, JobsWorkingSetDialogState::class.java, state) {

  override val masksTable by lazy {
    ValidatingTableView(
      ValidatingListTableModel(PrefixColumn, OwnerColumn, JobIdColumn).apply {
        items = state.maskRow
      },
      disposable
    ).apply { rowHeight = DEFAULT_ROW_HEIGHT }
  }

  override val tableTitle = "Job Filters included in Working Set"

  init {
    init()
  }

  override fun emptyTableRow(): JobsWorkingSetDialogState.TableRow = JobsWorkingSetDialogState.TableRow()

  override fun validateOnApply(validationBuilder: ValidationInfoBuilder, component: JComponent): ValidationInfo? {
    return when {
      masksTable.listTableModel.validationInfos.asMap.isNotEmpty() -> {
        ValidationInfo("Fix errors in the table and try again", component)
      }
      masksTable.listTableModel.rowCount == 0 -> {
        validationBuilder.warning("You are going to create a Working Set that doesn't fetch anything")
      }
      else -> null
    }
  }


  object PrefixColumn : ValidatingColumnInfo<JobsWorkingSetDialogState.TableRow>("Prefix") {
    override fun valueOf(item: JobsWorkingSetDialogState.TableRow?): String? = item?.prefix
    override fun validateOnInput(
      oldItem: JobsWorkingSetDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      return if ((oldItem.owner.isNotEmpty() || newValue.isNotEmpty()) && oldItem.jobId.isNotEmpty()) {
        ValidationInfo("You must provide either an owner and a prefix or a job id.", component)
      } else null
    }

    override fun validateEntered(item: JobsWorkingSetDialogState.TableRow, component: JComponent): ValidationInfo? {
      return if ((item.owner.isNotEmpty() || item.prefix.isNotEmpty()) && item.jobId.isNotEmpty()) {
        ValidationInfo("You must provide either an owner and a prefix or a job id.", component)
      } else null
    }

    override fun isCellEditable(item: JobsWorkingSetDialogState.TableRow?): Boolean = true

    override fun setValue(item: JobsWorkingSetDialogState.TableRow, value: String) {
      item.prefix = value
    }
  }

  object OwnerColumn : ValidatingColumnInfo<JobsWorkingSetDialogState.TableRow>("Owner") {
    override fun valueOf(item: JobsWorkingSetDialogState.TableRow?): String? = item?.owner
    override fun validateOnInput(
      oldItem: JobsWorkingSetDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      return if ((oldItem.prefix.isNotEmpty() || newValue.isNotEmpty()) && oldItem.jobId.isNotEmpty()) {
        ValidationInfo("You must provide either an owner and a prefix or a job id.", component)
      } else null
    }

    override fun validateEntered(item: JobsWorkingSetDialogState.TableRow, component: JComponent): ValidationInfo? {
      return if ((item.prefix.isNotEmpty() || item.owner.isNotEmpty()) && item.jobId.isNotEmpty()) {
        ValidationInfo("You must provide either an owner and a prefix or a job id.", component)
      } else null
    }

    override fun isCellEditable(item: JobsWorkingSetDialogState.TableRow?): Boolean = true

    override fun setValue(item: JobsWorkingSetDialogState.TableRow, value: String) {
      item.owner = value
    }
  }

  object JobIdColumn : ValidatingColumnInfo<JobsWorkingSetDialogState.TableRow>("Job ID") {
    override fun valueOf(item: JobsWorkingSetDialogState.TableRow?): String? = item?.jobId
    override fun validateOnInput(
      oldItem: JobsWorkingSetDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      return if ((oldItem.prefix.isNotEmpty() || oldItem.owner.isNotEmpty()) && newValue.isNotEmpty()) {
        ValidationInfo("You must provide either an owner and a prefix or a job id.", component)
      } else null
    }

    override fun validateEntered(item: JobsWorkingSetDialogState.TableRow, component: JComponent): ValidationInfo? {
      return if ((item.prefix.isNotEmpty() || item.owner.isNotEmpty()) && item.jobId.isNotEmpty()) {
        ValidationInfo("You must provide either an owner and a prefix or a job id.", component)
      } else null
    }

    override fun isCellEditable(item: JobsWorkingSetDialogState.TableRow?): Boolean = true

    override fun setValue(item: JobsWorkingSetDialogState.TableRow, value: String) {
      item.jobId = value
    }
  }
}
