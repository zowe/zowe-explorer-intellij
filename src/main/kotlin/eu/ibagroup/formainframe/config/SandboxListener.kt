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

package eu.ibagroup.formainframe.config

import com.intellij.util.messages.Topic

/**
 * Interface which serves as an object for sending sandboxListener topics
 */
interface SandboxListener {

  /**
   * Static topic to be sent
   */
  companion object {
    @JvmField
    val TOPIC = Topic.create("sandboxListener", SandboxListener::class.java)
  }

  /**
   * Abstract method to update rows in specified configurable
   */
  fun <E : Any> update(clazz: Class<out E>)

  /**
   * Abstract method to reload rows in specified configurable
   */
  fun <E : Any> reload(clazz: Class<out E>)

}
