package auxiliary.containers

import auxiliary.ClosableCommonContainerFixture
import auxiliary.clickButton
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Finds the Settings Dialog and modifies the fixtureStack.
 */
fun ContainerFixture.settingsDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: SettingsDialog.() -> Unit = {}) {
    find<SettingsDialog>(SettingsDialog.xPath(), timeout).apply {
        fixtureStack.add(SettingsDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}

/**
 * Class representing the Settings Dialog.
 */
@FixtureName("Settings Dialog")
class SettingsDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

    /**
     * The close function, which is used to close the dialog in the tear down method.
     */
    override fun close() {
        cancel()
    }

    /**
     * Clicks on the Cancel button.
     */
    fun cancel() {
        clickButton("Cancel")
    }
    companion object {
        const val name = "Settings Dialog"
        /**
         * Returns the xPath of the Settings Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath( name,"//div[@accessiblename='Settings' and @class='MyDialog']")
    }
}