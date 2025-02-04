/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package tests.utils

val UI_TEST_HTTP_SCHEME: String by lazy { System.getProperty("ui.test.http.scheme", "undef") }
val UI_TEST_HOST: String by lazy { System.getProperty("ui.test.host", "undef") }
val UI_TEST_PORT: String by lazy { System.getProperty("ui.test.port", "undef") }
val UI_TEST_USERNAME: String by lazy { System.getProperty("ui.test.username", "UNDEF") }
val UI_TEST_PASSWORD: String by lazy { System.getProperty("ui.test.password", "undef") }
val UI_TEST_ALLOW_SELF_SIGNED: String by lazy { System.getProperty("ui.test.allow.self.signed", "undef") }

