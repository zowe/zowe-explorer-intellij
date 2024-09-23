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

package testutils

import workingset.IdeaInteractionClass

class ProcessManager : AutoCloseable {

    private var utilObject = IdeaInteractionClass()

    init {

        try {
            utilObject.startIdea()
        } catch (e: Exception) {
            println("Init process error: ${e.message}")
            throw e
        }
    }


    override fun close() {
        utilObject.cleanUp()
    }
}
