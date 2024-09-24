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

package org.zowe.explorer.dataops.fetch

import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.config.connect.ConnectionConfigBase
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.AttributesService
import org.zowe.explorer.dataops.attributes.FileAttributes

/**
 * Common abstract class which represents attributed virtual file in VFS.
 * Every virtual file object with attributes in explorer(WS, JES) extends this class
 */
abstract class RemoteAttributedFileFetchBase<Connection : ConnectionConfigBase, Request : Any, Response : FileAttributes, File : VirtualFile>(
  dataOpsManager: DataOpsManager
) : RemoteFileFetchProviderBase<Connection, Request, Response, File>(dataOpsManager) {

  protected val attributesService: AttributesService<Response, File>
      by lazy { dataOpsManager.getAttributesService(responseClass, vFileClass) }

  /**
   * Overloaded method to create a virtual file with attributes in VFS from given response
   */
  override fun convertResponseToFile(response: Response): File? {
    return attributesService.getOrCreateVirtualFile(response)
  }
}
