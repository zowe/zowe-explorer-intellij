/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.newoperations

/**
 * Operation interface. Provides a representation of an operation instance.
 * Must contain an [operationData] as parameters of the operation to execute.
 * Also inherited classes should implement [run] method for the operation to be able to run
 */
interface Operation {
  val operationData: OperationData

  /**
   * Run the operation
   * @return [OperationResult] as the result of the operation run
   */
  fun run(): OperationResult
}
