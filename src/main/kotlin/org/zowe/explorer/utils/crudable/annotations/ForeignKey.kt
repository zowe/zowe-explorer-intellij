/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package org.zowe.explorer.utils.crudable.annotations

<<<<<<<< HEAD:src/main/java/org/zowe/explorer/utils/crudable/annotations/Contains.java
package org.zowe.explorer.utils.crudable.annotations;
========
import java.lang.annotation.Inherited
import kotlin.reflect.KClass
>>>>>>>> release/v0.7.0:src/main/kotlin/org/zowe/explorer/utils/crudable/annotations/ForeignKey.kt

/**
 * Interface to describe the foreign key class assigned to the property
 */
@Target(AnnotationTarget.FIELD)
@Inherited
annotation class ForeignKey(val foreignClass: KClass<*>)
