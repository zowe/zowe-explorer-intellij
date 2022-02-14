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

/**
 * Mainframe job process information necessary to extract job log or status.
 */
class JobProcessInfo(
  val jobId: String?,
  val jobName: String?,
  override val connectionConfig: ConnectionConfig
): MFProcessInfo

/**
 * Implementation of LogFetcher class to work with mainframe job logs.
 */
class JobLogFetcher: LogFetcher<JobProcessInfo> {


  override val mfProcessInfoClass = JobProcessInfo::class.java
  /**
   * Content of each spool file that was fetched after last request to mainframe.
   */
  private var cachedLog = mapOf<SpoolFile, String>()

  /**
   * Mainframe job status that was saved after last request to mainframe.
   */
  private var cachedJobStatus: JobStatus? = null

  /**
   * Extracts list of job spool files without creating any virtual files.
   * @param jobInfo job process information.
   * @return list of spool files data.
   */
  private fun fetchSpoolFiles(jobInfo: JobProcessInfo): List<SpoolFile> {
    var result = listOf<SpoolFile>()
    val jobIdNotNull = jobInfo.jobId ?: return result
    val jobNameNotNull = jobInfo.jobName ?: return result
    val response = api<JESApi>(jobInfo.connectionConfig).getJobSpoolFiles(
      basicCredentials = jobInfo.connectionConfig.authToken,
      jobName = jobNameNotNull,
      jobId = jobIdNotNull
    ).execute()
    if (response.isSuccessful) {
      response.body()?.let { result = it }
    }
    return result
  }

  /**
   * Fetches spool file content.
   * @param jobInfo job process information.
   * @param spoolId id of spool file to fetch content.
   */
  private fun fetchSpoolLog(jobInfo: JobProcessInfo, spoolId: Int): String {
    var result = ""
    val jobIdNotNull = jobInfo.jobId ?: return result
    val jobNameNotNull = jobInfo.jobName ?: return result

    val response = apiWithBytesConverter<JESApi>(jobInfo.connectionConfig).getSpoolFileRecords(
      basicCredentials = jobInfo.connectionConfig.authToken,
      jobName = jobNameNotNull,
      jobId = jobIdNotNull,
      fileId = spoolId
    ).execute()
    if (response.isSuccessful) {
      response.body()?.let { result = String(it) }
    }
    return result
  }

  /**
   * Requestes job status from mainframe.
   * @param jobInfo job process information.
   * @return statis of requested job.
   */
  private fun getJobStatus (jobInfo: JobProcessInfo): JobStatus? {
    var result: JobStatus? = null
    val jobIdNotNull = jobInfo.jobId ?: return null
    val jobNameNotNull = jobInfo.jobName ?: return null

    val response = api<JESApi>(jobInfo.connectionConfig).getJobStatus(
      basicCredentials = jobInfo.connectionConfig.authToken,
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

  /**
   * Check if job is finished.
   * @param mfProcessInfo job process information.
   * @return true if job finished and false otherwise.
   */
  override fun isLogFinished(mfProcessInfo: JobProcessInfo): Boolean {
    return getJobStatus(mfProcessInfo)?.status == JobStatus.Status.OUTPUT
  }

  /**
   * Forms job log execution result.
   * @param mfProcessInfo job process information.
   * @return formed postfix.
   */
  override fun logPostfix(mfProcessInfo: JobProcessInfo): String {
    val jobStatus = getJobStatus(mfProcessInfo) ?: return ""

    return "\n------------------------------------------" +
        "\nJOB ${jobStatus.jobName}(${jobStatus.jobId}) EXECUTED\n" +
        "OWNER: ${jobStatus.owner}\n" +
        "RETURN CODE: ${jobStatus.returnedCode}\n" +
        "------------------------------------------"
  }

  /**
   * Fetches full job log.
   * @param mfProcessInfo job process information.
   * @return log of spool files wrapped in array. Join this array to get full log.
   */
  override fun fetchLog(mfProcessInfo: JobProcessInfo): Array<String> {
    return fetchSpoolFiles(mfProcessInfo)
      .associateWith { fetchSpoolLog(mfProcessInfo, it.id) }
      .also { cachedLog = it }
      .map { it.value }
      .toTypedArray()
  }

  /**
   * Getter for cached log
   * @see JobLogFetcher.cachedLog
   */
  fun getCachedLog(): Map<SpoolFile, String> {
    return cachedLog
  }

  /**
   * Getter for job status
   * @see JobLogFetcher.cachedJobStatus
   */
  fun getCachedJobStatus(): JobStatus? {
    return cachedJobStatus
  }

}
