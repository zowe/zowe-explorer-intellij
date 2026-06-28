/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.dialogs

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.profiles.EditProfileDialog

/**
 * Dialog for editing an existing files profile inside an `explorer_ij` profile
 * in the target `zowe.config.json`.
 *
 * @param project the current project (used to resolve local config path)
 * @param configType the config type determining which zowe.config.json to write to
 * @param profileName the name of the profile being edited
 */
class EditFilesProfileDialog(
  project: Project,
  configType: ConfigType,
  profileName: String
) : EditProfileDialog(project, configType, "Edit Files Profile", profileName)
