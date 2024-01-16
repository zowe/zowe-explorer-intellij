/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package workingset



import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.utils.keyboard
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import workingset.auxiliary.components.dialogs.*
import workingset.auxiliary.components.elements.ButtonElement
import java.awt.event.KeyEvent

/**
 * Tests allocating datasets with valid and invalid inputs.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
open class WorkingSetBase {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    open val connectionName = "valid connection"

    var okButton: ButtonElement = ButtonElement()
    var yesButton: ButtonElement = ButtonElement()
    var cancelButton: ButtonElement = ButtonElement()

    open val wsName = "WS1"
    internal open val datasetName = "$ZOS_USERID.ALLOC."

    var editWorkingSetSubDialog: EditWorkingSetSubDialog = EditWorkingSetSubDialog()
    var createMaskSubDialog: CreateMaskSubDialog = CreateMaskSubDialog()
    var renameDatasetMaskDialog: RenameDatasetMaskDialog = RenameDatasetMaskDialog()
    var deletionOfDSMask: DeletionOfDSMask = DeletionOfDSMask()
    var deletionOfUssPathRoot: DeletionOfUssPathRoot = DeletionOfUssPathRoot()
    var addWorkingSetDialog = AddWorkingSetSubDialog()

    internal fun buildDatasetConfigString(
        dsName: String,
        dsntp: String,
        datasetOrganization: String,
        recordLength: Int,
        recordFormatShort: String
    ): String {
        return "{\n" +
                "      \"dsname\": \"${dsName}\",\n" +
                "      \"blksz\": \"3200\",\n" +
                "      \"catnm\": \"TEST.CATALOG.MASTER\",\n" +
                "      \"cdate\": \"2021/11/15\",\n" +
                "      \"dev\": \"3390\",\n" +
                "      \"dsntp\": \"${dsntp}\",\n" +
                "      \"dsorg\": \"${datasetOrganization}\",\n" +
                "      \"edate\": \"***None***\",\n" +
                "      \"extx\": \"1\",\n" +
                "      \"lrecl\": \"${recordLength}\",\n" +
                "      \"migr\": \"NO\",\n" +
                "      \"mvol\": \"N\",\n" +
                "      \"ovf\": \"NO\",\n" +
                "      \"rdate\": \"2021/11/17\",\n" +
                "      \"recfm\": \"${recordFormatShort}\",\n" +
                "      \"sizex\": \"10\",\n" +
                "      \"spacu\": \"TRACKS\",\n" +
                "      \"used\": \"1\",\n" +
                "      \"vol\": \"TESTVOL\",\n" +
                "      \"vols\": \"TESTVOL\"\n" +
                "    },"
    }

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

    internal fun allocateDataSet(
        wsName: String,
        datasetName: String,
        datasetOrganization: String,
        allocationUnit: String,
        primaryAllocation: Int,
        secondaryAllocation: Int,
        directory: Int,
        recordFormat: String,
        recordLength: Int,
        blockSize: Int,
        averageBlockLength: Int,
        remoteRobot: RemoteRobot,
    ) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).rightClick()
            }
            actionMenu(remoteRobot, NEW_POINT_TEXT).click()
            actionMenuItem(remoteRobot, DATASET_POINT_TEXT).click()
            allocateDatasetDialog(fixtureStack) {
                allocateDataset(
                    datasetName,
                    datasetOrganization,
                    allocationUnit,
                    primaryAllocation,
                    secondaryAllocation,
                    directory,
                    recordFormat,
                    recordLength,
                    blockSize,
                    averageBlockLength
                )
                clickByText(OK_TEXT, fixtureStack, remoteRobot)

                closableFixtureCollector.closeOnceIfExists(AllocateDatasetDialog.name)

            }
//            Thread.sleep(3000)
//            find<ContainerFixture>(myDialogXpathLoc).findText("Dataset $datasetName Has Been ")
//            clickButton("No")

        }
    }

    /**
     * Creates working set and z/OS mask.
     */
    internal fun createWsAndMask(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createWsWithoutMask(wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createMask(wsName, fixtureStack, closableFixtureCollector)
            createMaskDialog(fixtureStack) {
                createMask(Pair("$datasetName*", ZOS_MASK))
                clickByText(OK_TEXT, fixtureStack, remoteRobot)
            }
            closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        }
    }

    internal fun buildFinalListDatasetJson(mapListDatasets: MutableMap<String, String>): String {
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

    private fun openDatasetProperty(datasetName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(datasetName).rightClick()
            }
        }
    }

    internal fun migrateDataset(
        datasetName: String,
        migratedDs: String,
        mapListDatasets: MutableMap<String, String>,
        fixtureStack: MutableList<Locator>,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        openDatasetProperty(datasetName, remoteRobot)
        responseDispatcher.injectMigratePdsRestFiles(datasetName, HMIGRATE_MIGRATE_OPTIONS)
        mapListDatasets[datasetName] = migratedDs
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            actionMenuItem(remoteRobot, MIGRATE_POINT_TEXT).click()
        }
    }

    internal fun recallDataset(
        datasetName: String,
        fixtureStack: MutableList<Locator>,
        mapListDatasets: MutableMap<String, String>,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        openDatasetProperty(datasetName, remoteRobot)
        responseDispatcher.injectRecallPds(datasetName)
        mapListDatasets[datasetName] = listDS(datasetName, PDS_TYPE, PO_ORG_SHORT)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            actionMenuItem(remoteRobot, RECALL_POINT_TEXT).click()
        }
    }

    internal fun callRenameDatasetPoint(
        fixtureStack: MutableList<Locator>,
        datasetName: String,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                Thread.sleep(3000)
                find<ComponentFixture>(viewTree).findText(datasetName).rightClick()
            }
            actionMenuItem(remoteRobot, RENAME_POINT_TEXT).click()
        }
    }

    internal fun callRenameMemberPoint(
        fixtureStack: MutableList<Locator>,
        datasetName: String,
        memberName: String,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(datasetName).doubleClick()
                find<ComponentFixture>(viewTree).findText(memberName).rightClick()
            }
            actionMenuItem(remoteRobot, RENAME_POINT_TEXT).click()
        }
    }

    internal fun callEditWSFromContextMenu(
        datasetName: String,
        fixtureStack: MutableList<Locator>,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(datasetName).doubleClick()
                find<ComponentFixture>(viewTree).findText(datasetName).rightClick()
            }
            actionMenuItem(remoteRobot, EDIT_POINT_TEXT).click()
        }
    }

    internal fun callTreesElementProperty(
        treesElement: String,
        fixtureStack: MutableList<Locator>,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(treesElement).rightClick()
            }
            actionMenuItem(remoteRobot, PROPERTIES_POINT_TEXT).click()
        }
    }

    internal fun newMemberNameInput(newName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                dialog(RENAME_MEMBER_NAME) {
                    find<JTextFieldFixture>(inputFieldLoc).text = newName
                }
            }
        }

    internal fun newDatasetNameInput(newName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                dialog(RENAME_DATASET_NAME) {
                    find<JTextFieldFixture>(inputFieldLoc).text = newName
                }
            }
        }

    /**
     * Checks if File Explorer contains expected message.
     */
    internal fun findMessageInExplorer(msg: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        if (!find<ComponentFixture>(viewTree).findAllText().map(RemoteText::text).joinToString("")
                .contains(msg)
        ) {
            throw Exception("Expected message is not found")
        }
    }

    internal fun closeIntelligentProject(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                close()
            }
        }

    internal fun refreshWorkSpace(wsName:String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack)
        {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).rightClick()
            }
            actionMenuItem(remoteRobot, REFRESH_POINT_TEXT).click()
        }
    }

    internal fun compressAndDecompressTree(wsName:String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                explorer {
                    find<ComponentFixture>(viewTree).findAllText(wsName).last().doubleClick()
                }
            }
        }

    /**
     * Closes opened dataset member or PS dataset.
     */
    internal fun closeMemberOrDataset(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            with(textEditor()) {
                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_F4)
                }
            }
        }
    }

    /**
     * Open property
     */
    internal fun openPropertyDatasetName(dsName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                explorer {
                    fileExplorer.click()
                    Thread.sleep(1000)
                    find<ComponentFixture>(viewTree).findText(dsName).rightClick()
                }
                actionMenuItem(remoteRobot, PROPERTIES_POINT_TEXT).click()
            }
        }

    fun openTreesElement(treesElement: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                find<ComponentFixture>(viewTree).findAllText(treesElement).last().doubleClick()
            }
        }
    }

    fun callCreateWorkingSetFromActionButton(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                callCreateWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
            }
        }

    fun callCreateWSFromContextMenu(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                createWSFromContextMenu(fixtureStack, closableFixtureCollector)
            }
        }

//    fun createWsWithConnection(connectionName: String, wsName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) =
//        with(remoteRobot) {
//            callCreateWSFromContextMenu(fixtureStack, remoteRobot)
//            addWorkingSetDialog.fillAddWorkingSet(connectionName, wsName, fixtureStack, remoteRobot)
//            okButton.click()
//        }
//    fun createWsWithConnection(connectionName: String, wsName: String, mask: Pair<String, String>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) =
//        with(remoteRobot) {
//            callCreateWSFromContextMenu(fixtureStack, remoteRobot)
//            addWorkingSetDialog.fillAddWorkingSet(connectionName, wsName, mask, fixtureStack, remoteRobot)
//            okButton.click()
//        }
//    fun callCreateConnectionFromActionButton(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) =
//        with(remoteRobot) {
//            ideFrameImpl(PROJECT_NAME, fixtureStack) {
//                createConnectionFromActionButton(closableFixtureCollector, fixtureStack)
//            }
//        }

    fun createWsWithConnectionFromAction(
        connectionName: String,
        wsName: String,
        fixtureStack: MutableList<Locator>,
        closableFixtureCollector: ClosableFixtureCollector,
        remoteRobot: RemoteRobot,
    ) {
        callCreateWorkingSetFromActionButton(fixtureStack, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, wsName, fixtureStack, remoteRobot)
        return addWorkingSetDialog.clickButtonByName(OK_TEXT)
    }

    fun createWsWithConnectionFromAction(connectionName: String, wsName: String, mask: Pair<String, String>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) {
        callCreateWorkingSetFromActionButton(fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, wsName, mask, fixtureStack, remoteRobot)
        return addWorkingSetDialog.clickButtonByName(OK_TEXT)
    }

    fun fillConnectionFilds(
        connectionName: String = PROJECT_NAME, hostName: String = mockServer.hostName, port: Int = mockServer.port,
        userId: String = ZOS_USERID, userPwd: String=ZOS_PWD, ssl: Boolean=true, protocol: String = "https",
        fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                addConnectionDialog(fixtureStack)
                {
                    addConnection(connectionName, "${protocol}://${hostName}:${port}", userId, userPwd, ssl)
                }
            }
        }




    fun deleteInEditWorkingSet(masks: List<String>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editWorkingSetDialog(fixtureStack) {
                deleteMasks(masks)
            }
        }
    }

    fun changeConnectionInEditWorkingSet(newConnectionName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editWorkingSetDialog(fixtureStack) {
                changeConnection(newConnectionName)
            }
        }
    }



    fun fillEditWorkingSet(connectionName:String, wsName:String, mask: Pair<String, String>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName, mask)
            }
        }
    }


    fun isButtonEnableByTextAddWorkingSet(buttonText:String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot): Boolean = with(remoteRobot) {
        var status = false
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            addWorkingSetDialog(fixtureStack) {
                status = button(buttonText).isEnabled()
            }
        }
        return status
    }


    fun hoverToByTextAddWorkingSet(text:String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            addWorkingSetDialog(fixtureStack) {
                findText(text).moveMouse()
            }
        }
    }

    fun clickToByTextAddWorkingSet(text:String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            addWorkingSetDialog(fixtureStack) {
                findText(text).click()
            }
        }
    }

    fun clickActionButtonByXpath(xpath: Locator, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            addWorkingSetDialog(fixtureStack) {
                clickActionButton(xpath)
            }
        }
    }

    fun setInComboBox(newConnectionName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editWorkingSetDialog(fixtureStack) {
                changeConnection(newConnectionName)
            }
        }
    }

    fun deleteWSFromContextMenu(wsName:String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            deleteWSFromContextMenu(wsName)
        }
        yesButton.click()
    }

    fun callCreateMask(wsName:String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                createMask(wsName, fixtureStack, closableFixtureCollector)
        }
    }

    fun decompressWsIfCompressed(wsName1: String, compressedId: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        try {
            find<HeavyWeightWindowFixture>(treesLoc).findText(compressedId)
        }catch(_: NoSuchElementException){
            compressAndDecompressTree(wsName1, fixtureStack, remoteRobot)
        }
    }

    fun compressWsIfcDecompressed(wsName1: String, compressedId: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        try {
            find<HeavyWeightWindowFixture>(treesLoc).findText(compressedId)
            compressAndDecompressTree(wsName1, fixtureStack, remoteRobot)
        }catch(_: NoSuchElementException){}
    }
}











