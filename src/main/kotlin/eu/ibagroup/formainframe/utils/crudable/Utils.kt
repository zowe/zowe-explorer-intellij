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

import eu.ibagroup.formainframe.utils.crudable.annotations.Column
import eu.ibagroup.formainframe.utils.crudable.annotations.ForeignKey
import java.lang.reflect.Field
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

/** Crudable utils */
internal object Utils {

  /** Cast the field to a column if it is possible */
  fun Field.castToColumnOrNull(): Column? {
    return if (this.isAnnotationPresent(Column::class.java)) this.getAnnotation(Column::class.java) else null
  }

  /** Cast the field to a foreign key if it is possible */
  private fun Field.castToForeignKeyOrNull(): ForeignKey? {
    return if (this.isAnnotationPresent(ForeignKey::class.java)) this.getAnnotation(ForeignKey::class.java) else null
  }

  /**
   * Check if the column name is null or equals to the field name
   * @param columnName the column name to check
   * @param column the column to check if it is the right column to check
   * @param field the field to check if it is equal to the column name
   */
  private fun isColumnNameNullOrEqualsToFieldName(columnName: String?, column: Column?, field: Field): Boolean {
    return columnName == null || column?.name == columnName || field.name == columnName
  }

  /**
   * Get field filtering predicate. Sets the field accessible and generates the predicate for filtering
   * @param columnName the column name to check if it is the same as the field name
   * @param unique the variable to check the field uniqueness
   */
  private fun getFieldFilterPredicate(
    columnName: String?,
    unique: Boolean
  ): Predicate<in Field> {
    return Predicate { field: Field ->
      field.trySetAccessible()
      val column: Column? = field.castToColumnOrNull()
      (column?.unique == true || !unique) && isColumnNameNullOrEqualsToFieldName(columnName, column, field)
    }
  }

  @JvmStatic
  val uniqueFieldPredicate: Predicate<in Field>
    get() = getFieldFilterPredicate(null, true)

  /**
   * Get fields of the class and all superclasses until it is not Any
   * @param clazz the class to get fields of
   * @return array of the classes fields
   */
  @JvmStatic
  fun <T : Any> getFieldsDeeply(clazz: Class<T>): Array<Field> {
    val fields: MutableList<Field> = ArrayList()
    var currentClass: Class<*> = clazz
    while (currentClass != Any::class.java) {
      fields.addAll(listOf(*currentClass.declaredFields))
      currentClass = currentClass.superclass
    }
    return fields.toTypedArray()
  }

  /**
   * Get the field by the foreign key
   * @param crudable to get the field by the unique key
   * @param row the row to get all declared field of the class and its superclass and the unique key of the field
   * @param columnName the column name to check if it does not exist or is the same as the field name
   * @param foreignRowClass the foreign row class to check if it is assignable from the foreign key
   * @return optional value, that should contain the field
   */
  @JvmStatic
  fun <RowClass : Any, ForeignRowClass : Any> getByForeignKeyInternal(
    crudable: Crudable,
    row: RowClass,
    columnName: String?,
    foreignRowClass: Class<out ForeignRowClass>
  ): Optional<ForeignRowClass>? {
    return Stream
      .concat(
        Arrays.stream(row.javaClass.declaredFields),
        Arrays.stream(row.javaClass.superclass.declaredFields)
      )
      .filter { field: Field ->
        field.trySetAccessible()
        val column: Column? = field.castToColumnOrNull()
        val foreignKey: ForeignKey? = field.castToForeignKeyOrNull()
        val isFieldAnnotationAndColumn =
          field.isAnnotationPresent(ForeignKey::class.java) && field.isAnnotationPresent(Column::class.java)
        val isForeignRowClassAssignableFromForeignKey =
          foreignKey?.foreignClass?.java?.let { foreignRowClass.isAssignableFrom(it) } == true
        isFieldAnnotationAndColumn
          && isColumnNameNullOrEqualsToFieldName(columnName, column, field)
          && isForeignRowClassAssignableFromForeignKey
      }
      .findAny()
      .flatMap { field: Field ->
        field.trySetAccessible()
        try {
          val foreignKey: ForeignKey? = field.castToForeignKeyOrNull()
          @Suppress("UNCHECKED_CAST") // Suppressed as there is no chance to check belonging to the ForeignRowClass
          return@flatMap foreignKey
            ?.foreignClass
            ?.let {
              crudable.getByUniqueKey(it.java, field[row])
            } as Optional<ForeignRowClass>?
        } catch (e: IllegalAccessException) {
          return@flatMap Optional.empty<ForeignRowClass>()
        }
      }
  }

  /**
   * Get a list of unique values for each row in the collection
   * @param collection the collection of rows to transform
   */
  @JvmStatic
  fun <E : Any> mapWithUniqueValues(
    collection: Collection<E>
  ): List<Pair<Optional<*>, E>> {
    return collection.stream().map { e: E -> Pair(Crudable.getUniqueValueForRow(e), e) }
      .collect(Collectors.toList())
  }
}
