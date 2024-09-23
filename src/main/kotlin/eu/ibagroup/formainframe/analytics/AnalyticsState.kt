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

package eu.ibagroup.formainframe.analytics

/**
 * State of user configurations for analytics that will be saved by Intellij.
 */
data class AnalyticsState(
  /** Defines is the analytics tracking enabled by the user. */
  var isAnalyticsEnabled: Boolean = false,
  /** Defines the latest version of analytics policy the user agreed with. */
  var lastAcknowledgedPolicyVersion: Int = POLICY_NOT_ACKNOWLEDGED,
  /** Defines if the user has aware with of analytics policy at least once. */
  var wasIdentified: Boolean = false,
)

const val POLICY_NOT_ACKNOWLEDGED = 0
