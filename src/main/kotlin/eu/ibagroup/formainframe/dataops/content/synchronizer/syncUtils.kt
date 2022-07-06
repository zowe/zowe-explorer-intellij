/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.content.synchronizer

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.r2z.CodePage
import eu.ibagroup.r2z.XIBMDataType
import eu.ibagroup.r2z.annotations.ZVersion

private const val NEW_LINE = "\n"

// TODO: doc Valiantsin
fun updateDataTypeWithEncoding(connectionConfig: ConnectionConfig, oldDataType: XIBMDataType): XIBMDataType {
  return if (connectionConfig.zVersion >= ZVersion.ZOS_2_4 && oldDataType.encoding != null && oldDataType.encoding != CodePage.IBM_1047 && oldDataType.type == XIBMDataType.Type.TEXT) {
    XIBMDataType(oldDataType.type, connectionConfig.codePage)
  } else {
    oldDataType
  }
}

/** Remove string's last blank line */
fun String.removeLastNewLine(): String {
  return if (endsWith(NEW_LINE)) {
    removeSuffix(NEW_LINE)
  } else {
    this
  }
}

/** Add new blank line to the string */
fun ByteArray.addNewLine(): ByteArray {
  return this.plus(NEW_LINE.toByteArray())
}
