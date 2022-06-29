package auxiliary.containers

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Finds the Edit Connection Dialog and modifies the fixtureStack.
 */
fun ContainerFixture.editConnectionDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: EditConnectionDialog.() -> Unit = {}) {
    find<EditConnectionDialog>(EditConnectionDialog.xPath(), timeout).apply {
        fixtureStack.add(EditConnectionDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}

/**
 * Class representing the Edit Connection Dialog. It is a child of AddConnectionDialog, since
 * it is the same dialog, just with a different name.
 */
class EditConnectionDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : AddConnectionDialog(remoteRobot, remoteComponent) {
    companion object {
        const val name = "Edit Connection Dialog"

        /**
         * Returns the xPath of the Edit Connection Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath( name,"//div[@accessiblename='Edit Connection' and @class='MyDialog']")
    }
}