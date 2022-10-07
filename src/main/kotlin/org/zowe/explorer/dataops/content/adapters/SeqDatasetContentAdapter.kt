/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.content.adapters

import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.vfs.MFVirtualFile

/**
 * Factory for registering SeqDatasetContentAdapter in Intellij IoC container.
 * @author Valiantsin Krus
 */
class SeqDatasetContentAdapterFactory : MFContentAdapterFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): MFContentAdapter {
    return SeqDatasetContentAdapter(dataOpsManager)
  }
}

/**
 * Adapts content for sequential dataset.
 * @see MFContentAdapter
 * @author Valiantsin Krus
 */
class SeqDatasetContentAdapter(
  dataOpsManager: DataOpsManager
) : LReclContentAdapter<RemoteDatasetAttributes>(dataOpsManager) {

  override val vFileClass = MFVirtualFile::class.java
  override val attributesClass = RemoteDatasetAttributes::class.java

  /**
   * Checks if text exceed record length of dataset. If it is then it transfers
   * the end of the line on the next row. It also checks file on variable format.
   * If the first letter of format is V (variable) then mainframe uses some columns
   * for configuration (for V and VB it first 4 columns on each row, for VA - first 5).
   * @see MFContentAdapterBase.adaptContentToMainframe
   */
  override fun adaptContentToMainframe(content: ByteArray, attributes: RemoteDatasetAttributes): ByteArray {
    var lrecl = attributes.datasetInfo.recordLength ?: 80
    if (attributes.hasVariableFormatRecords()) {
      lrecl -= 4
    }
    if (attributes.hasVariablePrintFormatRecords()) {
      lrecl -= 1
    }
    return transferLinesByLRecl(content, lrecl)
  }

  /**
   * Removes first character if dataset has VA format.
   * @see MFContentAdapterBase.adaptContentFromMainframe
   */
  override fun adaptContentFromMainframe(content: ByteArray, attributes: RemoteDatasetAttributes): ByteArray {
    if (attributes.hasVariablePrintFormatRecords()) {
      return removeFirstCharacter(content)
    }
    return content
  }
}
