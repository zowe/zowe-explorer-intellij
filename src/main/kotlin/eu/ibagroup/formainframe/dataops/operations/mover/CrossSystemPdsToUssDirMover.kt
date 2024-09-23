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
import eu.ibagroup.formainframe.api.apiWithBytesConverter
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import retrofit2.Response

/**
 * Factory for registering CrossSystemPdsToUssDirMover in Intellij IoC container.
 * @see CrossSystemPdsToUssDirMover
 * @author Valiantsin Krus
 */
class CrossSystemPdsToUssDirMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return CrossSystemPdsToUssDirMover(dataOpsManager)
  }
}

/**
 * Implements copying partitioned data set between different systems.
 * @author Valiantsin Krus
 */
class CrossSystemPdsToUssDirMover(dataOpsManager: DataOpsManager) : AbstractPdsToUssFolderMover(dataOpsManager) {

  /**
   * Checks that source is PDS and dest is uss directory and source and directory located inside different systems.
   * @see OperationRunner.canRun
   */
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.source.isDirectory &&
      operation.destination.isDirectory &&
      operation.destinationAttributes is RemoteUssAttributes &&
      operation.sourceAttributes is RemoteDatasetAttributes &&
      operation.source is MFVirtualFile &&
      operation.destination is MFVirtualFile &&
      operation.commonUrls(dataOpsManager).isEmpty()
  }

  override val log = log<CrossSystemPdsToUssDirMover>()

  /**
   * Implements copying member from one system to another.
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
    val memberFile = operation.source.children.firstOrNull { it.name == memberName }
      ?: throw IllegalArgumentException("No member with name '$memberName' found.")
    val contentSynchronizer = dataOpsManager.getContentSynchronizer(memberFile)
      ?: throw IllegalStateException("Cannot find content synchronizer for file '${libraryAttributes.name}($memberName)'.")

    val syncProvider = DocumentedSyncProvider(memberFile)
    val memberAttributes = dataOpsManager.tryToGetAttributes(memberFile).castOrNull<RemoteMemberAttributes>()
      ?: throw IllegalStateException("Cannot find attribute for file '${libraryAttributes.name}($memberName)'.")

    contentSynchronizer.synchronizeWithRemote(syncProvider, progressIndicator)

    return apiWithBytesConverter<DataAPI>(destConnectionConfig).writeToUssFile(
      authorizationToken = destConnectionConfig.authToken,
      filePath = FilePath("$destinationPath/$memberName"),
      body = memberFile.contentsToByteArray(),
      xIBMDataType = memberAttributes.contentMode
    ).applyIfNotNull(progressIndicator) { indicator ->
      cancelByIndicator(indicator)
    }.execute()
  }

  /**
   * Starts operation execution. Throws throwable if something went wrong.
   * @throws Throwable
   * @see OperationRunner.run
   */
  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    val throwable: Throwable? = try {
      val sourceAttributes = operation.sourceAttributes as RemoteDatasetAttributes
      val destAttributes = operation.destinationAttributes as RemoteUssAttributes
      val sourceConnectionConfig = sourceAttributes.requesters.firstOrNull()?.connectionConfig
        ?: throw IllegalStateException("Cannot find connection for source PDS '${sourceAttributes.name}'.")
      val destConnectionConfig = destAttributes.requesters.firstOrNull()?.connectionConfig
        ?: throw IllegalStateException("Cannot find connection for dest USS folder '${destAttributes.path}'.")

      log.info("Trying to move PDS ${operation.source.name} from ${sourceConnectionConfig.url} to USS directory ${operation.destinationAttributes.path} on ${destConnectionConfig.url}")
      proceedPdsMove(sourceConnectionConfig, destConnectionConfig, operation, progressIndicator)
    } catch (t: Throwable) {
      t
    }
    if (throwable != null) {
      log.info("Failed to move dataset")
      throw throwable
    }
    log.info("Dataset has been moved successfully")
  }
}
