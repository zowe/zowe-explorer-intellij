/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.synchronizer.adapters

import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.kotlinsdk.RecordFormat

abstract class LReclContentAdapter<Attributes: FileAttributes>(
  dataOpsManager: DataOpsManager
): MFContentAdapterBase<Attributes>(dataOpsManager) {

  fun RemoteDatasetAttributes.hasVariableFormatRecords(): Boolean {
    val recordFormat = datasetInfo.recordFormat
    return recordFormat == RecordFormat.V || recordFormat == RecordFormat.VA || recordFormat == RecordFormat.VB
  }

  fun RemoteDatasetAttributes.hasVariablePrintFormatRecords(): Boolean {
    val recordFormat = datasetInfo.recordFormat
    return recordFormat == RecordFormat.VA
  }

  protected fun transferLinesByLRecl (content: ByteArray, lrecl: Int): ByteArray {
    val contentString = String(content)
    val contentRows = contentString.split(Regex("\n|\r|\r\n"))
    val resultRows = mutableListOf<String>()
    contentRows.forEach {
      if (it.length <= lrecl) {
        resultRows.add(it)
      } else {
        var nextLine = it
        while (nextLine.length > lrecl) {
          resultRows.add(nextLine.slice(IntRange(0, lrecl-1)))
          nextLine = nextLine.slice(IntRange(lrecl, nextLine.length-1))
        }
        resultRows.add(nextLine)
      }
    }
    val resultContent = resultRows.joinToString("\n")
    return resultContent.toByteArray()
  }

  protected fun removeFirstCharacter (content: ByteArray): ByteArray {
    val contentString = String(content)
    val contentRows = contentString.split(Regex("\n|\r|\r\n"))
    val resultRows = mutableListOf<String>()
    contentRows.forEach {
      resultRows.add(it.slice(IntRange(1, it.length-1)))
    }
    val resultContent = resultRows.joinToString("\n")
    return resultContent.toByteArray()
  }
}
