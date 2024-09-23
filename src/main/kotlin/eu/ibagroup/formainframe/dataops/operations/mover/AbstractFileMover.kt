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
package eu.ibagroup.formainframe.dataops.operations.mover

import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.utils.log

/** Class to represent a file mover abstraction */
abstract class AbstractFileMover : OperationRunner<MoveCopyOperation, Unit> {

  override val operationClass = MoveCopyOperation::class.java

  override val resultClass = Unit::class.java

  override val log = log<MoveCopyOperation>()

}
