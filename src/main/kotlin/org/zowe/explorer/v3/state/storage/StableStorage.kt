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

package org.zowe.explorer.v3.state.storage

/** Annotation class to restrict the access to the storage for any unrelated entities */
@RequiresOptIn(message = "This class functionality provides interaction with stable storage and needs an opt-in. Do it only if you are totally aware of the consequences of working with the stable storage")
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class StableStorage
