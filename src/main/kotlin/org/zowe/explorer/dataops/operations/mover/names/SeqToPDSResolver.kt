/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package org.zowe.explorer.dataops.operations.mover.names

import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes

class SeqToPDSResolverFactory : CopyPasteNameResolverFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): CopyPasteNameResolver {
    return SeqToPDSResolver(dataOpsManager)
  }
}

/**
 * Implementation of [CopyPasteNameResolver] for copying Sequential Dataset to PDS.
 * @author Valiantsin Krus
 */
class SeqToPDSResolver(val dataOpsManager: DataOpsManager) : IndexedNameResolver() {
  override fun accepts(source: VirtualFile, destination: VirtualFile): Boolean {
    val sourceAttributes = dataOpsManager.tryToGetAttributes(source)
    val destinationAttributes = dataOpsManager.tryToGetAttributes(destination)
    return sourceAttributes is RemoteDatasetAttributes &&
        !source.isDirectory &&
        destinationAttributes is RemoteDatasetAttributes
  }

  override fun resolveNameWithIndex(source: VirtualFile, destination: VirtualFile, index: Int?): String {
    val lastQualifier = source.name.split(".").last()
    return if (index == null) lastQualifier else "${lastQualifier.take(7)}${index}"
  }
}
