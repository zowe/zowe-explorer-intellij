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
package workingset



import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText
import com.intellij.remoterobot.launcher.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.stepsProcessing.StepLogger
import com.intellij.remoterobot.stepsProcessing.StepWorker
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import ideLauncher.IdeDownloader
import ideLauncher.IdeLauncher
import io.kotest.matchers.collections.shouldNotContain
import okhttp3.OkHttpClient
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher
import workingset.Constants.ideLaunchFolder
import workingset.Constants.ideaBuildVersionForTest
import workingset.Constants.ideaVersionForTest
import workingset.Constants.robotServerForTest
import workingset.auxiliary.components.dialogs.*
import workingset.auxiliary.components.elements.ButtonElement
import java.awt.event.KeyEvent
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

/**
 * Tests allocating datasets with valid and invalid inputs.
 */
@ExtendWith(IdeaInteractionClass.IdeTestWatcher::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
open class IdeaInteractionClass {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    open val connectionName = "valid connection"

    lateinit var okButton: ButtonElement
    lateinit var  yesButton: ButtonElement
    lateinit var  noButton: ButtonElement
    lateinit var  cancelButton: ButtonElement


    open val wsName = "WS1"
    internal open val datasetName = "$ZOS_USERID.ALLOC."

    var editWorkingSetSubDialog: EditWorkingSetSubDialog = EditWorkingSetSubDialog()
    var createMaskSubDialog: CreateMaskSubDialog = CreateMaskSubDialog()
    var renameDatasetMaskDialog: RenameDatasetMaskDialog = RenameDatasetMaskDialog()
    var deletionOfDSMask: DeletionOfDSMask = DeletionOfDSMask()
    var deletionOfUssPathRoot: DeletionOfUssPathRoot = DeletionOfUssPathRoot()
    var addWorkingSetDialog = AddWorkingSetSubDialog()
    var createFileDialog = CreateFileDialog()
    var createDirectoryDialog = CreateDirectoryDialog()

    private lateinit var tmpDir: Path
    private val forMainframeZipPath: Path = Paths.get("").toAbsolutePath().resolve(Paths.get(Constants.forMainframePath))

    companion object {
        lateinit var ideaProcess: Process
        lateinit var remoteRobot: RemoteRobot
    }
    class IdeTestWatcher : TestWatcher {
        override fun testFailed(context: ExtensionContext, cause: Throwable?) {
            ImageIO.write(remoteRobot.getScreenshot(), "png", File("build/reports", "${context.displayName}.png"))
        }
    }
    fun startIdea() {
        StepWorker.registerProcessor(StepLogger())
        tmpDir = Paths.get("").toAbsolutePath().resolve(Paths.get(ideLaunchFolder))
        val client = OkHttpClient()
        remoteRobot = RemoteRobot(CONNECTION_URL_UI, client)
        val ideDownloader = IdeDownloader(client)

        val idePath: Path = if (Files.exists(tmpDir)) {
            tmpDir.resolve(ideaBuildVersionForTest)
        } else {
            Files.createDirectory(tmpDir)
            ideDownloader.downloadRobotPlugin(tmpDir)
            ideDownloader.downloadAndExtractLatestEap(Ide.IDEA_COMMUNITY, tmpDir, ideaVersionForTest)

        }

        val robotExtensionPath = tmpDir.resolve(robotServerForTest)
        ideaProcess = IdeLauncher.launchIde(
            idePath,
            mapOf("robot-server.port" to REMOTE_PORT),
            emptyList(),
            listOf(robotExtensionPath, forMainframeZipPath),
            tmpDir
        )

        waitFor(Duration.ofSeconds(120), Duration.ofSeconds(5)) {
            remoteRobot.isAvailable()
        }
    }

    fun cleanUp() {
        ideaProcess.destroy()
        if (!ideaProcess.waitFor(10, TimeUnit.SECONDS)) {
            println("Forcibly destroying process")
            ideaProcess.destroyForcibly()
        }
    }

    private fun RemoteRobot.isAvailable(): Boolean = runCatching {
        callJs<Boolean>("true")
    }.getOrDefault(false)

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
        }
    }

    private fun clickByXpath(locator: Locator, remoteRobot:RemoteRobot, waitTime: Long = 60) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            val button = button(locator)
            waitFor(Duration.ofSeconds(waitTime)) {
                button.isEnabled()
            }
            button.click()
        }

    }

    /**
     * Creates working set and z/OS mask.
     */
    internal fun createWsAndMask(remoteRobot:RemoteRobot) = with(remoteRobot) {
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


    /**
     * Creates working set and z/OS mask.
     */
    internal fun createWsAndMask(wsName:String, connectionName:String, fixtureStack: MutableList<Locator>, closableFixtureCollector: ClosableFixtureCollector, remoteRobot:RemoteRobot, mask:Pair<String,String>?=null) = with(remoteRobot) {
        createWsWithoutMask(wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createMask(wsName, fixtureStack, closableFixtureCollector)
            createMaskDialog(fixtureStack) {
                if(mask == null){
                    createMask(Pair("$datasetName*", ZOS_MASK))
                }else{
                    createMask(mask)
                }

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

    private fun callNewPoint(
        mask: String,
        fixtureStack: MutableList<Locator>,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                find<ComponentFixture>(viewTree).findText(mask).rightClick()
            }
            actionMenu(remoteRobot, NEW_POINT_TEXT).click()
        }
    }

    private fun callSubMenuPoint(
        subMenuPoint: String,
        fixtureStack: MutableList<Locator>,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            actionMenuItem(remoteRobot, subMenuPoint).click()
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
        remoteRobot: RemoteRobot,
        isFullName: Boolean = true
    ) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                if (isFullName){
                find<ComponentFixture>(viewTree).findText(treesElement).rightClick()
                }
                else{
                    find<ComponentFixture>(viewTree).findAllText{ it.text.startsWith(treesElement)}.first().rightClick()
                }
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

//    internal fun closeIntelligentProject(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) =
//        with(remoteRobot) {
//            ideFrameImpl(PROJECT_NAME, fixtureStack) {
//                close()
//            }
//        }

    internal fun refreshWorkSpace(wsName:String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack)
        {
            explorer {

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
    fun callCreateJesWorkingSetFromActionButton(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                createJesWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
            }
        }

    /**
     * Creates a JES working set via context menu from explorer.
     */
    fun callCreateJWSFromContextMenu(
        fixtureStack: MutableList<Locator>,
        closableFixtureCollector: ClosableFixtureCollector,
        remoteRobot: RemoteRobot
    )=with(remoteRobot)  {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            ButtonElement(jesExplorerTabNameLoc, fixtureStack, remoteRobot).click()
            find<ComponentFixture>(explorerLoc).rightClick()

            actionMenu(remoteRobot, NEW_POINT_TEXT).click()
            actionMenuItem(remoteRobot, JES_WORKING_SET_POINT).click()
            closableFixtureCollector.add(addJesWorkingSetDialogLoc, fixtureStack)
        }
    }

    fun callCreateJobsFilterFromContextMenu(
        jwsName: String, fixtureStack: MutableList<Locator>,
        closableFixtureCollector: ClosableFixtureCollector,
        remoteRobot: RemoteRobot
    )=with(remoteRobot)  {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                jesExplorer.click()
                find<ComponentFixture>(viewTree).findText(jwsName).rightClick()
            }
            actionMenu(remoteRobot, NEW_POINT_TEXT).click()
            actionMenuItem(remoteRobot, JOBS_FILTER).click()
            closableFixtureCollector.add(CreateJobsFilterDialog.xPath(), fixtureStack)
        }
    }

    fun callEditJWSFromContextMenu(
        jwsName: String, fixtureStack: MutableList<Locator>,
        closableFixtureCollector: ClosableFixtureCollector,remoteRobot:RemoteRobot
    )=with(remoteRobot)  {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                jesExplorer.click()
                find<ComponentFixture>(viewTree).findText(jwsName).rightClick()
            }
            actionMenuItem(remoteRobot, EDIT_POINT_TEXT).click()
            closableFixtureCollector.add(editJesWorkingSetDialogLoc, fixtureStack)
        }
    }

    fun callEditJobFilter(
        oldFilter: Triple<String, String, String>,
        fixtureStack: MutableList<Locator>,
        closableFixtureCollector: ClosableFixtureCollector,
        remoteRobot: RemoteRobot,
    ) = with(remoteRobot) {

        val textToFind = convertJobFilterToString(oldFilter)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                jesExplorer.click()
                find<ComponentFixture>(viewTree).findText(textToFind).rightClick()
            }
            actionMenuItem(remoteRobot, EDIT_POINT_TEXT).click()
            closableFixtureCollector.add(editJobFilterDialogLoc, fixtureStack)
        }
    }


    fun callCreateWSFromContextMenu(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                createWSFromContextMenu(fixtureStack, closableFixtureCollector)
            }
        }

    fun openJesExplorerTab(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                jesExplorer.click()
            }
        }
    }

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
        addWorkingSetDialog.clickButtonByName(OK_TEXT)
        return addWorkingSetDialog.clickButtonByName(OK_TEXT)
    }

    fun createWsWithConnectionFromAction(connectionName: String, wsName: String, mask: List<String>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) {
        callCreateWorkingSetFromActionButton(fixtureStack, remoteRobot)
        mask.forEach{
            addWorkingSetDialog.setMaskPair(Pair(it, ZOS_MASK), fixtureStack, remoteRobot)

        }
        addWorkingSetDialog.setWsName(wsName,fixtureStack,remoteRobot)
        addWorkingSetDialog.setConnectionName(connectionName,fixtureStack, remoteRobot)
        addWorkingSetDialog.clickButtonByName(OK_TEXT)
    }

    fun createWsWithConnectionFromAction(connectionName: String, wsName: String, mask: Pair<String, String>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) {
        callCreateWorkingSetFromActionButton(fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, wsName, mask, fixtureStack, remoteRobot)
        addWorkingSetDialog.clickButtonByName(OK_TEXT)
        addWorkingSetDialog.clickButtonByName(OK_TEXT)
    }

    fun fillConnectionFields(
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

    fun deleteJobFromContextMenu(wsName:String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            deleteJobFromContextMenu(wsName)
        }
    }

    fun deleteJwsFromContextMenu(wsName:String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            deleteJWSFromContextMenu(wsName)
        }
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

    fun createUssFileForMask(
        ussMask: String,ussFileName: String, remoteRobot: RemoteRobot, ownerPermissions: String=RWE_TYPES_PERMISSION,
        groupPermissions: String=R_PERMISSION, allPermissions:String=RW_TYPES_PERMISSION
    ){
        callNewPoint(ussMask, fixtureStack, remoteRobot)
        callSubMenuPoint(FILE_POINT_TEXT, fixtureStack, remoteRobot)
        createFileDialog.dialogTitle = createFileDialog.dialogTitle.format(workingset.ussMask)
        createFileDialog.setValues(ussFileName, ownerPermissions,groupPermissions, allPermissions)
        createFileDialog.clickButtonByName(OK_TEXT)
    }
    fun createUssDirForMask(
        ussMask: String,ussFileName: String, remoteRobot: RemoteRobot, ownerPermissions: String=RWE_TYPES_PERMISSION,
        groupPermissions: String=R_PERMISSION, allPermissions:String=RW_TYPES_PERMISSION
    ){
        callNewPoint(ussMask, fixtureStack, remoteRobot)
        callSubMenuPoint(DIRECTORY_POINT_TEXT, fixtureStack, remoteRobot)
        createDirectoryDialog.dialogTitle = createDirectoryDialog.dialogTitle.format(workingset.ussMask)
        createDirectoryDialog.setValues(ussFileName, ownerPermissions,groupPermissions, allPermissions)
        createDirectoryDialog.clickButtonByName(OK_TEXT)
    }

    fun callSettingsByAction(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            clickByXpath(callSettingButtonLoc,remoteRobot)
        }
    }

    /**
     * Double-clicks on the job filter in explorer to open it and checks the message if required.
     */
    fun checkJesErrorInTrees(
        filter: Triple<String, String, String>, expectedError: String,
        fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
    ) = with(remoteRobot) {
        val textToFind = if (filter.third == "") {
            prefixAndOwnerPattern.format(filter.first, filter.second)
        } else {
            jobIdPattern.format(filter.third)
        }
        ButtonElement(jesExplorerTabNameLoc, fixtureStack, remoteRobot).click()
        find<ComponentFixture>(viewTree).findText(textToFind).doubleClick()
        waitFor(Duration.ofSeconds(20)){ find<ComponentFixture>(viewTree).hasText(LOADING_TEXT).not() }
        if (expectedError.isEmpty().not()) {
            find<ComponentFixture>(viewTree).findText(expectedError)
        } else {
            find<ComponentFixture>(viewTree).findAllText().shouldNotContain(ERROR_TEXT)
        }
    }

    fun closeFilterInExplorer(
        filter: Triple<String, String, String>,
        fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
    ) =
        with(remoteRobot) {
            val textToFind = if (filter.third == "") {
                prefixAndOwnerPattern.format(filter.first, filter.second)
            } else {
                jobIdPattern.format(filter.third)
            }
            ButtonElement(explorerLoc, fixtureStack, remoteRobot).click()
            find<ComponentFixture>(viewTree).findText(textToFind).doubleClick()
//            ideFrameImpl(PROJECT_NAME, fixtureStack) {
//                explorer {
//                    jesExplorer.click()
//                    find<ComponentFixture>(viewTree).findText(textToFind).doubleClick()
//                }
//            }
        }

    /**
     * Double-clicks on the job filter in explorer to open it and checks the message if required.
     */
    fun openJobFilterInExplorer(
        filter: Triple<String, String, String>, expectedError: String,
        fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot,
    ) = with(remoteRobot) {
        val textToFind = if (filter.third == "") {
            prefixAndOwnerPattern.format(filter.first, filter.second)
        } else {
            jobIdPattern.format(filter.third)
        }

        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
//                jesExplorer.click()
                waitFor(Duration.ofSeconds(20)) {find<ComponentFixture>(viewTree).hasText(textToFind)}
                find<ComponentFixture>(viewTree).findText(textToFind).doubleClick()
                waitFor(Duration.ofSeconds(20)) { find<ComponentFixture>(viewTree).hasText(LOADING_TEXT).not() }
                if (expectedError.isEmpty().not()) {
                    find<ComponentFixture>(viewTree).findText(expectedError)
                } else {
                    find<ComponentFixture>(viewTree).findAllText().shouldNotContain(ERROR_TEXT)
                }
            }
        }
    }
    fun doSelectAll(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot){
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
                }
            }
    }

    private fun clickDeletePoint(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            actionMenuItem(remoteRobot, DELETE_TEXT).click()
        }
    }

    fun removeAllJwsWorkingSets(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot, maxDatasets:Int = 20) = with(remoteRobot){
        if (!find<ComponentFixture>(viewTree).hasText(NOTHING_TO_SHOW_MSG)){

            find<ComponentFixture>(viewTree).click()
            doSelectAll(fixtureStack, remoteRobot)
            find<ComponentFixture>(viewTree).rightClick()
            clickDeletePoint(fixtureStack, remoteRobot)
            for (i in 1..maxDatasets) {
                ButtonElement(YES_TEXT,fixtureStack, remoteRobot).click()
                try {
                    if (ButtonElement(YES_TEXT,fixtureStack, remoteRobot).isEnabled())
                    {continue}
                }catch (exception: WaitForConditionTimeoutException)
                { break }
            }
        }
    }

    /**
     * Creates JES working set.
     */
    fun createJWS(jwsName:String, connectionName: String, filters: List<Triple<String,String,String>>,fixtureStack: MutableList<Locator>,closableFixtureCollector: ClosableFixtureCollector, remoteRobot: RemoteRobot) = with(remoteRobot) {
        callCreateJWSFromContextMenu(fixtureStack,closableFixtureCollector,remoteRobot)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            addJesWorkingSetDialog(fixtureStack) {
                addJesWorkingSet(jwsName, connectionName, ZOS_USERID, filters)
                clickButton(OK_TEXT)
            }

        }

        closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
    }

    /**
     * Creates JES working set.
     */
    fun createJWS(jwsName:String, connectionName: String, filters: Triple<String,String,String>,fixtureStack: MutableList<Locator>,closableFixtureCollector: ClosableFixtureCollector, remoteRobot: RemoteRobot) = with(remoteRobot) {
        callCreateJWSFromContextMenu(fixtureStack,closableFixtureCollector,remoteRobot)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            addJesWorkingSetDialog(fixtureStack) {
                addJesWorkingSet(jwsName, connectionName, ZOS_USERID, filters)
                clickButton(OK_TEXT)
            }

        }

        closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
    }

    /**
     * Checks and closes notification after performed action on running job.
     */
    fun checkNotificationAfterJobAction(
        action: JobAction,
        jobId: String,
        jobName: String,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        val textToFind = when (action) {
            JobAction.CANCEL -> jobCancelNotification.format(jobName, jobId)
            JobAction.HOLD -> jobHoldNotification.format(jobName, jobId)
            JobAction.RELEASE -> jobReleseNotification.format(jobName, jobId)
            JobAction.SUBMIT -> jobSubmitNotification.format(jobName)
            JobAction.PURGED -> jobPurgeNotification.format(jobName, jobId)
            JobAction.ERROR_PURGED -> jobErrorPurgeNotification.format(jobName, jobId)
        }

        waitFor(Duration.ofSeconds(30)) {
            findAll<JLabelFixture>(notificationTitle).last().hasText(textToFind)
        }

        ButtonElement(closeNotificationLoc,fixtureStack,remoteRobot).click()
    }


    /**
     * Closes tab for job in jobs panel.
     */
    fun closeTabInJobsPanel(datasetName: String,jobName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        find<ComponentFixture>(jobConsoleHeaderLoc)
            .findText(jobsPanelTabName.format(datasetName,jobName)).click()
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            keyboard {
                hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F4)
            }

        }
    }

    fun callPurgeJob(jobName: String,jobId: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                find<ComponentFixture>(viewTree).findAllText { it.text.startsWith("$jobName ($jobId)") }.first()
                    .rightClick()
            }
            actionMenuItem(remoteRobot, PURGE_JOB_POINT).click()
        }
    }

//    fun callSeveralEscape(remoteRobot:RemoteRobot, repeat:Int =3)=with(remoteRobot)  {
//        ideFrameImpl(PROJECT_NAME, fixtureStack) {
//            keyboard {
//                for (i in 1..repeat) {
//                    hotKey(KeyEvent.VK_ESCAPE)
//                }
//            }
//        }
//    }

}
