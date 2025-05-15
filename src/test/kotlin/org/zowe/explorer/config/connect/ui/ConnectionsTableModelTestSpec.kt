/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Dzianis Lisiankou
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.config.connect.ui

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.zowe.explorer.config.*
import org.zowe.explorer.config.connect.*
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionDialogState
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionsTableModel
import org.zowe.explorer.config.connect.ui.zosmf.initEmptyUuids
import org.zowe.explorer.config.makeCrudableWithoutListeners
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable

class ConnectionsTableModelTestSpec : AppInitShouldSpec("config/connect/ui/ConnectionsTableModel", {
  context("all functions") {
    lateinit var crudable: Crudable
    lateinit var connTab: ConnectionsTableModel

    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

    val connectionConfig = mockk<ConnectionConfig> {
      every { uuid } returns "fake_uuid"
      every { owner } returns ""
    }

    val connectionDialogState = ConnectionDialogState(
      connectionName = "a",
      connectionUrl = "https://a.com",
      username = CredentialService.getUsername(connectionConfig),
      password = CredentialService.getPassword(connectionConfig),
      owner = CredentialService.getOwner(connectionConfig)
    )

    beforeEach {
      every { connectionConfig.uuid } returns "fake_uuid"

      val configCollections: MutableMap<String, MutableList<*>> = mutableMapOf(
        Pair(ConnectionConfig::class.java.name, mutableListOf<ConnectionConfig>()),
        Pair(FilesWorkingSetConfig::class.java.name, mutableListOf<ConnectionConfig>()),
        Pair(JesWorkingSetConfig::class.java.name, mutableListOf<ConnectionConfig>()),
      )
      val sandboxState = SandboxState(ConfigStateV2(configCollections))

      crudable =
        makeCrudableWithoutListeners(true, { sandboxState.credentials }) { sandboxState.configState }
      connectionDialogState.initEmptyUuids(crudable)
      connTab = ConnectionsTableModel(crudable)

      val configServiceImpl = ConfigService.getService()
      every {
        configServiceImpl.getConfigDeclaration(any<Class<*>>())
      } answers {
        when (val rowClass = firstArg<Class<*>>()) {
          ConnectionConfig::class.java -> ZOSMFConnectionConfigDeclaration() as ConfigDeclaration<Any>
          Credentials::class.java -> CredentialsConfigDeclaration() as ConfigDeclaration<Any>
          else -> fail("Unknown config class: $rowClass")
        }
      }
    }

    context("fetch") {
      should("fetch connections from crudable") {
        connTab.addRow(connectionDialogState)

        val actual = connTab.fetch(crudable)
        val expected = mutableListOf(connectionDialogState)

        assertSoftly {
          actual shouldBe expected
        }
      }
    }

    context("onAdd") {
      should("add connection to crudable") {
        val connectionDialogStateB = ConnectionDialogState(
          connectionName = "b",
          connectionUrl = "https://b.com",
          username = CredentialService.getUsername(connectionConfig),
          password = CredentialService.getPassword(connectionConfig),
          owner = CredentialService.getOwner(connectionConfig)
        )
        connectionDialogStateB.initEmptyUuids(crudable)

        connTab.onAdd(crudable, connectionDialogState)
        connTab.onAdd(crudable, connectionDialogStateB)

        val actual = connTab.fetch(crudable)
        val expected = mutableListOf(connectionDialogState, connectionDialogStateB)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("add connection with existing name") {
        val connectionDialogStateB = ConnectionDialogState(connectionName = connectionDialogState.connectionName)
        connectionDialogStateB.initEmptyUuids(crudable)

        connTab.onAdd(crudable, connectionDialogState)
        connTab.onAdd(crudable, connectionDialogStateB)

        val actual = connTab.fetch(crudable)
        val expected = mutableListOf(connectionDialogState)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("add connection with existing url") {
        val connectionDialogStateB = ConnectionDialogState(
          connectionUrl = connectionDialogState.connectionUrl,
          username = CredentialService.getUsername(connectionConfig),
          password = CredentialService.getPassword(connectionConfig),
          owner = CredentialService.getOwner(connectionConfig),
        )
        connectionDialogStateB.initEmptyUuids(crudable)

        connTab.onAdd(crudable, connectionDialogState)
        connTab.onAdd(crudable, connectionDialogStateB)

        val actual = connTab.fetch(crudable)
        val expected = mutableListOf(connectionDialogState, connectionDialogStateB)

        assertSoftly {
          actual shouldBe expected
        }
      }
    }

    context("onDelete") {
      should("delete connection from crudable") {
        connTab.onAdd(crudable, connectionDialogState)
        connTab.onDelete(crudable, connectionDialogState)

        val actual = connTab.fetch(crudable)
        val expected = mutableListOf<ConnectionDialogState>()

        assertSoftly {
          actual shouldBe expected
        }
      }
    }

    context("set") {
      should("set connection to crudable") {
        connTab.addRow(ConnectionDialogState().initEmptyUuids(crudable))
        connTab[0] = connectionDialogState

        assertSoftly {
          connTab[0].connectionName shouldBe connectionDialogState.connectionName
          connTab[0].connectionUrl shouldBe connectionDialogState.connectionUrl
          connTab[0].username shouldBe connectionDialogState.username
          connTab[0].password shouldBe connectionDialogState.password
          connTab[0].connectionUuid shouldNotBe connectionDialogState.connectionUuid
        }
      }
    }
  }
})