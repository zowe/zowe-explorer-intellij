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

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.*
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.findAnyNullable
import org.zowe.explorer.utils.getParentsChain
import org.zowe.kotlinsdk.CopyDataZOS
import org.zowe.kotlinsdk.DataAPI
import retrofit2.Call
import java.io.FileNotFoundException
import java.io.IOException

class MemberToPdsFileMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return MemberToPdsFileMover(dataOpsManager)
  }
}

class MemberToPdsFileMover(dataOpsManager: DataOpsManager) : DefaultFileMover(dataOpsManager) {

  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.destinationAttributes is RemoteDatasetAttributes
        && operation.destination.isDirectory
        && !operation.source.isDirectory
        && operation.sourceAttributes is RemoteMemberAttributes
        && operation.commonUrls(dataOpsManager).isNotEmpty()
        && !operation.destination.getParentsChain().containsAll(operation.source.getParentsChain())
  }

  override fun buildCall(
    operation: MoveCopyOperation,
    requesterWithUrl: Pair<Requester, ConnectionConfig>
  ): Call<Void> {
    val destinationAttributes = operation.destinationAttributes as RemoteDatasetAttributes
    var memberName: String
    val datasetName = (operation.sourceAttributes as RemoteMemberAttributes).run {
      memberName = name
      dataOpsManager.tryToGetAttributes(parentFile)?.name
        ?: throw FileNotFoundException("Cannot find attributes for ${parentFile.path}")
    }
    return api<DataAPI>(
      url = requesterWithUrl.second.url,
      isAllowSelfSigned = requesterWithUrl.second.isAllowSelfSigned
    ).copyToDatasetMember(
      authorizationToken = requesterWithUrl.first.connectionConfig.authToken,
      body = CopyDataZOS.CopyFromDataset(
        dataset = CopyDataZOS.CopyFromDataset.Dataset(
          datasetName = datasetName,
          memberName = memberName
        ),
        replace = operation.forceOverwriting
      ),
      toDatasetName = destinationAttributes.name,
      memberName = operation.newName ?: memberName
    )
  }

}
