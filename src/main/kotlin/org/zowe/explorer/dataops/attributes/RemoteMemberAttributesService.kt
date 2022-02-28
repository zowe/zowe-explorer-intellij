/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.attributes

import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.explorer.vfs.MFVirtualFileSystem
import eu.ibagroup.r2z.Member
import eu.ibagroup.r2z.XIBMDataType

class RemoteMemberAttributesServiceFactory : AttributesServiceFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): AttributesService<*, *> {
    return RemoteMemberAttributesService(dataOpsManager)
  }
}

class RemoteMemberAttributesService(
  val dataOpsManager: DataOpsManager
) :
  DependentFileAttributesService<RemoteMemberAttributes, Member, RemoteDatasetAttributes, MFVirtualFile>(dataOpsManager) {

  companion object {
    private val fsModel = MFVirtualFileSystem.instance.model
  }

  override val findOrCreateFileInVFSModel = fsModel::findOrCreate
  override val moveFileAndReplaceInVFSModel = fsModel::moveFileAndReplace


  override fun buildAttributes(
    info: Member, file: MFVirtualFile, contentMode: XIBMDataType?
  ): RemoteMemberAttributes {
    return RemoteMemberAttributes(info, file, contentMode ?: XIBMDataType(XIBMDataType.Type.TEXT))
  }


  override val attributesClass = RemoteMemberAttributes::class.java
  override val vFileClass = MFVirtualFile::class.java
  override val parentAttributesClass = RemoteDatasetAttributes::class.java
}
