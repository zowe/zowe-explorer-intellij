/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.ds.tree.nodes

import com.intellij.icons.AllIcons
import org.zowe.explorer.v3.impl.api.ZosmfApiService
import org.zowe.explorer.v3.impl.connection.ConnectionProfileRelated
import org.zowe.explorer.v3.impl.files.tree.nodes.FilesExplorerRelated
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.Navigable
import org.zowe.explorer.v3.tree.nodes.Traversable
import org.zowe.kotlinsdk.core.DataType
import org.zowe.kotlinsdk.core.connectivity.HttpConnection
import org.zowe.kotlinsdk.providers.zowe.zosmf.datasets.messaging.ZosmfRetrieveDatasetContentRequest

/**
 * Node descriptor for a sequential data set.
 * @param elemName the data set name shown in the tree
 * @param placingPath the path segments for placing in the virtual file system
 * @param connectionProfile the connection profile path from the Zowe Team Config
 */
class SequentialDatasetNodeDescriptor(
  override val elemName: String,
  override val placingPath: List<String>,
  override val connectionProfile: String
) : ExplorerTreeNodeDescriptor(
  elemName,
  "Sequential data set",
  AllIcons.FileTypes.Any_type
), ConnectionProfileRelated, Traversable, FilesExplorerRelated, Navigable {

  override fun getFileName(): String = elemName

  override suspend fun fetchContent(connection: HttpConnection): String {
    val request = ZosmfRetrieveDatasetContentRequest(connection, dsName = elemName)
    val response = ZosmfApiService.getService().datasets.retrieveDatasetContent(request)
    if (response.fetchedDataType == DataType.ERROR) {
      throw IllegalStateException("Failed to retrieve dataset content")
    }
    return response.fetchedText ?: ""
  }
}