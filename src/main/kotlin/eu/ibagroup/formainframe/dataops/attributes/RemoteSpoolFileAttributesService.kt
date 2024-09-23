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
import org.zowe.kotlinsdk.SpoolFile
import org.zowe.kotlinsdk.XIBMDataType

/**
 * Factory for registering RemoteSpoolFileAttributesService.
 * @author Valiantsin Krus
 */
class RemoteSpoolFileAttributesServiceFactory : AttributesServiceFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): AttributesService<*, *> {
    return RemoteSpoolFileAttributesService(dataOpsManager)
  }
}

/**
 * Implementation of attributes service for working with spool files of the job.
 * @see AttributesService
 * @see DependentFileAttributesService
 * @author Valiantsin Krus
 */
class RemoteSpoolFileAttributesService(
  val dataOpsManager: DataOpsManager
) : DependentFileAttributesService<RemoteSpoolFileAttributes, SpoolFile, RemoteJobAttributes, MFVirtualFile>(
  dataOpsManager
) {

  companion object {
    private val fsModel = MFVirtualFileSystem.instance.model
  }

  override val attributesClass = RemoteSpoolFileAttributes::class.java
  override val vFileClass = MFVirtualFile::class.java
  override val parentAttributesClass = RemoteJobAttributes::class.java

  /**
   * Initialize attributes for spool file.
   * @see RemoteSpoolFileAttributes
   * @see DependentFileAttributesService.buildAttributes
   */
  override fun buildAttributes(
    info: SpoolFile,
    file: MFVirtualFile,
    contentMode: XIBMDataType?
  ): RemoteSpoolFileAttributes {
    return RemoteSpoolFileAttributes(info, file, contentMode ?: XIBMDataType(XIBMDataType.Type.TEXT))
  }

  override val findOrCreateFileInVFSModel = fsModel::findOrCreateDependentFile
  override val moveFileAndReplaceInVFSModel = fsModel::moveFileAndReplace
}
