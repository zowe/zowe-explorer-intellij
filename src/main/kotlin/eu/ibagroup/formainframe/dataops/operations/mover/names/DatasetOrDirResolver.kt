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
package eu.ibagroup.formainframe.dataops.operations.mover.names

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes

class DatasetOrDirResolverFactory : CopyPasteNameResolverFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): CopyPasteNameResolver {
    return DatasetOrDirResolver(dataOpsManager)
  }
}

/**
 * Implementation of [CopyPasteNameResolver] for copying dataset or directory to uss or local system.
 * @author Valiantsin Krus
 */
class DatasetOrDirResolver(val dataOpsManager: DataOpsManager): IndexedNameResolver() {
  override fun accepts(source: VirtualFile, destination: VirtualFile): Boolean {
    val sourceAttributes = dataOpsManager.tryToGetAttributes(source)
    val destinationAttributes = dataOpsManager.tryToGetAttributes(destination)
    return (source.isDirectory || sourceAttributes is RemoteDatasetAttributes) && destinationAttributes !is RemoteDatasetAttributes
  }


  override fun resolveNameWithIndex(source: VirtualFile, destination: VirtualFile, index: Int?): String {
    return if (index == null) source.name else "${source.name}_(${index})"
  }
}
