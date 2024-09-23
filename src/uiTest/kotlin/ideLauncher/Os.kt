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
package ideLauncher

enum class Os {
    WINDOWS, LINUX, MAC;

    companion object {
        fun hostOS(): Os {
            val osName = System.getProperty("os.name").lowercase()
            return when {
                osName.contains("win") -> WINDOWS
                osName.contains("mac") -> MAC
                osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> LINUX
                else -> throw Exception("Unknown operation system with name: \"$osName\"")
            }
        }
    }
}
