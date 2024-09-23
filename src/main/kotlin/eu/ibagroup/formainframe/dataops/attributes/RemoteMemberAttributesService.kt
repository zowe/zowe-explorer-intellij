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

package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import org.zowe.kotlinsdk.Member
import org.zowe.kotlinsdk.XIBMDataType

/**
 * Factory for registering RemoteMemberAttributesService
 * @author Viktar Mushtsin
 */
class RemoteMemberAttributesServiceFactory : AttributesServiceFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): AttributesService<*, *> {
    return RemoteMemberAttributesService(dataOpsManager)
  }
}

/**
 * Implementation of attributes service for working with members of the dataset.
 * @see AttributesService
 * @see DependentFileAttributesService
 * @author Viktar Mushtsin
 * @author Valiantsin Krus
 */
class RemoteMemberAttributesService(
  val dataOpsManager: DataOpsManager
) :
  DependentFileAttributesService<RemoteMemberAttributes, Member, RemoteDatasetAttributes, MFVirtualFile>(dataOpsManager) {

  companion object {
    private val fsModel = MFVirtualFileSystem.instance.model
  }

  override val findOrCreateFileInVFSModel = fsModel::findOrCreateDependentFile
  override val moveFileAndReplaceInVFSModel = fsModel::moveFileAndReplace

  /**
   * Initialize attributes for member.
   * @see RemoteMemberAttributes
   * @see DependentFileAttributesService.buildAttributes
   */
  override fun buildAttributes(
    info: Member, file: MFVirtualFile, contentMode: XIBMDataType?
  ): RemoteMemberAttributes {
    return RemoteMemberAttributes(info, file, contentMode ?: XIBMDataType(XIBMDataType.Type.TEXT))
  }


  override val attributesClass = RemoteMemberAttributes::class.java
  override val vFileClass = MFVirtualFile::class.java
  override val parentAttributesClass = RemoteDatasetAttributes::class.java
}
