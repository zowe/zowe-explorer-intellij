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

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.vfs.createAttributes
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystemModel
import org.zowe.kotlinsdk.XIBMDataType
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Abstraction for attributes services that work with child files (e.g. member of dataset, spool file of job).
 * @param InfoType class of Zowe Kotlin SDK response (e.g. Member, SpoolFile)
 * @param ParentAttributes attributes of parent file (for example for member it is RemoteDatasetAttributes)
 * @see AttributesService
 * @author Viktar Mushtsin
 * @author Valiantsin Krus
 */
abstract class DependentFileAttributesService<Attributes : DependentFileAttributes<InfoType, VFile>, InfoType, ParentAttributes : FileAttributes, VFile : VirtualFile>(
  private val dataOpsManager: DataOpsManager
) : AttributesService<Attributes, VFile> {


  protected abstract val parentAttributesClass: Class<out ParentAttributes>

  /**
   * Attributes service for working with files of corresponding parent file type.
   */
  private val remoteParentAttributesService by lazy {
    dataOpsManager.getAttributesService(parentAttributesClass, vFileClass)
  }

  private var fileToInfoMap = ConcurrentHashMap<VFile, InfoType>()
  private var fileToContentTypeMap = ConcurrentHashMap<VFile, XIBMDataType>()

  /**
   * Creates attributes for specified file.
   * @param info information about file on mainframe (instance of class Member or SpoolFile).
   * @param file instance of virtual file for which to build attributes.
   * @param contentMode IBM Data Type of passed file (not required).
   * @return created attributes.
   */
  protected abstract fun buildAttributes(info: InfoType, file: VFile, contentMode: XIBMDataType?): Attributes

  /**
   * Method of file system model that finds (or creates if not exist) file.
   * @see MFVirtualFileSystemModel.findOrCreateDependentFile
   */
  protected abstract val findOrCreateFileInVFSModel:
            (Any?, VFile, FileAttributes, com.intellij.openapi.util.io.FileAttributes) -> VFile

  /**
   * Method of file system model that moves and replaces file.
   * @see MFVirtualFileSystemModel.moveFileAndReplace
   */
  protected abstract val moveFileAndReplaceInVFSModel: (requestor: Any?, vFile: VFile, newParent: VFile) -> Unit

  /**
   * Checks if file has attributes in parent attributes service.
   * @param parentFile parent for which to find attributes.
   * @return instance of file if it has corresponding attributes or null otherwise
   */
  private fun annulParentIfNoAttributesFound(parentFile: VFile): VFile? {
    val parentAttributes = runReadAction { remoteParentAttributesService.getAttributes(parentFile) }
    return if (parentAttributes != null) {
      parentFile
    } else null
  }

  /**
   * Finds file in VFS model by attributes. Creates such file if it doesn't exist.
   * @see AttributesService.getOrCreateVirtualFile
   */
  override fun getOrCreateVirtualFile(attributes: Attributes): VFile {
    val parent = annulParentIfNoAttributesFound(attributes.parentFile)
    return if (parent != null && parent.isDirectory) {
      findOrCreateFileInVFSModel(this, parent, attributes, createAttributes(directory = false)).also {
        fileToInfoMap[it] = attributes.info
        fileToContentTypeMap[it] = attributes.contentMode
        sendTopic(
          topic = AttributesService.ATTRIBUTES_CHANGED,
          componentManager = dataOpsManager.componentManager
        ).onCreate(attributes, it)
      }
    } else throw IOException("Cannot find child")
  }

  /**
   * Finds file in VFS model by attributes.
   * @see AttributesService.getVirtualFile
   */
  override fun getVirtualFile(attributes: Attributes): VFile? {
    val parent = annulParentIfNoAttributesFound(attributes.parentFile)
    return parent?.findChild(attributes.name).castOrNull(vFileClass)
  }

  /**
   * Finds attributes for virtual file.
   * @see AttributesService.getAttributes
   */
  override fun getAttributes(file: VFile): Attributes? {
    val parent = file.parent?.castOrNull(vFileClass)?.let { annulParentIfNoAttributesFound(it) }
    val info = fileToInfoMap[file]
    return if (parent != null && info != null) {
      buildAttributes(info, parent, fileToContentTypeMap[file])
    } else null
  }

  /**
   * Updates attributes for specified file.
   * @see AttributesService.updateAttributes
   */
  override fun updateAttributes(file: VFile, newAttributes: Attributes) {
    val oldAttributes = getAttributes(file)
    if (oldAttributes != null) {
      var changed = false
      if (oldAttributes.parentFile != newAttributes.parentFile) {
        changed = true
        moveFileAndReplaceInVFSModel(this, oldAttributes.parentFile, newAttributes.parentFile)
      }
      if (oldAttributes.info != newAttributes.info) {
        changed = true
        fileToInfoMap[file] = newAttributes.info
      }
      if (oldAttributes.contentMode != newAttributes.contentMode) {
        changed = true
        fileToContentTypeMap[file] = newAttributes.contentMode
      }
      if (changed) {
        sendTopic(
          topic = AttributesService.ATTRIBUTES_CHANGED,
          componentManager = dataOpsManager.componentManager
        ).onUpdate(oldAttributes, newAttributes, file)
      }
    }
  }

  /**
   * Removes attributes of specified file and send message in ATTRIBUTES_CHANGED topic.
   * @param file file whose attributes should be removed.
   */
  override fun clearAttributes(file: VFile) {
    val attributes = getAttributes(file) ?: return
    fileToInfoMap.remove(file)
    fileToContentTypeMap.remove(file)
    sendTopic(
      topic = AttributesService.ATTRIBUTES_CHANGED,
      componentManager = dataOpsManager.componentManager
    ).onDelete(attributes, file)
  }
}
