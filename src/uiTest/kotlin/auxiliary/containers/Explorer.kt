package auxiliary.containers

import auxiliary.clickActionButton
import auxiliary.closable.ClosableFixtureCollector
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Finds the Explorer and modifies the fixtureStack.
 */
fun ContainerFixture.explorer(function: Explorer.() -> Unit) {
    find<Explorer>(Explorer.xPath(), Duration.ofSeconds(60)).apply(function)
}

/**
 * Class representing the Explorer.
 */
@FixtureName("Explorer")
class Explorer(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : CommonContainerFixture(remoteRobot, remoteComponent) {
    /**
     * Clicks on the settings action and adds the Settings Dialog to the list of fixtures needed to close.
     */
    fun settings(closableFixtureCollector: ClosableFixtureCollector, fixtureStack: List<Locator>) {
        clickActionButton(byXpath("//div[@class='ActionButton' and @myaction=' ()']"))
        closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)
    }
    companion object {
        /**
         * Returns the xPath of the Explorer.
         */
        @JvmStatic
        fun xPath() = byXpath( "//div[@accessiblename='File Explorer Tool Window' and @class='InternalDecoratorImpl']")
    }
}