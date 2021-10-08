/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.common.ui

import com.intellij.util.ui.ComboBoxCellEditor

abstract class ObjectComboBoxCellEditor<Item> : ComboBoxCellEditor() {

  abstract fun getComboBoxObjects(): MutableList<Item>

  abstract fun mapObjectToString(item: Item): String

  abstract fun restoreObjectFromString(string: String): Item

  override fun getComboBoxItems(): MutableList<String> {
    return getComboBoxObjects().map { mapObjectToString(it) }.toMutableList()
  }

}