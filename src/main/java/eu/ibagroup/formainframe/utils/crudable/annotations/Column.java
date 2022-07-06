/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.utils.crudable.annotations;

import java.lang.annotation.*;

/**
 * Interface to describe the column in configuration services. Accepts the name of the column and the "unique" property to represent the uniqueness of the column
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface Column {

  String name() default "";

  boolean unique() default false;

}
