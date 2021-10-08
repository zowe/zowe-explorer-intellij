/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

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

  override var contentMode: XIBMDataType = XIBMDataType(XIBMDataType.Type.TEXT)

  override val isCopyPossible: Boolean
    get() = !isDirectory && !isMigrated

  override val isPastePossible: Boolean
    get() = isDirectory && !isMigrated

}