package eu.ibagroup.formainframe.utils.crudable

interface ReloadableEventHandler : EventHandler {

  fun onEvent(rowClass: Class<*>, row: Any)

  fun onReload(rowClass: Class<*>)

  override fun <E : Any> onAdd(rowClass: Class<out E>, added: E) {
    onEvent(rowClass, added)
  }

  override fun <E : Any> onUpdate(rowClass: Class<out E>, oldRow: E, newRow: E) {
    onEvent(rowClass, newRow)
  }

  override fun <E : Any> onDelete(rowClass: Class<out E>, row: E) {
    onEvent(rowClass, row)
  }
}