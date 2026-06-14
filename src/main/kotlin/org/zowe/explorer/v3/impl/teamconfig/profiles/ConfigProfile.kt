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
 * Common interface for all Zowe profile types used in the Team Config dialog.
 *
 * Each implementation corresponds to one profile section in the generated
 * `zowe.config.json` file. Implementations are responsible for producing their
 * own JSON-serialisable representation via [jsonFriendly].
 *
 * @property profileName human-readable label shown in the dialog UI.
 * @property profileType JSON key used for this profile type in the generated config
 *   (e.g. `"zosmf"`, `"ssh"`, `"tso"`). Also used as the key in the `defaults` map.
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 */
interface ConfigProfile {
  val profileName: String
  val profileType: String
  var shouldCreate: Boolean

  /**
   * Returns a JSON-serialisable map representing this profile entry, ready to be
   * inserted into the `profiles` section of `zowe.config.json`.
   *
   * The returned map always contains `"type"` and `"properties"` keys, and a
   * `"secure"` key listing the names of fields stored in the credential store
   * (omitted when there are no secure fields).
   */
  fun jsonFriendly(): Map<String, Any>

  /**
   * Builds a profile entry map with the given [type], [properties], and optional
   * [secure] field list.
   *
   * The `"secure"` key is omitted from the result when [secure] is empty, so
   * callers can safely pass `emptyList()` for profiles that have no secure fields.
   */
  fun entry(type: String, properties: Map<String, Any?>, secure: List<String>): Map<String, Any> =
    linkedMapOf("type" to type, "properties" to properties.filterValues { it != null })
      .also { if (secure.isNotEmpty()) it["secure"] = secure }
}