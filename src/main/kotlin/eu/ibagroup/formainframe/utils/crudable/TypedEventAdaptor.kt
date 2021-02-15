package eu.ibagroup.formainframe.utils.crudable

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

inline fun <reified Row> eventAdaptor(init: TypedEventAdaptorBuilder<Row>.() -> Unit): TypedEventAdaptor<Row> {
  val proxy = TypedEventAdaptorBuilder<Row>().apply(init)
  return object : TypedEventAdaptor<Row>(Row::class.java) {
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