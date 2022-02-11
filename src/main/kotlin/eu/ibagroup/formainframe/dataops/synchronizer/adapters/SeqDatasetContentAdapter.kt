/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.synchronizer.adapters

import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class SeqDatasetContentAdapterFactory: MFContentAdapterFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): MFContentAdapter {
    return SeqDatasetContentAdapter(dataOpsManager)
  }
}

class SeqDatasetContentAdapter(
  dataOpsManager: DataOpsManager
): LReclContentAdapter<RemoteDatasetAttributes>(dataOpsManager) {

  override val vFileClass = MFVirtualFile::class.java
  override val attributesClass = RemoteDatasetAttributes::class.java

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

  override fun adaptContentFromMainframe(content: ByteArray, attributes: RemoteDatasetAttributes): ByteArray {
    if (attributes.hasVariablePrintFormatRecords()) {
      return removeFirstCharacter(content)
    }
    return content
  }
}
