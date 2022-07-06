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

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.CopyDataUSS
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.FilePath
import eu.ibagroup.r2z.XIBMBpxkAutoCvt
import retrofit2.Response

// TODO: doc Valiantsin
class PdsToUssFolderMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return PdsToUssFolderMover(dataOpsManager, MFVirtualFile::class.java)
  }
}

class PdsToUssFolderMover<VFile : VirtualFile>(
  dataOpsManager: DataOpsManager,
  val vFileClass: Class<out VFile>
) : AbstractPdsToUssFolderMover(dataOpsManager) {
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.sourceAttributes is RemoteDatasetAttributes
            && operation.destinationAttributes is RemoteUssAttributes
            && operation.sourceAttributes.isDirectory
            && operation.destinationAttributes.isDirectory
            && operation.commonUrls(dataOpsManager).isNotEmpty()
  }


  override fun copyMember(
    operation: MoveCopyOperation,
    libraryAttributes: RemoteDatasetAttributes,
    memberName: String,
    sourceConnectionConfig: ConnectionConfig,
    destinationPath: String,
    destConnectionConfig: ConnectionConfig,
    progressIndicator: ProgressIndicator
  ): Response<*>? {
    return api<DataAPI>(sourceConnectionConfig).copyDatasetOrMemberToUss(
      sourceConnectionConfig.authToken,
      XIBMBpxkAutoCvt.OFF,
      CopyDataUSS.CopyFromDataset(
        from = CopyDataUSS.CopyFromDataset.Dataset(libraryAttributes.name, memberName.uppercase())
      ),
      FilePath(destinationPath)
    ).cancelByIndicator(progressIndicator).execute()
  }

  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    var throwable: Throwable? = null
    for ((requester, _) in operation.commonUrls(dataOpsManager)) {
      try {
        throwable = proceedPdsMove(requester.connectionConfig, requester.connectionConfig, operation, progressIndicator)
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
