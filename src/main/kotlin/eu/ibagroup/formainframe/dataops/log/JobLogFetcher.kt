package eu.ibagroup.formainframe.dataops.log

import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.api.apiWithBytesConverter
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.r2z.JESApi
import eu.ibagroup.r2z.JobStatus
import eu.ibagroup.r2z.SpoolFile

class JobLogFetcherFactory: LogFetcherFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): LogFetcher<*> {
    return JobLogFetcher()
  }
}

class JobLogInfo(
  val jobId: String?,
  val jobName: String?,
  override val connectionConfig: ConnectionConfig
): LogInfo

class JobLogFetcher: LogFetcher<JobLogInfo> {

  override val logInfoClass = JobLogInfo::class.java
  private var cachedLog = mapOf<SpoolFile, String>()
  private var cachedJobStatus: JobStatus? = null

  private fun fetchSpoolFiles(jobLogInfo: JobLogInfo): List<SpoolFile> {
    var result = listOf<SpoolFile>()
    val jobIdNotNull = jobLogInfo.jobId ?: return result
    val jobNameNotNull = jobLogInfo.jobName ?: return result
    val response = api<JESApi>(jobLogInfo.connectionConfig).getJobSpoolFiles(
      basicCredentials = jobLogInfo.connectionConfig.authToken,
      jobName = jobNameNotNull,
      jobId = jobIdNotNull
    ).execute()
    if (response.isSuccessful) {
      response.body()?.let { result = it }
    }
    return result
  }

  private fun fetchSpoolLog(jobLogInfo: JobLogInfo, spoolId: Int): String {
    var result = ""
    val jobIdNotNull = jobLogInfo.jobId ?: return result
    val jobNameNotNull = jobLogInfo.jobName ?: return result

    val response = apiWithBytesConverter<JESApi>(jobLogInfo.connectionConfig).getSpoolFileRecords(
      basicCredentials = jobLogInfo.connectionConfig.authToken,
      jobName = jobNameNotNull,
      jobId = jobIdNotNull,
      fileId = spoolId
    ).execute()
    if (response.isSuccessful) {
      response.body()?.let { result = String(it) }
    }
    return result
  }

  private fun getJobStatus (jobLogInfo: JobLogInfo): JobStatus? {
    var result: JobStatus? = null
    val jobIdNotNull = jobLogInfo.jobId ?: return result
    val jobNameNotNull = jobLogInfo.jobName ?: return result

    val response = api<JESApi>(jobLogInfo.connectionConfig).getJobStatus(
      basicCredentials = jobLogInfo.connectionConfig.authToken,
      jobName = jobNameNotNull,
      jobId = jobIdNotNull
    ).execute()
    if (response.isSuccessful) {
      response.body()?.let { result = it }
    }
    return result.also {
      cachedJobStatus = result
    }
  }

  override fun isLogFinished(logInfo: JobLogInfo): Boolean {
    return getJobStatus(logInfo)?.status == JobStatus.Status.OUTPUT
  }

  override fun logPostfix(logInfo: JobLogInfo): String {
    val jobStatus = getJobStatus(logInfo) ?: return ""

    return "\n------------------------------------------" +
        "\nJOB ${jobStatus.jobName}(${jobStatus.jobId}) EXECUTED\n" +
        "OWNER: ${jobStatus.owner}\n" +
        "RETURN CODE: ${jobStatus.returnedCode}\n" +
        "------------------------------------------"
  }

  override fun fetchLog(logInfo: JobLogInfo): Array<String> {
    return fetchSpoolFiles(logInfo)
      .associateWith { fetchSpoolLog(logInfo, it.id) }
      .also { cachedLog = it }
      .map { it.value }
      .toTypedArray()
  }

  fun getCachedLog(): Map<SpoolFile, String> {
    return cachedLog
  }

  fun getCachedJobStatus(): JobStatus? {
    return cachedJobStatus
  }

}
