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
 * Holds the fields for the Zowe **Endevor location** profile section.
 *
 * Describes the Endevor inventory location (instance, environment, system, subsystem,
 * stage) used as defaults for element operations, so they do not need to be specified
 * on every command invocation.
 *
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 * @property instance STC/datasource name of the Endevor session (default `"ENDEVOR"`).
 * @property environment Endevor environment where the project resides (default `"DEV"`).
 * @property system Endevor system where the target element resides.
 * @property subsystem Endevor subsystem where the target element resides.
 * @property type name of the Endevor element type.
 * @property stageNumber Endevor stage number (`"1"` or `"2"`).
 * @property comment comment to attach when performing an Endevor action.
 * @property ccid CCID to attach when performing an Endevor action.
 * @property maxrc maximum Endevor return code that is treated as success (default `8`).
 * @property overrideSignout if `true`, always overrides element signout without requiring
 *   an explicit flag on each command (default `false`).
 * @property fileExtension strategy for file extensions during bulk retrieve or workspace sync:
 *   `"none"`, `"type-name"`, `"file-ext"`, or `"mixed"` (default `"mixed"`).
 */
data class EndevorLocationProfile(
  override var shouldCreate: Boolean = false,
  val instance: ProfileField<String?> = ProfileField("ENDEVOR", "Instance", "instance", "The STC/datasource of the session"),
  val environment: ProfileField<String?> = ProfileField("DEV", "Environment", "environment", "The Endevor environment where your project resides"),
  val system: ProfileField<String?> = ProfileField(null, "System", "system", "The Endevor system where the element resides"),
  val subsystem: ProfileField<String?> = ProfileField(null, "Subsystem", "subsystem", "The Endevor subsystem where your element resides"),
  val type: ProfileField<String?> = ProfileField(null, "Type", "type", "Name of the Endevor element's type"),
  val stageNumber: ProfileField<String?> = ProfileField(null, "Stage number", "stageNumber", "The Endevor stage where your project resides (1 or 2)"),
  val comment: ProfileField<String?> = ProfileField(null, "Comment", "comment", "The Endevor comment you want to use when performing an action"),
  val ccid: ProfileField<String?> = ProfileField(null, "CCID", "ccid", "The Endevor CCID you want to use when performing an action"),
  val maxrc: ProfileField<Int?> = ProfileField(8, "Max RC", "maxrc", "The return code of Endevor that defines a failed action"),
  val overrideSignout: ProfileField<Boolean?> = ProfileField(false, "Override signout", "override-signout", "Always override element signout, without having to specify the override signout option on each command"),
  val fileExtension: ProfileField<String?> = ProfileField("mixed", "File extension", "file-extension", "The strategy for deciding what file extension to use during a bulk retrieve or workspace synchronization (none, type-name, file-ext, mixed)")
) : ConfigProfile {
  override val profileName: String = "Endevor location profile"
  override val profileType: String = "endevor-location"

  override fun jsonFriendly(): Map<String, Any> = entry(
    profileType,
    mapOf(
      instance.prop,
      environment.prop,
      system.prop,
      subsystem.prop,
      type.prop,
      stageNumber.prop,
      comment.prop,
      ccid.prop,
      maxrc.prop,
      overrideSignout.prop,
      fileExtension.prop
    ),
    emptyList()
  )
}