/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.analytics

import eu.ibagroup.formainframe.common.message

class PolicyProviderImpl : PolicyProvider {

  private val licenceText by lazy {
    this::class.java.classLoader.getResource("policy.txt")?.readText()
  }

  private val licenceVersion by lazy {
    analyticsProperties.getProperty("policy.version").toInt()
  }

  override val text: String
    get() = licenceText ?: "N/A"

  override fun buildAgreementText(action: String): String {
    return message("analytics.agreement.text", action)
  }

  override val version
    get() = if (licenceText != null) {
      licenceVersion
    } else {
      Int.MAX_VALUE
    }

}
