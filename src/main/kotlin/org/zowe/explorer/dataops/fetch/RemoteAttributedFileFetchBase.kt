/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.fetch

import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.AttributesService
import org.zowe.explorer.dataops.attributes.FileAttributes

abstract class RemoteAttributedFileFetchBase<Request : Any, Response : FileAttributes, File : VirtualFile>(
  dataOpsManager: DataOpsManager
) : RemoteFileFetchProviderBase<Request, Response, File>(dataOpsManager) {

  protected val attributesService: AttributesService<Response, File>
      by lazy { dataOpsManager.getAttributesService(responseClass, vFileClass) }

  override fun convertResponseToFile(response: Response): File? {
    return attributesService.getOrCreateVirtualFile(response)
  }
}
