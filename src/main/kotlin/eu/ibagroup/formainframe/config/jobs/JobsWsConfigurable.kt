package eu.ibagroup.formainframe.config.jobs

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.common.ui.DEFAULT_ROW_HEIGHT
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.ValidatingTableView
import eu.ibagroup.formainframe.common.ui.toolbarTable
import eu.ibagroup.formainframe.config.*
import eu.ibagroup.formainframe.utils.isThe

class JobsWsConfigurable: BoundSearchableConfigurable("Job Working Sets", "mainframe") {
  private var panel: DialogPanel? = null

  override fun createPanel(): DialogPanel {
    val jobsWsTableModel = JobsWsTableModel(sandboxCrudable)
    val jobsWsTable = ValidatingTableView(jobsWsTableModel, disposable!!).apply {
      rowHeight = DEFAULT_ROW_HEIGHT
    }

    ApplicationManager.getApplication()
      .messageBus
      .connect(disposable!!)
      .subscribe(SandboxListener.TOPIC, object : SandboxListener {
        override fun <E : Any> update(clazz: Class<out E>) {
        }

        override fun <E : Any> reload(clazz: Class<out E>) {
          if (clazz.isThe<JobsWorkingSetConfig>()) {
            jobsWsTableModel.reinitialize()
          }
        }
      })

    return panel {
      row {
        cell(isVerticalFlow = true, isFullWidth = false) {
//          ActionLink("There is no defined Connections to create a Working Set with") {
//            goToAddConnection()
//          }()
//            .enableIf(ActionLinkPredicate(disposable!!))
//            .applyIfEnabled()
          toolbarTable(message("configurable.ws.tables.ws.title"), jobsWsTable) {
            addNewItemProducer { JobsWorkingSetConfig() }
            configureDecorator {
              disableUpDownActions()
              setAddAction {
                JobsWorkingSetConfig().toDialogState().initEmptyUuids(sandboxCrudable).let { s ->
                  JobsWsDialog(sandboxCrudable, s)
                    .apply {
                      if (showAndGet()) {
                        jobsWsTableModel.addRow(state.jobsWorkingSetConfig)
                        jobsWsTableModel.reinitialize()
                      }
                    }
                }
              }
              setEditAction {
                jobsWsTable.selectedObject?.let { selected ->
                  JobsWsDialog(sandboxCrudable, selected.toDialogState().apply { mode = DialogMode.UPDATE }).apply {
                    if (showAndGet()) {
                      val idx = jobsWsTable.selectedRow
                      val res = state.jobsWorkingSetConfig
                      jobsWsTableModel[idx] = res
                      jobsWsTableModel.reinitialize()
                    }
                  }
                }
              }
              setToolbarPosition(ActionToolbarPosition.BOTTOM)
            }
          }
        }
      }
    }.also {
      panel = it
    }
  }

  override fun apply() {
    val wasModified = isModified
    applySandbox<JobsWorkingSetConfig>()
    if (wasModified) {
      panel?.updateUI()
    }
  }

  override fun isModified(): Boolean {
    return isSandboxModified<JobsWorkingSetConfig>()
  }

  override fun reset() {
    val wasModified = isModified
    rollbackSandbox<JobsWorkingSetConfig>()
    if (wasModified) {
      panel?.updateUI()
    }
  }

  override fun cancel() {
    reset()
  }

}
