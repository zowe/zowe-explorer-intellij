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

import eu.ibagroup.formainframe.common.message

/**
 * Implementation of PolicyProvider.
 * @see PolicyProvider
 * @author Uladzislau Kalesnikau
 */
class PolicyProviderImpl : PolicyProvider {

  /** Policy text from resources. */
  private val licenseText by lazy {
    this::class.java.classLoader.getResource("policy.txt")?.readText()
  }

  /** Policy version from analytics configuration in resources. */
  private val licenseVersion by lazy {
    analyticsProperties.getProperty("policy.version").toInt()
  }

  /** Policy text if it was found or N/A otherwise. */
  override val text: String
    get() = licenseText ?: "N/A"

  /**
   * Builds agreement text.
   * @see PolicyProvider.buildAgreementText
   */
  override fun buildAgreementText(action: String): String {
    return message("analytics.agreement.text", action)
  }

  /** Policy version if it was found or max value of Int otherwise.*/
  override val version
    get() = if (licenseText != null) {
      licenseVersion
    } else {
      Int.MAX_VALUE
    }

}
