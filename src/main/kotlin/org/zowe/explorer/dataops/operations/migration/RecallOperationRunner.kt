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

package org.zowe.explorer.dataops.operations.migration

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.operations.OperationRunnerFactory
import org.zowe.explorer.dataops.operations.RemoteUnitOperation
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.HRecall

/**
 * Class which represents recall operation runner
 */
class RecallOperationRunner : MigrationRunner<RecallOperation> {

  /**
   * Determined if operation can be run on selected object
   * @param operation specifies a recall operation object
   */
  override fun canRun(operation: RecallOperation): Boolean {
    val file = operation.request.file
    val attributes = DataOpsManager.getService().tryToGetAttributes(file)
    return if (attributes is RemoteDatasetAttributes) {
      attributes.isMigrated
    } else {
      false
    }
  }

  override val operationClass = RecallOperation::class.java

  override val log = log<RecallOperationRunner>()

  /**
   * Runs a recall operation
   * @param operation recall operation to be run
   * @param progressIndicator progress indicator object
   * @throws CallException if request is not successful
   * @return Void
   */
  override fun run(operation: RecallOperation, progressIndicator: ProgressIndicator) {
    progressIndicator.checkCanceled()
    val response = api<DataAPI>(operation.connectionConfig).recallMigratedDataset(
      authorizationToken = operation.connectionConfig.authToken,
      datasetName = operation.request.file.name,
      body = HRecall(wait = true)
    ).cancelByIndicator(progressIndicator).execute()
    if (!response.isSuccessful) {
      throw CallException(
        response,
        "Cannot recall dataset ${operation.request.file.name} on ${operation.connectionConfig.name}"
      )
    }
  }
}

/**
 * Class which represents factory for recall operation runner. Defined in plugin.xml
 */
class RecallOperationFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): MigrationRunner<*> {
    return RecallOperationRunner()
  }
}

/**
 * Data class which represents input parameters for recall operation
 */
data class RecallOperationParams(
  val file: VirtualFile
)

/**
 * Data class which represents recall operation object
 */
data class RecallOperation(
  override val request: RecallOperationParams,
  override val connectionConfig: org.zowe.explorer.config.connect.ConnectionConfig,
) : RemoteUnitOperation<RecallOperationParams>
