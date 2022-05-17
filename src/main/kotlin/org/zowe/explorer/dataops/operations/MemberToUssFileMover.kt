/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.operations

import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.*
import org.zowe.explorer.utils.getParentsChain
import org.zowe.kotlinsdk.CopyDataUSS
import org.zowe.kotlinsdk.CopyDataZOS
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import retrofit2.Call

class MemberToUssFileMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return MemberToUssFileMover(dataOpsManager)
  }
}

class MemberToUssFileMover(dataOpsManager: DataOpsManager): DefaultFileMover(dataOpsManager) {
  override fun buildCall(
    operation: MoveCopyOperation,
    requesterWithUrl: Pair<Requester, ConnectionConfig>
  ): Call<Void> {
    val destinationAttributes = operation.destinationAttributes as RemoteUssAttributes
    val sourceAttributes = operation.sourceAttributes as RemoteMemberAttributes
    val pdsAttributes = sourceAttributes.getLibraryAttributes(dataOpsManager)
      ?: throw IllegalArgumentException("Cannot get PDS attributes of member \"${sourceAttributes.name}\"")
    val to = destinationAttributes.path + USS_DELIMITER + (operation.newName ?: sourceAttributes.name)
    return api<DataAPI>(
      url = requesterWithUrl.second.url,
      isAllowSelfSigned = requesterWithUrl.second.isAllowSelfSigned
    ).copyDatasetOrMemberToUss(
      authorizationToken = requesterWithUrl.first.connectionConfig.authToken,
      body = CopyDataUSS.CopyFromDataset(
        from = CopyDataUSS.CopyFromDataset.Dataset(
          datasetName = pdsAttributes.name,
          memberName = sourceAttributes.name
        )
      ),
      filePath = FilePath(to)
    )
  }

  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.destinationAttributes is RemoteUssAttributes
        && operation.destination.isDirectory
        && !operation.source.isDirectory
        && operation.sourceAttributes is RemoteMemberAttributes
        && operation.commonUrls(dataOpsManager).isNotEmpty()
        && !operation.destination.getParentsChain().containsAll(operation.source.getParentsChain())
  }
}
