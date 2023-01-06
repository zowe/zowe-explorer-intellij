/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.content.adapters

import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.r2z.RecordFormat

/**
 * Abstraction with utils methods to perform adapting content for files with record length restriction.
 * @param Attributes attributes of files to work with.
 * @param dataOpsManager instance of DataOpsManager service to pass it to MFContentAdapterBase.
 * @author Valiantsin Krus
 */
abstract class LReclContentAdapter<Attributes : FileAttributes>(
  dataOpsManager: DataOpsManager
) : MFContentAdapterBase<Attributes>(dataOpsManager) {

  /**
   * Checks if attributes of dataset have variable format (V, VA, VB) of records.
   * @return true if they are and false otherwise.
   */
  fun RemoteDatasetAttributes.hasVariableFormatRecords(): Boolean {
    val recordFormat = datasetInfo.recordFormat
    return recordFormat == RecordFormat.V || recordFormat == RecordFormat.VA || recordFormat == RecordFormat.VB
  }

  /**
   * Checks if attributes of dataset have variable print format (VA) of records.
   * @return true if they are and false otherwise.
   */
  fun RemoteDatasetAttributes.hasVariablePrintFormatRecords(): Boolean {
    val recordFormat = datasetInfo.recordFormat
    return recordFormat == RecordFormat.VA
  }

  /**
   * Adapts content by record length. Cut the end of the line after
   * record length exceeded and put it on the next line. See example below.
   *
   * lrecl = 5
   * Before
   * ---------------
   * Hello|, Wor|ld!
   *      ^     ^
   * ---------------
   * After
   * ---------------
   * Hello
   * , Wor
   * ld!
   * ---------------
   *
   * @param content content bytes of the file to adapt.
   * @param lrecl record length of the file.
   * @return content bytes with transferred lines.
   */
  protected fun transferLinesByLRecl(content: ByteArray, lrecl: Int): ByteArray {
    val contentString = String(content)
    val lineSeparatorRegex = "\r\n|\n|\r"
    val contentRows = contentString.split(Regex(lineSeparatorRegex))
    val resultRows = mutableListOf<String>()
    contentRows.forEach {
      if (it.length <= lrecl) {
        resultRows.add(it)
      } else {
        var nextLine = it
        while (nextLine.length > lrecl) {
          resultRows.add(nextLine.slice(IntRange(0, lrecl - 1)))
          nextLine = nextLine.slice(IntRange(lrecl, nextLine.length - 1))
        }
        resultRows.add(nextLine)
      }
    }
    val resultContent = resultRows.joinToString("\n")
    return resultContent.toByteArray()
  }

  /**
   * Removes first character on each line of the content.
   * @param content content bytes of file to adapt.
   * @return content bytes without first character on each line.
   */
  protected fun removeFirstCharacter(content: ByteArray): ByteArray {
    val contentString = String(content)
    val contentRows = contentString.split(Regex("\n|\r|\r\n"))
    val resultRows = mutableListOf<String>()
    contentRows.forEach {
      resultRows.add(it.slice(IntRange(1, it.length - 1)))
    }
    val resultContent = resultRows.joinToString("\n")
    return resultContent.toByteArray()
  }
}
