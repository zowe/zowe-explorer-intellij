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

package eu.ibagroup.formainframe.common

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

const val BUNDLE = "messages.FMBundle"

/**
 * Class that provides an instance for getting string messages from a common bundle.
 */
class MainframeCommonBundle private constructor() : DynamicBundle(BUNDLE) {
  companion object {
    @JvmStatic
    val instance by lazy { MainframeCommonBundle() }
  }
}

/**
 * Method that returns a string message by a special key. Keys and values defined in FMBundle.properties.
 * @param key special unique key.
 * @param params additional parameters for setting the message format.
 * @return final string message.
 */
fun message(
  @PropertyKey(resourceBundle = BUNDLE) key: String,
  vararg params: Any
): String {
  return MainframeCommonBundle.instance.getMessage(key, *params)
}

/**
 * Method that returns a wrapped string message by a special key with lazy initialization.
 * Keys and values defined in FMBundle.properties.
 * @param key special unique key.
 * @param params additional parameters for setting the message format.
 * @return final wrapped string message.
 */
fun lazyMessage(
  @PropertyKey(resourceBundle = BUNDLE) key: String,
  vararg params: Any
): Supplier<String> {
  return MainframeCommonBundle.instance.getLazyMessage(key, *params)
}
