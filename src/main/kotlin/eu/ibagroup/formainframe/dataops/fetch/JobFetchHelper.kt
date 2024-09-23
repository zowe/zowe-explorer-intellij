/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.dataops.fetch

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.JobsRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.log.JobLogFetcher
import eu.ibagroup.formainframe.dataops.log.JobProcessInfo
import eu.ibagroup.formainframe.utils.asMutableList
import org.zowe.kotlinsdk.Job

/**
 * Helper runner class for getting job execution timestamps
 * @param query - jobs query for particular tree in JES Explorer
 * @param jobAttributes - remote job attributes to be updated
 */
class JobFetchHelper(private val query: RemoteQuery<ConnectionConfig, JobsFilter, Unit>, private val jobAttributes: RemoteJobAttributes) : Thread() {

  private val startKeyword = "STARTED"
  private val endKeyword = "ENDED"
  private val jobExecEndDateKeyword = "JOB EXECUTION DATE"
  private val jobJclNotAvailable = "JCL NOT AVAILABLE"
  private val jobJclNotRun = "JOB NOT RUN"

  private val JESMSGLG_SPOOL_FILE_ID = 1

  private val dateRegex = "[0-9]+\\s[a-zA-Z][a-zA-Z][a-zA-Z]\\s[0-9]+".toRegex()
  private val timeRegex = "[0-9]+\\.[0-9]+\\.[0-9]+".toRegex()

  private var updatedJobAttributes : RemoteJobAttributes = jobAttributes

  private var exception : Throwable? = null

  private val jobLogFetcher : JobLogFetcher = JobLogFetcher()

  private val jobProcessInfo = JobProcessInfo(jobAttributes.jobInfo.jobId, jobAttributes.jobInfo.jobName, query.connectionConfig)

  /**
   * Entry point for runner class. It's called first
   */
  override fun run() {
    runCatching {
      val currentJobLog = jobLogFetcher.fetchLogsBySpoolId(jobProcessInfo, JESMSGLG_SPOOL_FILE_ID)
      if (currentJobLog.isEmpty()) {
        updatedJobAttributes = updateAttributesForJob(JobTimestamps(null, null, jobJclNotAvailable, null))
      } else {
        val jobValues = parseJobLog(currentJobLog, jobProcessInfo)
        updatedJobAttributes = updateAttributesForJob(jobValues)
      }
    }.onFailure {
      exception = it
    }
  }

  /**
   * Parses JESMSGLN spool file to get necessary timestamps
   * @param jobLog - job log returned from JobLogFetcher
   * @param key - job key for parsing spool file
   * @return JobTimestamps instance for particular job key
   */
  private fun parseJobLog(jobLog : Array<String>, key: JobProcessInfo) : JobTimestamps {
    var execStartedDate : String? = null
    var execEndedDate : String? = null
    var execStartedTime : String? = null
    var execEndedTime : String? = null
    var foundStartDate = false
    var foundEndDate = false
    var foundStartTime = false
    var foundEndTime = false
    val logRecord = jobLog[0]
    val splitLogRecord = logRecord.split("\n")
    val reversedLogRecord = splitLogRecord.reversed()

    for(record in splitLogRecord) {
      if(!foundStartDate && dateRegex.containsMatchIn(record)) {
        execStartedDate = dateRegex.find(record)?.value
        foundStartDate = true
        if (!foundStartTime && key.jobId != null && record.indexOf(key.jobId) != -1) {
          execStartedTime = timeRegex.find(record)?.value
          foundStartTime = true
        }
      } else if(!foundStartTime && record.indexOf("${key.jobName}") != -1 && record.indexOf(startKeyword) != -1) {
        execStartedTime = timeRegex.find(record)?.value
        foundStartTime = true
      } else if(foundStartDate && foundStartTime) {
        break
      }
    }
    for(record in reversedLogRecord) {
      if (!foundEndDate && record.indexOf(jobExecEndDateKeyword) != -1) {
        execEndedDate = dateRegex.find(record.substringBefore(jobExecEndDateKeyword))?.value
        foundEndDate = true
      } else if (!foundEndTime && record.indexOf("${key.jobName}") != -1 && (record.indexOf(endKeyword) != -1 || record.indexOf(jobJclNotRun) != -1)) {
        execEndedTime = timeRegex.find(record)?.value
        foundEndTime = true
      } else if (!foundEndDate && foundEndTime) {
        break
      }
    }

    if (!foundEndDate && foundEndTime) {
      execEndedDate = execStartedDate
    }
    if (!foundEndDate && !foundEndTime && !foundStartDate && !foundStartTime) {
      return JobTimestamps(null, null, null, null)
    }
    return JobTimestamps(execStartedDate, execStartedTime, execEndedDate, execEndedTime)
  }

  /**
   * Updates remote job attributes with corresponding timestamps for particular job key
   * @param key - job key to be updated
   * @param jobValues - job timestamps for this key
   * @return Void
   */
  private fun updateAttributesForJob(jobValues : JobTimestamps) : RemoteJobAttributes {
    return RemoteJobAttributes(
      buildJobInfo(jobAttributes.jobInfo, jobValues),
      query.connectionConfig.url,
      JobsRequester(query.connectionConfig, query.request).asMutableList()
    )
  }

  /**
   * Builds updated job info to pass in remote job attributes instance
   * @param oldJobInfo - old job info before update
   * @param jobValues - job execution timestamps
   * @return serialized Job instance to pass in remote job attributes
   */
  private fun buildJobInfo(oldJobInfo: Job, jobValues: JobTimestamps) : Job {
    return Job(
      oldJobInfo.jobId,
      oldJobInfo.jobName,
      oldJobInfo.subSystem,
      oldJobInfo.owner,
      oldJobInfo.status,
      oldJobInfo.type,
      oldJobInfo.jobClass,
      oldJobInfo.returnedCode,
      oldJobInfo.filesUrl,
      oldJobInfo.url,
      oldJobInfo.jobCorrelator,
      oldJobInfo.phase,
      oldJobInfo.phaseName,
      emptyList(),
      oldJobInfo.reasonNotRunning,
      oldJobInfo.execSystem,
      oldJobInfo.execMember,
      oldJobInfo.execSubmitted,
      (jobValues.execStartedDate ?: "") + " " + (jobValues.execStartedTime ?: ""),
      (jobValues.execEndedDate ?: "") + " " + (jobValues.execEndedTime ?: "")
    )
  }

  /**
   * Getter for the updated job attributes
   */
  fun getUpdatedJobAttributes() : RemoteJobAttributes {
    return updatedJobAttributes
  }

  /**
   * Getter for any exception returned during processing
   */
  fun getException() : Throwable? {
    return exception
  }

  /**
   * Data class to store job timestamps for each job key
   */
  data class JobTimestamps(
    val execStartedDate : String?,
    val execStartedTime : String?,
    val execEndedDate : String?,
    val execEndedTime : String?
  )
}
