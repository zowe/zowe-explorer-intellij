/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package auxiliary

import auxiliary.closable.ClosableFixtureCollector
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.components.stripeButton
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.string.shouldContain
import java.awt.event.KeyEvent
import java.time.Duration

//Change ZOS_USERID, ZOS_PWD, CONNECTION_URL with valid values before UI tests execution
const val ZOS_USERID = "ZOSMFAD"
const val ZOS_PWD = "ZOSMF"
const val CONNECTION_URL = "https://172.20.2.69:10443"

const val ENTER_VALID_DS_MASK_MESSAGE = "Enter valid dataset mask"
const val IDENTICAL_MASKS_MESSAGE = "You cannot add several identical masks to table"
const val EMPTY_DATASET_MESSAGE = "You are going to create a Working Set that doesn't fetch anything"
const val TEXT_FIELD_LENGTH_MESSAGE = "Text field must not exceed 8 characters."
const val MEMBER_NAME_LENGTH_MESSAGE = "Member name must not exceed 8 characters."
const val INVALID_MEMBER_NAME_MESSAGE = "Member name should contain only A-Z a-z 0-9 or national characters"
const val INVALID_MEMBER_NAME_BEGINNING_MESSAGE = "Member name should start with A-Z a-z or national characters"
const val JOB_ID_LENGTH_MESSAGE = "Job ID length must be 8 characters."
const val TEXT_FIELD_CONTAIN_MESSAGE = "Text field should contain only A-Z, a-z, 0-9, *, %."
const val JOBID_CONTAIN_MESSAGE = "Text field should contain only A-Z, a-z, 0-9"
const val PREFIX_OWNER_JOBID_MESSAGE = "You must provide either an owner and a prefix or a job ID."
const val IDENTICAL_FILTERS_MESSAGE = "You cannot add several identical job filters to table"

val maskWithLength44 =
    "$ZOS_USERID." + "A2345678.".repeat((44 - (ZOS_USERID.length + 1)) / 9) + "A".repeat((44 - (ZOS_USERID.length + 1)) % 9)
val maskWithLength45 =
    "$ZOS_USERID." + "A2345678.".repeat((45 - (ZOS_USERID.length + 1)) / 9) + "A".repeat((45 - (ZOS_USERID.length + 1)) % 9)

//todo add mask *.* when bug is fixed
val maskMessageMap = mapOf(
    "1$ZOS_USERID.*" to ENTER_VALID_DS_MASK_MESSAGE,
    "$ZOS_USERID.{!" to ENTER_VALID_DS_MASK_MESSAGE,
    "$ZOS_USERID.A23456789.*" to "Qualifier must be in 1 to 8 characters",
    "$ZOS_USERID." to ENTER_VALID_DS_MASK_MESSAGE,
    maskWithLength45 to "Dataset mask length must not exceed 44 characters",
    "$ZOS_USERID.***" to "Invalid asterisks in the qualifier"
)

val validZOSMasks = listOf(
    "$ZOS_USERID.*", "$ZOS_USERID.**", "$ZOS_USERID.@#%", "$ZOS_USERID.@#%.*", "Q.*", "WWW.*", maskWithLength44,
    ZOS_USERID
)

val validUSSMasks = listOf("/u", "/etc/ssh", "/u/$ZOS_USERID")

val validJobsFilters = listOf(
    Triple("*", ZOS_USERID, ""),
    Triple("TEST1", ZOS_USERID, ""),
    Triple("", "", "JOB01234"),
    Triple("TEST*", ZOS_USERID, ""),
    Triple("TEST**", ZOS_USERID, ""),
    Triple("TEST***", ZOS_USERID, ""),
    Triple("TEST1", "$ZOS_USERID*", ""),
    Triple("TEST1", "$ZOS_USERID**", ""),
    Triple("TEST1", "$ZOS_USERID***", ""),
    Triple("TEST***", "$ZOS_USERID***", "")
)

val invalidJobsFiltersMap = mapOf(
    Pair(Triple("123456789", ZOS_USERID, ""), 1) to TEXT_FIELD_LENGTH_MESSAGE,
    Pair(Triple("123456789", "A23456789", ""), 1) to TEXT_FIELD_LENGTH_MESSAGE,
    Pair(Triple("*", "A23456789", ""), 2) to TEXT_FIELD_LENGTH_MESSAGE,
    Pair(Triple("", "", "A23456789"), 3) to JOB_ID_LENGTH_MESSAGE,
    Pair(Triple("", "", "A2"), 3) to JOB_ID_LENGTH_MESSAGE,
    Pair(Triple("@", ZOS_USERID, ""), 1) to TEXT_FIELD_CONTAIN_MESSAGE,
    Pair(Triple("*", "@", ""), 2) to TEXT_FIELD_CONTAIN_MESSAGE,
    Pair(Triple("", "", "@@@@@@@@"), 3) to JOBID_CONTAIN_MESSAGE,
    Pair(Triple("*", ZOS_USERID, "JOB45678"), 1) to PREFIX_OWNER_JOBID_MESSAGE,
    Pair(Triple("*", "", "JOB45678"), 1) to PREFIX_OWNER_JOBID_MESSAGE,
    Pair(Triple("", ZOS_USERID, "JOB45678"), 2) to PREFIX_OWNER_JOBID_MESSAGE
)

val viewTree = byXpath("//div[@class='JBViewport'][.//div[@class='DnDAwareTree']]")

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
    closableFixtureCollector: ClosableFixtureCollector
) {
    explorer {
        fileExplorer.click()
        find<ComponentFixture>(viewTree).rightClick()
        Thread.sleep(3000)
    }
    actionMenu(remoteRobot, "New").click()

    //workaround when an action menu contains more than 2 action menu items, and you need to choose the 3d item
    runJs(
        """
            const point = new java.awt.Point(${locationOnScreen.x}, ${locationOnScreen.y});
            robot.moveMouse(component, point);
        """
    )
    actionMenuItem(remoteRobot, "Working Set").click()
    closableFixtureCollector.add(AddWorkingSetDialog.xPath(), fixtureStack)
}

/**
 * Edits a working set via context menu from explorer.
 */
fun ContainerFixture.editWSFromContextMenu(
    wsName: String, fixtureStack: MutableList<Locator>,
    closableFixtureCollector: ClosableFixtureCollector
) {
    explorer {
        fileExplorer.click()
        find<ComponentFixture>(viewTree).findText(wsName)
            .rightClick()
        Thread.sleep(3000)
    }
    actionMenuItem(remoteRobot, "Edit").click()
    closableFixtureCollector.add(EditWorkingSetDialog.xPath(), fixtureStack)
}

/**
 * Creates a mask in the working set via context menu from explorer.
 */
fun ContainerFixture.createMask(
    wsName: String, fixtureStack: MutableList<Locator>,
    closableFixtureCollector: ClosableFixtureCollector
) {
    explorer {
        fileExplorer.click()
        find<ComponentFixture>(viewTree).findText(wsName)
            .rightClick()
        Thread.sleep(3000)
    }
    actionMenu(remoteRobot, "New").click()
    actionMenuItem(remoteRobot, "Mask").click()
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
        Thread.sleep(3000)
    }
    actionMenuItem(remoteRobot, "Delete").click()
    find<ComponentFixture>(byXpath("//div[@class='MyDialog' and @title='Deletion of Working Set $wsName']"))
}

/**
 * Creates a jobs working set via context menu from explorer.
 */
fun ContainerFixture.createJWSFromContextMenu(
    fixtureStack: MutableList<Locator>,
    closableFixtureCollector: ClosableFixtureCollector
) {
    explorer {
        jesExplorer.click()
        find<ComponentFixture>(viewTree).rightClick()
        Thread.sleep(3000)
    }
    actionMenu(remoteRobot, "New").click()
    actionMenuItem(remoteRobot, "Jobs Working Set").click()
    closableFixtureCollector.add(AddJobsWorkingSetDialog.xPath(), fixtureStack)
}

/**
 * Creates a jobs filter in the jobs working set via context menu from explorer.
 */
fun ContainerFixture.createJobsFilter(
    jwsName: String, fixtureStack: MutableList<Locator>,
    closableFixtureCollector: ClosableFixtureCollector
) {
    explorer {
        jesExplorer.click()
        find<ComponentFixture>(viewTree).findText(jwsName)
            .rightClick()
        Thread.sleep(3000)
    }
    actionMenu(remoteRobot, "New").click()
    actionMenuItem(remoteRobot, "Jobs Filter").click()
    closableFixtureCollector.add(CreateJobsFilterDialog.xPath(), fixtureStack)
}

/**
 * Steps to create a connection(valid or invalid) from settings .
 */
fun createConnection(
    projectName: String, fixtureStack: MutableList<Locator>, closableFixtureCollector: ClosableFixtureCollector,
    connectionName: String, isValidConnection: Boolean, remoteRobot: RemoteRobot
) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
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
                    addConnection(connectionName, CONNECTION_URL, ZOS_USERID, ZOS_PWD, true)
                } else {
                    addConnection(connectionName, "${CONNECTION_URL}1", ZOS_USERID, ZOS_PWD, true)
                }
                clickButton("OK")
                Thread.sleep(10000)
            }
            closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
            if (isValidConnection.not()) {
                errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
                    clickButton("Yes")
                }
                closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
            }
            clickButton("OK")
        }
        closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
}

/**
 * Deletes all jobs working sets, working sets and connections. To be used in BeforeAll and AfterAll tests methods.
 */
fun clearEnvironment(
    projectName: String,
    fixtureStack: MutableList<Locator>,
    closableFixtureCollector: ClosableFixtureCollector,
    remoteRobot: RemoteRobot
) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
        explorer {
            fileExplorer.click()
            settings(closableFixtureCollector, fixtureStack)
        }
        settingsDialog(fixtureStack) {
            configurableEditor {
                workingSetsTab.click()
                deleteAllItems()
                jobsWorkingSetsTab.click()
                deleteAllItems()
                conTab.click()
                deleteAllItems()
            }
            clickButton("OK")
        }
        closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
}

/**
 * Opens a project and an explorer, clears test environment before tests execution.
 */
fun setUpTestEnvironment(
    projectName: String,
    fixtureStack: MutableList<Locator>,
    closableFixtureCollector: ClosableFixtureCollector,
    remoteRobot: RemoteRobot
) = with(remoteRobot) {
    welcomeFrame {
        open(projectName)
    }
    Thread.sleep(10000)

    ideFrameImpl(projectName, fixtureStack) {
        try {
            if (dialog("For Mainframe Plugin Privacy Policy and Terms and Conditions").isShowing) {
                clickButton("I Agree")
            }
        } catch (e: WaitForConditionTimeoutException) {
            e.message.shouldContain("Failed to find 'Dialog' by 'title For Mainframe Plugin Privacy Policy and Terms and Conditions'")
            stripeButton(byXpath("//div[@accessiblename='Project' and @class='StripeButton' and @text='Project']"))
                .click()
        }
        forMainframe()
    }
    clearEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
}

/**
 * Opens a mask in the working set in explorer.
 */
fun openWSOpenMaskInExplorer(
    wsName: String, maskName: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) =
    with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
                Thread.sleep(3000)
                find<ComponentFixture>(viewTree).findText(maskName).doubleClick()
                Thread.sleep(20000)
                find<ComponentFixture>(viewTree).findAllText().shouldNotContain("Error")
                find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
            }
        }
    }

/**
 * Double-clicks on the working set to open or close it in explorer.
 */
fun openOrCloseWorkingSetInExplorer(
    wsName: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
        explorer {
            fileExplorer.click()
            find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
            Thread.sleep(3000)
        }
    }
}

/**
 * Double-clicks on the mask in explorer to open it and checks the message if required.
 */
fun openMaskInExplorer(
    maskName: String, expectedError: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) =
    with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(maskName).doubleClick()
                Thread.sleep(20000)
                if (expectedError.isEmpty().not()) {
                    find<ComponentFixture>(viewTree).findText(expectedError)
                } else {
                    find<ComponentFixture>(viewTree).findAllText().shouldNotContain("Error")
                }
            }
        }
    }

/**
 * Double-clicks on the job filter in explorer to open it and checks the message if required.
 */
fun openJobFilterInExplorer(
    filter: Triple<String, String, String>, expectedError: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) = with(remoteRobot) {
    val textToFind = if (filter.third == "") {
        "PREFIX=${filter.first} OWNER=${filter.second}"
    } else {
        "JobID=${filter.third}"
    }
    ideFrameImpl(projectName, fixtureStack) {
        explorer {
            jesExplorer.click()
            find<ComponentFixture>(viewTree).findText(textToFind).doubleClick()
            Thread.sleep(2000)
            waitFor(Duration.ofSeconds(20)) { find<ComponentFixture>(viewTree).hasText("loading…").not() }
            if (expectedError.isEmpty().not()) {
                find<ComponentFixture>(viewTree).findText(expectedError)
            } else {
                find<ComponentFixture>(viewTree).findAllText().shouldNotContain("Error")
            }
        }
    }
}

/**
 * Double-clicks on job filter to close it in explorer.
 */
fun closeFilterInExplorer(
    filter: Triple<String, String, String>, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) =
    with(remoteRobot) {
        val textToFind = if (filter.third == "") {
            "PREFIX=${filter.first} OWNER=${filter.second}"
        } else {
            "JobID=${filter.third}"
        }
        ideFrameImpl(projectName, fixtureStack) {
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
    maskName: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) =
    with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
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
    deletedItem: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
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
    deletedFilter: Triple<String, String, String>, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) = with(remoteRobot) {
    val textToFind = if (deletedFilter.third == "") {
        "PREFIX=${deletedFilter.first} OWNER=${deletedFilter.second}"
    } else {
        "JobID=${deletedFilter.third}"
    }
    ideFrameImpl(projectName, fixtureStack) {
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
    datasetName: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
        explorer {
            fileExplorer.click()
            find<ComponentFixture>(viewTree).findAllText(datasetName).last().rightClick()
        }
        actionMenuItem(remoteRobot, "Delete").click()
        dialog("Confirm Files Deletion") {
            clickButton("Yes")
        }
        Thread.sleep(10000)
    }
}

/**
 * Opens the file and copies it's content.
 */
fun openLocalFileAndCopyContent(
    filePath: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
        actionMenu(remoteRobot, "File").click()
        runJs(
            """
            const point = new java.awt.Point(${locationOnScreen.x}, ${locationOnScreen.y});
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
    jobName: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
        explorer {
            fileExplorer.click()
            find<ComponentFixture>(viewTree).findText(jobName).rightClick()
        }
        actionMenuItem(remoteRobot, "Submit Job").click()
    }
}

/**
 * Creates a member in the dataset and pastes content to the member.
 */
fun createMemberAndPasteContent(
    datasetName: String,
    memberName: String, projectName: String,
    fixtureStack: MutableList<Locator>,
    remoteRobot: RemoteRobot
) =
    with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
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
            Thread.sleep(10000)
            explorer {
                find<ComponentFixture>(viewTree).findAllText(memberName).last().doubleClick()
            }
            with(textEditor()) {
                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_V)
                    Thread.sleep(10000)
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_S)
                    Thread.sleep(10000)
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_F4)
                }
            }
        }
    }

/**
 * Allocates a dataset and creates a mask for it.
 * @param maskName when specified, mask will be created with the provided name, otherwise maskName will be equal to datasetName
 */
fun allocatePDSAndCreateMask(wsName: String,
                             datasetName: String,
                             projectName: String,
                             fixtureStack: MutableList<Locator>,
                             closableFixtureCollector: ClosableFixtureCollector,
                             remoteRobot: RemoteRobot,
                             maskName: String? = null,
                             directory: Int = 1) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            if (maskName != null){
                createMask(wsName, fixtureStack, closableFixtureCollector)
                createMaskDialog(fixtureStack){
                    createMask(Pair(maskName, "z/OS"))
                    clickButton("OK")
                    Thread.sleep(3000)
                }
            }

            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).rightClick()
            }
            actionMenu(remoteRobot, "New").click()
            actionMenuItem(remoteRobot, "Dataset").click()
            allocateDatasetDialog(fixtureStack) {
                allocateDataset(datasetName, "PO", "TRK", 10, 1, directory, "VB", 255, 6120)
                clickButton("OK")
                Thread.sleep(10000)
            }
            find<ContainerFixture>(byXpath("//div[@class='MyDialog']")).findText("Dataset $datasetName Has Been Created")

            if (maskName != null){
                clickButton("No")
                Thread.sleep(5000)
                /*explorer {
                    fileExplorer.click()
                    find<ComponentFixture>(viewTree).findText(wsName).rightClick()
                }
                actionMenu(remoteRobot, "Refresh").click()*/
            } else {
                clickButton("Yes")
                Thread.sleep(5000)
            }
        }
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    }

/**
 * Creates working set without masks.
 */
fun createWsWithoutMask(projectName: String,
                        wsName: String,
                        connectionName: String,
                        fixtureStack: MutableList<Locator>,
                        closableFixtureCollector: ClosableFixtureCollector,
                        remoteRobot: RemoteRobot) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
        createWSFromContextMenu(fixtureStack, closableFixtureCollector)
        addWorkingSetDialog(fixtureStack) {
            addWorkingSet(wsName, connectionName)
            clickButton("OK")
            Thread.sleep(3000)
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                EMPTY_DATASET_MESSAGE
            )
            clickButton("OK")
            Thread.sleep(3000)
        }
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
    }
}

/**
 * Allocates member for pds.
 */
fun allocateMemberForPDS(datasetName: String,
                         memberName: String,
                         projectName: String,
                         fixtureStack: MutableList<Locator>,
                         remoteRobot: RemoteRobot) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
        explorer {
            fileExplorer.click()
            find<ComponentFixture>(viewTree).findText(datasetName).rightClick()
        }

        actionMenu(remoteRobot, "New").click()
        actionMenuItem(remoteRobot, "Member").click()
        allocateMemberDialog(fixtureStack) {
            allocateMember(memberName)
            clickButton("OK")
            Thread.sleep(5000)
        }
    }
}