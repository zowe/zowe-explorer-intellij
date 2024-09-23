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
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import testutils.ProcessManager
import testutils.injectSingleMember
import workingset.testutils.injectListAllAllocatedDatasetsWithContents
import workingset.testutils.injectMemberContent
import workingset.testutils.injectPsDatasetContent
import java.util.stream.Stream

/**
 * Tests viewing dataset and members.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class ViewDatasetTest :IdeaInteractionClass(){
  private var closableFixtureCollector = ClosableFixtureCollector()
  private var fixtureStack = mutableListOf<Locator>()
  private var wantToClose = mutableListOf("Add Working Set Dialog", "Create Mask Dialog")

  private var mapListDatasets = mutableMapOf<String, String>()
  private var listMembersInDataset = mutableListOf<String>()

  private val noItemsFoundMsg = "No items found"
  private val wsNameWs1 = "WS1"
  private val userIdPrefix = ZOS_USERID.uppercase()
  private val emptyPdsDatasetNameTell = ".EMPTY.PDS".uppercase()
  private val pdsDatasetName = "$ZOS_USERID.NONEMPTY.PDS".uppercase()
  private lateinit var processManager: ProcessManager


  companion object {
    @JvmStatic
    fun pairProvider(): Stream<out Arguments> {
      return Stream.of(
        Arguments.of(WS_NAME_2, EMPTY_MEMBER_CONTENT, ".EMPTY.PS"),
        Arguments.of(WS_NAME_3, SHORT_MEMBER_CONTENT, ".NONEMPTY.PS"),
      )
    }
  }

  /**
   * Opens the project and Explorer, clears test environment, creates valid connection.
   */
  @BeforeAll
  fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) {
    processManager = ProcessManager()
    startMockServer()
    setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
    createValidConnectionWithMock(
      testInfo,
      connectionName,
      fixtureStack,
      closableFixtureCollector,
      remoteRobot
    )
  }

  /**
   * Closes the project and clears test environment.
   */
  @AfterAll
  fun tearDownAll() {
    processManager.close()
    mockServer.shutdown()
  }

  /**
   * Closes all unclosed closable fixtures that we want to close.
   */
  @AfterEach
  fun tearDown(remoteRobot: RemoteRobot) {
    closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    responseDispatcher.removeAllEndpoints()
    mapListDatasets.clear()
    listMembersInDataset.clear()
  }

  /**
   * Test to view empty PDS dataset and check that File Explorer contains correct message.
   */
  @Test
  fun testViewEmptyPDS(testInfo: TestInfo, remoteRobot: RemoteRobot) {
    createWsAndMaskWithMock(wsNameWs1, userIdPrefix+emptyPdsDatasetNameTell, EMPTY_MEMBER_CONTENT, PDS_TYPE, PO_ORG_SHORT, testInfo, remoteRobot)
    openOrCloseWorkingSetInExplorer(wsNameWs1, fixtureStack, remoteRobot)
    findMessageInExplorer(noItemsFoundMsg, remoteRobot)
  }

  /**
   * Views empty/non-empty PS dataset.
   */
  @ParameterizedTest
  @MethodSource("pairProvider")
  fun testViewPsDataset(wsName: String, memberContent:String, tell: String, testInfo: TestInfo, remoteRobot: RemoteRobot){
    val datasetName = userIdPrefix + tell
    createWsAndMaskWithMock(wsName, datasetName, memberContent, EMPTY_STRING, SEQUENTIAL_ORG_SHORT, testInfo, remoteRobot)
    openOrCloseWorkingSetInExplorer(wsName, fixtureStack, remoteRobot)

    injectPsDatasetContent(testInfo, datasetName, memberContent)
    openTreesElement(datasetName, remoteRobot)
    closeMemberOrDataset(remoteRobot)
  }


  /**
   * Test to view non-empty PDS dataset and view empty/non-empty members.
   */
  @Test
  fun testViewNonEmptyPDS(testInfo: TestInfo, remoteRobot: RemoteRobot) {
    createWsAndMaskWithMock(WS_NAME_4, pdsDatasetName, SHORT_MEMBER_CONTENT, PDS_TYPE, PO_ORG_SHORT, testInfo, remoteRobot)
    openOrCloseWorkingSetInExplorer(WS_NAME_4, fixtureStack, remoteRobot)

    injectMemberContent(testInfo,pdsDatasetName, MEMBER_NAME_1)
    openTreesElement(MEMBER_NAME_1, remoteRobot)
    closeMemberOrDataset(remoteRobot)

    injectMemberContent(testInfo,pdsDatasetName, MEMBER_NAME_2, "content")
    openTreesElement(MEMBER_NAME_2, remoteRobot)
    closeMemberOrDataset(remoteRobot)

  }

  /**
   * Creates working set with z/os mask.
   */
  private fun createWsAndMaskWithMock(
    wsName: String,
    datasetName: String,
    memberContent: String,
    dsNtp: String,
    dsOrg: String,
    testInfo: TestInfo,
    remoteRobot: RemoteRobot
  ) {
    val maskList = listOf(Pair(datasetName, ZOS_MASK))
    mapListDatasets[datasetName] = listDS(datasetName, dsNtp, dsOrg)

    injectListAllAllocatedDatasetsWithContents(testInfo, datasetName, mapListDatasets)

    if (memberContent != EMPTY_MEMBER_CONTENT && dsNtp == PDS_TYPE) {
      for (i in 1..5) {
        listMembersInDataset.add(MEMBER_NAME_PATTERN+"$i")
      }
      }
    injectSingleMember(testInfo, datasetName, listMembersInDataset)
    createWsAndMask(wsName, maskList, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
  }

}
