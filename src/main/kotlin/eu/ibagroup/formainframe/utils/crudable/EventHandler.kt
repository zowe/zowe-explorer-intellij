package eu.ibagroup.formainframe.utils.crudable

interface EventHandler {
  fun <E : Any> onAdd(rowClass: Class<out E>, added: E)
  fun <E : Any> onUpdate(rowClass: Class<out E>, oldRow: E, newRow: E)
  fun <E : Any> onDelete(rowClass: Class<out E>, row: E)
}