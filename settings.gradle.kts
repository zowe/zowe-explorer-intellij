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
 *   Uladzislau Kalesnikau
 */

rootProject.name = "zowe-explorer"

pluginManagement {
  // https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
  val kotlinVersion = when (System.getenv("PRODUCT_NAME") ?: "IC-251") {
    "IC-231" -> "1.8.0"
    "IC-233" -> "1.9.21"
    "IC-242" -> "1.9.24"
    "IC-243" -> "2.0.21"
    "IC-251" -> "2.1.10"
    else -> "2.0.21"
  }

  plugins {
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
  }
}
