package eu.ibagroup.formainframe.config

import eu.ibagroup.formainframe.utils.crudable.EventHandler

class ConfigEventHandler : EventHandler {

  override fun <E : Any> onAdd(rowClass: Class<out E>, added: E) {
    sendConfigServiceTopic().onAdd(rowClass, added)
  }

  override fun <E : Any> onUpdate(rowClass: Class<out E>, oldRow: E, newRow: E) {
    sendConfigServiceTopic().onUpdate(rowClass, oldRow, newRow)
  }

  override fun <E : Any> onDelete(rowClass: Class<out E>, row: E) {
    sendConfigServiceTopic().onDelete(rowClass, row)
  }

}