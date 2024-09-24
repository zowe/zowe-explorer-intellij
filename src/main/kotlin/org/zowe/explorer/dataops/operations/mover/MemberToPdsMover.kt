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

package org.zowe.explorer.dataops.operations.mover

import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.Requester
import org.zowe.explorer.dataops.operations.OperationRunner
import org.zowe.explorer.dataops.operations.OperationRunnerFactory
import org.zowe.explorer.utils.getParentsChain
import org.zowe.kotlinsdk.CopyDataZOS
import org.zowe.kotlinsdk.DataAPI
import retrofit2.Call
import java.io.FileNotFoundException

/**
 * Factory for registering MemberToPdsMover in Intellij IoC container.
 * @see MemberToPdsMover
 * @author Valiantsin Krus
 */
class MemberToPdsMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return MemberToPdsMover(dataOpsManager)
  }
}

/**
 * Implements copying of member to partitioned data set inside 1 system.
 * @author Viktar Mushtsin
 */
class MemberToPdsMover(dataOpsManager: DataOpsManager) : DefaultFileMover(dataOpsManager) {

  /**
   * Checks that source is member, destination is partitioned data set,
   * and source and destination are located within the same system.
   * @see OperationRunner.canRun
   */
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.destinationAttributes is RemoteDatasetAttributes
            && operation.destination.isDirectory
            && !operation.source.isDirectory
            && operation.sourceAttributes is RemoteMemberAttributes
            && operation.commonUrls(dataOpsManager).isNotEmpty()
            && !operation.destination.getParentsChain().containsAll(operation.source.getParentsChain())
  }

  /**
   * Builds call for copying member to partitioned data set.
   * @see DefaultFileMover.buildCall
   */
  override fun buildCall(
    operation: MoveCopyOperation,
    requesterWithUrl: Pair<Requester<ConnectionConfig>, ConnectionConfig>
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
