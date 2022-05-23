package eu.ibagroup.formainframe.dataops.content.synchronizer

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.r2z.CodePage
import eu.ibagroup.r2z.XIBMDataType
import eu.ibagroup.r2z.annotations.ZVersion

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

fun ByteArray.addNewLine(): ByteArray {
  return this.plus(NEW_LINE.toByteArray())
}
