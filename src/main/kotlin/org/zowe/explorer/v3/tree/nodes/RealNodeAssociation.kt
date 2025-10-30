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
 * Interface to mark an element as something that could be associated with a real node
 * @property placingPath the path where the element is placed
 * @property elemName the element name to identify the element placed under the [placingPath]
 */
interface RealNodeAssociation {
  val placingPath: List<String>
  val elemName: String
}