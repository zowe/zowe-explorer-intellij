/*
 * Copyright (c) 2020 IBA Group.
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

package eu.ibagroup.formainframe.testutils

import java.lang.reflect.Modifier

/**
 * Mock private/protected field of the class for the object
 * @param sourceObj the source object to mock the field for
 * @param classWithTheField the class where the field is declared
 * @param fieldName the field name to mock
 * @param mockValue the mock value to set for the field
 */
fun mockPrivateField(sourceObj: Any, classWithTheField: Class<*>, fieldName: String, mockValue: Any) {
  return classWithTheField
    .declaredFields
    .filter { it.modifiers.and(Modifier.PRIVATE) > 0 || it.modifiers.and(Modifier.PROTECTED) > 0 }
    .find { it.name == fieldName }
    ?.also { it.isAccessible = true }
    ?.set(sourceObj, mockValue)
    ?: throw NoSuchFieldException("Field with name '$fieldName' is not found amongst private or protected fields")
}
