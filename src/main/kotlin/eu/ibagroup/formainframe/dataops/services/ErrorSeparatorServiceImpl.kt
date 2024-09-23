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

package eu.ibagroup.formainframe.dataops.services

import java.util.*

/**
 * Implementation for ErrorSeparatorService.
 * @see ErrorSeparatorService
 * @author Valiantsin Krus
 */
class ErrorSeparatorServiceImpl : ErrorSeparatorService {

  /**
   * Parses error message from z/OS.
   * @param errorMessage error message received from z/OS.
   * @return properties containing error code, error postfix, error description
   *         in properties error.code, error.description, error.postfix respectively.
   */
  override fun separateErrorMessage(errorMessage: String): Properties {
    val errorProperties = Properties()
    try {
      val indOfErrorCodeDelimiter = errorMessage.indexOf(' ')
      val indOfErrorPostfix = errorMessage.lastIndexOf('(')

      val errorCode = errorMessage.subSequence(0, indOfErrorCodeDelimiter)
      val description = errorMessage.subSequence(indOfErrorCodeDelimiter + 1, indOfErrorPostfix - 1);
      val errorPostfix = errorMessage.subSequence(indOfErrorPostfix, errorMessage.length)

      errorProperties["error.code"] = errorCode
      errorProperties["error.description"] = description
      errorProperties["error.postfix"] = errorPostfix

      return errorProperties
    } catch (e: Exception) {
      errorProperties["error.description"] = errorMessage
    }
    return errorProperties
  }
}
