package eu.ibagroup.formainframe.dataops.jeslog

import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.api.apiWithBytesConverter
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.r2z.JESApi
import eu.ibagroup.r2z.JobStatus
import eu.ibagroup.r2z.SpoolFile
import eu.ibagroup.r2z.SubmitJobRequest

class JobLogProvider(
  val connectionConfig: ConnectionConfig,
  val jobRequest: SubmitJobRequest
): LogProvider {

  fun fetchSpoolFiles(): List<SpoolFile> {
    var result = listOf<SpoolFile>()
    val jobIdNotNull = jobRequest.jobid ?: return result
    val jobNameNotNull = jobRequest.jobname ?: return result
    val response = api<JESApi>(connectionConfig).getJobSpoolFiles(
      basicCredentials = connectionConfig.authToken,
      jobName = jobNameNotNull,
      jobId = jobIdNotNull
    ).execute()
    if (response.isSuccessful) {
      response.body()?.let { result = it }
    }
    return result
  }

  fun fetchSpoolLog(spoolId: Int): String {
    var result = ""
    val jobIdNotNull = jobRequest.jobid ?: return result
    val jobNameNotNull = jobRequest.jobname ?: return result

    val response = apiWithBytesConverter<JESApi>(connectionConfig).getSpoolFileRecords(
      basicCredentials = connectionConfig.authToken,
      jobName = jobNameNotNull,
      jobId = jobIdNotNull,
      fileId = spoolId
    ).execute()
    if (response.isSuccessful) {
      response.body()?.let { result = String(it) }
    }
    return result
  }

  fun getJobStatus (): JobStatus? {
    var result: JobStatus? = null
    val jobIdNotNull = jobRequest.jobid ?: return result
    val jobNameNotNull = jobRequest.jobname ?: return result

    val response = api<JESApi>(connectionConfig).getJobStatus(
      basicCredentials = connectionConfig.authToken,
      jobName = jobNameNotNull,
      jobId = jobIdNotNull
    ).execute()
    if (response.isSuccessful) {
      response.body()?.let { result = it }
    }
    return result
  }

  override fun isLogFinished(): Boolean {
    return getJobStatus()?.status == JobStatus.Status.OUTPUT
  }

  override fun logPostfix(): String {
    val jobStatus = getJobStatus() ?: return ""

    return "\n------------------------------------------" +
        "\nJOB ${jobStatus.jobName}(${jobStatus.jobId}) EXECUTED\n" +
        "OWNER: ${jobStatus.owner}\n" +
        "RETURN CODE: ${jobStatus.returnedCode}\n" +
        "------------------------------------------"
  }

  override fun provideLog(): String {
    return fetchSpoolFiles().fold(StringBuilder("")) {
        acc, spool -> acc.append(fetchSpoolLog(spool.id))
    }.toString()
  }
}
