/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package eu.ibagroup.formainframe.dataops.operations.mover

<<<<<<<< HEAD:src/main/kotlin/org/zowe/explorer/dataops/operations/AbstractFileMover.kt
package org.zowe.explorer.dataops.operations
========
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
>>>>>>>> release/v0.7.0:src/main/kotlin/org/zowe/explorer/dataops/operations/mover/AbstractFileMover.kt

/** Class to represent a file mover abstraction */
abstract class AbstractFileMover : OperationRunner<MoveCopyOperation, Unit> {

  override val operationClass = MoveCopyOperation::class.java

  override val resultClass = Unit::class.java

}
