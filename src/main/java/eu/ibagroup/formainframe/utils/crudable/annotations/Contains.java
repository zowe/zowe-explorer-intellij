package eu.ibagroup.formainframe.utils.crudable.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Inherited
public @interface Contains {
  Class<?>[] entities();
}
