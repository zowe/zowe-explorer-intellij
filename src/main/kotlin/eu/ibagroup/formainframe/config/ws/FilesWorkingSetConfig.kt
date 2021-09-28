package eu.ibagroup.formainframe.config.ws

import eu.ibagroup.formainframe.utils.crudable.annotations.Column
import eu.ibagroup.formainframe.utils.isTheSameAs

class FilesWorkingSetConfig : WorkingSetConfig {

  @Column
  var dsMasks: MutableCollection<DSMask> = mutableListOf()

  @Column
  var ussPaths: MutableCollection<UssPath> = mutableListOf()

  constructor()

  constructor(
    name: String,
    connectionConfigUuid: String,
    dsMasks: MutableCollection<DSMask>,
    ussPaths: MutableCollection<UssPath>
  ) : super(name, connectionConfigUuid) {
    this.dsMasks = dsMasks
    this.ussPaths = ussPaths
  }

  constructor(
    uuid: String,
    name: String,
    connectionConfigUuid: String,
    dsMasks: MutableCollection<DSMask>,
    ussPaths: MutableCollection<UssPath>
  ) : super(name, connectionConfigUuid, uuid) {
    this.dsMasks = dsMasks
    this.ussPaths = ussPaths
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as FilesWorkingSetConfig

    if (name != other.name) return false
    if (connectionConfigUuid != other.connectionConfigUuid) return false
    if (!(dsMasks isTheSameAs  other.dsMasks)) return false
    if (!(ussPaths isTheSameAs  other.ussPaths)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + connectionConfigUuid.hashCode()
    result = 31 * result + dsMasks.hashCode()
    result = 31 * result + ussPaths.hashCode()
    return result
  }

  override fun toString(): String {
    return "WorkingSetConfig(name='$name', connectionConfigUuid='$connectionConfigUuid', dsMasks=$dsMasks, ussPaths=$ussPaths)"
  }

}
