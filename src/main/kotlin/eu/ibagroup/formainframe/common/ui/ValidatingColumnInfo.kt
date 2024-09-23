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

package eu.ibagroup.formainframe.common.ui

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.util.ui.ColumnInfo
import javax.swing.JComponent
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

/**
 * Abstract class which represents validators for info in columns in GUI
 */
abstract class ValidatingColumnInfo<Item>(
  name: String
) : ColumnInfo<Item, String>(name) {

  /**
   * Returns object which is responsible for validating of cell renderer
   */
  open fun getValidatingCellRenderer(item: Item): ValidatingCellRenderer<Item> {
    return ValidatingCellRenderer()
  }

  /**
   * Returns object which is responsible for validating of cell editor
   */
  open fun getValidatingCellEditor(item: Item): ValidatingCellEditor<Item> {
    return ValidatingCellEditor()
  }

  /**
   * Returns cell renderer object
   */
  override fun getRenderer(item: Item): TableCellRenderer {
    return getValidatingCellRenderer(item)
  }

  /**
   * Returns cell editor object
   */
  override fun getEditor(item: Item): TableCellEditor {
    return getValidatingCellEditor(item)
  }

  /**
   * Checks info in field of component during input process
   * @param oldItem status of component field before starting making some action
   * @param newValue status of component field during making some action
   * @param component object on which some action is performed
   * @return result if such status of component field acceptable
   */
  abstract fun validateOnInput(oldItem: Item, newValue: String, component: JComponent): ValidationInfo?

  /**
   * Checks info in field after accepting changes in component
   * @param item status of component field after making changes
   * @param component object on which some action was performed
   * @return result if such status of component field acceptable
   */
  abstract fun validateEntered(item: Item, component: JComponent): ValidationInfo?

}
