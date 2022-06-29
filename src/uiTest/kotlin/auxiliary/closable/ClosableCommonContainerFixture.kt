package auxiliary

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