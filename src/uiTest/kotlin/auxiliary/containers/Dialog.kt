package auxiliary.containers

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import java.time.Duration

/**
 * Finds a dialog. Not CLOSABLE in tear-down method!!!
 */
fun ContainerFixture.dialog(
    title: String,
    timeout: Duration = Duration.ofSeconds(20),
    function: Dialog.() -> Unit = {}): Dialog = step("Search for dialog with title $title") {
    find<Dialog>(Dialog.byTitle(title), timeout).apply(function)
}

/**
 * Class representing a dialog. Not CLOSABLE in tear-down method!!!
 */
@FixtureName("Dialog")
class Dialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent) : CommonContainerFixture(remoteRobot, remoteComponent) {

    companion object {
        /**
         * Returns the xPath of the dialog depending on its title.
         */
        @JvmStatic
        fun byTitle(title: String) = byXpath("title $title", "//div[@title='$title' and @class='MyDialog']")
    }
}