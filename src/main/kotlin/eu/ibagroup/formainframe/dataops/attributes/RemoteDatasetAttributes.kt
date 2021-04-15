package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.r2z.Dataset
import eu.ibagroup.r2z.DatasetOrganization
import eu.ibagroup.r2z.HasMigrated
import eu.ibagroup.r2z.XIBMDataType

data class RemoteDatasetAttributes(
  val datasetInfo: Dataset,
  override val url: String,
  override val requesters: MutableList<MaskedRequester>
) : MFRemoteFileAttributes<MaskedRequester> {

  override fun clone(): RemoteDatasetAttributes {
    return RemoteDatasetAttributes(datasetInfo.clone(), url, requesters.map {
      MaskedRequester(it.connectionConfig, it.queryMask)
    }.toMutableList())
  }

  override val name
    get() = datasetInfo.name

  override val length = 0L

  val isMigrated
    get() = datasetInfo.migrated == HasMigrated.YES

  val isDirectory = !isMigrated && with(datasetInfo.datasetOrganization) {
    this == DatasetOrganization.PO || this == DatasetOrganization.POE
  }

  val volser
    get() = datasetInfo.volumeSerial
  override var contentMode: XIBMDataType = XIBMDataType.TEXT

}