package eu.ibagroup.formainframe.utils.crudable;

import eu.ibagroup.formainframe.utils.crudable.annotations.Column;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.ibagroup.formainframe.utils.crudable.Utils.*;

public interface Crudable {

  @NotNull <E> Optional<E> add(@NotNull Class<? extends E> rowClass, @NotNull E row);

  @SuppressWarnings("unchecked")
  default <E> Optional<E> add(@NotNull E row) {
    return (Optional<E>) add(row.getClass(), row);
  }

  @NotNull <E> Stream<E> getAll(@NotNull Class<? extends E> rowClass);

  @NotNull <E> Optional<E> update(@NotNull Class<? extends E> rowClass, @NotNull E row);

  @SuppressWarnings("unchecked")
  default <E> Optional<E> update(@NotNull E row) {
    return (Optional<E>) update(row.getClass(), row);
  }

  default @NotNull <E> Optional<E> addOrUpdate(@NotNull Class<? extends E> rowClass, E row) {
    Optional<E> optional = add(rowClass, row);
    if (!optional.isPresent()) {
      optional = update(rowClass, row);
    }
    return optional;
  }

  @SuppressWarnings("unchecked")
  default @NotNull <E> Optional<E> addOrUpdate(@NotNull E row) {
    return (Optional<E>) addOrUpdate(row.getClass(), row);
  }

  @NotNull <E> Optional<E> delete(@NotNull Class<? extends E> rowClass, @NotNull E row);

  @SuppressWarnings("unchecked")
  default @NotNull <E> Optional<E> delete(@NotNull E row) {
    return (Optional<E>) delete(row.getClass(), row);
  }

  @NotNull <E, V> V nextUniqueValue(@NotNull Class<? extends E> rowClass);

  default <E> void replaceGracefully(@NotNull Class<? extends E> rowClass, @NotNull Stream<? extends E> rows) {
    final List<E> current = this.getAll(rowClass).collect(Collectors.toList());
    final List<E> newRows = rows.collect(Collectors.toList());
    applyMergedCollections(rowClass, mergeCollections(current, newRows));
  }

  default <E> void applyMergedCollections(@NotNull Class<? extends E> rowClass, @NotNull MergedCollections<? extends E> mergedCollections) {
    mergedCollections.getToDelete().forEach(e -> delete(rowClass, e));
    mergedCollections.getToUpdate().forEach(e -> update(rowClass, e));
    mergedCollections.getToAdd().forEach(e -> add(rowClass, e));
  }

  static <E> @NotNull MergedCollections<E> mergeCollections(@NotNull Collection<? extends E> oldCollection,
                                                      @NotNull Collection<? extends E> newCollection) {
    final List<Pair<Optional<?>, E>> oldCollectionWithUniqueValues = mapWithUniqueValues(oldCollection);
    final List<Pair<Optional<?>, E>> newCollectionWithUniqueValues = mapWithUniqueValues(newCollection);

    final List<E> toDelete = oldCollectionWithUniqueValues.stream()
        .filter(optionalEPair -> newCollectionWithUniqueValues.stream()
            .noneMatch(optionalEPairNew -> {
              final Optional<?> oldOptional = optionalEPair.getFirst();
              final Optional<?> newOptional = optionalEPairNew.getFirst();
              if (oldOptional.isPresent() && newOptional.isPresent()) {
                return oldOptional.get().equals(newOptional.get());
              }
              return false;
            })
        ).map(Pair::getSecond)
        .collect(Collectors.toList());

    final List<E> toAdd = newCollectionWithUniqueValues.stream()
        .filter(optionalEPair -> oldCollectionWithUniqueValues.stream()
            .noneMatch(optionalEPairOld -> {
              final Optional<?> newOptional = optionalEPair.getFirst();
              final Optional<?> oldOptional = optionalEPairOld.getFirst();
              if (oldOptional.isPresent() && newOptional.isPresent()) {
                return oldOptional.get().equals(newOptional.get());
              }
              return false;
            })
        ).map(Pair::getSecond)
        .collect(Collectors.toList());

    final List<E> toUpdate = newCollectionWithUniqueValues.stream()
        .filter(optionalEPair -> oldCollectionWithUniqueValues.stream()
            .anyMatch(optionalEPairOld -> {
              final Optional<?> newOptional = optionalEPair.getFirst();
              final Optional<?> oldOptional = optionalEPairOld.getFirst();
              if (oldOptional.isPresent() && newOptional.isPresent()) {
                return oldOptional.get().equals(newOptional.get()) && !optionalEPair.getSecond().equals(optionalEPairOld.getSecond());
              }
              return false;
            })
        ).map(Pair::getSecond)
        .collect(Collectors.toList());

    return new MergedCollections<>(
        toAdd, toUpdate, toDelete
    );
  }

  default <E> void addAll(@NotNull Stream<? extends E> rows) {
    rows.forEach(this::add);
  }

  default <E> void addAll(@NotNull Class<? extends E> rowClass,
                          @NotNull Stream<? extends E> rows) {
    rows.forEach(r -> add(rowClass, r));
  }

  @SuppressWarnings("unchecked")
  default @NotNull <E> Stream<E> find(@NotNull Class<? extends E> rowClass,
                                      @NotNull Predicate<? super E> predicate) {
    return (Stream<E>) getAll(rowClass).filter(predicate);
  }

  @SuppressWarnings("unchecked")
  default @NotNull <E, V> Stream<E> getByColumnValue(@NotNull Class<? extends E> rowClass,
                                                     @NotNull String columnName,
                                                     @NotNull V columnValue) {
    return Arrays.stream(getFieldsDeeply(rowClass))
        .filter(getPredicateByColumnName(columnName))
        .findAny()
        .map(field -> (Stream<E>) find(rowClass, e -> {
          try {
            field.setAccessible(true);
            return field.get(e).equals(columnValue);
          } catch (IllegalAccessException illegalAccessException) {
            return false;
          }
        })).orElse(Stream.empty());
  }

  default @NotNull <E, V> Stream<E> getByColumnValue(@NotNull Class<? extends E> rowClass,
                                                     @NotNull Function<? super E, ? extends V> columnGetter,
                                                     @NotNull V columnValue) {
    return find(rowClass, obj -> columnGetter.apply(obj).equals(columnValue));
  }

  default @NotNull <E, V> Stream<E> getByColumnValueFromRow(@NotNull Class<? extends E> rowClass,
                                                            @NotNull Function<? super E, ? extends V> columnGetter,
                                                            @NotNull E anotherRow) {
    return find(rowClass, obj -> columnGetter.apply(obj).equals(columnGetter.apply(anotherRow)));
  }

  @SuppressWarnings("unchecked")
  default @NotNull <E, U> Optional<E> getByUniqueKey(@NotNull Class<? extends E> rowClass,
                                                     @NotNull U uniqueKey) {
    final Field uniqueField = Arrays.stream(getFieldsDeeply(rowClass))
        .filter(getUniqueFieldPredicate())
        .findAny()
        .orElse(null);
    if (uniqueField != null) {
      return (Optional<E>) find(rowClass, e -> {
        try {
          uniqueField.setAccessible(true);
          return uniqueField.get(e).equals(uniqueKey);
        } catch (IllegalAccessException illegalAccessException) {
          return false;
        }
      }).findAny();
    } else {
      return Optional.empty();
    }
  }

  default <E, U> @NotNull Optional<? extends E> deleteByUniqueKey(@NotNull Class<? extends E> rowClass,
                                                                  @NotNull U uniqueKey) {
    final Optional<? extends E> optional = getByUniqueKey(rowClass, uniqueKey);
    if (optional.isPresent()) {
      return delete(optional.get());
    } else {
      return Optional.empty();
    }
  }

  default @NotNull <E> Stream<E> deleteIf(@NotNull Class<? extends E> rowClass,
                                          @NotNull Predicate<? super E> predicate) {
    return find(rowClass, predicate)
        .map(this::delete)
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  default <E, F> @NotNull Optional<F> getByForeignKey(@NotNull E row, @NotNull String columnName, @NotNull Class<? extends F> foreignRowClass) {
    return getByForeignKeyInternal(this, row, columnName, foreignRowClass);
  }

  default <E, F> @NotNull Optional<F> getByForeignKey(@NotNull E row, @NotNull Class<? extends F> foreignRowClass) {
    return getByForeignKeyInternal(this, row, null, foreignRowClass);
  }

  default <E, F> @NotNull Optional<F> getByForeignKeyDeeply(@NotNull E row, @NotNull Class<? extends F> foreignRowClass) {
    return getByForeignKeyDeeply(row.getClass(), row, foreignRowClass);
  }

  default <E, F> @NotNull Optional<F> getByForeignKeyDeeply(@NotNull Class<? extends E> rowClass, @NotNull E row, @NotNull Class<? extends F> foreignRowClass) {
    return Optional.ofNullable(getByForeignKeyDeeplyInternal(this, row, foreignRowClass));
  }

  static @NotNull <E, U> Optional<U> getUniqueValue(@NotNull E row, @NotNull Class<? extends U> uniqueValueClass) {
    return Arrays.stream(getFieldsDeeply(row))
        .filter(field -> {
          field.setAccessible(true);
          return field.isAnnotationPresent(Column.class) && field.getAnnotation(Column.class).unique();
        })
        .findAny()
        .map(field -> {
          try {
            final Object uniqueObj = field.get(row);
            if (uniqueValueClass.isAssignableFrom(uniqueObj.getClass())) {
              return uniqueValueClass.cast(uniqueObj);
            }
          } catch (IllegalAccessException ignored) {
          }
          return null;
        });
  }

  static <E> @NotNull Optional<?> getUniqueValueForRow(@NotNull E row) {
    return Arrays.stream(getFieldsDeeply(row))
        .filter(field -> {
          field.setAccessible(true);
          return field.isAnnotationPresent(Column.class) && field.getAnnotation(Column.class).unique();
        })
        .findAny()
        .map(field -> {
          field.setAccessible(true);
          try {
            return field.get(row);
          } catch (IllegalAccessException e) {
            return null;
          }
        });
  }

}
