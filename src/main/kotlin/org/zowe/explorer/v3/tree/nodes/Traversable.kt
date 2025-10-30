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

/**
 * Interface to mark an element as something that could be identified by the exact path
 * @see [RealNodeAssociation]
 */
interface Traversable : RealNodeAssociation {
  override val placingPath: List<String>
  override val elemName: String

  /**
   * Get the element's exact placing path
   * @return the list of strings that identify the element by the path
   */
  fun getExactPath(): List<String> = placingPath + elemName
}