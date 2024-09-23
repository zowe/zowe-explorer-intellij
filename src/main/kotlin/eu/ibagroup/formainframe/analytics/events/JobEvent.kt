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
 * Class for processing analytics events with jobs (e.g. submit, purge, hold and etc).
 * @param fileType type of the file the action was done on.
 * @param fileAction action that was done on the file.
 * @author Valiantsin Krus
 */
class JobEvent(
  private val jobAction: JobAction
) : AnalyticsEvent("jobs") {
  override fun getProps(): Map<String, String> {
    return mapOf(Pair("job_action", jobAction.toString()))
  }
}

/**
 * Actions on the jobs that can be tracked in analytics.
 * @see JobEvent
 * @author Valiantsin Krus
 */
enum class JobAction(val value: String) {
  SUBMIT("SUBMIT"),
  PURGE("PURGE"),
  HOLD("HOLD"),
  RELEASE("RELEASE");

  override fun toString(): String {
    return value
  }
}
