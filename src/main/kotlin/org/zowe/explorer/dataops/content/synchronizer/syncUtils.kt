/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.content.synchronizer

import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.kotlinsdk.CodePage
import org.zowe.kotlinsdk.XIBMDataType
import org.zowe.kotlinsdk.annotations.ZVersion

private const val NEW_LINE = "\n"

fun updateDataTypeWithEncoding(connectionConfig: ConnectionConfig, oldDataType: XIBMDataType) : XIBMDataType {
  return if (connectionConfig.zVersion >= ZVersion.ZOS_2_4 && oldDataType.encoding != null && oldDataType.encoding != CodePage.IBM_1047 && oldDataType.type == XIBMDataType.Type.TEXT) {
    XIBMDataType(oldDataType.type, connectionConfig.codePage)
  } else {
    oldDataType
  }
}

fun String.removeLastNewLine(): String {
  return if (endsWith(NEW_LINE)) {
    removeSuffix(NEW_LINE)
  } else {
    this
  }
}

fun String.addNewLine(): String {
  return this + NEW_LINE
}
