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

package auxiliary.closable

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture

/**
 * The container fixtures which will be needed to close in tear down method like dialogs will be
 * children of this class.
 *
 * The class sets up close method, which is overriden in each child, and is called upon in the tear-down method.
 */
abstract class ClosableCommonContainerFixture(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
): CommonContainerFixture(remoteRobot, remoteComponent) {
    abstract fun close()
}
