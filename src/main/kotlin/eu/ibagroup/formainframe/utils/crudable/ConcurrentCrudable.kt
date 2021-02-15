package eu.ibagroup.formainframe.utils.crudable

import eu.ibagroup.formainframe.utils.lock
import java.util.*
import java.util.stream.Stream

class ConcurrentCrudable(
  val wrappingCrudable: Crudable,
  private val locksManager: LocksManager
) : Crudable by wrappingCrudable {

  override fun <E : Any> add(rowClass: Class<out E>, row: E): Optional<E> {
    return lock(locksManager.getLockForAdding(rowClass)) {
      wrappingCrudable.add(rowClass, row)
    }
  }

  override fun <E : Any> getAll(rowClass: Class<out E>): Stream<E> {
    return lock(locksManager.getLockForGettingAll(rowClass)) {
      wrappingCrudable.getAll(rowClass)
    }
  }

  override fun <E : Any> update(rowClass: Class<out E>, row: E): Optional<E> {
    return lock(locksManager.getLockForDeleting(rowClass)) {
      wrappingCrudable.update(rowClass, row)
    }
  }

  override fun <E : Any> delete(rowClass: Class<out E>, row: E): Optional<E> {
    return lock(locksManager.getLockForAdding(rowClass)) {
      wrappingCrudable.delete(rowClass, row)
    }
  }

  override fun <E : Any?, V : Any?> nextUniqueValue(rowClass: Class<out E>): V {
    return lock(locksManager.getLockForNextUniqueValue(rowClass)) {
      wrappingCrudable.nextUniqueValue(rowClass)
    }
  }

}