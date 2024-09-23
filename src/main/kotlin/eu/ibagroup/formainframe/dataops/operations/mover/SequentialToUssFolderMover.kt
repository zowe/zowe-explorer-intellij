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
package eu.ibagroup.formainframe.dataops.operations.mover

import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.attributes.Requester
import eu.ibagroup.formainframe.dataops.attributes.USS_DELIMITER
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.utils.getParentsChain
import org.zowe.kotlinsdk.CopyDataUSS
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import retrofit2.Call

/**
 * Factory for registering SequentialToUssFolderMover in Intellij IoC container.
 * @see SequentialToUssFolderMover
 * @author Valiantsin Krus
 */
class SequentialToUssFolderFileMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return SequentialToUssFolderMover(dataOpsManager)
  }
}

/**
 * Implements copying of sequential data set to uss directory inside 1 system.
 * @author Viktar Mushtsin
 */
class SequentialToUssFolderMover(dataOpsManager: DataOpsManager) : DefaultFileMover(dataOpsManager) {

  /**
   * Checks that source is sequential data set, destination is uss directory,
   * and source and destination are located within the same system.
   * @see OperationRunner.canRun
   */
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.destinationAttributes is RemoteUssAttributes
            && operation.destination.isDirectory
            && !operation.source.isDirectory
            && operation.sourceAttributes is RemoteDatasetAttributes
            && operation.commonUrls(dataOpsManager).isNotEmpty()
            && !operation.destination.getParentsChain().containsAll(operation.source.getParentsChain())
  }

  /**
   * Builds call for copying sequential data set to uss directory.
   * @see DefaultFileMover.buildCall
   */
  override fun buildCall(
    operation: MoveCopyOperation,
    requesterWithUrl: Pair<Requester<ConnectionConfig>, ConnectionConfig>
  ): Call<Void> {
    val destinationAttributes = operation.destinationAttributes as RemoteUssAttributes
    val dataset = operation.sourceAttributes as RemoteDatasetAttributes
    val to = destinationAttributes.path + USS_DELIMITER + (operation.newName ?: dataset.name)
    return api<DataAPI>(
      url = requesterWithUrl.second.url,
      isAllowSelfSigned = requesterWithUrl.second.isAllowSelfSigned
    ).copyDatasetOrMemberToUss(
      authorizationToken = requesterWithUrl.first.connectionConfig.authToken,
      body = CopyDataUSS.CopyFromDataset(
        from = CopyDataUSS.CopyFromDataset.Dataset(dataset.name)
      ),
      filePath = FilePath(to)
    )
  }


}
