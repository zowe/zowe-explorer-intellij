/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.operations.migration

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.operations.OperationRunnerFactory
import org.zowe.explorer.dataops.operations.RemoteUnitOperation
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.HMigrate

/**
 * Data class which represents info about file needed for migration operation
 * @param file file that is needed to be migrated
 */
data class MigrateOperationParams(val file: VirtualFile)

/**
 * Class which represents migrate operation runner
 */
class MigrateOperationRunner : MigrationRunner<MigrateOperation> {

  /**
   * Checks if migrate operation can be applied to chosen object
   * @param operation migrate operation instance, contains info about file/object for migration
   * @return can migrate operation be run or not
   */
  override fun canRun(operation: MigrateOperation): Boolean {
    val file = operation.request.file
    val attributes = service<DataOpsManager>().tryToGetAttributes(file)
    return if (attributes is RemoteDatasetAttributes) {
      !attributes.isMigrated
    } else {
      false
    }
  }

  override val operationClass = MigrateOperation::class.java

  override val log = log<MigrateOperationRunner>()

  /**
   * Runs migrate operation
   * @param operation migrate operation instance which contains all info needed for performing operation
   * @param progressIndicator interrupts operation if the operation is canceled
   */
  override fun run(operation: MigrateOperation, progressIndicator: ProgressIndicator) {
    progressIndicator.checkCanceled()
    val response = api<DataAPI>(operation.connectionConfig).migrateDataset(
      authorizationToken = operation.connectionConfig.authToken,
      datasetName = operation.request.file.name,
      body = HMigrate(wait = true)
    ).cancelByIndicator(progressIndicator).execute()
    if (!response.isSuccessful) {
      throw CallException(
        response,
        "Cannot migrate dataset ${operation.request.file.name} on ${operation.connectionConfig.name}"
      )
    }
  }
}

/**
 * Class which represents factory for migrate operation
 */
class MigrateOperationFactory : OperationRunnerFactory {

  /**
   * Creates instance of migrate operation
   */
  override fun buildComponent(dataOpsManager: DataOpsManager): MigrationRunner<*> {
    return MigrateOperationRunner()
  }
}

/**
 * Data class which represents all info needed for migrate operation
 * @param request properties of object/file needed to be migrated
 * @param connectionConfig info about config to mainframe
 */
data class MigrateOperation(
  override val request: MigrateOperationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteUnitOperation<MigrateOperationParams>
