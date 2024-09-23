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
 * Abstraction for creating analytics events.
 * @author Valiantsin Krus
 */
abstract class AnalyticsEvent(
  open var eventName: String
) {
  /**
   * Gives properties of an action.
   * @return map of properties.
   */
  abstract fun getProps(): Map<String, String>
}

/**
 * Analytics event for tracking CRUD (create, read, update, delete) operations.
 * @param eventName name of the event.
 * @param actionType type of the crud action (create, read, update, delete).
 * @author Valiantsin Krus
 */
open class CrudAnalyticsEvent(
  override var eventName: String,
  open var actionType: ActionType
) : AnalyticsEvent(eventName) {

  /** Returns type of the crud action in property action_type. */
  override fun getProps(): Map<String, String> {
    return mutableMapOf(Pair("action_type", actionType.toString()))
  }
}

/**
 * Class for enumeration CRUD possible action types.
 * @author Valiantsin Krus
 */
enum class ActionType(val value: String) {
  CREATE("CREATE"),
  EDIT("EDIT"),
  UPDATE("UPDATE"),
  DELETE("DELETE"),
  READ("READ");

  override fun toString(): String {
    return value
  }
}
