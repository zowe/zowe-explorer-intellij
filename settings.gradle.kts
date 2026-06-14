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
  val kotlinVersion = when (System.getenv("PRODUCT_NAME") ?: "IC-261") {
    "IC-261" -> "2.3.20"
    else -> "2.3.20"
  }

  plugins {
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
  }
}
