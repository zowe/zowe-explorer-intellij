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

package eu.ibagroup.formainframe.analytics.events

/**
 * Event for tracking migrate operations with files.
 * @author Valiantsin Krus
 */
class MigrateEvent(
  private val migrateActionType: MigrateActionType
) : AnalyticsEvent("migrations") {

  /**
   * Determines migrate event properties.
   * @return map with "actionType" property inside that means migrate action type.
   * @see MigrateActionType
   */
  override fun getProps(): Map<String, String> {
    return mapOf(Pair("actionType", migrateActionType.toString()))
  }
}

/**
 * Possible types of migrate action.
 * @author Valiantsin Krus.
 */
enum class MigrateActionType(val value: String) {
  MIGRATE("MIGRATE"),
  RECALL("RECALL");

  override fun toString(): String {
    return value
  }
}
