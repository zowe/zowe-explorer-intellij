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

package settings.connection

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.search.locators.Locator
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import testutils.ProcessManager
import workingset.*
import workingset.auxiliary.components.dialogs.AddConnectionDialogUtil
import workingset.auxiliary.components.dialogs.EditConnectionDialogUtil
import workingset.auxiliary.components.dialogs.SettingsDialogUtil
import workingset.auxiliary.components.elements.ButtonElement
import workingset.testutils.*
import java.time.Duration

/**
 * Tests the ConnectionManager on UI level.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class ConnectionManager : IdeaInteractionClass()  {
  private var closableFixtureCollector = ClosableFixtureCollector()
  private var fixtureStack = mutableListOf<Locator>()
  private val wantToClose = listOf(
    "Settings Dialog", "Add Connection Dialog", "Error Creating Connection Dialog",
    "Edit Connection Dialog", "Add JES Working Set Dialog"
  )
  private val aHost = "a.com"
  private val aPassword = "a"
  private val cPassword = "a"
  private val aLogin = "a"
  private val cLogin = "a"
  private val spaceWithLogin = " testuser "
  private val aConnection = "a"
  private val bConnection = "b"
  private val dConnection = "d"
  private val cConnection = "c"
  private val invalidCredConnection = "invalidCredConnection"
  private val invalidUrl = "zzz"
  private val validConnectionName2 = "zzz"
  private val invalidConnectionName2 = "zzz2"

  private val validConnectionName = "valid connection"
  private val independentConnectionName = "independent connection"
  private val wsDependentConnectionName = "ws dependent connection"
  private val jesDependentConnectionName = "jes dependent connection"
  private val wsAndJesDependentConnectionName = "ws and jes dependent connection"

  private lateinit var settingsDialog: SettingsDialogUtil
  private lateinit var addConnectionDialog: AddConnectionDialogUtil
  private lateinit var editConnectionDialog: EditConnectionDialogUtil

  private lateinit var okButtonSub: ButtonElement
  private lateinit var proceedButton: ButtonElement
  private lateinit var cancelButtonSub: ButtonElement
  private lateinit var processManager: ProcessManager

  /**
   * Creates and starts mock server, opens the project and Explorer, clears test environment.
   */
  @BeforeAll
  fun setUpAll(remoteRobot: RemoteRobot) {
    processManager = ProcessManager()
    startMockServer()
    setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
    settingsDialog = SettingsDialogUtil(fixtureStack, remoteRobot)
    addConnectionDialog = AddConnectionDialogUtil(fixtureStack, remoteRobot)
    editConnectionDialog = EditConnectionDialogUtil(fixtureStack, remoteRobot)

    okButton = ButtonElement(OK_TEXT, fixtureStack, remoteRobot)
    okButtonSub = ButtonElement(subOkButtonLoc, fixtureStack, remoteRobot)
    cancelButton = ButtonElement(CANCEL_TEXT, fixtureStack, remoteRobot)
    cancelButtonSub = ButtonElement(subCancelButtonLoc, fixtureStack, remoteRobot)
    yesButton = ButtonElement(YES_TEXT, fixtureStack, remoteRobot)
    noButton = ButtonElement(NO_TEXT, fixtureStack, remoteRobot)
    proceedButton = ButtonElement(PROCEED_TEXT, fixtureStack, remoteRobot)

  }

  /**
   * Shutdowns mock server and closes the project and clears test environment.
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
    responseDispatcher.removeAllEndpoints()
    closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
  }

  /**
   * Test that should pass and leave a bunch of opened dialogs.
   */
  @Test
  fun testAddWrongConnection(remoteRobot: RemoteRobot) = with(remoteRobot) {

    callSettingsByAction(fixtureStack,remoteRobot)
    closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)

    settingsDialog.callAddConnection(fixtureStack,remoteRobot)
    addConnectionDialog.addConnection(aConnection, "https://${aHost}", aLogin, aPassword, true)
    addConnectionDialog.okButton.click()

    closableFixtureCollector.add(EditConnectionDialog.xPath(), fixtureStack)
    closableFixtureCollector.add(ErrorCreatingConnectionDialog.xPath(), fixtureStack)

    assertTrue(find<HeavyWeightWindowFixture>(errorConnectionNotification, Duration.ofSeconds(5)).hasText(hostUnknowableError.format(aHost)))
  }

  /**
   * Tests that checks whether it is possible on UI level to add two connections with the same name.
   */
  @Test
  fun testAddTwoConnectionsWithTheSameName(remoteRobot: RemoteRobot, testInfo: TestInfo) = with(remoteRobot) {
    injectTestInfo(testInfo)
    injectTestInfoRestTopology(testInfo)
    createConnection(
      fixtureStack, closableFixtureCollector, dConnection, true, remoteRobot,
      "https://${mockServer.hostName}:${mockServer.port}"
    )
    callSettingsByAction(fixtureStack,remoteRobot)

    settingsDialog.callAddConnection(fixtureStack,remoteRobot)
    addConnectionDialog.addConnection(dConnection, "https://${mockServer.hostName}:${mockServer.port}", aLogin, aPassword, true)
    addConnectionDialog.okButton.click()
    closableFixtureCollector.closeOnceIfExists(EditConnectionDialog.name)

    find<HeavyWeightWindowFixture>(messageLoc,Duration.ofSeconds(30)).findText(duplicateConnectionNameError.format(dConnection))

    addConnectionDialog.cancelButton.click()
    closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
    settingsDialog.cancelButton.click()

    closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)


  }

  /**
   * Tests to create connection with valid parameters.
   */
  @Test
  fun testAddValidConnection(remoteRobot: RemoteRobot, testInfo: TestInfo) {
    injectTestInfo(testInfo)
    injectTestInfoRestTopology(testInfo)
    createConnection(
      fixtureStack, closableFixtureCollector, validConnectionName, true, remoteRobot,
      "https://${mockServer.hostName}:${mockServer.port}"
    )
  }

  /**
   * Tests to create connection with spaces before and after connection url.
   */

  @Test
  fun testAddConnectionWithSpaces(remoteRobot: RemoteRobot, testInfo: TestInfo) = with(remoteRobot) {
    injectTestInfo(testInfo)
    injectTestInfoRestTopology(testInfo)
    callSettingsByAction(fixtureStack,remoteRobot)
    closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)

    settingsDialog.callAddConnection(fixtureStack,remoteRobot)
    addConnectionDialog.addConnection(
      bConnection, "   https://${mockServer.hostName}:${mockServer.port}    ",
      spaceWithLogin, ZOS_PWD, true)
    addConnectionDialog.okButton.click()
    proceedButton.click()
    closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
    settingsDialog.okButton.click()
    closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
  }

  /**
   * Tests to create connection with invalid credentials, checks that correct message returned.
   */
  @Disabled("https://jira.ibagroup.eu/browse/IJMP-1821")
  @Test
  fun testAddConnectionWithInvalidCredentials(remoteRobot: RemoteRobot, testInfo: TestInfo) = with(remoteRobot) {
    injectInvalidCreeds(testInfo)
    callSettingsByAction(fixtureStack,remoteRobot)
    closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)

    settingsDialog.callAddConnection(fixtureStack,remoteRobot)
    addConnectionDialog.addConnection(invalidCredConnection, "https://${mockServer.hostName}:${mockServer.port}", cLogin, cPassword, true)
    addConnectionDialog.okButton.click()
    find<HeavyWeightWindowFixture>(errorConnectionNotification,Duration.ofSeconds(30))
    find<HeavyWeightWindowFixture>(messageLoc,Duration.ofSeconds(30)).findText(INVALID_CREEDS_ERROR)

    addConnectionDialog.cancelButton.click()
    closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
    settingsDialog.cancelButton.click()

    closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
  }

  /**
   * Tests to create connection with unchecked checkBox for SSL certification, checks that correct message returned.
   */
  @Test
  fun testAddConnectionWithUncheckedSSL(remoteRobot: RemoteRobot, testInfo: TestInfo) = with(remoteRobot) {
    injectInvalidCertificate(testInfo)
    callSettingsByAction(fixtureStack,remoteRobot)
    closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)

    settingsDialog.callAddConnection(fixtureStack,remoteRobot)
    addConnectionDialog.addConnection(invalidCredConnection, "https://${mockServer.hostName}:${mockServer.port}", cLogin, cPassword, false)
    addConnectionDialog.okButton.click()


    find<HeavyWeightWindowFixture>(errorConnectionNotification,Duration.ofSeconds(30))
    find<HeavyWeightWindowFixture>(errorContainsWordYou,Duration.ofSeconds(30)).findText(CERTIFICATE_ERROR)
    noButton.click()
    closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)


    addConnectionDialog.cancelButton.click()
    closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
    settingsDialog.cancelButton.click()

    closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
  }

  /**
   * Tests to create connection with very long name.
   */
  @Test
  fun testAddConnectionWithVeryLongName(remoteRobot: RemoteRobot, testInfo: TestInfo) {
    injectTestInfo(testInfo)
    injectTestInfoRestTopology(testInfo)
    createConnection(
      fixtureStack, closableFixtureCollector, B_200, true, remoteRobot,
      "https://${mockServer.hostName}:${mockServer.port}"
    )
  }

  /**
   * Tests to create connection with invalid connection url, checks that correct message returned.
   */
  @Test
  fun testAddInvalidConnectionWithUrlByMask(remoteRobot: RemoteRobot, testInfo: TestInfo) = with(remoteRobot) {
    injectInvalidInfo(testInfo, INVALID_URL_ERROR)

    callSettingsByAction(fixtureStack,remoteRobot)
    closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)

    settingsDialog.callAddConnection(fixtureStack,remoteRobot)

    addConnectionDialog.addConnection(invalidCredConnection, invalidUrl, cLogin, cPassword, true)
    addConnectionDialog.okButton.click()

    val combinedText  = find<HeavyWeightWindowFixture>(messageLoc,Duration.ofSeconds(30)).findAllText().map { it.text }
      .reduce { first, lasts -> "$first$lasts" }//.findText(CERTIFICATE_ERROR)
    assertTrue(combinedText == INVALID_URL_ERROR)

    addConnectionDialog.cancelButton.click()
    closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)

    settingsDialog.cancelButton.click()
    closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
  }

  /**
   * Tests to edit valid connection to invalid, checks that correct message returned.
   */
  @Test
  fun testEditConnectionFromValidToInvalid(remoteRobot: RemoteRobot, testInfo: TestInfo) = with(remoteRobot) {

    injectTestInfo(testInfo)
    injectTestInfoRestTopology(testInfo)
    createConnection(
      fixtureStack, closableFixtureCollector, validConnectionName2, false, remoteRobot,
      "https://${mockServer.hostName}:${mockServer.port}"
    )

    callSettingsByAction(fixtureStack,remoteRobot)
    closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)

    settingsDialog.selectWs(validConnectionName2, fixtureStack, remoteRobot)
    settingsDialog.editWsButton.click()
    editConnectionDialog.setConnectionUrl("https://${mockServer.hostName}:$PORT_104431",fixtureStack,remoteRobot)
    editConnectionDialog.okButton.click()
    closableFixtureCollector.add(EditConnectionDialog.xPath(), fixtureStack)

    find<HeavyWeightWindowFixture>(errorConnectionNotification,Duration.ofSeconds(30))
    assertTrue(
      find<HeavyWeightWindowFixture>(
        errorContainsWordYou,Duration.ofSeconds(30)).findAllText()[0].text == invalidPort104431)
    closableFixtureCollector.add(ErrorCreatingConnectionDialog.xPath(), fixtureStack)
    closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)


  }

  /**
   * Tests to edit invalid connection to valid.
   */
  @Test
  fun testEditConnectionFromInvalidToValid(remoteRobot: RemoteRobot, testInfo: TestInfo) = with(remoteRobot) {
    injectTestInfo(testInfo)
    injectTestInfoRestTopology(testInfo)
    createConnection(
      fixtureStack, closableFixtureCollector, invalidConnectionName2, false, remoteRobot,
      "https://${mockServer.hostName}:${PORT_104431}"
    )
    callSettingsByAction(fixtureStack,remoteRobot)
    closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)

    settingsDialog.selectWs(invalidConnectionName2, fixtureStack, remoteRobot)
    settingsDialog.editWsButton.click()
    editConnectionDialog.setConnectionUrl("https://${mockServer.hostName}:${mockServer.port}",fixtureStack,remoteRobot)
    editConnectionDialog.okButton.click()

    settingsDialog.okButton.click()

    closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
  }

  /**
   * Tests to edit connection by unchecking SSL certification checkbox and returning it to back,
   * check that correct message returned.
   */
  @Test
  fun testEditConnectionUncheckSSLandReturnBack(remoteRobot: RemoteRobot, testInfo: TestInfo) = with(remoteRobot) {
    val saveInjectionName = injectInvalidInfo(testInfo, UNABLE_FIND_VALID_CERTIFICATE)
    injectTestInfoRestTopology(testInfo)

    callSettingsByAction(fixtureStack,remoteRobot)
    closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)

    settingsDialog.callAddConnection(fixtureStack,remoteRobot)
    addConnectionDialog.addConnection(cConnection, "https://${mockServer.hostName}:${mockServer.port}", cLogin, cPassword, false)
    addConnectionDialog.okButton.click()


    val combinedText  = find<HeavyWeightWindowFixture>(errorContainsWordYou,Duration.ofSeconds(30)).findAllText()
    val msgInUi = combinedText[0].text+combinedText[1].text
    assertTrue(msgInUi == UNABLE_FIND_VALID_CERTIFICATE)
    noButton.click()
    closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)

    responseDispatcher.removeEndpoint(saveInjectionName)
    injectTestInfo(testInfo)
    editConnectionDialog.setSsl(true, fixtureStack, remoteRobot)

    editConnectionDialog.okButton.click()
    closableFixtureCollector.closeOnceIfExists(EditConnectionDialog.name)
    settingsDialog.okButton.click()

    closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)


  }

  /**
   * Tests to delete connection with unique connection url and without any working sets.
   */
  @Test
  fun testDeleteConnectionWithUniqueUrlWithoutWSandJWS(remoteRobot: RemoteRobot,testInfo: TestInfo) {
    injectTestInfo(testInfo)
    injectTestInfoRestTopology(testInfo)
    createConnection(
      fixtureStack, closableFixtureCollector, independentConnectionName, false, remoteRobot,
      "https://${mockServer.hostName}:${PORT_104431}"
    )
    callSettingsByAction(fixtureStack,remoteRobot)
    closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)

    settingsDialog.selectWs(independentConnectionName, fixtureStack, remoteRobot)
    settingsDialog.removeButton.click()
    closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
  }

  /**
   * Tests to delete connection with working set, check that correct message returned.
   */
  @Test
  fun testDeleteConnectionWithWorkingSet(remoteRobot: RemoteRobot,testInfo: TestInfo) = with(remoteRobot) {
    injectTestInfo(testInfo)
    injectTestInfoRestTopology(testInfo)
    createConnection(
      fixtureStack, closableFixtureCollector, wsDependentConnectionName, true, remoteRobot,
      "https://${mockServer.hostName}:${mockServer.port}"
    )

    createWsWithConnectionFromAction(WS_NAME_1, wsDependentConnectionName, singleMask, fixtureStack, remoteRobot)
    callSettingsByAction(fixtureStack,remoteRobot)
    closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)

    settingsDialog.selectWs(wsDependentConnectionName, fixtureStack, remoteRobot)
    settingsDialog.removeButton.click()

    val combinedText  = find<HeavyWeightWindowFixture>(errorContainsWordYou,Duration.ofSeconds(30)).findAllText()
    val msgInUi = combinedText[0].text+combinedText[2].text
    assertTrue(msgInUi == EXIST_DEPENDED_WS_ERROR.format(WS_NAME_1))

    yesButton.click()
    closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
    settingsDialog.okButton.click()

    closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
  }

  /**
   * Tests to delete connection with JES working set, check that correct message returned.
   */
  @Test
  fun testDeleteConnectionWithJesWorkingSet(remoteRobot: RemoteRobot,testInfo: TestInfo) = with(remoteRobot) {
    injectTestInfo(testInfo)
    injectTestInfoRestTopology(testInfo)
    createConnection(
      fixtureStack, closableFixtureCollector, jesDependentConnectionName, true, remoteRobot,
      "https://${mockServer.hostName}:${mockServer.port}"
    )

    createJWS(WS_NAME_2, jesDependentConnectionName, filterAllAndZos, fixtureStack, closableFixtureCollector, remoteRobot)
    callSettingsByAction(fixtureStack,remoteRobot)
    closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)

    settingsDialog.selectWs(jesDependentConnectionName, fixtureStack, remoteRobot)
    settingsDialog.removeButton.click()

    val combinedText  = find<HeavyWeightWindowFixture>(errorContainsWordYou,Duration.ofSeconds(30)).findAllText()
    val msgInUi = combinedText[0].text+combinedText[2].text
    assertTrue(msgInUi == EXIST_DEPENDED_JWS_ERROR.format(WS_NAME_2))

    yesButton.click()
    closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
    settingsDialog.okButton.click()

    closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
  }

  /**
   * Tests to delete connection with working set and JES working set, check that correct message returned.
   */
  @Test
  fun testDeleteConnectionWithWSandJWS(remoteRobot: RemoteRobot, testInfo: TestInfo) = with(remoteRobot) {
    injectTestInfo(testInfo)
    injectTestInfoRestTopology(testInfo)
    createConnection(
      fixtureStack, closableFixtureCollector, wsAndJesDependentConnectionName, true, remoteRobot,
      "https://${mockServer.hostName}:${mockServer.port}"
    )

    createJWS(WS_NAME_3, wsAndJesDependentConnectionName, filterAllAndZos, fixtureStack, closableFixtureCollector, remoteRobot)
    createWsWithConnectionFromAction(WS_NAME_3, wsAndJesDependentConnectionName, singleMask, fixtureStack, remoteRobot)
    callSettingsByAction(fixtureStack,remoteRobot)
    closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)

    settingsDialog.selectWs(wsAndJesDependentConnectionName, fixtureStack, remoteRobot)
    settingsDialog.removeButton.click()

    val combinedText  = find<HeavyWeightWindowFixture>(errorContainsWordYou,Duration.ofSeconds(30)).findAllText()
    val msgInUiWs = combinedText[0].text+combinedText[2].text
    val msgInUiJws = combinedText[4].text+combinedText[6].text
    assertTrue(msgInUiWs == EXIST_DEPENDED_WS_ERROR.format(WS_NAME_3))
    assertTrue(msgInUiJws == EXIST_DEPENDED_JWS_ERROR.format(WS_NAME_3))

    yesButton.click()
    closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
    settingsDialog.okButton.click()
    closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
  }
}
