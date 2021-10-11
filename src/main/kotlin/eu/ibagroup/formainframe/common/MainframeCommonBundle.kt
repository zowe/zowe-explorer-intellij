/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.common

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

const val BUNDLE = "messages.CommonBundle"

class MainframeCommonBundle private constructor() : DynamicBundle(BUNDLE) {
  companion object {
    @JvmStatic
    val instance by lazy { MainframeCommonBundle() }
  }
}

fun message(@PropertyKey(resourceBundle = BUNDLE) key: String,
            vararg params: Any): String {
  return MainframeCommonBundle.instance.getMessage(key, *params)
}

fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String,
                vararg params: Any): Supplier<String> {
  return MainframeCommonBundle.instance.getLazyMessage(key, *params)
}
