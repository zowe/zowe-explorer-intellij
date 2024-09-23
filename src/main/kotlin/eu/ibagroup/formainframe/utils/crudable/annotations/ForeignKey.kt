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
package eu.ibagroup.formainframe.utils.crudable.annotations

import java.lang.annotation.Inherited
import kotlin.reflect.KClass

/**
 * Interface to describe the foreign key class assigned to the property
 */
@Target(AnnotationTarget.FIELD)
@Inherited
annotation class ForeignKey(val foreignClass: KClass<*>)
