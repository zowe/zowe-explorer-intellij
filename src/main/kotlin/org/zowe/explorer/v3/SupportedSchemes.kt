/*
 * Copyright (c) 2024 IBA Group.
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

package org.zowe.explorer.v3

/**
 * Supported schemes for a mainframe connection
 * @param scheme a scheme string representation
 */
enum class SupportedSchemes(val scheme: String) {
  HTTP("http"),
  HTTPS("https"),
  UNSUPPORTED("unsupported scheme");

  companion object {
    operator fun invoke(scheme: String): SupportedSchemes {
      return entries.find { it.scheme == scheme } ?: UNSUPPORTED
    }
  }

  override fun toString(): String {
    return scheme
  }
}
