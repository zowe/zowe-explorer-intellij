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

package org.zowe.explorer.v3.state.config.migration

import org.zowe.explorer.v3.state.config.Config
import org.zowe.explorer.v3.state.config.ConfigType

/** Represents config instances, that are ready to be converted to the next version */
abstract class OldConfig(uuid: String) : Config(uuid, ConfigType.UNKNOWN_CONFIG) {
  /**
   * Convert old config to the next version.
   * Should be implemented as a converting function for each of the old configs that are going to be migrated
   */
  abstract fun convertToNextVersion(): Config
}
