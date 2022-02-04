package eu.ibagroup.formainframe.config.jesrun

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.NopProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.jeslog.JobLogProvider
import eu.ibagroup.formainframe.dataops.jeslog.MFLogger
import eu.ibagroup.formainframe.dataops.operations.jobs.SubmitJobOperation
import eu.ibagroup.formainframe.dataops.operations.jobs.SubmitOperationParams
import eu.ibagroup.formainframe.utils.MfFilePath
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import java.lang.IllegalArgumentException

class JobSubmitConfiguration(
  project: Project,
  factory: ConfigurationFactory,
  name: String
): RunConfigurationBase<JobSubmitConfigurationOptions>(project, factory, name) {

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    return object: RunProfileState {
      override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val dataOpsManager = DataOpsManager.instance
        val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        lateinit var processHandler: ProcessHandler

        runCatching {
          val connectionConfig = configCrudable.getByUniqueKey<ConnectionConfig>(options.connectionConfigId)
            ?: throw IllegalArgumentException("Cannot find specified connection")

          val mfFilePath = MfFilePath(options.getJobSubmitFileType(), options.getJobSubmitFilePath(), options.getJobSubmitMemberName())
          val jobSubmitRequest = dataOpsManager.performOperation(SubmitJobOperation(
            SubmitOperationParams(mfFilePath.toString()),
            connectionConfig
          ))
          val mfLogger = MFLogger(JobLogProvider(connectionConfig, jobSubmitRequest), consoleView)
          mfLogger.startLogging()
          processHandler = mfLogger.processHandler
        }.onFailure {
          processHandler = NopProcessHandler()
          consoleView.attachToProcess(processHandler)
          processHandler.notifyTextAvailable(it.toString(), ProcessOutputType.STDERR)
          processHandler.destroyProcess()
        }

        return DefaultExecutionResult(consoleView, processHandler)
      }
    }
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    TODO("Not yet implemented")
  }

  override fun getOptions(): JobSubmitConfigurationOptions {
    return super.getOptions() as JobSubmitConfigurationOptions
  }

  fun getJobSubmitFileType() = options.jobSubmitFileType
  fun getJobSubmitFilePath() = options.jobSubmitFilePath
  fun getJobSubmitMemberName() = options.jobSubmitMemberName
}
