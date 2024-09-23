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

import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * Factory for registering MemberContentAdapter in Intellij IoC container.
 * @author Valiantsin Krus
 */
class MemberContentAdapterFactory : MFContentAdapterFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): MFContentAdapter {
    return MemberContentAdapter(dataOpsManager)
  }
}

/**
 * Adapts content for member.
 * @see MFContentAdapter
 * @author Valiantsin Krus
 */
class MemberContentAdapter(
  dataOpsManager: DataOpsManager
) : LReclContentAdapter<RemoteMemberAttributes>(dataOpsManager) {

  override val vFileClass = MFVirtualFile::class.java
  override val attributesClass = RemoteMemberAttributes::class.java

  /**
   * Checks if text exceed record length of parent dataset. If it is then it transfers
   * the end of the line on the next row. It also checks file on variable format.
   * If the first letter of format is V (variable) then mainframe uses some columns
   * for configuration (for V and VB it first 4 columns on each row, for VA - first 5).
   * @see MFContentAdapterBase.adaptContentToMainframe
   */
  @Suppress("UNCHECKED_CAST")
  override fun <T>adaptContentToMainframe(content: T, attributes: RemoteMemberAttributes): T {
    val pdsAttributes = dataOpsManager.tryToGetAttributes(attributes.parentFile) as RemoteDatasetAttributes
    var lrecl = pdsAttributes.datasetInfo.recordLength ?: 80
    if (pdsAttributes.hasVariableFormatRecords()) {
      lrecl -= 4
    }
    if (pdsAttributes.hasVariablePrintFormatRecords()) {
      lrecl -= 1
    }

    content.castOrNull<String>()?.let {
      return transferLinesByLRecl(it, lrecl) as T
    }
    content.castOrNull<ByteArray>()?.let {
      return transferLinesByLRecl(String(it), lrecl).toByteArray() as T
    }
    return content
  }

  /**
   * Removes first character if parent dataset has VA format.
   * @see MFContentAdapterBase.adaptContentFromMainframe
   */
  override fun adaptContentFromMainframe(content: ByteArray, attributes: RemoteMemberAttributes): ByteArray {
    val pdsAttributes = dataOpsManager.tryToGetAttributes(attributes.parentFile) as RemoteDatasetAttributes
    if (pdsAttributes.hasVariablePrintFormatRecords()) {
      return removeFirstCharacter(String(content)).toByteArray()
    }
    return content
  }

}
