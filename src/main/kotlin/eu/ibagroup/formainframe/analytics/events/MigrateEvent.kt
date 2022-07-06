/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.analytics.events

// TODO: doc Valiantsin
class MigrateEvent(
  private val migrateActionType: MigrateActionType
) : AnalyticsEvent("migrations") {
  override fun getProps(): Map<String, String> {
    return mapOf(Pair("actionType", migrateActionType.toString()))
  }
}

enum class MigrateActionType(val value: String) {
  MIGRATE("MIGRATE"),
  RECALL("RECALL");

  override fun toString(): String {
    return value
  }
}