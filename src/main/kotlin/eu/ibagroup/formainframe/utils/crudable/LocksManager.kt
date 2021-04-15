package eu.ibagroup.formainframe.utils.crudable

import java.util.concurrent.locks.Lock

interface LocksManager {

  fun <E : Any> getLockForAdding(rowClass: Class<out E>): Lock?

  fun <E : Any> getLockForGettingAll(rowClass: Class<out E>): Lock?

  fun <E : Any> getLockForUpdating(rowClass: Class<out E>): Lock?

  fun <E : Any> getLockForDeleting(rowClass: Class<out E>): Lock?

  fun <E : Any> getLockForNextUniqueValue(rowClass: Class<out E>): Lock?

}