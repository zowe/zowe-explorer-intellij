package eu.ibagroup.formainframe.utils.crudable;

import eu.ibagroup.formainframe.utils.crudable.annotations.Column;
import org.jetbrains.annotations.NotNull;

public class EntityWithUuid {

  public static final String EMPTY_ID = "";

  @Column(unique = true)
  protected @NotNull String uuid = EMPTY_ID;

  public @NotNull String getUuid() {
    return uuid;
  }

  public void setUuid(@NotNull String uuid) {
    this.uuid = uuid;
  }

  public EntityWithUuid() {
  }

  public EntityWithUuid(@NotNull String uuid) {
    this.uuid = uuid;
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EntityWithUuid that = (EntityWithUuid) o;

    return uuid.equals(that.uuid);
  }

  @Override
  public String toString() {
    return "EntityWithUuid{" +
        "uuid='" + uuid + '\'' +
        '}';
  }
}
