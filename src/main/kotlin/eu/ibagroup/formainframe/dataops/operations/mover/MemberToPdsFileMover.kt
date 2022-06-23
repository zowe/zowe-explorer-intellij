/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.operations.mover

import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.*
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.utils.getParentsChain
import eu.ibagroup.r2z.CopyDataZOS
import eu.ibagroup.r2z.DataAPI
import retrofit2.Call
import java.io.FileNotFoundException

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
