/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.actions.teamconfig

/**
 * Represents a single configurable field within a Zowe profile.
 *
 * Each field carries its current [value], a human-readable [description] shown
 * as a tooltip in the UI, and an optional validation predicate
 * [isConstraintFulfilled] that must return `true` for the field to be
 * considered valid before the config file is generated.
 *
 * @param T the type of the field value (e.g. [String], [Int], [Boolean], [CharArray]).
 * @property value the current value of the field; may be `null` for optional fields.
 * @property description a short human-readable explanation of what this field controls,
 *   displayed as a tooltip in the dialog.
 * @property isConstraintFulfilled a lambda that returns `true` when the field's value
 *   satisfies all required constraints; defaults to `{ true }` (always valid).
 */
data class ProfileField<T>(
  var value: T,
  val description: String,
  val isConstraintFulfilled: () -> Boolean = { true }
)
