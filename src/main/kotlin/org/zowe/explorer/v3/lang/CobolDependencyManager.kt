/*
 * Copyright (c) 2025 IBA Group.
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

package org.zowe.explorer.v3.lang

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId


class CobolDependencyManager {
    fun init() {
        if (PluginManager.isPluginInstalled(PluginId.getId("org.zowe.cobol"))) {
            println("Zowe COBOL dependencies are installed")
            // TODO set up message bus between plugins
        } else {
            println("Zowe COBOL dependencies are not installed")
        }
    }
    fun getStatus(): String {
        if (PluginManager.isPluginInstalled(PluginId.getId("org.zowe.cobol"))) {
            return "Zowe COBOL dependencies are installed"
            // TODO set up message bus between plugins
        } else {
            return "Zowe COBOL dependencies are not installed"
        }
    }
}
