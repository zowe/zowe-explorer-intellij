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

import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.vfs.MFVirtualFile

class DefaultContentAdapter(dataOpsManager: DataOpsManager): MFContentAdapterBase<FileAttributes>(dataOpsManager) {

  override val vFileClass = MFVirtualFile::class.java
  override val attributesClass = FileAttributes::class.java

  override fun adaptContentToMainframe(content: ByteArray, attributes: FileAttributes): ByteArray = content
  override fun adaptContentFromMainframe(content: ByteArray, attributes: FileAttributes): ByteArray = content
}
