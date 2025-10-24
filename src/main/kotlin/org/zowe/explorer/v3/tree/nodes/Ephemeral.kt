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

// TODO: doc update
/**
 * Describes node descriptors that do not represent the real object on the mainframe side.
 * These nodes are only needed to describe the state of the node path when there are no real children available for it
 */
interface Ephemeral : Traversable {
  override val elemName: String
    get() = ""
  override val placingPath: List<String>
    get() = listOf()

  override fun getExactPath(): List<String> = listOf()
}
