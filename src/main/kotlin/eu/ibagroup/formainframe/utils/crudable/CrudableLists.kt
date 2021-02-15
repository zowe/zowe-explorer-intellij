package eu.ibagroup.formainframe.utils.crudable

import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.Crudable.getUniqueValueForRow
import java.util.*
import java.util.stream.Stream

class CrudableListsBuilder(
  private val getListByClass: (Class<*>) -> MutableList<*>?
) {
  fun withNextUuidProvider(nextUuidProvider: (Class<*>) -> Any): CrudableLists {
    return CrudableLists(getListByClass = getListByClass, nextUuidProvider = nextUuidProvider)
  }
}

fun interface AddFilter {
  operator fun invoke(clazz: Class<*>, addingRow: Any): Boolean
}

fun interface UpdateFilter {
  operator fun invoke(clazz: Class<*>, currentRow: Any, updatingRow: Any): Boolean
}

open class CrudableLists(
  var eventHandler: EventHandler? = null,
  var addFilter: AddFilter = AddFilter { _, _ -> true },
  var updateFilter: UpdateFilter = UpdateFilter { _, _, _ -> true },
  val nextUuidProvider: (Class<*>) -> Any,
  val getListByClass: (Class<*>) -> MutableList<*>?
) : Crudable {

  @Suppress("UNCHECKED_CAST")
  override fun <E : Any, V : Any> nextUniqueValue(rowClass: Class<out E>): V {
    return nextUuidProvider(rowClass) as V
  }

  @Suppress("UNCHECKED_CAST")
  private fun <E : Any> classToList(rowClass: Class<out E>): MutableList<E>? {
    return getListByClass(rowClass) as MutableList<E>?
  }

  override fun <E : Any> getAll(rowClass: Class<out E>): Stream<E> {
    return classToList(rowClass).streamOrEmpty()
  }

  override fun <E : Any> add(rowClass: Class<out E>, row: E): Optional<E> {
    return runIfTrue(addFilter(rowClass, row)) {
      getUniqueValueForRow(row).nullable?.let { field ->
        classToList(rowClass)?.let { list ->
          val alreadyExists = list.any { o -> getUniqueValueForRow(o).nullable?.equals(field) ?: false }
          runIfTrue(!alreadyExists && list.add(row)) {
            eventHandler?.onAdd(rowClass, row)
            row
          }
        }
      }
    }.optional
  }

  override fun <E : Any> update(rowClass: Class<out E>, row: E): Optional<E> {
    return runIfPresent(getUniqueValueForRow(row)) { field ->
      classToList(rowClass)?.let { list ->
        list.indexOf { e ->
          getUniqueValueForRow(e).nullable?.equals(field) ?: false
        }?.let { index ->
          val old = list[index]
          runIfTrue(updateFilter(rowClass, old, row)) {
            if (old != row) {
              list[index] = row
              eventHandler?.onUpdate(rowClass, old, row)
            }
            old
          }
        }
      }
    }.optional
  }

  override fun <E : Any> delete(rowClass: Class<out E>, row: E): Optional<E> {
    return classToList(rowClass)?.let main@{ list ->
      list.remove(row).runIfTrue {
        eventHandler?.onDelete(rowClass, row)
        row
      }
    }.optional
  }

}