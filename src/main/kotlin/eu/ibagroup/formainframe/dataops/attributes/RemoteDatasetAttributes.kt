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

package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.utils.clone
import org.zowe.kotlinsdk.Dataset
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.HasMigrated
import org.zowe.kotlinsdk.XIBMDataType

/**
 * Attributes containing information about the dataset
 * @param datasetInfo information about the dataset that was received from zosmf
 * @param url resource URL based on original HTTP request
 * @param requesters list of information objects with query mask and connection configuration inside
 * @see MaskedRequester
 */
data class RemoteDatasetAttributes(
  val datasetInfo: Dataset,
  override val url: String,
  override val requesters: MutableList<MaskedRequester>
) : MFRemoteFileAttributes<ConnectionConfig, MaskedRequester> {

  /**
   * Clones current instance of dataset attributes.
   * @see FileAttributes.clone
   * @return cloned object
   */
  override fun clone(): RemoteDatasetAttributes {
    return RemoteDatasetAttributes(datasetInfo.clone(), url, requesters.map {
      MaskedRequester(it.connectionConfig, it.queryMask)
    }.toMutableList())
  }

  override val name
    get() = datasetInfo.name

  override val length = 0L

  val hasDsOrg = !isMigrated && datasetInfo.datasetOrganization != null

  val isMigrated
    get() = datasetInfo.migrated == HasMigrated.YES

  val isDirectory = !isMigrated && with(datasetInfo.datasetOrganization) {
    this == DatasetOrganization.PO || this == DatasetOrganization.POE
  }

  val volser
    get() = datasetInfo.volumeSerial

  override var contentMode: XIBMDataType = XIBMDataType(XIBMDataType.Type.TEXT)

  override val isCopyPossible: Boolean
    get() = hasDsOrg

  override val isPastePossible: Boolean
    get() = isDirectory && hasDsOrg

}
