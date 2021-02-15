package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.r2z.Dataset
import eu.ibagroup.r2z.DatasetOrganization
import eu.ibagroup.r2z.HasMigrated

data class RemoteDatasetAttributes(
  val datasetInfo: Dataset,
  override val url: String,
  override val requesters: MutableList<MaskedRequester>
) : MFRemoteFileAttributes<MaskedRequester> {

  override fun clone(): RemoteDatasetAttributes {
    return RemoteDatasetAttributes(datasetInfo.clone(), url, requesters.map {
      MaskedRequester(it.user, it.queryMask)
    }.toMutableList())
  }

  val name
    get() = datasetInfo.name

  val isMigrated
    get() = datasetInfo.migrated == HasMigrated.YES

  val isDirectory = !isMigrated && with(datasetInfo.datasetOrganization) {
    this == DatasetOrganization.PO || this == DatasetOrganization.POE
  }

  val volser
    get() = datasetInfo.volumeSerial

}