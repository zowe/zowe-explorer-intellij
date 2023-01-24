/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package settings.connection

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import eu.ibagroup.r2z.zowe.MockResponseDispatcher
import io.kotest.matchers.string.shouldContain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Tests the ConnectionManager on UI level.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class ConnectionManager {
  private var closableFixtureCollector = ClosableFixtureCollector()
  private var fixtureStack = mutableListOf<Locator>()
  private val wantToClose = listOf(
    "Settings Dialog", "Add Connection Dialog", "Error Creating Connection Dialog",
    "Edit Connection Dialog"
  )
  private val projectName = "untitled"

  private lateinit var mockServer: MockWebServer
  private lateinit var responseDispatcher: MockResponseDispatcher

  /**
   * Creates and starts mock server, opens the project and Explorer, clears test environment.
   */
  @BeforeAll
  fun setUpAll(remoteRobot: RemoteRobot) {
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
    mockServer.dispatcher = responseDispatcher
    mockServer.useHttps(serverCertificates.sslSocketFactory(), false)
    mockServer.start()

    setUpTestEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
  }

  /**
   * Shutdowns mock server and closes the project and clears test environment.
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
  fun tearDown(remoteRobot: RemoteRobot) {
    responseDispatcher.removeAllEndpoints()
    closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
  }

  /**
   * Test that should pass and leave a bunch of opened dialogs.
   */
  @Test
  @Order(1)
  fun testAddWrongConnection(remoteRobot: RemoteRobot) = with(remoteRobot) {
    val host = "a.com"

    ideFrameImpl(projectName, fixtureStack) {
      explorer {
        settings(closableFixtureCollector, fixtureStack)
      }
      settingsDialog(fixtureStack) {
        configurableEditor {
          conTab.click()
          add(closableFixtureCollector, fixtureStack)
        }
        addConnectionDialog(fixtureStack) {
          addConnection("a", "https://${host}", "a", "a", true)
          clickButton("OK")
        }
        errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
          findText("No such host is known (${host})")
          assertTrue(true)
        }
      }
    }
  }

  /**
   * Tests that checks whether it is possible on UI level to add two connections with the same name.
   */
  @Test
  @Order(2)
  fun testAddTwoConnectionsWithTheSameName(remoteRobot: RemoteRobot) = with(remoteRobot) {
    val connectionName = "a"
    ideFrameImpl(projectName, fixtureStack) {
      explorer {
        settings(closableFixtureCollector, fixtureStack)
      }
      settingsDialog(fixtureStack) {
        configurableEditor {
          conTab.click()
          add(closableFixtureCollector, fixtureStack)
        }
        addConnectionDialog(fixtureStack) {
          addConnection(connectionName, "https://a.com", "a", "a", true)
          clickButton("OK")
        }
        closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
        errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
          clickButton("Yes")
        }
        closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
        configurableEditor {
          add(closableFixtureCollector, fixtureStack)
        }
        addConnectionDialog(fixtureStack) {
          addConnection(connectionName, "https://b.com", "b", "b", true)
          clickButton("OK")
          findText(connectionName).moveMouse()
          find<HeavyWeightWindowFixture>(
            byXpath("//div[@class='HeavyWeightWindow']"),
            Duration.ofSeconds(30)
          ).findText("You must provide unique connection name. Connection $connectionName already exists.")
          assertFalse(button("OK").isEnabled())
          clickButton("Cancel")
        }
        closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
        clickButton("Cancel")
      }
      closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
  }

  /**
   * Tests to create connection with valid parameters.
   */
  @Test
  @Order(3)
  fun testAddValidConnection(remoteRobot: RemoteRobot) {
    responseDispatcher.injectEndpoint(
      "testAddValidConnection_info",
      { it?.requestLine?.contains("zosmf/info") ?: false },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    responseDispatcher.injectEndpoint(
      "testAddValidConnection_resttopology",
      { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    createConnection(
      projectName,
      fixtureStack,
      closableFixtureCollector,
      "valid connection1",
      true,
      remoteRobot,
      "https://${mockServer.hostName}:${mockServer.port}"
    )
  }

  /**
   * Tests to create connection with spaces before and after connection url.
   */
  @Test
  @Order(4)
  fun testAddConnectionWithSpaces(remoteRobot: RemoteRobot) = with(remoteRobot) {
    responseDispatcher.injectEndpoint(
      "testAddConnectionWithSpaces_info",
      { it?.requestLine?.contains("zosmf/info") ?: false },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    responseDispatcher.injectEndpoint(
      "testAddConnectionWithSpaces_resttopology",
      { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    ideFrameImpl(projectName, fixtureStack) {
      explorer {
        settings(closableFixtureCollector, fixtureStack)
      }
      settingsDialog(fixtureStack) {
        configurableEditor {
          conTab.click()
          add(closableFixtureCollector, fixtureStack)
        }
        addConnectionDialog(fixtureStack) {
          addConnection(
            "valid connection2",
            "   https://${mockServer.hostName}:${mockServer.port}   ",
            " testuser ",
            ZOS_PWD,
            true
          )
          clickButton("OK")
          Thread.sleep(1000)
        }
        closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
        clickButton("OK")
      }
      closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
  }

  /**
   * Tests to create connection with invalid credentials, checks that correct message returned.
   */
  @Test
  @Order(5)
  fun testAddConnectionWithInvalidCredentials(remoteRobot: RemoteRobot) = with(remoteRobot) {
    responseDispatcher.injectEndpoint(
      "testAddConnectionWithInvalidCredentials_info",
      { it?.requestLine?.contains("zosmf/info") ?: false },
      { MockResponse().setResponseCode(401) }
    )
    ideFrameImpl(projectName, fixtureStack) {
      explorer {
        settings(closableFixtureCollector, fixtureStack)
      }
      settingsDialog(fixtureStack) {
        configurableEditor {
          conTab.click()
          add(closableFixtureCollector, fixtureStack)
        }
        addConnectionDialog(fixtureStack) {
          addConnection(
            "invalid connection1",
            "https://${mockServer.hostName}:${mockServer.port}",
            "a",
            "a",
            true
          )
          clickButton("OK")
        }
        closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
        errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
          findText("Credentials are not valid")
          clickButton("Yes")
        }
        closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
        clickButton("OK")
      }
      closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
  }

  /**
   * Tests to create connection with unchecked checkBox for SSL certification, checks that correct message returned.
   */
  @Test
  @Order(6)
  fun testAddConnectionWithUncheckedSSL(remoteRobot: RemoteRobot) = with(remoteRobot) {
    responseDispatcher.injectEndpoint(
      "testAddConnectionWithUncheckedSSL_info",
      { it?.requestLine?.contains("zosmf/info") ?: false },
      { MockResponse().setBody("Unable to find valid certification path to requested target") }
    )
    ideFrameImpl(projectName, fixtureStack) {
      explorer {
        settings(closableFixtureCollector, fixtureStack)
      }
      settingsDialog(fixtureStack) {
        configurableEditor {
          conTab.click()
          add(closableFixtureCollector, fixtureStack)
        }
        addConnectionDialog(fixtureStack) {
          addConnection(
            "invalid connection2",
            "https://${mockServer.hostName}:${mockServer.port}",
            ZOS_USERID,
            ZOS_PWD,
            false
          )
          clickButton("OK")
        }
        closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
        errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
          (findAllText()[2].text + findAllText()[3].text).shouldContain("Unable to find valid certification path to requested target")
          clickButton("Yes")
        }
        closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
        clickButton("OK")
      }
      closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
  }

  /**
   * Tests to create connection with very long name.
   */
  @Test
  @Order(7)
  fun testAddConnectionWithVeryLongName(remoteRobot: RemoteRobot) {
    responseDispatcher.injectEndpoint(
      "testAddConnectionWithVeryLongName_info",
      { it?.requestLine?.contains("zosmf/info") ?: false },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    responseDispatcher.injectEndpoint(
      "testAddConnectionWithVeryLongName_resttopology",
      { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    createConnection(
      projectName,
      fixtureStack,
      closableFixtureCollector,
      "A".repeat(200),
      true,
      remoteRobot,
      "https://${mockServer.hostName}:${mockServer.port}"
    )
  }

  /**
   * Tests to create connection with invalid connection url, checks that correct message returned.
   */
  @Test
  @Order(8)
  fun testAddInvalidConnectionWithUrlByMask(remoteRobot: RemoteRobot) = with(remoteRobot) {
    responseDispatcher.injectEndpoint(
      "testAddInvalidConnectionWithUrlByMask_info",
      { it?.requestLine?.contains("zosmf/info") ?: false },
      { MockResponse().setBody("Please provide a valid URL to z/OSMF. Example: https://myhost.com:10443") }
    )
    ideFrameImpl(projectName, fixtureStack) {
      explorer {
        settings(closableFixtureCollector, fixtureStack)
      }
      settingsDialog(fixtureStack) {
        configurableEditor {
          conTab.click()
          add(closableFixtureCollector, fixtureStack)
        }
        addConnectionDialog(fixtureStack) {
          addConnection("invalid connection3", "zzz", "a", "a", true)
          clickButton("OK")
          Thread.sleep(1000)
          val messages = find<HeavyWeightWindowFixture>(
            byXpath("//div[@class='HeavyWeightWindow']"),
            Duration.ofSeconds(30)
          ).findAllText()
          (messages[0].text + messages[1].text).shouldContain("Please provide a valid URL to z/OSMF. Example: https://myhost.com:10443")
          assertFalse(button("OK").isEnabled())
          clickButton("Cancel")
        }
        closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
        clickButton("Cancel")
      }
      closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
  }

  //TODO: tests should be independent
  /**
   * Tests to edit valid connection to invalid, checks that correct message returned.
   */
  @Test
  @Order(9)
  fun testEditConnectionFromValidToInvalid(remoteRobot: RemoteRobot) = with(remoteRobot) {
    val testPort = "104431"
    ideFrameImpl(projectName, fixtureStack) {
      explorer {
        settings(closableFixtureCollector, fixtureStack)
      }
      settingsDialog(fixtureStack) {
        configurableEditor {
          conTab.click()
          editConnection("valid connection1", closableFixtureCollector, fixtureStack)
        }
        editConnectionDialog(fixtureStack) {
          addConnection(
            "invalid connection3",
            "https://${mockServer.hostName}:$testPort",
            ZOS_USERID,
            ZOS_PWD,
            true
          )
          Thread.sleep(1000)
          clickButton("OK")
        }
        closableFixtureCollector.closeOnceIfExists(EditConnectionDialog.name)
        errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
          findText("Invalid URL port: \"$testPort\"")
          clickButton("Yes")
        }
        closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
        clickButton("OK")
      }
      closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
  }

  //TODO: tests should be independent
  /**
   * Tests to edit invalid connection to valid.
   */
  @Test
  @Order(10)
  fun testEditConnectionFromInvalidToValid(remoteRobot: RemoteRobot) = with(remoteRobot) {
    responseDispatcher.injectEndpoint(
      "testEditConnectionFromInvalidToValid_info",
      { it?.requestLine?.contains("zosmf/info") ?: false },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    responseDispatcher.injectEndpoint(
      "testEditConnectionFromInvalidToValid_resttopology",
      { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    ideFrameImpl(projectName, fixtureStack) {
      explorer {
        settings(closableFixtureCollector, fixtureStack)
      }
      settingsDialog(fixtureStack) {
        configurableEditor {
          conTab.click()
          editConnection("invalid connection3", closableFixtureCollector, fixtureStack)
        }
        editConnectionDialog(fixtureStack) {
          addConnection(
            "valid connection1",
            "https://${mockServer.hostName}:${mockServer.port}",
            ZOS_USERID,
            ZOS_PWD,
            true
          )
          clickButton("OK")
          Thread.sleep(1000)
        }
        closableFixtureCollector.closeOnceIfExists(EditConnectionDialog.name)
        clickButton("OK")
      }
      closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
  }

  /**
   * Tests to edit connection by unchecking SSL certification checkbox and returning it to back,
   * check that correct message returned.
   */
  @Test
  @Order(11)
  fun testEditConnectionUncheckSSLandReturnBack(remoteRobot: RemoteRobot) = with(remoteRobot) {
    var isFirstRequest = true
    responseDispatcher.injectEndpoint(
      "testEditConnectionUncheckSSLandReturnBack_info_first",
      { it?.requestLine?.contains("zosmf/info") ?: false && isFirstRequest },
      { MockResponse().setBody("Unable to find valid certification path to requested target") }
    )
    responseDispatcher.injectEndpoint(
      "testEditConnectionUncheckSSLandReturnBack_info_second",
      { it?.requestLine?.contains("zosmf/info") ?: false && !isFirstRequest },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    responseDispatcher.injectEndpoint(
      "testEditConnectionUncheckSSLandReturnBack_resttopology",
      { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    ideFrameImpl(projectName, fixtureStack) {
      explorer {
        settings(closableFixtureCollector, fixtureStack)
      }
      settingsDialog(fixtureStack) {
        configurableEditor {
          conTab.click()
          editConnection("valid connection1", closableFixtureCollector, fixtureStack)
        }
        editConnectionDialog(fixtureStack) {
          uncheckSSLBox()
          clickButton("OK")
        }
        Thread.sleep(1000)
        closableFixtureCollector.closeOnceIfExists(EditConnectionDialog.name)
        errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
          (findAllText()[2].text + findAllText()[3].text).shouldContain("Unable to find valid certification path to requested target")
          clickButton("No")
        }
        isFirstRequest = false
        Thread.sleep(1000)
        closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
        editConnectionDialog(fixtureStack) {
          addConnection(
            "valid connection1",
            "https://${mockServer.hostName}:${mockServer.port}",
            ZOS_USERID,
            ZOS_PWD,
            true
          )
          clickButton("OK")
        }
        Thread.sleep(1000)
        closableFixtureCollector.closeOnceIfExists(EditConnectionDialog.name)
        clickButton("OK")
      }
      closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
  }

  /**
   * Tests to delete connection with unique connection url and without any working sets.
   */
  @Test
  @Order(12)
  fun testDeleteConnectionWithUniqueUrlWithoutWSandJWS(remoteRobot: RemoteRobot) {
    deleteConnection("invalid connection1", "", "", remoteRobot)
  }

  //TODO: tests should be independent
  /**
   * Tests to delete connection with working set, check that correct message returned.
   */
  @Test
  @Order(13)
  fun testDeleteConnectionWithWorkingSet(remoteRobot: RemoteRobot) = with(remoteRobot) {
    createWorkingSet("WS1", "valid connection1", remoteRobot)
    deleteConnection("valid connection1", "WS1", "", remoteRobot)
  }

  //TODO: tests should be independent
  /**
   * Tests to delete connection with JES working set, check that correct message returned.
   */
  @Test
  @Order(14)
  fun testDeleteConnectionWithJesWorkingSet(remoteRobot: RemoteRobot) = with(remoteRobot) {
    createJesWorkingSet("JWS1", "valid connection2", "testuser", remoteRobot)
    deleteConnection("valid connection2", "", "JWS1", remoteRobot)
  }

  /**
   * Tests to delete connection with working set and JES working set, check that correct message returned.
   */
  @Test
  @Order(15)
  fun testDeleteConnectionWithWSandJWS(remoteRobot: RemoteRobot) = with(remoteRobot) {
    val testUsername = "testuser"
    val connectionName = "testDeleteConnectionWithWSandJWS connection"
    responseDispatcher.injectEndpoint(
      "testAddValidConnection_info",
      { it?.requestLine?.contains("zosmf/info") ?: false },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    responseDispatcher.injectEndpoint(
      "testAddValidConnection_resttopology",
      { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
    createConnection(
      projectName,
      fixtureStack,
      closableFixtureCollector,
      connectionName,
      true,
      remoteRobot,
      "https://${mockServer.hostName}:${mockServer.port}",
      testUsername
    )
    createWorkingSet("WS2", connectionName, remoteRobot)
    createJesWorkingSet("JWS2", connectionName, testUsername, remoteRobot)
    deleteConnection(connectionName, "WS2", "JWS2", remoteRobot)
  }

  /**
   * Deletes connection and check message if connection has any working sets.
   */
  private fun deleteConnection(connectionName: String, wsName: String, jwsName: String, remoteRobot: RemoteRobot) =
    with(remoteRobot) {
      ideFrameImpl(projectName, fixtureStack) {
        explorer {
          settings(closableFixtureCollector, fixtureStack)
        }
        settingsDialog(fixtureStack) {
          configurableEditor {
            conTab.click()
            deleteItem(connectionName)
          }
          if (wsName.isNotEmpty() && jwsName.isNotEmpty()) {
            dialog("Warning") {
              (findAllText()[2].text + findAllText()[4].text).shouldContain("The following Files working sets use selected connections:$wsName.")
              (findAllText()[6].text + findAllText()[8].text).shouldContain("The following JES working sets use selected connections:$jwsName.")
              clickButton("Yes")
            }
          } else if (wsName.isNotEmpty() && jwsName.isEmpty()) {
            dialog("Warning") {
              (findAllText()[2].text + findAllText()[4].text).shouldContain("The following Files working sets use selected connections:$wsName.")
              clickButton("Yes")
            }

          } else if (wsName.isEmpty() && jwsName.isNotEmpty()) {
            dialog("Warning") {
              (findAllText()[2].text + findAllText()[4].text).shouldContain("The following JES working sets use selected connections:$jwsName.")
              clickButton("Yes")
            }
          }
          clickButton("OK")
        }
        closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
      }
    }

  /**
   * Creates JES working set.
   */
  private fun createJesWorkingSet(
    jwsName: String,
    connectionName: String,
    connectionUsername: String,
    remoteRobot: RemoteRobot
  ) =
    with(remoteRobot) {
      ideFrameImpl(projectName, fixtureStack) {
        explorer {
          settings(closableFixtureCollector, fixtureStack)
        }
        settingsDialog(fixtureStack) {
          configurableEditor {
            jesWorkingSetsTab.click()
            addJWS(closableFixtureCollector, fixtureStack)
          }
          addJesWorkingSetDialog(fixtureStack) {
            addJesWorkingSet(jwsName, connectionName, connectionUsername, Triple("*", ZOS_USERID, ""))
            clickButton("OK")
            Thread.sleep(1000)
          }
          closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
          clickButton("OK")
        }
        closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
      }
    }

  /**
   * Creates working set.
   */
  private fun createWorkingSet(wsName: String, connectionName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
      explorer {
        settings(closableFixtureCollector, fixtureStack)
      }
      settingsDialog(fixtureStack) {
        configurableEditor {
          workingSetsTab.click()
          addWS(closableFixtureCollector, fixtureStack)
        }
        addWorkingSetDialog(fixtureStack) {
          addWorkingSet(wsName, connectionName, Pair("$ZOS_USERID.*", "z/OS"))
          clickButton("OK")
          Thread.sleep(1000)
        }
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        clickButton("OK")
      }
      closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
  }
}
