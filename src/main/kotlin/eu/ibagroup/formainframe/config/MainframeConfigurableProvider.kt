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

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider

class MainframeConfigurableProvider : ConfigurableProvider() {
  override fun createConfigurable(): Configurable {
    return MainframeConfigurable()
  }
}
