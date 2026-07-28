/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.tree.nodes

import org.zowe.kotlinsdk.core.connectivity.HttpConnection

/**
 * Marker interface for node descriptors that support opening their content
 * in the editor on double-click or Enter.
 * Implementors define what resource name to display and how to fetch text content
 * from the mainframe
 */
interface Navigable {
  /**
   * @return the display name for the file tab in the editor (e.g. "DATASET.NAME" or "MEMBER")
   */
  fun getFileName(): String

  /**
   * Fetches the text content of the mainframe resource
   * @param connection the HTTP connection to use for the z/OSMF request
   * @return the text content of the resource
   */
  suspend fun fetchContent(connection: HttpConnection): String
}