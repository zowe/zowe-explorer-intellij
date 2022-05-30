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
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.vfs.MFVirtualFile

class MemberContentAdapterFactory: MFContentAdapterFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): MFContentAdapter {
    return MemberContentAdapter(dataOpsManager)
  }
}

class MemberContentAdapter(
  dataOpsManager: DataOpsManager
): LReclContentAdapter<RemoteMemberAttributes>(dataOpsManager) {

  override val vFileClass = MFVirtualFile::class.java
  override val attributesClass = RemoteMemberAttributes::class.java

  override fun adaptContentToMainframe(content: ByteArray, attributes: RemoteMemberAttributes): ByteArray {
    val pdsAttributes = dataOpsManager.tryToGetAttributes(attributes.parentFile) as RemoteDatasetAttributes
    var lrecl = pdsAttributes.datasetInfo.recordLength ?: 80
    if (pdsAttributes.hasVariableFormatRecords()) {
      lrecl -= 4
    }
    if (pdsAttributes.hasVariablePrintFormatRecords()) {
      lrecl -= 1
    }
    return transferLinesByLRecl(content, lrecl)
  }

  override fun adaptContentFromMainframe(content: ByteArray, attributes: RemoteMemberAttributes): ByteArray {
    val pdsAttributes = dataOpsManager.tryToGetAttributes(attributes.parentFile) as RemoteDatasetAttributes
    if (pdsAttributes.hasVariablePrintFormatRecords()) {
      return removeFirstCharacter(content)
    }
    return content
  }

}
