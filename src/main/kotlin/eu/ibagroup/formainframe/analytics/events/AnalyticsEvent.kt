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

abstract class AnalyticsEvent(
  open var eventName: String
) {
  abstract fun getProps(): Map<String, String>
}

open class CrudAnalyticsEvent(
  override var eventName: String,
  open var actionType: ActionType
) : AnalyticsEvent(eventName){
  override fun getProps(): Map<String, String> {
    return mutableMapOf(Pair("action_type", actionType.toString()))
  }
}

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