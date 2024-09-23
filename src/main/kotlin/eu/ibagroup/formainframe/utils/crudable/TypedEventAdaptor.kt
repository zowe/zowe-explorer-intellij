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

package eu.ibagroup.formainframe.utils.crudable

/** Abstract class to handle typed events */
abstract class TypedEventAdaptor<Row>(
  private val clazz: Class<out Row>,
) : EventHandler {

  abstract fun onOurRowAdd(added: Row)

  abstract fun onOurRowUpdate(oldRow: Row, newRow: Row)

  abstract fun onOurRowDelete(row: Row)

  override fun <E : Any> onAdd(rowClass: Class<out E>, added: E) {
    if (rowClass == clazz) {
      @Suppress("UNCHECKED_CAST")
      onOurRowAdd(added as Row)
    }
  }

  override fun <E : Any> onUpdate(rowClass: Class<out E>, oldRow: E, newRow: E) {
    if (rowClass == clazz) {
      @Suppress("UNCHECKED_CAST")
      onOurRowUpdate(oldRow as Row, newRow as Row)
    }
  }

  override fun <E : Any> onDelete(rowClass: Class<out E>, row: E) {
    if (rowClass == clazz) {
      @Suppress("UNCHECKED_CAST")
      onOurRowDelete(row as Row)
    }
  }
}

/** Class to provide default typed event handling methods */
class TypedEventAdaptorBuilder<Row> {

  private lateinit var myOnAdd: (Row) -> Unit
  fun onAdd(action: (Row) -> Unit) {
    myOnAdd = action
  }

  private lateinit var myOnUpdate: (old: Row, new: Row) -> Unit
  fun onUpdate(action: (Row, Row) -> Unit) {
    myOnUpdate = action
  }

  private lateinit var myOnDelete: (Row) -> Unit
  fun onDelete(action: (Row) -> Unit) {
    myOnDelete = action
  }

  val onAdd: (Row) -> Unit
    get() = if (::myOnAdd.isInitialized) myOnAdd else {
      {}
    }

  val onUpdate: (old: Row, new: Row) -> Unit
    get() = if (::myOnUpdate.isInitialized) myOnUpdate else {
      { _, _ -> }
    }

  val onDelete: (Row) -> Unit
    get() = if (::myOnDelete.isInitialized) myOnDelete else {
      {}
    }

}

/**
 * Make an event adapter for the provided initializer.
 * Non-initialized event handlers will be handled by the default event handler
 * @param rowClass the row class for CRUDable processing
 * @param init the event handlers initializer
 */
fun <Row> eventAdapter(rowClass: Class<Row>, init: TypedEventAdaptorBuilder<Row>.() -> Unit): TypedEventAdaptor<Row> {
  val proxy = TypedEventAdaptorBuilder<Row>().apply(init)
  return object : TypedEventAdaptor<Row>(rowClass) {
    override fun onOurRowAdd(added: Row) {
      proxy.onAdd(added)
    }

    override fun onOurRowUpdate(oldRow: Row, newRow: Row) {
      proxy.onUpdate(oldRow, newRow)
    }

    override fun onOurRowDelete(row: Row) {
      proxy.onDelete(row)
    }
  }
}

/**
 * Provide a generic event adapter for the events
 * @param block the block of code to run on every event triggered
 */
inline fun <reified Row> anyEventAdaptor(crossinline block: (Row) -> Unit): TypedEventAdaptor<Row> {
  return object : TypedEventAdaptor<Row>(Row::class.java) {
    override fun onOurRowAdd(added: Row) {
      block(added)
    }

    override fun onOurRowUpdate(oldRow: Row, newRow: Row) {
      block(newRow)
    }

    override fun onOurRowDelete(row: Row) {
      block(row)
    }
  }
}
