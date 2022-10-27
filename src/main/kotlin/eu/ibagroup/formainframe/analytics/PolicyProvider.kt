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

/**
 * Service for providing analytics policy
 * @author Uladzislau Kalesnikau
 */
interface PolicyProvider {

  /** Text of analytics policy. */
  val text: String

  /**
   * Text of user agreement.
   * @param action action that will be performed on analytics policy (clicking).
   * @return agreement text.
   */
  fun buildAgreementText(action: String): String

  /** Version of current analytics policy. */
  val version: Int

}
