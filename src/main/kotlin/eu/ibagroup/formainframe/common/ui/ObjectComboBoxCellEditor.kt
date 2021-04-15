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