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

class JobEvent(
  private val jobAction: JobAction
) : AnalyticsEvent("jobs") {
  override fun getProps(): Map<String, String> {
    return mapOf(Pair("job_action", jobAction.toString()))
  }
}


enum class JobAction(val value: String) {
  SUBMIT("SUBMIT"),
  PURGE("PURGE"),
  HOLD("HOLD"),
  RELEASE("RELEASE");

  override fun toString(): String {
    return value
  }
}
