/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.ZOSMFConnectionConfigDeclaration
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.connect.CredentialsConfigDeclaration
import eu.ibagroup.formainframe.config.connect.ui.zosmf.ConnectionDialogState
import eu.ibagroup.formainframe.config.connect.ui.zosmf.ConnectionsTableModel
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject

class ConfigTestSpec : ShouldSpec({
  context("config module: connect") {
    context("ui/ConnectionsTableModel") {

      lateinit var crudable: Crudable
      lateinit var connTab: ConnectionsTableModel

      fun mockConfigService() {
        val mockConfigServiceInstance = mockk<ConfigService>()
        every { mockConfigServiceInstance.getConfigDeclaration(ConnectionConfig::class.java) } returns ZOSMFConnectionConfigDeclaration(crudable)
        every { mockConfigServiceInstance.getConfigDeclaration(Credentials::class.java) } returns CredentialsConfigDeclaration(crudable)

        mockkObject(ConfigService)
        every { ConfigService.instance } returns mockConfigServiceInstance
      }

      beforeEach {
        val configCollections: MutableMap<String, MutableList<*>> = mutableMapOf(
          Pair(ConnectionConfig::class.java.name, mutableListOf<ConnectionConfig>()),
          Pair(FilesWorkingSetConfig::class.java.name, mutableListOf<ConnectionConfig>()),
          Pair(JesWorkingSetConfig::class.java.name, mutableListOf<ConnectionConfig>()),
        )
        val sandboxState = SandboxState(ConfigStateV2(configCollections))

        crudable =
          makeCrudableWithoutListeners(true, { sandboxState.credentials }) { sandboxState.configState }
        connTab = ConnectionsTableModel(crudable)
        mockConfigService()
      }


      val connectionDialogState = ConnectionDialogState(
        connectionName = "a", connectionUrl = "https://a.com", username = "a", password = "a"
      )

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
            connectionName = "b", connectionUrl = "https://b.com", username = "b", password = "b"
          )

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

          connTab.onAdd(crudable, connectionDialogState)
          connTab.onAdd(crudable, connectionDialogStateB)

          val actual = connTab.fetch(crudable)
          val expected = mutableListOf(connectionDialogState)

          assertSoftly {
            actual shouldBe expected
          }
        }
        should("add connection with existing url") {

          val connectionDialogStateB = ConnectionDialogState(connectionUrl = connectionDialogState.connectionUrl)

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

          connTab.addRow(ConnectionDialogState())
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
    context("Credentials.hashCode") {
      should("check hashcode for uniqueness") {
        val credentials = Credentials("uuid", "username", "password")
        val credentials2 = Credentials("uuid", "username", "password")
        val hashcode = credentials.hashCode()
        val hashcode2 = credentials2.hashCode()

        assertSoftly {
          hashcode shouldNotBe hashcode2
        }
      }
    }
  }
  context("config module: xmlUtils") {
    // get
    should("get XML child element by tag") {}
    // toElementList
    should("convert node list to elements list") {}
  }
  context("config module: ConfigSandboxImpl") {
    // apply
    should("apply changes of the config sandbox") {}
    // rollback
    should("rollback all the changes of the config sandbox") {}
    // isModified
    should("check if the sandbox is modified") {}
  }
  context("config module: ws") {
    // WSNameColumn.validateEntered
    should("check that the entered working set name is not empty") {}
    should("check that the entered working set name is not blank") {}
    // jes/JesWsDialog.validateOnApply
    should("check that there are no errors for job filters") {}
    should("check that the error appears on any errors for job filters") {}
    should("check that the error appears on empty JES working set") {}
    should("check that the error appears on adding the same job filter again") {}
    // files/WorkingSetDialog.validateOnApply
    should("check that there are no errors for file masks") {}
    should("check that the error appears on any errors for file masks") {}
    should("check that the error appears on empty file working set") {}
    should("check that the error appears on adding the same file mask again") {}
  }
})
