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
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.utils.keyboard
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.awt.event.KeyEvent

/**
 * Tests viewing dataset and members.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class ViewDatasetTest {
  private var closableFixtureCollector = ClosableFixtureCollector()
  private var fixtureStack = mutableListOf<Locator>()
  private var wantToClose = mutableListOf("Add Working Set Dialog", "Create Mask Dialog")

  private val projectName = "untitled"
  private val connectionName = "valid connection"
  private var mapListDatasets = mutableMapOf<String, String>()
  private var listMembersInDataset = mutableListOf<String>()

  private val noItemsFoundMsg = "No items found"

  /**
   * Opens the project and Explorer, clears test environment, creates valid connection.
   */
  @BeforeAll
  fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) {
    startMockServer()
    setUpTestEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
    createValidConnectionWithMock(
      testInfo,
      connectionName,
      projectName,
      fixtureStack,
      closableFixtureCollector,
      remoteRobot
    )

  }

  /**
   * Closes the project and clears test environment.
   */
  @AfterAll
  fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
    mockServer.shutdown()
    clearEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
    ideFrameImpl(projectName, fixtureStack) {
      close()
    }
  }

  /**
   * Closes all unclosed closable fixtures that we want to close.
   */
  @AfterEach
  fun tearDown(remoteRobot: RemoteRobot) = with(remoteRobot) {
    closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    responseDispatcher.removeAllEndpoints()
    mapListDatasets.clear()
    listMembersInDataset.clear()
  }

  /**
   * Test to view empty PDS dataset and check that File Explorer contains correct message.
   */
  @Test
  fun testViewEmptyPDS(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
    val wsName = "WS1"
    val datasetName = "$ZOS_USERID.EMPTY.PDS".uppercase()
    createWsAndMaskWithMock(wsName, datasetName, true, "PDS", "PO", testInfo, remoteRobot)
    openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    findMessageInExplorer(noItemsFoundMsg, remoteRobot)
    openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
  }

  /**
   * Test to view empty PS dataset.
   */
  @Test
  fun testViewEmptyPS(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
    val wsName = "WS2"
    val datasetName = "$ZOS_USERID.EMPTY.PS".uppercase()
    viewPSDataset(wsName, datasetName, true, testInfo, remoteRobot)
  }

  /**
   * Test to view non-empty PS dataset.
   */
  @Test
  fun testViewNonEmptyPS(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
    val wsName = "WS3"
    val datasetName = "$ZOS_USERID.NONEMPTY.PS".uppercase()
    viewPSDataset(wsName, datasetName, false, testInfo, remoteRobot)
  }

  /**
   * Test to view non-empty PDS dataset and view empty/non-empty members.
   */
  @Test
  fun testViewNonEmptyPDS(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
    val wsName = "WS4"
    val datasetName = "$ZOS_USERID.NONEMPTY.PDS".uppercase()
    createWsAndMaskWithMock(wsName, datasetName, false, "PDS", "PO", testInfo, remoteRobot)
    openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    openMember(datasetName, "MEMBER1", true, testInfo, remoteRobot)
    closeMemberOrDataset(remoteRobot)
    openMember(datasetName, "MEMBER2", false, testInfo, remoteRobot)
    closeMemberOrDataset(remoteRobot)
    openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
  }

  /**
   * Views empty/non-empty PS dataset.
   */
  private fun viewPSDataset(
    wsName: String,
    datasetName: String,
    isEmpty: Boolean,
    testInfo: TestInfo,
    remoteRobot: RemoteRobot
  ) = with(remoteRobot) {
    createWsAndMaskWithMock(wsName, datasetName, isEmpty, "", "PS", testInfo, remoteRobot)
    openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    openPSDataset(datasetName, isEmpty, testInfo, remoteRobot)
    closeMemberOrDataset(remoteRobot)
    openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
  }

  /**
   * Closes opened dataset member or PS dataset.
   */
  private fun closeMemberOrDataset(remoteRobot: RemoteRobot) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
      with(textEditor()) {
        keyboard {
          hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_F4)
        }
      }
    }
  }

  /**
   * Opens PS dataset.
   */
  private fun openPSDataset(datasetName: String, isEmpty: Boolean, testInfo: TestInfo, remoteRobot: RemoteRobot) =
    with(remoteRobot) {
      val psContent = if (isEmpty) {
        ""
      } else {
        "content"
      }
      responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restfiles_getcontent",
        { it?.requestLine?.contains("GET /zosmf/restfiles/ds/-(TESTVOL)/$datasetName") ?: false },
        { MockResponse().setBody(psContent) }
      )
      ideFrameImpl(projectName, fixtureStack) {
        explorer {
          find<ComponentFixture>(viewTree).findAllText(datasetName).last().doubleClick()
          Thread.sleep(2000)
        }
      }
    }

  /**
   * Opens dataset member.
   */
  private fun openMember(
    datasetName: String,
    memberName: String,
    isEmpty: Boolean,
    testInfo: TestInfo,
    remoteRobot: RemoteRobot
  ) = with(remoteRobot) {
    val memberContent = if (isEmpty) {
      ""
    } else {
      "content"
    }
    responseDispatcher.injectEndpoint(
      "${testInfo.displayName}_restfiles_getmember",
      { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${datasetName}($memberName)") ?: false },
      { MockResponse().setBody(memberContent) }
    )
    ideFrameImpl(projectName, fixtureStack) {
      explorer {
        find<ComponentFixture>(viewTree).findAllText(memberName).last().doubleClick()
        Thread.sleep(2000)
      }
    }
  }

  /**
   * Creates working set with z/os mask.
   */
  private fun createWsAndMaskWithMock(
    wsName: String,
    datasetName: String,
    isEmpty: Boolean,
    dsNtp: String,
    dsOrg: String,
    testInfo: TestInfo,
    remoteRobot: RemoteRobot
  ) = with(remoteRobot) {
    val maskList = listOf(Pair(datasetName, "z/OS"))
    mapListDatasets[datasetName] = listDS(datasetName, dsNtp, dsOrg)
    responseDispatcher.injectEndpoint(
      "${testInfo.displayName}_restfiles",
      {
        it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${datasetName}")
          ?: false
      },
      { MockResponse().setBody(buildResponseListJson(mapListDatasets, true)) }
    )
    if (!isEmpty && dsNtp == "PDS") {
      for (i in 1..5) {
        listMembersInDataset.add("MEMBER$i")
      }
    }
    responseDispatcher.injectEndpoint(
      "${testInfo.displayName}_restfiles_listmembers",
      { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${datasetName}/member") ?: false },
      {
        MockResponse().setBody(buildListMembersJson())
      }
    )
    createWsAndMask(projectName, wsName, maskList, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
  }

  private fun buildListMembersJson(): String {
    var members = "[ "
    if (listMembersInDataset.isNotEmpty()) {
      listMembersInDataset.forEach { members += "{\"member\": \"${it}\"}," }
    }
    members = members.dropLast(1) + "]"
    return "{\"items\":$members,\"returnedRows\": ${listMembersInDataset.size},\"JSONversion\": 1}"
  }

  /**
   * Checks if File Explorer contains expected message.
   */
  private fun findMessageInExplorer(msg: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
    if (!find<ComponentFixture>(viewTree).findAllText().map(RemoteText::text).joinToString("")
        .contains(msg)
    ) {
      throw Exception("Expected message is not found")
    }
  }
}