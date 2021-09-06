package eu.ibagroup.formainframe.config

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig

data class ConfigState(
  var connections: MutableList<ConnectionConfig> = mutableListOf(),
  var workingSets: MutableList<WorkingSetConfig> = mutableListOf(),
  var jobsWorkingSets: MutableList<JobsWorkingSetConfig> = mutableListOf()
)
