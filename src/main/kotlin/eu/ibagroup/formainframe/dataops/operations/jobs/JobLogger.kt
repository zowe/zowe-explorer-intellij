package eu.ibagroup.formainframe.dataops.operations.jobs

import com.intellij.execution.ui.ConsoleView
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.JobsRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.r2z.JESApi
import eu.ibagroup.r2z.SpoolFile
import eu.ibagroup.r2z.SubmitJobRequest

class JobLogger(
  val dataOpsManger: DataOpsManager,
  var jobName: String,
  val connectionConfig: ConnectionConfig,
  val consoleView: ConsoleView,
  var jobId: String? = null
) {
  val cachedLog: String = ""

  fun getSpoolFiles (): List<SpoolFile> {
    var result = listOf<SpoolFile>()
    val jobIdNotNull = jobId ?: return result
    val response = api<JESApi>(connectionConfig).getJobSpoolFiles(
      basicCredentials = connectionConfig.authToken,
      jobName = jobName,
      jobId = jobIdNotNull
    ).execute()
    if (response.isSuccessful) {
      response.body()?.let { result = it }
    }
    return result
  }


  fun startLogging() {
  }
}
