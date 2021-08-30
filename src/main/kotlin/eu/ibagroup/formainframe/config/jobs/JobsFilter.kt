package eu.ibagroup.formainframe.config.jobs

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.utils.crudable.EntityWithUuid
import eu.ibagroup.formainframe.utils.crudable.annotations.Column
import eu.ibagroup.formainframe.utils.crudable.annotations.ForeignKey
import org.jetbrains.annotations.NotNull


class JobsFilter : EntityWithUuid {
  @Column
  @ForeignKey(foreignClass = ConnectionConfig::class)
  var connectionConfigUuid: String = ""

  @Column
  var owner: String = ""

  @Column
  var prefix: String = ""

  @Column
  var jobId: String = ""

  @Column
  var userCorrelatorFilter: String = ""

  constructor()

  constructor(
    uuid: @NotNull String,
    connectionConfigUuid: String,
    owner: String,
    prefix: String,
    jobId: String,
    userCorrelatorFilter: String
  ) : super(uuid) {
    this.connectionConfigUuid = connectionConfigUuid
    this.owner = owner
    this.prefix = prefix
    this.jobId = jobId
    this.userCorrelatorFilter = userCorrelatorFilter
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as JobsFilter

    if (connectionConfigUuid != other.connectionConfigUuid) return false
    if (owner != other.owner) return false
    if (prefix != other.prefix) return false
    if (jobId != other.jobId) return false
    if (userCorrelatorFilter != other.userCorrelatorFilter) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + connectionConfigUuid.hashCode()
    result = 31 * result + owner.hashCode()
    result = 31 * result + prefix.hashCode()
    result = 31 * result + jobId.hashCode()
    result = 31 * result + userCorrelatorFilter.hashCode()
    return result
  }

  override fun toString(): String = if (jobId.isEmpty()) {
    "Owner = $owner, Prefix = $prefix"
  } else {
    "JobID = $jobId"
  }


}