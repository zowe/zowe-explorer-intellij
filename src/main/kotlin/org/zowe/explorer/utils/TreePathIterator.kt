/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.utils

import javax.swing.tree.TreePath

class TreePathIterator(private val treePath: TreePath) : Iterator<Any> {

  @Volatile
  private var currentIndex = 0

  override fun hasNext(): Boolean {
    return currentIndex < treePath.pathCount
  }

  override fun next(): Any {
    return treePath.getPathComponent(currentIndex++)
  }

}
