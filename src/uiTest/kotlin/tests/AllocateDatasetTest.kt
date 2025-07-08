/*
 * Copyright (c) 2024 IBA Group.
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
 *   Uladzislau Kalesnikau
 */

package tests

import io.kotest.core.annotation.Description
import com.intellij.driver.client.Driver
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import tests.utils.*
import tests.utils.uidefinitions.dialogs.AddConnectionDialog
import tests.utils.uidefinitions.dialogs.AllocateDatasetDialog
import tests.utils.notification.AddWorkingSetSuccessNotification
import tests.utils.uidefinitions.dialogs.UnsecureConnectionDialog
import tests.utils.uidefinitions.ActionMenuPoints
import tests.utils.uidefinitions.FilesExplorerPanel
import tests.utils.uidefinitions.dialogs.AddWorkingSetDialog
import java.util.stream.Stream

private const val dsTemplate =
  "{" +
  "\"dsname\": \"###dsname###\"," +
  "\"blksz\": \"###blksz###\"," +
  "\"catnm\": \"CATALOG.Z23D.MASTER\"," +
  "\"cdate\": \"2023/08/28\"," +
  "\"dev\": \"3390\"," +
  "\"dsorg\": \"###dsorg###\"," +
  "\"edate\": \"***None***\"," +
  "\"extx\": \"1\"," +
  "\"lrecl\": \"###lrecl###\"," +
  "\"migr\": \"NO\"," +
  "\"mvol\": \"N\"," +
  "\"ovf\": \"NO\"," +
  "\"rdate\": \"2024/10/09\"," +
  "\"recfm\": \"###recfm###\"," +
  "\"sizex\": \"6\"," +
  "\"spacu\": \"###spacu###\"," +
  "\"used\": \"16\"," +
  "\"vol\": \"D3SYS1\"," +
  "\"vols\": \"D3SYS1\"" +
  "}"

@Description("Tests to check the functionality associated with dataset allocation")
class AllocateDatasetTest {
  lateinit var addConnectionDialog: AddConnectionDialog
  private lateinit var allocateDatasetDialog: AllocateDatasetDialog
  private lateinit var addWsNotification: AddWorkingSetSuccessNotification

  companion object {
    const val connectionName = "CON00001"
    const val wsName = "WS000001"
    private val tstHlq = "$UI_TEST_USERNAME.UI.REGRESS.TEST"
    private val allocMask = Pair("$tstHlq.*", "z/OS")
    private val createdDatasets = mutableListOf<String>()

    private lateinit var ideDriver: Driver
    private lateinit var filesExplorerPanel: FilesExplorerPanel

    @JvmStatic
    fun provideOrgTypes(): Stream<Arguments> {
      val dsNameToDsOrg = listOf(DsOrg.PS, DsOrg.PO, DsOrg.POE)

      return RecFM
        .entries
        .filter { recordFormat -> recordFormat != RecFM.U }
        .flatMap { recordFormat ->
          dsNameToDsOrg.map { dsOrg ->
            val dirBlock = if (dsOrg != DsOrg.PS) "1" else null
            val lrecl = if (recordFormat == RecFM.F) "3200" else "80"
            val allocateDatasetParams = AllocateDatasetParams(
              name = "${tstHlq}.${calcDsHlqLastElem(dsOrg.value.short)}",
              preset = "Custom Dataset",
              dsOrg = dsOrg,
              primAlloc = "10",
              secAlloc = "1",
              blksz = "3200",
              avgBlkLen = "0",
              unit = AllocUnit.TRK,
              recfm = recordFormat,
              dirBlock = dirBlock,
              lrecl = lrecl
            )
            Arguments.of(allocateDatasetParams)
          }
        }
        .stream()
    }

    @JvmStatic
    fun getInvalidDatasetConfigs(): Stream<Arguments> {
      return invalidAllocateScenarios.stream().map{
        Arguments.of(it.first, it.second)
      }
    }

    @JvmStatic
    fun getInvalidDatasetConfigsForServerValidation(): Stream<Arguments> {
      return invalidAllocateScenariosOnServer.stream().map{
        Arguments.of(it.first, it.second)
      }
    }

    @JvmStatic
    @BeforeAll
    fun prepareBeforeAll(testInfo: TestInfo) {
      IdeRunManager.prepareRunManager()
        .runningIde
        .resetTestEnv()

      ideDriver = IdeRunManager.getIdeDriver()

      openZoweExplorerPanel(ideDriver)

      MockWebServerManager.injectEndpoint(
        "${testInfo.displayName}_info",
        jsonMock = "infoResponse",
        endpointResolver = { it?.requestLine?.contains("zosmf/info") ?: false }
      )
      MockWebServerManager.injectEndpoint(
        "${testInfo.displayName}_resttopology",
        jsonMock = "infoResponse",
        endpointResolver = { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false }
      )
      MockWebServerManager.injectEndpoint(
        "${testInfo.displayName}_dslevel",
        endpointResolver = { it?.requestLine?.contains("/zosmf/restfiles/ds?dslevel=${allocMask.first}") ?: false },
        customHandler = {
          MockResponse()
            .setBody(
              "{\n" +
              "\"items\": [${createdDatasets.joinToString(",")}],\n" +
              "\"returnedRows\": ${createdDatasets.size},\n" +
              "\"JSONversion\": 1\n" +
              "}"
            )
        }
      )

      MockWebServerManager.injectEndpoint(
        "${testInfo.displayName}_DELETE_ds",
        endpointResolver = { it?.requestLine?.contains("DELETE /zosmf/restfiles/ds/") ?: false },
        customHandler = {
          if (it == null) {
            fail("It is expected that the request is not empty")
          }
          val dsn = it.requestLine
            .substringAfter("/zosmf/restfiles/ds/")
            .substringBefore(" HTTP")
          val dsToDelete = createdDatasets.find { datasetDefinition -> datasetDefinition.contains("\"$dsn\"") }
          createdDatasets.remove(dsToDelete)
          MockResponse().setResponseCode(204)
        }
      )

      filesExplorerPanel = FilesExplorerPanel(ideDriver)
      filesExplorerPanel.createValidConnection(ideDriver, connectionName)

      filesExplorerPanel.openDialogByPlusButtonInExplorer(ActionMenuPoints.WORKING_SET)
      val addWsDialog = AddWorkingSetDialog(ideDriver)
      addWsDialog.fillDialog(connectionName, wsName, listOf(allocMask))
      addWsDialog.okButton.click()
    }

    @JvmStatic
    @AfterAll
    fun afterAll() {
      IdeRunManager.prepareRunManager()
        .runningIde
        .resetTestEnv()
      filesExplorerPanel.deleteAllMaskElements(1)
      deleteConfigEntities(ideDriver, "Working Sets")
      deleteConfigEntities(ideDriver, "Connections")
      MockWebServerManager.removeAllEndpoints()
    }
  }

  @BeforeEach
  fun prepareTestEnv() {
    IdeRunManager.prepareRunManager()
      .runningIde
      .resetTestEnv()
    ideDriver = IdeRunManager.getIdeDriver()
    filesExplorerPanel = FilesExplorerPanel(ideDriver)
    addConnectionDialog = AddConnectionDialog(ideDriver)
    allocateDatasetDialog = AllocateDatasetDialog(ideDriver)
    addWsNotification = AddWorkingSetSuccessNotification(ideDriver)
  }

  /**
   * @see
   * <a href="https://github.com/zowe/zowe-explorer-intellij/wiki/Manual-and-automated-test-cases-consistency#-allocate-data-sets">
   *   Regression: Allocate data sets
   * </a>
   */
  @Tag("New")
  @ParameterizedTest
  @MethodSource("provideOrgTypes")
  fun allocateDatasetsTest(allocationParams: AllocateDatasetParams) {
    // Right-click on a working set, New -> Dataset
    filesExplorerPanel.selectRightClickMenuItem(0, "New", "Dataset")
    allocateDatasetDialog.fillDialog(allocationParams)

    val dsName = allocationParams.name
    val dsOrg = allocationParams.dsOrg

    MockWebServerManager.injectEndpoint(
      "testAllocateValid_${dsOrg.value.full}_Datasets_${allocationParams.recfm}_restfiles",
      endpointResolver = {
        it?.requestLine?.contains("POST /zosmf/restfiles/ds/${dsName}") ?: false
      },
      customHandler = {
        if (it == null) {
          fail("It is expected that the request is not empty")
        } else {
          val dsn = it.requestLine
            .substringAfter("/zosmf/restfiles/ds/")
            .substringBefore(" HTTP")
          val allocRequestParams = it.body.clone().readUtf8()
          val blksz = allocRequestParams.substringAfter("blksize\":")
            .substringBefore(",")
//            TODO: change matching
            .substringBefore("}")
          val dsorg = allocRequestParams.substringAfter("dsorg\":\"").substringBefore("\"")
          val lrecl = allocRequestParams.substringAfter("lrecl\":")
            .substringBefore(",")
//            TODO: change matching
            .substringBefore("}")
          val recfm = allocRequestParams.substringAfter("recfm\":\"").substringBefore("\"")
          val alcunit = allocRequestParams.substringAfter("alcunit\":\"").substringBefore("\"")
          val spacu = if (alcunit == AllocUnit.TRK.toString()) "TRACKS" else "CYLINDERS"
          val newDs = dsTemplate
            .replace("###dsname###", dsn)
            .replace("###blksz###", blksz)
            .replace("###dsorg###", dsorg)
            .replace("###lrecl###", lrecl)
            .replace("###recfm###", recfm)
            .replace("###spacu###", spacu)
          createdDatasets.add(newDs)
        }
        MockResponse()
      }
    )

    allocateDatasetDialog.okButton.click()
    // Right-click on a dataset mask, Refresh
    filesExplorerPanel.selectRightClickMenuItem(1, "Refresh")

    filesExplorerPanel
      .waitForTreeToLoadRow(filesExplorerPanel.fileExplorerTree.collectExpandedPaths().size - 1)
    val fileExplorerTreePaths = filesExplorerPanel.fileExplorerTree.collectExpandedPaths()
    assert(fileExplorerTreePaths.last().path.any { it.contains(dsName) })

    // TODO: the other test???
//    val isNotificationShown = addWsNotification.isAllocNotificationVisible(dsName)
//    addWsNotification.skipButton.click()

//    assert(isNotificationShown)
  }
  /**
  * @see
  * <a href="https://github.com/zowe/zowe-explorer-intellij/wiki/Manual-and-automated-test-cases-consistency#-allocating-data-sets-with-invalid-parameters">
  *   Regression: Allocate data sets with invalid parameters
  * </a>
  */
  @Tag("New")
  @ParameterizedTest
  @MethodSource("getInvalidDatasetConfigs")
  fun allocateDatasetsWithInvalidParamsTest(allocationParams: AllocateDatasetParams, expectedMsg: String){
    filesExplorerPanel.selectRightClickMenuItem(0, "New", "Dataset")
    allocateDatasetDialog.fillDialog(allocationParams)
    allocateDatasetDialog.okButton.click()
    val uiErrorMsg = allocateDatasetDialog.errorMsg.allTextAsString()
    assert(uiErrorMsg==expectedMsg){"Incorrect error msg. expected '$expectedMsg', in ui: '$uiErrorMsg'"}
  }

  /**
   * @see
   * <a href="https://github.com/zowe/zowe-explorer-intellij/wiki/Manual-and-automated-test-cases-consistency#-allocating-data-sets-with-invalid-parameters">
   *   Regression: Allocate data sets with invalid parameters
   * </a>
   */
  @Tag("New")
  @ParameterizedTest
  @MethodSource("getInvalidDatasetConfigsForServerValidation")
  fun serverDatasetConfigValidationTest(allocationParams: AllocateDatasetParams, expectedMsg: String, testInfo: TestInfo) {
    MockWebServerManager.injectEndpoint(
      "${testInfo.displayName}_dslevel",
      endpointResolver = { it?.requestLine?.contains("POST /zosmf/restfiles/ds/${allocationParams.name}") ?: false },
      customHandler = {
        MockResponse()
          .setResponseCode(500)
          .setBody(
            "{\n" +
              "\"category\":\"1\"," +
              "\"rc\":\"4\"," +
              "\"reason\":13," +
              "\"message\": \"For a V file, the LRECL must be greater than 4 bytes.\"\n" +
              "}"
          )
      }
    )
    filesExplorerPanel.selectRightClickMenuItem(0, "New", "Dataset")
    allocateDatasetDialog.fillDialog(allocationParams)
    allocateDatasetDialog.okButton.click()

    val uiErrorMsg = allocateDatasetDialog.errorDialogMsg.allTextAsString()
    allocateDatasetDialog.cancelButton.click()
    assert(uiErrorMsg==expectedMsg){"Incorrect error msg. expected '$expectedMsg', in ui: '$uiErrorMsg'"}
  }
}
