package eu.ibagroup.formainframe.utils.crudable.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface ForeignKey {

  Class<?> foreignClass();

}
