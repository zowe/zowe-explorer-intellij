/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.teamconfig

/**
 * Defines the scope and location of a generated Zowe Team Config file.
 *
 * Zowe supports two file names ([fileName]) and two scopes ([isGlobal]):
 * - **team** configs (`zowe.config.json`) are shared among all users of the project.
 * - **user** configs (`zowe.config.user.json`) contain personal overrides and are
 *   typically git-ignored.
 * - **local** configs reside in the project directory; **global** configs reside in
 *   the user's `~/.zowe/` directory.
 *
 * @property displayName human-readable label shown in the combo box of the dialog.
 * @property fileName the name of the file that will be written to disk.
 * @property isGlobal `true` when the config should be placed in `~/.zowe/`,
 *   `false` when it should be placed in the project root.
 */
enum class ConfigType(val displayName: String, val fileName: String, val isGlobal: Boolean) {
  LOCAL_TEAM("local (team)", "zowe.config.json", false),
  LOCAL_USER("local (user)", "zowe.config.user.json", false),
  GLOBAL_TEAM("global (team)", "zowe.config.json", true),
  GLOBAL_USER("global (user)", "zowe.config.user.json", true)
}