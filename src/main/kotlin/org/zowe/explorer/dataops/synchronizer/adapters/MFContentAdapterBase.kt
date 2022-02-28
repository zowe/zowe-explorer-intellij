/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.synchronizer.adapters

import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.utils.`is`

abstract class MFContentAdapterBase<Attributes: FileAttributes>(
  protected val dataOpsManager: DataOpsManager
): MFContentAdapter {
  abstract val vFileClass: Class<out VirtualFile>
  abstract val attributesClass: Class<out Attributes>


  override fun accepts(file: VirtualFile): Boolean {
    val fileAttributesClass = dataOpsManager.tryToGetAttributes(file)?.javaClass ?: return false
    return vFileClass.isAssignableFrom(file::class.java) &&
        attributesClass.isAssignableFrom(fileAttributesClass)
  }

  abstract fun adaptContentToMainframe(content: ByteArray, attributes: Attributes): ByteArray

  @Suppress("UNCHECKED_CAST")
  override fun performAdaptingToMainframe(content: ByteArray, file: VirtualFile): ByteArray {
    val attributes = dataOpsManager.tryToGetAttributes(file) ?: return content
    return if (attributes.`is`(attributesClass)) adaptContentToMainframe(content, attributes as Attributes) else content
  }

  abstract fun adaptContentFromMainframe(content: ByteArray, attributes: Attributes): ByteArray

  @Suppress("UNCHECKED_CAST")
  override fun performAdaptingFromMainframe(content: ByteArray, file: VirtualFile): ByteArray {
    val attributes = dataOpsManager.tryToGetAttributes(file) ?: return content
    return if (attributes.`is`(attributesClass)) adaptContentFromMainframe(content, attributes as Attributes) else content
  }
}
