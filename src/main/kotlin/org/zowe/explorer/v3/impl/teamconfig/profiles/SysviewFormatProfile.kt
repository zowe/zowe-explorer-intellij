/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.teamconfig.profiles

/**
 * Holds the fields for the Zowe **SYSVIEW format** profile section.
 *
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 * @property contextFields list of context field names to display; `null` hides all context.
 * @property overview whether to display the overview section.
 * @property info whether to display the information area (if present).
 * @property pretty whether to apply formatted (pretty-printed) display of data.
 * @property blankIfZero whether to render `0` values as blank spaces.
 * @property truncate whether to truncate output that exceeds the console width (default `false`).
 */
data class SysviewFormatProfile(
  override var shouldCreate: Boolean = false,
  val contextFields: ProfileField<List<String>?> = ProfileField(null, "Context fields", "contextFields", "Context fields to display. Defaults to hiding all context"),
  val overview: ProfileField<Boolean?> = ProfileField(null, "Overview", "overview", "Display the overview section"),
  val info: ProfileField<Boolean?> = ProfileField(null, "Info", "info", "Display the information area, if any"),
  val pretty: ProfileField<Boolean?> = ProfileField(null, "Pretty", "pretty", "Display formatted data"),
  val blankIfZero: ProfileField<Boolean?> = ProfileField(null, "Blank if zero", "blankIfZero", "Show a blank space instead of '0' values"),
  val truncate: ProfileField<Boolean?> = ProfileField(false, "Truncate", "truncate", "Truncate displays that are too wide for the console")
) : ConfigProfile {
  override val profileName: String = "SYSVIEW format profile"
  override val profileType: String = "sysview-format"

  override fun jsonFriendly(): Map<String, Any> = entry(
    profileType,
    mapOf(
      contextFields.prop,
      overview.prop,
      info.prop,
      pretty.prop,
      blankIfZero.prop,
      truncate.prop
    ),
    emptyList()
  )
}