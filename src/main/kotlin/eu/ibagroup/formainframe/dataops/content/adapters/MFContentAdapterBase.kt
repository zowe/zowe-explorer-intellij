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

package eu.ibagroup.formainframe.dataops.content.adapters

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.utils.`is`

/**
 * Abstraction with some implementations to facilitate implementations.
 * @param Attributes class of attributes of files that would be adapted by the implmenetation.
 * @param dataOpsManager instance of DataOpsManager service.
 * @author Valiantsin Krus
 */
abstract class MFContentAdapterBase<Attributes : FileAttributes>(
  protected val dataOpsManager: DataOpsManager
) : MFContentAdapter {
  abstract val vFileClass: Class<out VirtualFile>
  abstract val attributesClass: Class<out Attributes>

  /**
   * Checks if the current implementation of adapter can adapt content of specified file.
   * @see MFContentAdapter.accepts
   */
  override fun accepts(file: VirtualFile): Boolean {
    val fileAttributesClass = dataOpsManager.tryToGetAttributes(file)?.javaClass ?: return false
    return vFileClass.isAssignableFrom(file::class.java) &&
            attributesClass.isAssignableFrom(fileAttributesClass)
  }

  /**
   * Prepares content for uploading to the mainframe using virtual file attributes.
   * @param content content to adapt.
   * @param attributes attributes of the file whose content will be adapted.
   * @return adapted bytes.
   */
  abstract fun <T>adaptContentToMainframe(content: T, attributes: Attributes): T

  /**
   * Prepares content for uploading to the mainframe using virtual file instance.
   * @see MFContentAdapter.prepareContentToMainframe
   */
  @Suppress("UNCHECKED_CAST")
  override fun <T>prepareContentToMainframe(content: T, file: VirtualFile): T {
    val attributes = dataOpsManager.tryToGetAttributes(file) ?: return content
    return if (attributes.`is`(attributesClass)) adaptContentToMainframe(content, attributes as Attributes) else content
  }

  /**
   * Prepares content for uploading from mainframe to Intellij content storage using virtual file attributes.
   * @param content content bytes to adapt.
   * @param attributes attributes of the file whose content will be adapted.
   * @return adapted content bytes.
   */
  abstract fun adaptContentFromMainframe(content: ByteArray, attributes: Attributes): ByteArray

  /**
   * Prepares content for uploading from mainframe to Intellij content storage using virtual file.
   * @see MFContentAdapter.adaptContentFromMainframe
   */
  @Suppress("UNCHECKED_CAST")
  override fun adaptContentFromMainframe(content: ByteArray, file: VirtualFile): ByteArray {
    val attributes = dataOpsManager.tryToGetAttributes(file) ?: return content
    return if (attributes.`is`(attributesClass)) adaptContentFromMainframe(
      content,
      attributes as Attributes
    ) else content
  }
}
