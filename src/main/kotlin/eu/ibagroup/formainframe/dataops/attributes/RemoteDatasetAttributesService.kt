package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.util.SmartList
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.mergeWith
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.createAttributes
import eu.ibagroup.r2z.Dataset

const val MIGRATED = "Migrated"
const val DATASETS_SUBFOLDER_NAME = "Data Sets"

class RemoteDatasetAttributesServiceFactory : AttributesServiceFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): AttributesService<*, *> {
    return RemoteDatasetAttributesService(dataOpsManager)
  }
}

class RemoteDatasetAttributesService(
  dataOpsManager: DataOpsManager
) : MFRemoteAttributesServiceBase<RemoteDatasetAttributes>(dataOpsManager) {

  override val attributesClass = RemoteDatasetAttributes::class.java

  override val subFolderName = DATASETS_SUBFOLDER_NAME

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

  override fun continuePathChain(attributes: RemoteDatasetAttributes): List<PathElementSeed> {
    return listOf(
      PathElementSeed(attributes.volser ?: MIGRATED, createAttributes(directory = true)),
      PathElementSeed(attributes.name, createAttributes(directory = attributes.isDirectory))
    )
  }

  override fun reassignAttributesAfterUrlFolderRenaming(
    file: MFVirtualFile,
    urlFolder: MFVirtualFile,
    oldAttributes: RemoteDatasetAttributes,
    newAttributes: RemoteDatasetAttributes
  ) {
    if (oldAttributes.volser != newAttributes.volser) {
      val volserDir = fsModel.findOrCreate(
        this, subDirectory, newAttributes.volser ?: MIGRATED, createAttributes(directory = true)
      )
      fsModel.moveFile(this, file, volserDir)
    }
    if (oldAttributes.name != newAttributes.name) {
      fsModel.renameFile(this, file, newAttributes.name)
    }
  }

}