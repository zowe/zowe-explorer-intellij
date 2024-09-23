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

package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.util.SmartList
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.mergeWith
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.createAttributes
import org.zowe.kotlinsdk.Job

const val JOBS_FOLDER_NAME = "Jobs"

/**
 * Factory for registering RemoteJobAttributesService.
 * @author Valiantsin Krus
 */
class RemoteJobAttributesServiceFactory : AttributesServiceFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): AttributesService<*, *> {
    return RemoteJobAttributesService(dataOpsManager)
  }

}

/**
 * Service to implement work with jobs attributes (e.g. create, update, clear).
 * @author Viktar Mushtsin
 * @author Valiantsin Krus
 */
class RemoteJobAttributesService(
  dataOpsManager: DataOpsManager
) : MFRemoteAttributesServiceBase<ConnectionConfig, RemoteJobAttributes>(dataOpsManager) {
  override val attributesClass = RemoteJobAttributes::class.java
  override val subFolderName = JOBS_FOLDER_NAME

  /**
   * Creates unique attributes based on the data from passed one ???
   * @see MFRemoteAttributesServiceBase.buildUniqueAttributes
   * @param attributes attributes to get data from.
   * @return unique attributes
   */
  override fun buildUniqueAttributes(attributes: RemoteJobAttributes): RemoteJobAttributes {
    return RemoteJobAttributes(
      Job(
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

  /**
   * Merge requesters from 2 attributes.
   * @param oldAttributes old attributes data.
   * @param newAttributes new attributes data.
   * @return job attributes with data from the new attributes and with the requesters merged with the old one.
   */
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

  // TODO: learn more and rewrite doc a bit
  /**
   * Reassigns attributes to another file, if it was moved or renamed ???
   * @see MFRemoteAttributesServiceBase.reassignAttributesAfterUrlFolderRenaming
   */
  override fun reassignAttributesAfterUrlFolderRenaming(
    file: MFVirtualFile,
    oldAttributes: RemoteJobAttributes,
    newAttributes: RemoteJobAttributes
  ) {
    if (oldAttributes.name != newAttributes.name) {
      file.rename(this, newAttributes.name)
      fsModel.setWritable(file, false)
    }
  }

  /**
   * Continues path chain with path of the job specified in the order subsystem -> jobName -> jobId.
   * Look at example below.
   *
   * Base path: For Mainframe -> <connection> -> Jobs
   * Job: subsystem = JES2, jobName = TESTJOB, jobId = TST00001
   * Result path: For Mainframe -> <connection> -> Jobs -> JES2 -> TESTJOB -> TST00001
   */
  override fun continuePathChain(attributes: RemoteJobAttributes): List<PathElementSeed> {
    return listOf(
      PathElementSeed(attributes.jobInfo.subSystem ?: "NOSYS", createAttributes(directory = true)),
      PathElementSeed(attributes.name, createAttributes(directory = true)),
      PathElementSeed(attributes.jobInfo.jobId, createAttributes(directory = true))
    )
  }
}
