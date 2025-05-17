/*
 * Copyright (c) 2024 IBA Group.
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

package org.zowe.explorer.v3.operations

import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.v3.ConnectionConfigOldStruct

/**
 * Base abstract class to represent operation runner
 * @property operationDataClass the operation class supported by the operation runner
 * @property resultClass the result class of the operation
 */
abstract class OperationRunner<R : Any, C : ConnectionConfigOldStruct, O : OperationData<R, C>> {

  abstract val operationDataClass: Class<out OperationData<*, *>>

  abstract val resultClass: Class<out R>

  /**
   * Determines if an operation could be run with the provided operation class instance
   * @param operationData the operation data class instance to check before the operation run
   */
  abstract fun canRun(operationData: O): Boolean

  /**
   * Run the operation
   * @param operationData the related operation data to run the operation with the params
   * @param progressIndicator the progress indicator to provide an information about the progress of the operation and
   * cancel it when the progress indicator is finished
   */
  abstract fun run(operationData: O, progressIndicator: ProgressIndicator = DumbProgressIndicator.INSTANCE): R

}
