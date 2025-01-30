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

package eu.ibagroup.formainframe.config

import com.intellij.openapi.application.EDT
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import eu.ibagroup.formainframe.config.connect.*
import eu.ibagroup.formainframe.config.connect.ui.zosmf.ConnectionDialogState
import eu.ibagroup.formainframe.config.connect.ui.zosmf.ConnectionsTableModel
import eu.ibagroup.formainframe.config.connect.ui.zosmf.initEmptyUuids
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsDialog
import eu.ibagroup.formainframe.config.ws.ui.FilesWorkingSetDialogState
import eu.ibagroup.formainframe.config.ws.ui.files.FilesWorkingSetDialog
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestConfigServiceImpl
import eu.ibagroup.formainframe.utils.crudable.Crudable
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*
import java.util.stream.Stream
import javax.swing.JComponent

class ConfigTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("config module: connect") {
    context("ui/ConnectionsTableModel") {

      lateinit var crudable: Crudable
      lateinit var connTab: ConnectionsTableModel

      val connectionConfig = mockk<ConnectionConfig>()
      every { connectionConfig.uuid } returns "fake_uuid"
      every { connectionConfig.owner } returns ""

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

        val configServiceImpl = ConfigService.getService() as TestConfigServiceImpl
        configServiceImpl.testInstance = object : TestConfigServiceImpl() {
          override fun <T : Any> getConfigDeclaration(rowClass: Class<out T>): ConfigDeclaration<T> {
            return when (rowClass) {
              ConnectionConfig::class.java -> ZOSMFConnectionConfigDeclaration() as ConfigDeclaration<T>
              Credentials::class.java -> CredentialsConfigDeclaration() as ConfigDeclaration<T>
              else -> super.getConfigDeclaration(rowClass)
            }
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
    context("Credentials.hashCode") {
      should("check hashcode for uniqueness") {
        val credentials = Credentials("uuid", "username", "password".toCharArray())
        val credentials2 = Credentials("uuid", "username", "password".toCharArray())
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

    lateinit var crudableMockk: Crudable

    beforeEach {
      mockkObject(AbstractWsDialog)
      every { AbstractWsDialog["initialize"](any<() -> Unit>()) } returns Unit

      crudableMockk = mockk<Crudable>()
      every { crudableMockk.getAll(ConnectionConfig::class.java) } returns Stream.of()
      every {
        crudableMockk.getByUniqueKey(ConnectionConfig::class.java, any<String>())
      } returns Optional.of(ConnectionConfig())

      mockkConstructor(DialogPanel::class)
      every { anyConstructed<DialogPanel>().registerValidators(any(), any()) } answers {
        val componentValidityChangedCallback = secondArg<(Map<JComponent, ValidationInfo>) -> Unit>()
        componentValidityChangedCallback(mapOf())
      }
    }

    afterEach {
      unmockkAll()
      clearAllMocks()
    }

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
    // ui/AbstractWsDialog.init
    should("check that OK action is enabled if validation map is empty") {

      val dialog = runBlocking {
        withContext(Dispatchers.EDT) {
          FilesWorkingSetDialog(crudableMockk, FilesWorkingSetDialogState())
        }
      }

      verify { anyConstructed<DialogPanel>().registerValidators(any(), any()) }
      assertSoftly { dialog.isOKActionEnabled shouldBe true }
    }
  }
})
