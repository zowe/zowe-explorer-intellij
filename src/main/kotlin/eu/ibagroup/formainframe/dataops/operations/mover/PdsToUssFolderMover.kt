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
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import org.zowe.kotlinsdk.CopyDataUSS
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import org.zowe.kotlinsdk.XIBMBpxkAutoCvt
import retrofit2.Response

/**
 * Factory for registering PdsToUssFolderMover in Intellij IoC container.
 * @see PdsToUssFolderMover
 * @author Valiantsin Krus
 */
class PdsToUssFolderMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return PdsToUssFolderMover(dataOpsManager, MFVirtualFile::class.java)
  }
}

/**
 * Implements copying partitioned data set inside 1 system.
 * @author Valiantsin Krus
 */
class PdsToUssFolderMover<VFile : VirtualFile>(
  dataOpsManager: DataOpsManager,
  val vFileClass: Class<out VFile>
) : AbstractPdsToUssFolderMover(dataOpsManager) {

  /**
   * Checks that source is PDS and destination is uss directory and source and destination located inside 1 system.
   * @see OperationRunner.canRun
   */
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.sourceAttributes is RemoteDatasetAttributes
            && operation.destinationAttributes is RemoteUssAttributes
            && operation.sourceAttributes.isDirectory
            && operation.destinationAttributes.isDirectory
            && operation.commonUrls(dataOpsManager).isNotEmpty()
  }

  override val log = log<PdsToUssFolderMover<VFile>>()

  /**
   * Implements copying member inside 1 system.
   * @see AbstractPdsToUssFolderMover.canRun
   */
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

  /**
   * Starts operation execution.
   * @see OperationRunner.run
   */
  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    var throwable: Throwable? = null
    for ((requester, _) in operation.commonUrls(dataOpsManager)) {
      try {
        log.info("Trying to move PDS ${operation.source.name} to USS folder ${operation.destination.path} on ${requester.connectionConfig.url}")
        throwable = proceedPdsMove(requester.connectionConfig, requester.connectionConfig, operation, progressIndicator)
        break
      } catch (t: Throwable) {
        throwable = t
      }
    }
    if (throwable != null) {
      log.info("Failed to move PDS")
      throw throwable
    }
    log.info("PDS has been moved successfully")
  }
}
