package eu.ibagroup.formainframe.utils.crudable;

import eu.ibagroup.formainframe.utils.crudable.annotations.Column;
import eu.ibagroup.formainframe.utils.crudable.annotations.ForeignKey;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
class Utils {

  private static @NotNull Predicate<? super Field> getFieldFilterPredicate(@Nullable String columnName,
                                                                           final boolean unique) {
    return field -> {
      if (!field.isAccessible()) {
        field.setAccessible(true);
      }
      Column column;
      return field.isAnnotationPresent(Column.class)
          && ((column = field.getAnnotation(Column.class)).unique() || !unique)
          && (columnName == null || column.name().equals(columnName) || field.getName().equals(columnName));

    };
  }

  public static @NotNull Predicate<? super Field> getUniqueFieldPredicate() {
    return getFieldFilterPredicate(null, true);
  }

  public static @NotNull Predicate<? super Field> getPredicateByColumnName(@NotNull String columnName) {
    return getFieldFilterPredicate(columnName, false);
  }

  public static <T> Field[] getFieldsDeeply(T t) {
    return getFieldsDeeply(t.getClass());
  }

  public static <T> Field[] getFieldsDeeply(Class<? extends T> clazz) {
    final List<Field> fields = new ArrayList<>();
    Class<?> currentClass = clazz;
    while (currentClass != Object.class) {
      fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
      currentClass = currentClass.getSuperclass();
    }
    return fields.toArray(new Field[0]);
  }

  @SuppressWarnings("unchecked")
  public static <E, F> @NotNull Optional<F> getByForeignKeyInternal(@NotNull Crudable crudable, @NotNull E row, @Nullable String columnName, @NotNull Class<? extends F> foreignRowClass) {
    return Arrays.stream(row.getClass().getDeclaredFields())
        .filter(field -> {
          field.setAccessible(true);
          return field.isAnnotationPresent(ForeignKey.class)
              && field.isAnnotationPresent(Column.class)
              && (columnName == null || (field.getName().equals(columnName) || field.getAnnotation(Column.class).name().equals(columnName)))
              && foreignRowClass.isAssignableFrom(field.getAnnotation(ForeignKey.class).foreignClass());
        })
        .findAny()
        .flatMap(field -> {
          field.setAccessible(true);
          try {
            return (Optional<F>) crudable.getByUniqueKey(field.getAnnotation(ForeignKey.class).foreignClass(), field.get(row));
          } catch (IllegalAccessException e) {
            return Optional.empty();
          }
        });
  }

  @SuppressWarnings("unchecked")
  public static <F> @Nullable F getByForeignKeyDeeplyInternal(@NotNull Crudable crudable,
                                                              @NotNull Object row,
                                                              @NotNull Class<? extends F> foreignRowClass) {
    final List<Field> fieldList = Arrays.stream(row.getClass().getDeclaredFields())
        .filter(field -> {
          field.setAccessible(true);
          return field.isAnnotationPresent(ForeignKey.class)
              && field.isAnnotationPresent(Column.class);
        })
        .collect(Collectors.toList());
    for (Field field : fieldList) {
      field.setAccessible(true);
      try {
        final Class<?> foundForeignRowClass = field.getAnnotation(ForeignKey.class).foreignClass();
        final Object foundRowUniqueValue = field.get(row);
        if (foundRowUniqueValue != null) {
          final Object foundRow = crudable.getByUniqueKey(foundForeignRowClass, foundRowUniqueValue).orElse(null);
          if (foundRow != null) {
            if (foundForeignRowClass.equals(foreignRowClass)) {
              return (F) foundRow;
            } else {
              return getByForeignKeyDeeplyInternal(crudable, foundRow, foreignRowClass);
            }
          }
        }
      } catch (IllegalAccessException ignored) {
      }
    }
    return null;
  }

  public static <E> @NotNull List<Pair<Optional<?>, E>> mapWithUniqueValues(@NotNull Collection<? extends E> collection) {
    return collection.stream().map(
        e -> new Pair<Optional<?>, E>(Crudable.getUniqueValueForRow(e), e)
    ).collect(Collectors.toList());
  }

}
