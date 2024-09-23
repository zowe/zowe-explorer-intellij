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

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider

/**
 * Class which represents object responsible for maintaining configuration on mainframe
 */
class MainframeConfigurableProvider : ConfigurableProvider() {

  /**
   * Creates instance of object responsible for managing configurations on mainframe
   */
  override fun createConfigurable(): Configurable {
    return MainframeConfigurable()
  }
}
