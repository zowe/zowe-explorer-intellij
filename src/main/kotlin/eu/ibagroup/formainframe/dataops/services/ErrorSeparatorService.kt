/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.services

import com.intellij.openapi.application.ApplicationManager
import java.util.*

/**
 * Service for parsing error messages from zos.
 * @author Valiantsin Krus
 */
interface ErrorSeparatorService {
  companion object {
    @JvmStatic
    val instance: ErrorSeparatorService
      get() = ApplicationManager.getApplication().getService(ErrorSeparatorService::class.java)
  }

  /**
   * Parses error message from z/OS.
   * @param errorMessage error message received from z/OS.
   * @return properties containing error code, error postfix, error description.
   */
  fun separateErrorMessage(errorMessage: String): Properties
}