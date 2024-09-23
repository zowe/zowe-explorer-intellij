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
import org.zowe.kotlinsdk.Dataset

const val MIGRATED = "Migrated"
const val DATASETS_SUBFOLDER_NAME = "Data Sets"

/**
 * Factory class which builds remote dataset attributes service instance. Defined in plugin.xml
 */
class RemoteDatasetAttributesServiceFactory : AttributesServiceFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): AttributesService<*, *> {
    return RemoteDatasetAttributesService(dataOpsManager)
  }
}

/**
 * Service class to work and manipulate with remote dataset attributes
 */
class RemoteDatasetAttributesService(
  dataOpsManager: DataOpsManager
) : MFRemoteAttributesServiceBase<ConnectionConfig, RemoteDatasetAttributes>(dataOpsManager) {

  override val attributesClass = RemoteDatasetAttributes::class.java

  override val subFolderName = DATASETS_SUBFOLDER_NAME

  /**
   * Method to build unique attributes for a dataset
   * @param attributes - attributes from which to build unique remote attributes
   * @return a new instance of RemoteDatasetAttributes
   */
  override fun buildUniqueAttributes(attributes: RemoteDatasetAttributes): RemoteDatasetAttributes {
    return RemoteDatasetAttributes(
      Dataset(
        name = attributes.name,
        volumeSerial = attributes.volser
      ),
      url = attributes.url,
      requesters = SmartList()
    )
  }

  /**
   * Method to build a new remote dataset attributes by merging them with old attributes (by mask requester)
   * @param oldAttributes - old attributes from which to merge
   * @param newAttributes - new attributes to be merged
   */
  override fun mergeAttributes(
    oldAttributes: RemoteDatasetAttributes,
    newAttributes: RemoteDatasetAttributes
  ): RemoteDatasetAttributes {
    return RemoteDatasetAttributes(
      datasetInfo = newAttributes.datasetInfo,
      url = newAttributes.url,
      requesters = oldAttributes.requesters.mergeWith(newAttributes.requesters)
    )
  }

  /**
   * Method to build a list of paths elements from given attributes
   * @param attributes - attributes to build a path element chain
   */
  override fun continuePathChain(attributes: RemoteDatasetAttributes): List<PathElementSeed> {
    return listOf(
      PathElementSeed(attributes.volser ?: MIGRATED, createAttributes(directory = true)),
      PathElementSeed(attributes.name, createAttributes(directory = attributes.isDirectory))
    )
  }

  /**
   * Method to reassign attributes when original path(folder) is being moved, renamed or the file attribute is changed
   * @param file virtual file to be renamed
   * @param oldAttributes old attributes of the virtual file
   * @param newAttributes new attributes of the virtual file after folder renaming
   */
  override fun reassignAttributesAfterUrlFolderRenaming(
    file: MFVirtualFile,
    oldAttributes: RemoteDatasetAttributes,
    newAttributes: RemoteDatasetAttributes
  ) {
    if (oldAttributes.volser != newAttributes.volser) {
      val volserDir = fsModel.findOrCreate(
        this, subDirectory, newAttributes.volser ?: MIGRATED, createAttributes(directory = true)
      )
      file.move(this, volserDir)
    }
    if (oldAttributes.name != newAttributes.name) {
      file.rename(this, newAttributes.name)
    }
    if (oldAttributes.isDirectory != newAttributes.isDirectory) {
      fsModel.changeFileType(this, file, newAttributes.isDirectory)
    }
  }

}
