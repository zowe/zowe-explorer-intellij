package workingset

/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Tests allocating datasets with valid and invalid inputs.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
open class WorkingSetBase {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var mapListDatasets = mutableMapOf<String, String>()
    private val projectName = "untitled"
    open val connectionName = "valid connection"

    //
    private val wsName = "WS1"
    internal open val datasetName = "$ZOS_USERID.ALLOC."

    internal fun buildDatasetConfigString(dsName: String, dsntp: String, datasetOrganization:String, recordLength:Int, recordFormatShort:String): String{
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

    internal fun allocateDataSetWithDefaultConfig(
            wsName: String,
            datasetName: String,
            projectName: String,
            fixtureStack: MutableList<Locator>,
            remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).rightClick()
            }
            actionMenu(remoteRobot, NEW_POINT_TEXT).click()
            actionMenuItem(remoteRobot, DATASET_POINT_TEXT).click()
            allocateDatasetDialog(fixtureStack) {
                allocateDataset(datasetName, PARTITIONED_EXTENDED_NAME_FULL, "TRK", 10, 1, 0, "VB", 255, 6120)
                clickButton(OK_TEXT)
//      Thread.sleep(10000)
            }
            find<ContainerFixture>(myDialogXpathLoc).findText(datasetHasBeenCreated.format(datasetName))
            clickButton(NO_TEXT)
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).rightClick()
            }
            actionMenuItem(remoteRobot, REFRESH_POINT_TEXT).click()
//            Thread.sleep(3000)
        }
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
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).rightClick()
            }
            actionMenu(remoteRobot, "New").click()
            actionMenuItem(remoteRobot, "Dataset").click()
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
                clickButton("OK")
                find<ContainerFixture>(myDialogXpathLoc).findText("Dataset $datasetName Has Been ")
                clickButton("No")
//                closableFixtureCollector.closeOnceIfExists(AllocateDatasetDialog.name)

            }

        }
    }

    /**
     * Creates working set and z/OS mask.
     */
    internal fun createWsAndMask(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createWsWithoutMask(projectName, wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            createMask(wsName, fixtureStack, closableFixtureCollector)
            createMaskDialog(fixtureStack) {
                createMask(Pair("$datasetName*", "z/OS"))
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        }
    }

    internal fun buildFinalListDatasetJson(): String {
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
}