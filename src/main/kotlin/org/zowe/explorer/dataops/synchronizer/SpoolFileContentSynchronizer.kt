/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.synchronizer

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.api.apiWithBytesConverter
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.*
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.applyIfNotNull
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.*
import retrofit2.Response

class SpoolFileContentSynchronizerFactory: ContentSynchronizerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): ContentSynchronizer {
    return SpoolFileContentSynchronizer(dataOpsManager)
  }
}

class SpoolFileContentSynchronizer(
  dataOpsManager: DataOpsManager
) : DependentFileContentSynchronizer<MFVirtualFile, SpoolFile, JobsRequester, RemoteSpoolFileAttributes, RemoteJobAttributes>(dataOpsManager, log<SpoolFileContentSynchronizer>()) {
  override val vFileClass = MFVirtualFile::class.java

  override val storageNamePostfix = "jobs"

  override val attributesClass = RemoteSpoolFileAttributes::class.java

  override val parentAttributesClass = RemoteJobAttributes::class.java

  override fun executeGetContentRequest(
    attributes: RemoteSpoolFileAttributes,
    parentAttributes: RemoteJobAttributes,
    progressIndicator: ProgressIndicator?,
    requester: Requester
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
    requester: Requester,
    newContentBytes: ByteArray
  ): Response<Void>? = null

  override fun uploadNewContent(attributes: RemoteSpoolFileAttributes, newContentBytes: ByteArray) {}

}
