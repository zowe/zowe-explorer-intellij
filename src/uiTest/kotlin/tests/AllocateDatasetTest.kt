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
 */

package tests

import auxiliary.ZOS_USERID
import auxiliary.mockServer
import auxiliary.responseDispatcher
import auxiliary.startMockServer
import io.kotest.core.annotation.Description
import testutils.*

import com.intellij.driver.client.Driver
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import tests.utils.FilesExplorerPanel
import tests.utils.dialogs.AddConnectionDialog
import tests.utils.dialogs.AllocateDatasetDialog
import tests.utils.notification.AddWorkingSetSuccessNotification
import tests.utils.notification.UnsecureConnectionDialog

import workingset.*

import java.util.stream.Stream

@Description("Tests to check the functionality associated with dataset allocation")
class AllocateDatasetTest {
  private lateinit var ideDriver: Driver
  private lateinit var filesExplorerPanel: FilesExplorerPanel
  lateinit var addConnectionDialog: AddConnectionDialog
  private lateinit var unsecureConnectionDialog: UnsecureConnectionDialog
  private lateinit var allocateDatasetDialog: AllocateDatasetDialog
  private lateinit var addWsNotification: AddWorkingSetSuccessNotification

  companion object {
    const val connectionName = "valid connection"
    const val wsName = "WS1"
    private val allocMask = Pair("$ZOS_USERID.ALLOC.*", "z/OS")

    @JvmStatic
    fun provideOrgTypes(): Stream<Arguments> {
      val datasetOrg = listOf(
        DatasetOrganization.SEQUENTIAL_ORG_FULL_ITEM,
        DatasetOrganization.PO_ORG_FULL_ITEM,
        DatasetOrganization.POE_ORG_FULL_ITEM
      )

      return RecordFormatsShort.entries.flatMap { recordFormat ->
        datasetOrg.map { organisationType ->
          val datasetOrgShort = organisationType.value.substringAfter('(').substringBefore(')')
          val allocateDatasetPram = AllocateDatasetParams(
            name = datasetOrgShort,
            preset = "Custom Dataset",
            primaryAlloc = "10",
            blockSize = "3200",
            secondaryAlloc = "1",
            avgBlockLen = "0",
            unit = TRACKS_ALLOCATION_UNIT_SHORT,
            dsOrganisationShort = datasetOrgShort
          )
          allocateDatasetPram.organisation = organisationType
          allocateDatasetPram.format = recordFormat.name
          if (organisationType != DatasetOrganization.SEQUENTIAL_ORG_FULL_ITEM) {
            allocateDatasetPram.dirBlock = "1"
          }
          allocateDatasetPram.recordLength = if (recordFormat.name == F_RECORD_FORMAT_SHORT) "3200" else "80"

          Arguments.of(allocateDatasetPram)
        }
      }.stream()
    }

    @JvmStatic
    @BeforeAll
    fun prepareBeforeAll(testInfo: TestInfo) {
      startMockServer()

      IdeRunManager.prepareRunManager()
        .runningIde
        .resetTestEnv()

      val ideDriver = IdeRunManager.getIdeDriver()

      callRightSidePanel(ideDriver)

      createValidConnectionWithMock(
        testInfo,
        connectionName,
        ideDriver
      )
      // Avoid check connection
      Thread.sleep(3000)
      createWsAndMask(ideDriver, wsName, connectionName, listOf(allocMask))


    }

    @JvmStatic
    @AfterAll
    fun afterAll() {
      mockServer.shutdown()
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
    unsecureConnectionDialog = UnsecureConnectionDialog(ideDriver)
    allocateDatasetDialog = AllocateDatasetDialog(ideDriver)
    addWsNotification = AddWorkingSetSuccessNotification(ideDriver)
  }

  @AfterEach
  fun finalizeTestEnv() {
    IdeRunManager.prepareRunManager()
      .runningIde
      .resetTestEnv()
  }

  @Tag("New")
  @ParameterizedTest
  @MethodSource("provideOrgTypes")
  fun allocateDatasetsTest(allocationParams: AllocateDatasetParams) {
    filesExplorerPanel.callSubMenuForRow(0, NEW_POINT_TEXT, DATASET_POINT_TEXT)
    allocateDatasetDialog.fillDialog(allocationParams)
    responseDispatcher.injectAllocationResultPo(
      allocationParams.organisation.toString(),
      allocationParams.format.toString(),
      allocationParams.name,
      allocationParams.dsOrganisationShort.toString(),
      allocationParams.recordLength.toString().toInt()
    )
    allocateDatasetDialog.okButton.click()

    val isNotificationShown = addWsNotification.isAllocNotificationVisible(allocationParams.name)
    addWsNotification.skipButton.click()

    assert(isNotificationShown)

  }

}
