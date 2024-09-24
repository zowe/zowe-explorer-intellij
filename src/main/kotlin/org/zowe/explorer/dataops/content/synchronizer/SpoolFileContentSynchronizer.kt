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

package org.zowe.explorer.dataops.content.synchronizer

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.apiWithBytesConverter
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.JobsRequester
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.attributes.RemoteSpoolFileAttributes
import org.zowe.explorer.dataops.attributes.Requester
import org.zowe.explorer.utils.applyIfNotNull
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.JESApi
import org.zowe.kotlinsdk.SpoolFile
import retrofit2.Response

/**
 * Factory for registering SpoolFileContentSynchronizer in Intellij IoC container
 */
class SpoolFileContentSynchronizerFactory : ContentSynchronizerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): ContentSynchronizer {
    return SpoolFileContentSynchronizer(dataOpsManager)
  }
}

/** Content synchronizer class for spool files */
class SpoolFileContentSynchronizer(
  dataOpsManager: DataOpsManager
) :
  DependentFileContentSynchronizer<MFVirtualFile, SpoolFile, JobsRequester, RemoteSpoolFileAttributes, RemoteJobAttributes>(
    dataOpsManager,
    log<SpoolFileContentSynchronizer>()
  ) {
  override val vFileClass = MFVirtualFile::class.java

  override val entityName = "jobs"

  override val attributesClass = RemoteSpoolFileAttributes::class.java

  override val parentAttributesClass = RemoteJobAttributes::class.java

  /**
   * Get content of the spool file
   * @param attributes spool file attributes to get its id
   * @param parentAttributes attributes of the job containing the spool file
   * @param requester instance to get connection configuration
   * @param progressIndicator a progress indicator for the operation
   */
  override fun executeGetContentRequest(
    attributes: RemoteSpoolFileAttributes,
    parentAttributes: RemoteJobAttributes,
    requester: Requester<ConnectionConfig>,
    progressIndicator: ProgressIndicator?
  ): Response<ByteArray> {
    return apiWithBytesConverter<JESApi>(requester.connectionConfig).getSpoolFileRecords(
      basicCredentials = requester.connectionConfig.authToken,
      jobName = parentAttributes.jobInfo.jobName,
      jobId = parentAttributes.jobInfo.jobId,
      fileId = attributes.info.id
    ).applyIfNotNull(progressIndicator) { indicator ->
      cancelByIndicator(indicator)
    }.execute()
  }

  override fun executePutContentRequest(
    attributes: RemoteSpoolFileAttributes,
    parentAttributes: RemoteJobAttributes,
    requester: Requester<ConnectionConfig>,
    newContentBytes: ByteArray,
    progressIndicator: ProgressIndicator?
  ): Response<Void>? = null

  override fun uploadNewContent(
    attributes: RemoteSpoolFileAttributes,
    newContentBytes: ByteArray,
    progressIndicator: ProgressIndicator?
  ) {
  }
}
