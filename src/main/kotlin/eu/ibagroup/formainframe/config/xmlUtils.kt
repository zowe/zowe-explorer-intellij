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

package eu.ibagroup.formainframe.config

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.util.function.Supplier

/**
 * Operator for extracting child xml tag elements through [].
 * @param tagName child tag name.
 * @return list of all tag elements with name equal to tagName parameter.
 */
operator fun Element.get(tagName: String): List<Element> {
  if (!this.hasChildNodes()) {
    return emptyList()
  }
  return this.childNodes.toElementList().filter { it.tagName == tagName }
}

/**
 * Filters nodes that are instance of Element class and wraps them in list.
 * @return list of Element instances.
 */
fun NodeList.toElementList(): List<Element> {
  val result = mutableListOf<Element>()
  for (i in 0 until length) {
    if (item(i) is Element) {
      result.add(item(i) as Element)
    }
  }
  return result
}
