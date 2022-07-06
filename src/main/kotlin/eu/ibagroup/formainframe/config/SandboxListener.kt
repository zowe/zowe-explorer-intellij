/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config

import com.intellij.util.messages.Topic

// TODO: doc
interface SandboxListener {

  companion object {
    @JvmField
    val TOPIC = Topic.create("sandboxListener", SandboxListener::class.java)
  }

  fun <E : Any> update(clazz: Class<out E>)

  fun <E : Any> reload(clazz: Class<out E>)

}