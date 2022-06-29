package settings.connection

import auxiliary.RemoteRobotExtension
import auxiliary.clickButton
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Tests the ConnectionManager on UI level.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class ConnectionManager {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private val wantToClose = listOf("Settings Dialog", "Add Connection Dialog", "Error Creating Connection Dialog",
        "Edit Connection Dialog")
    private val projectName = "untitled"

    /**
     * Opens the project and Explorer.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        welcomeFrame {
            open(projectName)
        }
        Thread.sleep(30000)
        ideFrameImpl(projectName, fixtureStack) {
            forMainframe()
        }
    }

    /**
     * Closes the project.
     */
    @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            close()
        }
    }

    /**
     * Closes all unclosed closable fixtures that we want to clsoe.
     */
    @AfterEach
    fun tearDown(remoteRobot: RemoteRobot) {
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    }

    /**
     * Test that should fail and leave a bunch of opened dialogs.
     */
    @Test
    fun testA(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    conTab.click()
                    add(closableFixtureCollector, fixtureStack)
                }
                addConnectionDialog(fixtureStack) {
                    addConnection("a","https://a.com","a","a",true)
                    ok()
                }
                errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack)
                assertFalse(true)
            }
        }
    }

    /**
     * Tests that checks whether it si possible on UI level to add two connections with the same name.
     */
    @Test
    fun testB(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    conTab.click()
                    add(closableFixtureCollector, fixtureStack)
                }
                addConnectionDialog(fixtureStack) {
                    addConnection("a","https://a.com","a","a",true)
                    ok()
                }
                closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
                errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
                    yes()
                }
                closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
                configurableEditor {
                    add(closableFixtureCollector, fixtureStack)
                }
                addConnectionDialog(fixtureStack) {
                    addConnection("a","https://b.com","b","b",true)
                    assertFalse(button("OK").isEnabled())
                    clickButton("Cancel")
                }
                closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }
}

