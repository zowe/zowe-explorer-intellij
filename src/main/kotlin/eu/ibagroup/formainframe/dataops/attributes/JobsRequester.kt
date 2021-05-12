package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.jobs.JobsFilter

class JobsRequester(
  override val connectionConfig: ConnectionConfig,
  val jobsFilter: JobsFilter
) : Requester