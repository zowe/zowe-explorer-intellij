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

class NotSeqToPDSResolverFactory : CopyPasteNameResolverFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): CopyPasteNameResolver {
    return NotSeqToPDSResolver(dataOpsManager)
  }
}

/**
 * Implementation of [CopyPasteNameResolver] for copying anything except of Sequential Dataset to PDS.
 * @author Valiantsin Krus
 */
class NotSeqToPDSResolver(val dataOpsManager: DataOpsManager) : IndexedNameResolver() {
  override fun accepts(source: VirtualFile, destination: VirtualFile): Boolean {
    val sourceAttributes = dataOpsManager.tryToGetAttributes(source)
    val destinationAttributes = dataOpsManager.tryToGetAttributes(destination)
    return destinationAttributes is RemoteDatasetAttributes &&
        sourceAttributes !is RemoteDatasetAttributes
  }

  override fun resolveNameWithIndex(source: VirtualFile, destination: VirtualFile, index: Int?): String {
    val memberName = source.name.filter { it.isLetterOrDigit() }.uppercase().ifEmpty { "EMPTY" }
    return if (index == null) memberName.take(8) else "${memberName.take(8 - index.toString().length)}$index"
  }

}
