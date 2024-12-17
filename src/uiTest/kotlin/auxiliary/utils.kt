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

package auxiliary

import auxiliary.closable.ClosableFixtureCollector
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.components.stripeButton
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.jupiter.api.TestInfo
import testutils.MockResponseDispatcher
import workingset.*
import workingset.*
//import workingset.testutils.InjectDispatcher
import java.awt.event.KeyEvent
import java.net.InetAddress
import java.time.Duration
import java.util.concurrent.TimeUnit

lateinit var mockServer: MockWebServer
lateinit var responseDispatcher: MockResponseDispatcher
//lateinit var injectDispatcher: InjectDispatcher

enum class JobStatus { ACTIVE, INPUT, OUTPUT }

//Change ZOS_USERID, ZOS_PWD, CONNECTION_URL with valid values before UI tests execution
const val ZOS_USERID = "ZOSMFAD"
const val ZOS_PWD = "ZOSMF"
const val CONNECTION_URL = "127.0.0.1"
const val REMOTE_URL = "127.0.0.1"
const val REMOTE_PORT = "8580"
const val CONNECTION_URL_UI = "http://$REMOTE_URL:$REMOTE_PORT"

const val ENTER_VALID_DS_MASK_MESSAGE = "Enter valid dataset mask"


const val TEXT_FIELD_LENGTH_MESSAGE = "Text field must not exceed 8 characters."
const val MEMBER_NAME_LENGTH_MESSAGE = "Member name must not exceed 8 characters."
const val JOB_ID_LENGTH_MESSAGE = "Job ID length must be 8 characters."
const val TEXT_FIELD_CONTAIN_MESSAGE = "Text field should contain only A-Z, a-z, 0-9, *, %."
const val JOBID_CONTAIN_MESSAGE = "Text field should contain only A-Z, a-z, 0-9"

const val PREFIX_OWNER_JOBID_MESSAGE = "You must provide either an owner and a prefix or a job ID."
const val IDENTICAL_FILTERS_MESSAGE = "You cannot add several identical job filters to table"
const val QUALIFIER_ONE_TO_EIGHT = "Qualifier must be in 1 to 8 characters"
val maskWithLength44 =
    ZOS_USERID + ".A2345678".repeat((44 - (ZOS_USERID.length + 1)) / 9) + "A".repeat((44 - (ZOS_USERID.length + 1)) % 9)
val maskWithLength45 =
    "$ZOS_USERID." + "A2345678.".repeat((45 - (ZOS_USERID.length + 1)) / 9) + "A".repeat((45 - (ZOS_USERID.length + 1)) % 9)











val viewTree = byXpath("//div[@class='JBViewport'][.//div[@class='DnDAwareTree']]")

//enum class UssFileType { File, Directory }

/**
 * Waits 60 seconds for the button to be enabled and then clicks on it.
 */
fun CommonContainerFixture.clickButton(text: String) {
    val button = button(text)
    waitFor(Duration.ofSeconds(60)) {
        button.isEnabled()
    }
    button.click()
}

/**
 * Waits a specific amount of time for the button to be enabled and then clicks on it.
 */
fun CommonContainerFixture.clickButton(locator: Locator, duration: Duration = Duration.ofSeconds(60)) {
    val button = button(locator)
    waitFor(duration) {
        button.isEnabled()
    }
    button.click()
}

/**
 * Waits 60 seconds for the action button to be enabled and then clicks on it.
 */
fun CommonContainerFixture.clickActionButton(locator: Locator) {
    val button = actionButton(locator)
    waitFor(Duration.ofSeconds(60)) {
        button.isEnabled()
    }
    button.click()
}

/**
 * Creates a working set via context menu from explorer.
 */
fun ContainerFixture.createWSFromContextMenu(
  fixtureStack: MutableList<Locator>,
  closableFixtureCollector: ClosableFixtureCollector,
) {
    explorer {
        fileExplorer.click()
        find<ComponentFixture>(viewTree).rightClick()
    }
    actionMenu(remoteRobot, NEW_POINT_TEXT).click()

    //workaround when an action menu contains more than 2 action menu items, and you need to choose the 3d item
    runJs(
        """
            const point = new java.awt.Point(${locationOnScreen.x}, ${locationOnScreen.y});
            robot.moveMouse(component, point);
        """
    )
    actionMenuItem(remoteRobot, WORKING_SET).click()
    closableFixtureCollector.add(AddWorkingSetDialog.xPath(), fixtureStack)
}

/**
 * Edits a working set via context menu from explorer.
 */
fun ContainerFixture.editWSFromContextMenu(
  wsName: String, fixtureStack: MutableList<Locator>,
  closableFixtureCollector: ClosableFixtureCollector,
) {
    explorer {
        fileExplorer.click()
        find<ComponentFixture>(viewTree).findText(wsName)
            .rightClick()
        Thread.sleep(3000)
    }
    actionMenuItem(remoteRobot, EDIT_POINT_TEXT).click()
    closableFixtureCollector.add(EditWorkingSetDialog.xPath(), fixtureStack)
}

/**
 * Creates a mask in the working set via context menu from explorer.
 */
fun ContainerFixture.createMask(
  wsName: String, fixtureStack: MutableList<Locator>,
  closableFixtureCollector: ClosableFixtureCollector,
) {
    explorer {
        fileExplorer.click()
        find<ComponentFixture>(viewTree).findText(wsName)
            .rightClick()
    }
    actionMenu(remoteRobot, NEW_POINT_TEXT).click()
    actionMenuItem(remoteRobot, MASK_POINT_TEXT).click()
    closableFixtureCollector.add(CreateMaskDialog.xPath(), fixtureStack)
}

/**
 * Deletes a working set via context menu from explorer.
 */
fun ContainerFixture.deleteWSFromContextMenu(wsName: String) {
    explorer {
        fileExplorer.click()
        find<ComponentFixture>(viewTree).findText(wsName)
            .rightClick()
    }
    actionMenuItem(remoteRobot, DELETE_TEXT).click()
}

/**
 * Deletes a JES working set via context menu from explorer.
 */
fun ContainerFixture.deleteJWSFromContextMenu(jwsName: String) {
    explorer {
        jesExplorer.click()
        find<ComponentFixture>(viewTree).findText(jwsName)
            .rightClick()
//        Thread.sleep(3000)
    }
    actionMenuItem(remoteRobot, DELETE_TEXT).click()
    find<ComponentFixture>(byXpath("//div[@class='MyDialog' and @title='Deletion of JES Working Set $jwsName']"))
}

/**
 * Deletes a JES working set via context menu from explorer.
 */
fun ContainerFixture.deleteJobFromContextMenu(jwsName: String) {
    explorer {
        jesExplorer.click()
        find<ComponentFixture>(viewTree).findText(jwsName)
            .rightClick()
//        Thread.sleep(3000)
    }
    actionMenuItem(remoteRobot, DELETE_TEXT).click()
    find<ComponentFixture>(byXpath("//div[@class='MyDialog' and @title='Deletion Of Jobs Filter']"))
}



/**
 * Creates a JES working set via context menu from explorer.
 */
fun ContainerFixture.createJWSFromContextMenu(
  fixtureStack: MutableList<Locator>,
  closableFixtureCollector: ClosableFixtureCollector,
) {
    explorer {
        jesExplorer.click()
        find<ComponentFixture>(viewTree).rightClick()
        Thread.sleep(3000)
    }
    actionMenu(remoteRobot, NEW_POINT_TEXT).click()
    actionMenuItem(remoteRobot, "JES Working Set").click()
    closableFixtureCollector.add(AddJesWorkingSetDialog.xPath(), fixtureStack)
}

/**
 * Creates a jobs filter in the JES working set via context menu from explorer.
 */
fun ContainerFixture.createJobsFilter(
  jwsName: String, fixtureStack: MutableList<Locator>,
  closableFixtureCollector: ClosableFixtureCollector,
) {
    explorer {
        jesExplorer.click()
        find<ComponentFixture>(viewTree).findText(jwsName)
            .rightClick()
        Thread.sleep(3000)
    }
    actionMenu(remoteRobot, NEW_POINT_TEXT).click()
    actionMenuItem(remoteRobot, "Jobs Filter").click()
    closableFixtureCollector.add(CreateJobsFilterDialog.xPath(), fixtureStack)
}

/**
 * Edites a JES working set via context menu from explorer.
 */
fun ContainerFixture.editJWSFromContextMenu(
  jwsName: String, fixtureStack: MutableList<Locator>,
  closableFixtureCollector: ClosableFixtureCollector,
) {
    explorer {
        jesExplorer.click()
        find<ComponentFixture>(viewTree).findText(jwsName)
            .rightClick()
        Thread.sleep(3000)
    }
    actionMenuItem(remoteRobot, EDIT_POINT_TEXT).click()
    closableFixtureCollector.add(EditJesWorkingSetDialog.xPath(), fixtureStack)
}

/**
 * Creates a working set via action button.
 */
fun ContainerFixture.callCreateWorkingSetFromActionButton(
  closableFixtureCollector: ClosableFixtureCollector,
  fixtureStack: MutableList<Locator>,
) {
    explorer {
        fileExplorer.click()
        createConfigItem()
    }
    find<HeavyWeightWindowFixture>(
        byXpath("//div[@class='HeavyWeightWindow']"),
        Duration.ofSeconds(30)
    ).findAllText().forEach {
        if (it.text == WORKING_SET) {
            it.click()
            closableFixtureCollector.add(AddWorkingSetDialog.xPath(), fixtureStack)
        }
    }
}

/**
 * Creates a JES working set via action button.
 */
fun ContainerFixture.createJesWorkingSetFromActionButton(
  closableFixtureCollector: ClosableFixtureCollector,
  fixtureStack: MutableList<Locator>,
) {
    explorer {
        jesExplorer.click()
        createConfigItem()
    }
    find<HeavyWeightWindowFixture>(
        messageLoc,
        Duration.ofSeconds(30)
    ).findAllText().forEach {
        if (it.text == "JES Working Set") {
            it.click()
            closableFixtureCollector.add(AddJesWorkingSetDialog.xPath(), fixtureStack)
        }
    }
}

/**
 * Creates a connection via action button.
 */
fun ContainerFixture.createConnectionFromActionButton(
  closableFixtureCollector: ClosableFixtureCollector,
  fixtureStack: MutableList<Locator>,
) {
    explorer {
        jesExplorer.click()
        createConfigItem()
    }
    find<HeavyWeightWindowFixture>(
        messageLoc,
        Duration.ofSeconds(30)
    ).findAllText().forEach {
        if (it.text == "Connection") {
            it.click()
            closableFixtureCollector.add(AddConnectionDialog.xPath(), fixtureStack)
        }
    }
}

/**
 * Steps to create a connection(valid or invalid) from settings .
 */
fun createConnection(
  fixtureStack: MutableList<Locator>,
  closableFixtureCollector: ClosableFixtureCollector,
  connectionName: String,
  isValidConnection: Boolean,
  remoteRobot: RemoteRobot,
  url: String = CONNECTION_URL,
  user: String = ZOS_USERID,
  password: String = ZOS_PWD,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        explorer {
            fileExplorer.click()
            settings(closableFixtureCollector, fixtureStack)
        }
        settingsDialog(fixtureStack) {
            configurableEditor {
                conTab.click()
                add(closableFixtureCollector, fixtureStack)
            }
            addConnectionDialog(fixtureStack) {
                if (isValidConnection) {
                    addConnection(connectionName, url, user, password, true)
                } else {
                    addConnection(connectionName, "${url}1", user, password, true)
                }
                clickButton(PROCEED_TEXT)
                clickButton(OK_TEXT)
            }
            clickButton(PROCEED_TEXT)
            Thread.sleep(3000)
            closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
            if (isValidConnection.not()) {
                errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
                    clickButton(YES_TEXT)
                }
                closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
            }
            clickButton(OK_TEXT)
        }
        closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
}

/**
 * Deletes all JES working sets, working sets and connections. To be used in BeforeAll and AfterAll tests methods.
 */
fun clearEnvironment(
  fixtureStack: MutableList<Locator>,
  closableFixtureCollector: ClosableFixtureCollector,
  remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        explorer {
            fileExplorer.click()
            settings(closableFixtureCollector, fixtureStack)
        }
        settingsDialog(fixtureStack) {
            configurableEditor {
                workingSetsTab.click()
                deleteAllItems()
                jesWorkingSetsTab.click()
                deleteAllItems()
                conTab.click()
                deleteAllItems()
            }
            clickButton(OK_TEXT)
        }
        closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
}

/**
 * Opens a project and an explorer, clears test environment before tests execution.
 */
fun setUpTestEnvironment(
  fixtureStack: MutableList<Locator>,
  closableFixtureCollector: ClosableFixtureCollector,
  remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    welcomeFrame {
        open()
    }
    Thread.sleep(10000)

    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        try {
            if (dialog("For Mainframe Plugin Privacy Policy and Terms and Conditions").isShowing) {
                clickButton("Dismiss")
            }
        } catch (e: WaitForConditionTimeoutException) {
            e.message.shouldContain("Failed to find 'Dialog' by 'title For Mainframe Plugin Privacy Policy and Terms and Conditions'")
        }
        try {
            find<ComponentFixture>(byXpath("//div[@class='ProjectViewTree']"))
            stripeButton(byXpath("//div[@accessiblename='Project' and @class='StripeButton' and @text='Project']"))
                .click()
        } catch (e: WaitForConditionTimeoutException) {
            //do nothing if ProjectViewTree is hidden
        }
        forMainframe()
    }
    clearEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
}

/**
 * Opens a mask in the working set in explorer.
 */
fun openWSOpenMaskInExplorer(
  wsName: String, maskName: String,
  fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
) =
    with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
                Thread.sleep(3000)
                find<ComponentFixture>(viewTree).findText(maskName).doubleClick()
                Thread.sleep(2000)
                waitFor(Duration.ofSeconds(20)) { find<ComponentFixture>(viewTree).hasText("loading…").not() }
                find<ComponentFixture>(viewTree).findAllText().shouldNotContain("Error")
                find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
            }
        }
    }

/**
 * Double-clicks on the working set to open or close it in explorer.
 */
fun openOrCloseWorkingSetInExplorer(
  wsName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        explorer {
            fileExplorer.click()
            find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
            Thread.sleep(1000)
        }
    }
}

/**
 * Double-clicks on the jes working set to open or close it in explorer.
 */
fun openOrCloseJesWorkingSetInExplorer(
  jwsName: String,
  fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        explorer {
            jesExplorer.click()
            find<ComponentFixture>(viewTree).findText(jwsName).doubleClick()
            Thread.sleep(3000)
        }
    }
}

/**
 * Double-clicks on the mask in explorer to open it and checks the message if required.
 */
fun openMaskInExplorer(
  maskName: String, expectedError: String,
  fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
) =
    with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(maskName).doubleClick()
                Thread.sleep(20000)
                var allText = ""
                find<ComponentFixture>(viewTree).findAllText().forEach { allText += it.text }
                if (expectedError.isEmpty().not()) {
                    allText.shouldContain(expectedError)
                } else {
                    allText.shouldNotContain("Error")
                }
            }
        }
    }

fun convertJobFilterToString(jobFilter: Triple<String, String, String>): String {
    val textToFind = if (jobFilter.third == "") {
        "PREFIX=${jobFilter.first} OWNER=${jobFilter.second}".uppercase()
    } else {
        "JobID=${jobFilter.third}"
    }
    return textToFind
}

/**
 * Double-clicks on job filter to close it in explorer.
 */
fun closeFilterInExplorer(
  filter: Triple<String, String, String>,
  fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
) =
    with(remoteRobot) {
        val textToFind = if (filter.third == "") {
            "PREFIX=${filter.first} OWNER=${filter.second}".uppercase()
        } else {
            "JobID=${filter.third}"
        }
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                jesExplorer.click()
                find<ComponentFixture>(viewTree).findText(textToFind).doubleClick()
            }
        }
    }

/**
 * Double-clicks on mask to close it in explorer.
 */
fun closeMaskInExplorer(
  maskName: String,
  fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
) =
    with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(maskName).doubleClick()
            }
        }
    }

/**
 * Checks that the mask or the working set is not displayed in explorer.
 */
fun checkItemWasDeletedWSRefreshed(
  deletedItem: String,
  fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        explorer {
            shouldThrow<NoSuchElementException> {
                find<ComponentFixture>(viewTree).findText(deletedItem)
            }
        }
    }
}

/**
 * Checks that the job filter is not displayed in explorer.
 */
fun checkFilterWasDeletedJWSRefreshed(
  deletedFilter: Triple<String, String, String>,
  fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    val textToFind = convertJobFilterToString(deletedFilter)
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        explorer {
            jesExplorer.click()
            shouldThrow<NoSuchElementException> {
                find<ComponentFixture>(viewTree).findText(textToFind)
            }
        }
    }
}

/**
 * Deletes dataset via context menu.
 */
fun deleteDataset(
  datasetName: String,
  fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        explorer {
            fileExplorer.click()
            find<ComponentFixture>(viewTree).findAllText(datasetName).last().rightClick()
        }
        actionMenuItem(remoteRobot, "Delete").click()
        dialog("Confirm Files Deletion") {
            clickButton(YES_TEXT)
        }
//    Thread.sleep(3000)
    }
}

/**
 * Opens the file and copies it's content.
 */
fun openLocalFileAndCopyContent(
  filePath: String,
  fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        actionMenu(remoteRobot, "File").click()
        runJs(
            """
            const point = new java.awt.Point(${-8}, ${25});
            robot.moveMouse(component, point);
        """
        )
        actionMenuItem(remoteRobot, "Open...").click()
        Thread.sleep(3000)
        dialog("Open File or Project") {
            textField(byXpath("//div[@class='BorderlessTextField']")).text =
                filePath
            Thread.sleep(5000)
            clickButton("OK")
        }
        with(textEditor()) {
            keyboard {
                hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
                hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_C)
                hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_F4)
            }
        }
    }
}

/**
 * Submits the job via context menu.
 */
fun submitJob(
  jobName: String,
  fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        explorer {
            fileExplorer.click()
            find<ComponentFixture>(viewTree).findText(jobName).rightClick()
        }
        actionMenuItem(remoteRobot, SUBMIT_JOB_POINT).click()
    }
}

/**
 * Creates a member in the dataset and pastes content to the member.
 */
fun createMemberAndPasteContent(
  datasetName: String,
  memberName: String,
  fixtureStack: MutableList<Locator>,
  remoteRobot: RemoteRobot,
) =
    with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findAllText(datasetName).last().rightClick()
            }
            actionMenu(remoteRobot, NEW_POINT_TEXT).click()
            actionMenuItem(remoteRobot, "Member").click()
            dialog("Create Member") {
                find<JTextFieldFixture>(datasetNameInputLoc).text = memberName
            }
            clickButton(OK_TEXT)
            Thread.sleep(5000)
            explorer {
                find<ComponentFixture>(viewTree).findAllText(memberName).last().doubleClick()
            }
            with(textEditor()) {
                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_V)
                    Thread.sleep(2000)
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_S)
                    Thread.sleep(2000)
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_F4)
                }
            }
        }
    }

/**
 * Allocates a PDS dataset and creates a mask for it.
 * @param maskName when specified, mask will be created with the provided name, otherwise maskName will be equal to datasetName
 */
fun allocatePDSAndCreateMask(
  wsName: String,
  datasetName: String,
  fixtureStack: MutableList<Locator>,
  closableFixtureCollector: ClosableFixtureCollector,
  remoteRobot: RemoteRobot,
  maskName: String? = null,
  directory: Int = 1,
  openWs: Boolean = true,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        if (maskName != null) {
            createMask(wsName, fixtureStack, closableFixtureCollector)
            createMaskDialog(fixtureStack) {
                createMask(Pair(maskName, ZOS_MASK))
                clickButton(OK_TEXT)
            }
        }

        explorer {
            fileExplorer.click()
            find<ComponentFixture>(viewTree).findText(wsName).rightClick()
        }
        actionMenu(remoteRobot, NEW_POINT_TEXT).click()
        actionMenuItem(remoteRobot, DATASET_POINT_TEXT).click()
        allocateDatasetDialog(fixtureStack) {
            allocateDataset(datasetName, PO_ORG_FULL, "TRK", 10, 1, directory, "VB", 255, 6120)
            clickButton(OK_TEXT)
            Thread.sleep(500)
        }

        val textToFind = "Dataset ${datasetName.uppercase()} has been created"
        val dialog = find<ContainerFixture>(byXpath("(//div[@class='JPanel'][.//div[@class='LinkLabel']])[1]"))
        val dialogContents = dialog.findAllText().map(RemoteText::text).joinToString("")
        val hasText = dialogContents.contains(textToFind)
        if (!hasText) {
            throw Exception("Text is not found in dialog: $textToFind")
        }

        if (maskName == null) {
            clickButton("Add mask")
            Thread.sleep(200)
        }
    }
    if (openWs) {
        openOrCloseWorkingSetInExplorer(wsName, fixtureStack, remoteRobot)
    }
}

/**
 * Creates working set without masks.
 */
fun createWsWithoutMask(
  wsName: String,
  connectionName: String,
  fixtureStack: MutableList<Locator>,
  closableFixtureCollector: ClosableFixtureCollector,
  remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        createWSFromContextMenu(fixtureStack, closableFixtureCollector)
        addWorkingSetDialog(fixtureStack) {
            addWorkingSet(wsName, connectionName)
            clickButton(OK_TEXT)
            clickButton(OK_TEXT)
        }
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
    }
}

/**
 * Creates working set without masks from action button.
 */
fun createWsWithoutMaskActionButton(
    wsName: String,
    connectionName: String,
    fixtureStack: MutableList<Locator>,
    closableFixtureCollector: ClosableFixtureCollector,
    remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        callCreateWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
        addWorkingSetDialog(fixtureStack) {
            addWorkingSet(wsName, connectionName)
            clickButton(OK_TEXT)
            clickButton(OK_TEXT)
        }
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
    }
}

/**
 * Allocates member for pds.
 */
fun allocateMemberForPDS(
  datasetName: String,
  memberName: String,
  fixtureStack: MutableList<Locator>,
  remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        explorer {
            fileExplorer.click()
            find<ComponentFixture>(viewTree).findText(datasetName).rightClick()
        }

        actionMenu(remoteRobot, "New").click()
        actionMenuItem(remoteRobot, "Member").click()
        createMemberDialog(fixtureStack) {
            createMember(memberName)
            clickButton("OK")
//      Thread.sleep(5000)

        }
    }
}

fun closeNotificztion(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot){
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        find<ComponentFixture>(closeNotificationLoc).click()
    }
}
/**
 * Allocates a sequential dataset.
 */
fun allocateDataSet(
  wsName: String,
  datasetName: String,
  fixtureStack: MutableList<Locator>,
  remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        explorer {
            fileExplorer.click()
            find<ComponentFixture>(viewTree).findText(wsName).rightClick()
        }
        actionMenu(remoteRobot, "New").click()
        actionMenuItem(remoteRobot, "Dataset").click()
        allocateDatasetDialog(fixtureStack) {
            allocateDataset(datasetName, POE_ORG_FULL, "TRK", 10, 1, 1, "VB", 255, 6120)
            clickButton(OK_TEXT)
      Thread.sleep(3000)
        }
//        try {
//            find<ContainerFixture>(myDialogXpathLoc).findText("Dataset $datasetName Has Been ")
//        }
//        catch (e: NoSuchElementException) {
//            find<ContainerFixture>(myDialogXpathLoc).findText("Dataset $datasetName Has Been Created")
//        }
//        finally{
//            clickButton(NO_TEXT)}
//        find<ContainerFixture>(myDialogXpathLoc).findText("Dataset $datasetName Has Been Created")
//        clickButton(NO_TEXT)
        explorer {
            fileExplorer.click()
            find<ComponentFixture>(viewTree).findText(wsName).rightClick()
        }
        actionMenuItem(remoteRobot, REFRESH_POINT_TEXT).click()
//    Thread.sleep(3000)
    }
}

/**
 * Checks title and error information for popup window.
 */
fun checkErrorNotification(
  errorHeader: String,
  errorType: String,
  errorDetail: String,
  fixtureStack: MutableList<Locator>,
  remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        var errorMessage = ""
        try {
            find<ComponentFixture>(linkLoc).click()
        } catch (e: WaitForConditionTimeoutException) {
            e.message.shouldContain("Failed to find 'ComponentFixture' by '//div[@class='LinkLabel']'")
        }
        find<JLabelFixture>(errorDetailHeaderLoc).findText(errorHeader)
        find<ContainerFixture>(errorDetailBodyLoc).findAllText().forEach {
            errorMessage += it.text
        }
        find<ComponentFixture>(closeDialogLoc).click()
        find<ComponentFixture>(closeDialogLoc).click()
        if (!(errorMessage.contains(errorType) && errorMessage.contains(errorDetail))) {
            throw Exception("Error message is different from expected")
        }
    }
}

fun isErrorNotificationValid(
    errorHeader: String,
    errorType: String,
    errorDetail: String,
    fixtureStack: MutableList<Locator>,
    remoteRobot: RemoteRobot,
): Boolean = with(remoteRobot) {
    var errorMessage = ""
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        try {
            find<ComponentFixture>(linkLoc).click()
        } catch (e: WaitForConditionTimeoutException) {
            e.message.shouldContain(ABSENT_ERROR_MSG)
        }
        find<JLabelFixture>(errorDetailHeaderLoc).findText(errorHeader)
        find<ContainerFixture>(errorDetailBodyLocAlt).findAllText().forEach {
            errorMessage += it.text
        }
        find<ComponentFixture>(closeDialogLoc).click()
        find<ComponentFixture>(closeDialogLoc).click()
    }
    return errorMessage.contains(errorType) && errorMessage.contains(errorDetail)
}

fun buildFinalListDatasetJson(mapListDatasets: MutableMap<String, String>): String {
    var result = "{}"
    if (mapListDatasets.isNotEmpty()) {
        var listDatasetsJson = "{\"items\":["
        mapListDatasets.forEach {
            listDatasetsJson += it.value
        }
        result = listDatasetsJson.dropLast(1) + "],\n" +
                "  \"returnedRows\": ${mapListDatasets.size},\n" +
                "  \"totalRows\": ${mapListDatasets.size},\n" +
                "  \"JSONversion\": 1\n" +
                "}"
    }
    return result
}

/**
 * Builds json list of objects based on provided map
 */
fun buildResponseListJson(
    elementList: Map<String, String>,
    containsTotal: Boolean
): String {
    var result = "{\"items\":[],\"returnedRows\":0," +
            if (containsTotal) {
                "  \"totalRows\": ${elementList.size},\n"
            } else {
                ""
            } + "\"JSONversion\":1}"
    if (elementList.isNotEmpty()) {
        var elementsListJson = "{\"items\":["
        elementList.forEach {
            elementsListJson += it.value
        }
        result = elementsListJson.dropLast(1) + "],\n" +
                "  \"returnedRows\": ${elementList.size},\n" +
                if (containsTotal) {
                    "  \"totalRows\": ${elementList.size},\n"
                } else {
                    ""
                } +
                "  \"JSONversion\": 1\n" +
                "}"
    }
    return result
}

/**
 * Starts mock server for UI tests.
 */
fun startMockServer() {
    val localhost = InetAddress.getByName("localhost").canonicalHostName
    val localhostCertificate = HeldCertificate.Builder()
        .addSubjectAlternativeName(localhost)
        .duration(10, TimeUnit.MINUTES)
        .build()
    val serverCertificates = HandshakeCertificates.Builder()
        .heldCertificate(localhostCertificate)
        .build()
    mockServer = MockWebServer()
    responseDispatcher = MockResponseDispatcher()
//    injectDispatcher = InjectDispatcher()
    mockServer.dispatcher = responseDispatcher
    mockServer.useHttps(serverCertificates.sslSocketFactory(), false)
    mockServer.start()
}




/**
 * Creates working set and a mask.
 */
fun createWsAndMask(
  wsName: String,
  masks: List<Pair<String, String>>,
  connectionName: String,
  fixtureStack: MutableList<Locator>,
  closableFixtureCollector: ClosableFixtureCollector,
  remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    createWsWithoutMaskActionButton(wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        masks.forEach { mask ->
            createMask(wsName, fixtureStack, closableFixtureCollector)
            createMaskDialog(fixtureStack) {
                createMask(mask)
//        Thread.sleep(3000)
                clickButton(OK_TEXT)
            }
            closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        }
    }
}

/**
 * Creates json to list dataset.
 */
fun listDS(dsName: String, dsNtp: String, dsOrg: String): String {
    return "{\n" +
            "      \"dsname\": \"${dsName}\",\n" +
            "      \"blksz\": \"3200\",\n" +
            "      \"catnm\": \"TEST.CATALOG.MASTER\",\n" +
            "      \"cdate\": \"2021/11/15\",\n" +
            "      \"dev\": \"3390\",\n" +
            "      \"dsntp\": \"${dsNtp}\",\n" +
            "      \"dsorg\": \"${dsOrg}\",\n" +
            "      \"edate\": \"***None***\",\n" +
            "      \"extx\": \"1\",\n" +
            "      \"lrecl\": \"255\",\n" +
            "      \"migr\": \"NO\",\n" +
            "      \"mvol\": \"N\",\n" +
            "      \"ovf\": \"NO\",\n" +
            "      \"rdate\": \"2021/11/17\",\n" +
            "      \"recfm\": \"VB\",\n" +
            "      \"sizex\": \"10\",\n" +
            "      \"spacu\": \"TRACKS\",\n" +
            "      \"used\": \"1\",\n" +
            "      \"vol\": \"TESTVOL\",\n" +
            "      \"vols\": \"TESTVOL\"\n" +
            "    },"
}

/**
 * Creates valid connection to mock server.
 */
fun createValidConnectionWithMock(
  testInfo: TestInfo, connectionName: String, fixtureStack: MutableList<Locator>,
  closableFixtureCollector: ClosableFixtureCollector, remoteRobot: RemoteRobot,
) = with(remoteRobot) {
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_info",
        { it?.requestLine?.contains("zosmf/info") ?: false },
        { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_resttopology",
        { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false },
        { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    createConnection(
        fixtureStack,
        closableFixtureCollector,
        connectionName,
        true,
        remoteRobot,
        "https://${mockServer.hostName}:${mockServer.port}"
    )
}

fun createEmptyDatasetMember(
  datasetName: String,
  memberName: String,
  fixtureStack: MutableList<Locator>,
  remoteRobot: RemoteRobot,
) =
    with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findAllText(datasetName).last().rightClick()
            }
            actionMenu(remoteRobot, "New").click()
            actionMenuItem(remoteRobot, "Member").click()
            dialog("Create Member") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = memberName
            }
            clickButton("OK")
        }
    }

/**
 * Pastes content to dataset member from buffer.
 */
fun pasteContent(
  memberName: String,
  fixtureStack: MutableList<Locator>,
  remoteRobot: RemoteRobot,
) =
    with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                find<ComponentFixture>(viewTree).findAllText(memberName).last().doubleClick()
                Thread.sleep(2000)
            }
            with(textEditor()) {
                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_V)
                    Thread.sleep(2000)
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_S)
                    Thread.sleep(2000)
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_F4)
                }
            }
        }
    }
fun closeTabByKeyboard(
  fixtureStack: MutableList<Locator>,
  remoteRobot: RemoteRobot,
) =
    with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            with(textEditor()) {
                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_F4)
                }
            }
        }
    }

/**
 * Creates json for submitted job.
 */
fun setBodyJobSubmit(jobName: String, jobStatus: JobStatus): String {
    return "{\n" +
            "  \"owner\": \"${ZOS_USERID.uppercase()}\",\n" +
            "  \"phase\": 14,\n" +
            "  \"subsystem\": \"JES2\",\n" +
            "  \"phase-name\": \"Job is actively executing\",\n" +
            "  \"job-correlator\": \"J0007380S0W1....DB23523B.......:\",\n" +
            "  \"type\": \"JOB\",\n" +
            "  \"url\": \"https://${mockServer.hostName}:${mockServer.port}/zosmf/restjobs/jobs/J0007380S0W1....DB23523B.......%3A\",\n" +
            "  \"jobid\": \"JOB07380\",\n" +
            "  \"class\": \"B\",\n" +
            "  \"files-url\": \"https://${mockServer.hostName}:${mockServer.port}/zosmf/restjobs/jobs/J0007380S0W1....DB23523B.......%3A/files\",\n" +
            "  \"jobname\": \"${jobName}\",\n" +
            "  \"status\": \"${jobStatus}\",\n" +
            "  \"retcode\": null\n" +
            "}\n"
}
/**
 * Click by button with text.
 */
fun clickByText(buttonText: String,fixtureStack: MutableList<Locator>,remoteRobot: RemoteRobot)= with(remoteRobot){
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
        clickButton(buttonText)
    }
}


fun replaceInJson(fileName: String, valuesMap: Map<String, String>): String {
    var sourceJson = responseDispatcher.readMockJson(fileName) ?: ""
    valuesMap.forEach { entry ->
        sourceJson = sourceJson.replace(entry.key, entry.value)
    }

    return sourceJson
}

fun createMask(wsName: String, maskName: String, fixtureStack: MutableList<Locator>,closableFixtureCollector: ClosableFixtureCollector, mask_type: String, remoteRobot: RemoteRobot)= with(remoteRobot){
    ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createMask(wsName, fixtureStack, closableFixtureCollector)
            createMaskDialog(fixtureStack) {
                createMask(Pair(maskName, mask_type))
                clickButton(OK_TEXT)
        }
    }
}

fun buildListMembersJson(listMembersInDataset:  MutableList<String>): String {
    var members = "[ "
    if (listMembersInDataset.isNotEmpty()) {
        listMembersInDataset.forEach { members += "{\"member\": \"${it}\"}," }
    }
    members = members.dropLast(1) + "]"
    return "{\"items\":$members,\"returnedRows\": ${listMembersInDataset.size},\"JSONversion\": 1}"
}

/**
 * build text for cansel job nessage in console.
 */
fun setBodyJobCancelled(jobName: String): String {
    return "{\n" +
            "\"jobid\":\"JOB07380\",\n" +
            "\"jobname\":\"$jobName\",\n" +
            "\"original-jobid\":\"JOB07380\",\n" +
            "\"owner\":\"${ZOS_USERID.uppercase()}\",\n" +
            "\"member\":\"JES2\",\n" +
            "\"sysname\":\"SY1\",\n" +
            "\"job-correlator\":\"JOB07380SY1.....CC20F378.......:\",\n" +
            "\"status\":\"0\",\n" +
            "\"retcode\":\"CANCELED\"\n" +
            "}"
}

fun setBodyJobHoldOrReleased(jobName: String): String {
    return "{\n" +
            "\"jobid\":\"JOB07380\",\n" +
            "\"jobname\":\"$jobName\",\n" +
            "\"original-jobid\":\"JOB07380\",\n" +
            "\"owner\":\"${ZOS_USERID.uppercase()}\",\n" +
            "\"member\":\"JES2\",\n" +
            "\"sysname\":\"SY1\",\n" +
            "\"job-correlator\":\"JOB07380SY1.....CC20F378.......:\",\n" +
            "\"status\":\"0\"\n" +
            "}"
}
fun setBodyJobPurged(jobName: String): String {
    return "{\n" +
    "  \"jobid\":\"JOB07380\",\n" +
    "  \"jobname\":\"${jobName}\",\n" +
    "  \"original-jobid\":\"JOB07380\",\n" +
    "  \"owner\":\"${ZOS_USERID.uppercase()}\",\n" +
    "  \"member\":\"JES2\",\n" +
    "  \"sysname\":\"SY1\",\n" +
    "  \"job-correlator\":\"JOB07380SY1.....CC20F380.......:\",\n" +
    "  \"status\":\"0\"\n" +
    "}\n"
}
