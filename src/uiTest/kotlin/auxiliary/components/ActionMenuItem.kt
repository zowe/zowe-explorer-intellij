package auxiliary.components

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.waitFor

/**
 * Function, which looks for the ActionMenuItem.
 */
fun ContainerFixture.actionMenuItem(remoteRobot: RemoteRobot, text: String): ActionMenuItemFixture {
    val xpath = byXpath("text '$text'", "//div[@class='ActionMenuItem' and @text='$text']")
    waitFor {
        findAll<ActionMenuItemFixture>(xpath).isNotEmpty()
    }
    return findAll<ActionMenuItemFixture>(xpath).first()
}

/**
 * This class was copied from ui-robot at jet brains.
 *
 * It represents the ActionMenuItem.
 */
@FixtureName("ActionMenuItem")
class ActionMenuItemFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : ComponentFixture(remoteRobot, remoteComponent)