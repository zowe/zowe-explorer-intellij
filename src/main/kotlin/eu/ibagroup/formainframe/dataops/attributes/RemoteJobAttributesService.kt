/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.util.SmartList
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.mergeWith
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.createAttributes
import eu.ibagroup.r2z.JobStatus


const val JOBS_FOLDER_NAME = "Jobs"

class RemoteJobAttributesServiceFactory : AttributesServiceFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): AttributesService<*, *> {
    return RemoteJobAttributesService(dataOpsManager)
  }

}

class RemoteJobAttributesService(
  val dataOpsManager: DataOpsManager
) : MFRemoteAttributesServiceBase<RemoteJobAttributes>(dataOpsManager){
  override val attributesClass = RemoteJobAttributes::class.java
  override val subFolderName = JOBS_FOLDER_NAME

  override fun buildUniqueAttributes(attributes: RemoteJobAttributes): RemoteJobAttributes {
    return RemoteJobAttributes(
      JobStatus(
        owner = attributes.jobInfo.owner,
        phase = attributes.jobInfo.phase,
        phaseName = attributes.jobInfo.phaseName,
        type = attributes.jobInfo.type,
        url = attributes.jobInfo.url,
        jobId = attributes.jobInfo.jobId,
        jobName = attributes.name,
        jobCorrelator = attributes.jobInfo.jobCorrelator,
        filesUrl = attributes.jobInfo.filesUrl
      ),
      url = attributes.url,
      requesters = SmartList()
    )
  }

  override fun mergeAttributes(
    oldAttributes: RemoteJobAttributes,
    newAttributes: RemoteJobAttributes
  ): RemoteJobAttributes {
    return RemoteJobAttributes(
      jobInfo = newAttributes.jobInfo,
      url = newAttributes.url,
      requesters = oldAttributes.requesters.mergeWith(newAttributes.requesters)
    )
  }

  override fun reassignAttributesAfterUrlFolderRenaming(
    file: MFVirtualFile,
    urlFolder: MFVirtualFile,
    oldAttributes: RemoteJobAttributes,
    newAttributes: RemoteJobAttributes
  ) {
    if (oldAttributes.name != newAttributes.name) {
      fsModel.renameFile(this, file, newAttributes.name);
    }
  }

  override fun continuePathChain(attributes: RemoteJobAttributes): List<PathElementSeed> {
    return listOf(
      PathElementSeed(attributes.jobInfo.subSystem ?: "NOSYS", createAttributes(directory = true)),
      PathElementSeed(attributes.name, createAttributes(directory = true))
    )
  }
}