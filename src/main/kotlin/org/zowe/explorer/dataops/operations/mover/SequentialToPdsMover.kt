/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package org.zowe.explorer.dataops.operations.mover

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.CallException
import org.zowe.explorer.dataops.operations.OperationRunner
import org.zowe.explorer.dataops.operations.OperationRunnerFactory
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.getParentsChain
import org.zowe.kotlinsdk.CopyDataZOS
import org.zowe.kotlinsdk.DataAPI

/**
 * Factory for registering SequentialToPdsMover in Intellij IoC container.
 * @see SequentialToPdsMover
 * @author Valiantsin Krus
 */
class SequentialToPdsMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return SequentialToPdsMover(dataOpsManager)
  }
}

/**
 * Implements copying of sequential data set to partitioned data set inside 1 system.
 * @author Viktar Mushtsin
 */
class SequentialToPdsMover(val dataOpsManager: DataOpsManager) : AbstractFileMover() {

  /**
   * Checks that source is sequential data set, destination is partitioned data set,
   * and source and destination are located within the same system.
   * @see OperationRunner.canRun
   */
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.destinationAttributes is RemoteDatasetAttributes
            && operation.destination.isDirectory
            && !operation.source.isDirectory
            && operation.sourceAttributes is RemoteDatasetAttributes
            && operation.commonUrls(dataOpsManager).isNotEmpty()
            && !operation.destination.getParentsChain().containsAll(operation.source.getParentsChain())
  }

  /**
   * Builds call for copying sequential data set to partitioned data set.
   * @see DefaultFileMover.buildCall
   */
  private fun proceedMoveCopyToPds(
    connectionConfig: ConnectionConfig,
    operation: MoveCopyOperation,
    progressIndicator: ProgressIndicator
  ): Throwable? {
    val destinationAttributes = operation.destinationAttributes as RemoteDatasetAttributes
    var memberName: String
    val dataset = (operation.sourceAttributes as RemoteDatasetAttributes).also {
      memberName = it.name.split(".").last()
    }
    val response = api<DataAPI>(
      url = connectionConfig.url,
      isAllowSelfSigned = connectionConfig.isAllowSelfSigned
    ).copyToDatasetMember(
      authorizationToken = connectionConfig.authToken,
      body = CopyDataZOS.CopyFromDataset(
        dataset = CopyDataZOS.CopyFromDataset.Dataset(
          datasetName = dataset.name
        ),
        replace = operation.forceOverwriting
      ),
      toDatasetName = destinationAttributes.name,
      memberName = memberName
    ).cancelByIndicator(progressIndicator).execute()

    if (!response.isSuccessful && response.errorBody()?.string()?.contains("data set is empty") == true) {
      if (operation.isMove) {
        val deleteResponse = api<DataAPI>(
            url = connectionConfig.url,
            isAllowSelfSigned = connectionConfig.isAllowSelfSigned
        ).deleteDataset(
            authorizationToken = connectionConfig.authToken,
            datasetName = dataset.name
        ).cancelByIndicator(progressIndicator).execute()
        if (!deleteResponse.isSuccessful) {
          return CallException(deleteResponse, "Cannot delete source dataset '${dataset.name}'.")
        }
      }
    } else if (!response.isSuccessful) {
      return CallException(response, "Cannot move dataset '${dataset.name}'.")
    }
    return null
  }

  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    var throwable: Throwable? = null
    for ((requester, _) in operation.commonUrls(dataOpsManager)) {
      try {
        throwable = proceedMoveCopyToPds(requester.connectionConfig, operation, progressIndicator)
        break
      } catch (t: Throwable) {
        throwable = t
      }
    }
    if (throwable != null) {
      throw throwable
    }
  }
}
