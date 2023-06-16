/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.ui

import eu.ibagroup.formainframe.dataops.SortQueryKeys

/**
 * Interface which represents any USS sortable Node
 * @param Node - Nodes type to sort
 */
interface UssSortableNode<Node> {
  fun List<Node>.sortChildrenNodes(sortKeys: List<SortQueryKeys>): List<Node> = mutableListOf()
}