package auxiliary

import com.intellij.remoterobot.RemoteRobot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import auxiliary.containers.*
import com.intellij.remoterobot.search.locators.Locator
import org.junit.jupiter.api.Tag

/**
 * When adding UI tests to GitHub Actions pipeline, there is a need to first run dummy test, which
 * gets rid of tips and agrees to license agreement.
 */
@Tag("FirstTime")
@ExtendWith(RemoteRobotExtension::class)
class ConnectionManager {
    private var stack = mutableListOf<Locator>()
    private val projectName = "untitled"

    @Test
    fun firstTime(remoteRobot: RemoteRobot) = with(remoteRobot) {
        welcomeFrame {
            open(projectName)
        }
        Thread.sleep(60000)
        ideFrameImpl(projectName, stack) {
            dialog("For Mainframe Plugin Privacy Policy and Terms and Conditions") {
                clickButton("I Agree")
            }
            close()
        }
    }
}