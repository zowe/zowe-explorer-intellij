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

package eu.ibagroup.formainframe.tso.config

import com.intellij.openapi.application.EDT
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import eu.ibagroup.formainframe.common.ui.ValidatingTableView
import eu.ibagroup.formainframe.config.ConfigSandbox
import eu.ibagroup.formainframe.config.ConfigStateV2
import eu.ibagroup.formainframe.config.SandboxListener
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.makeCrudableWithoutListeners
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestConfigSandboxImpl
import eu.ibagroup.formainframe.tso.config.ui.TSOSessionConfigurable
import eu.ibagroup.formainframe.tso.config.ui.TSOSessionDialog
import eu.ibagroup.formainframe.tso.config.ui.TSOSessionDialogState
import eu.ibagroup.formainframe.tso.config.ui.table.TSOSessionTableModel
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.MergedCollections
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.zowe.kotlinsdk.TsoCodePage
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.stream.Stream
import javax.swing.JTextField

class TSOSessionConfigTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
  }

  context("config module: ui") {
    context("TSOSessionConfigurable") {

      val configurable = spyk<TSOSessionConfigurable>()

      lateinit var panelMock: DialogPanel

      var applyCalled = false
      var rollbackCalled = false
      var rowAdded = false
      var rowEdited = false
      var tableModelReinitialized = false

      val panelField = TSOSessionConfigurable::class.java.getDeclaredField("panel")
      panelField.isAccessible = true

      val tableModelMock = mockk<TSOSessionTableModel>()
      every { tableModelMock.addRow(any()) } answers {
        rowAdded = true
      }
      every { tableModelMock[any<Int>()] = any() } answers {
        rowEdited = true
      }
      every { tableModelMock.reinitialize() } answers {
        tableModelReinitialized = true
      }

      val tableModelField = TSOSessionConfigurable::class.java.getDeclaredField("tableModel")
      tableModelField.isAccessible = true

      val addSessionMethod = TSOSessionConfigurable::class.java.getDeclaredMethod("addSession")
      addSessionMethod.isAccessible = true

      val tableMock = mockk<ValidatingTableView<*>>()
      every { tableMock.selectedObject } returns TSOSessionDialogState()
      every { tableMock.selectedRow } returns 0

      val tableField = TSOSessionConfigurable::class.java.getDeclaredField("table")
      tableField.isAccessible = true

      val editSessionMethod = TSOSessionConfigurable::class.java.getDeclaredMethod("editSession")
      editSessionMethod.isAccessible = true

      val registerMouseListenerMethod = TSOSessionConfigurable::class.java.getDeclaredMethod("registerMouseListener")
      registerMouseListenerMethod.isAccessible = true

      val mouseEvent = mockk<MouseEvent>()

      val configSandbox = ConfigSandbox.getService() as TestConfigSandboxImpl

      beforeEach {
        panelMock = mockk<DialogPanel>()
        every { panelMock.updateUI() } returns Unit

        applyCalled = false
        rollbackCalled = false
        rowAdded = false
        rowEdited = false
        tableModelReinitialized = false

        every { configSandbox.crudable.nextUniqueValue<TSOSessionConfig, String>(TSOSessionConfig::class.java) } returns "uuid"
        every {
          configSandbox.crudable.getByUniqueKey(
            ConnectionConfig::class.java,
            any<String>()
          )
        } returns Optional.empty()
        every { configSandbox.crudable.getAll(TSOSessionConfig::class.java) } answers {
          Stream.of()
        }
        every { configSandbox.crudable.getAll(ConnectionConfig::class.java) } answers {
          Stream.of()
        }

        configSandbox.testInstance = object : TestConfigSandboxImpl() {
          override fun <T : Any> apply(clazz: Class<out T>) {
            applyCalled = true
          }

          override fun <T> rollback(clazz: Class<out T>) {
            rollbackCalled = true
          }

          override fun <T> isModified(clazz: Class<out T>): Boolean {
            return true
          }
        }

        configurable.createComponent()

        panelField.set(configurable, panelMock)

        mockkStatic(::initialize)
        every { initialize(any()) } returns Unit

        mockkConstructor(TSOSessionDialog::class)
        every { anyConstructed<TSOSessionDialog>().showAndGet() } returns true

        tableModelField.set(configurable, tableModelMock)

        tableField.set(configurable, tableMock)
      }

      afterEach {
        unmockkAll()
      }

      // apply
      should("apply changes when sandbox is modified") {
        configurable.apply()

        verify { panelMock.updateUI() }
        assertSoftly {
          applyCalled shouldBe true
        }
      }
      should("apply changes when sandbox is not modified") {
        configSandbox.testInstance = object : TestConfigSandboxImpl() {
          override fun <T : Any> apply(clazz: Class<out T>) {
            applyCalled = true
          }

          override fun <T> rollback(clazz: Class<out T>) {
            rollbackCalled = true
          }

          override fun <T> isModified(clazz: Class<out T>): Boolean {
            return false
          }
        }

        configurable.apply()

        verify(exactly = 0) { panelMock.updateUI() }
        assertSoftly {
          applyCalled shouldBe true
        }
      }
      // reset
      should("reset changes when sandbox is modified") {
        configurable.reset()

        verify { panelMock.updateUI() }
        assertSoftly {
          rollbackCalled shouldBe true
        }
      }
      should("reset changes when sandbox is not modified") {
        configSandbox.testInstance = object : TestConfigSandboxImpl() {
          override fun <T : Any> apply(clazz: Class<out T>) {
            applyCalled = true
          }

          override fun <T> rollback(clazz: Class<out T>) {
            rollbackCalled = true
          }

          override fun <T> isModified(clazz: Class<out T>): Boolean {
            return false
          }
        }

        configurable.reset()

        verify(exactly = 0) { panelMock.updateUI() }
        assertSoftly {
          rollbackCalled shouldBe true
        }
      }
      // cancel
      should("cancel changes") {
        configurable.cancel()

        verify { panelMock.updateUI() }
        assertSoftly {
          rollbackCalled shouldBe true
        }
      }
      // addSession
      should("add session when 'Ok' button is pressed") {
        runBlocking {
          withContext(Dispatchers.EDT) {
            addSessionMethod.invoke(configurable)
          }
        }

        assertSoftly {
          rowAdded shouldBe true
        }
      }
      should("do not add session when 'Cancel' button is pressed") {
        every { anyConstructed<TSOSessionDialog>().showAndGet() } returns false

        runBlocking {
          withContext(Dispatchers.EDT) {
            addSessionMethod.invoke(configurable)
          }
        }

        assertSoftly {
          rowAdded shouldBe false
        }
      }
      // editSession
      should("edit session when 'Ok' button is pressed") {
        runBlocking {
          withContext(Dispatchers.EDT) {
            editSessionMethod.invoke(configurable)
          }
        }

        assertSoftly {
          rowEdited shouldBe true
        }
      }
      should("do not edit session when 'Cancel' button is pressed") {
        every { anyConstructed<TSOSessionDialog>().showAndGet() } returns false

        runBlocking {
          withContext(Dispatchers.EDT) {
            editSessionMethod.invoke(configurable)
          }
        }

        assertSoftly {
          rowEdited shouldBe false
        }
      }
      should("do not edit session if selected object is null") {
        every { tableMock.selectedObject } returns null
        tableField.set(configurable, tableMock)

        runBlocking {
          withContext(Dispatchers.EDT) {
            editSessionMethod.invoke(configurable)
          }
        }

        assertSoftly {
          rowEdited shouldBe false
        }
      }
      // registerMouseListener
      should("single-click on a row in the TSO session table") {
        var editSessionCalled = false
        every { configurable["editSession"]() } answers {
          editSessionCalled = true
          Unit
        }
        every { mouseEvent.clickCount } returns 1

        val mouseAdapter = registerMouseListenerMethod.invoke(configurable) as MouseAdapter
        mouseAdapter.mouseClicked(mouseEvent)

        assertSoftly {
          editSessionCalled shouldBe false
        }
        clearMocks(configurable)
      }
      should("double-click on a row in the TSO session table") {
        var editSessionCalled = false
        every { configurable["editSession"]() } answers {
          editSessionCalled = true
          Unit
        }
        every { mouseEvent.clickCount } returns 2

        val mouseAdapter = registerMouseListenerMethod.invoke(configurable) as MouseAdapter
        mouseAdapter.mouseClicked(mouseEvent)

        assertSoftly {
          editSessionCalled shouldBe true
        }
        clearMocks(configurable)
      }
      // addSandboxListener
      should("catch reload call and reinitialize table model") {
        sendTopic(SandboxListener.TOPIC).reload(TSOSessionConfig::class.java)

        assertSoftly {
          tableModelReinitialized shouldBe true
        }
      }
      should("catch reload call and do not reinitialize table model") {
        sendTopic(SandboxListener.TOPIC).reload(ConnectionConfig::class.java)

        assertSoftly {
          tableModelReinitialized shouldBe false
        }
      }
    }

    context("table/TSOSessionTableModel") {

      lateinit var tableModel: TSOSessionTableModel

      val crudableMock = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })
      every { crudableMock.getAll(TSOSessionConfig::class.java) } answers {
        Stream.of(TSOSessionConfig())
      }

      val connectionConfig = ConnectionConfig()
      connectionConfig.name = "connectionName"
      every {
        crudableMock.getByUniqueKey(ConnectionConfig::class.java, any<String>())
      } returns Optional.of(connectionConfig)
      every { crudableMock.getAll(ConnectionConfig::class.java) } answers {
        Stream.of(connectionConfig)
      }

      beforeEach {
        tableModel = TSOSessionTableModel(crudableMock)
      }

      afterEach {
        unmockkAll()
      }

      // fetch
      should("fetch TSO sessions") {
        val list = tableModel.fetch(crudableMock)

        verify { crudableMock.getAll(TSOSessionConfig::class.java) }
        assertSoftly {
          list.size shouldBe 1
        }
      }
      // onApplyingMergedCollection
      should("merge new rows into crudable") {
        every { crudableMock.applyMergedCollections(TSOSessionConfig::class.java, any()) } returns Unit

        tableModel.onApplyingMergedCollection(crudableMock, MergedCollections(emptyList(), emptyList(), emptyList()))

        verify { crudableMock.applyMergedCollections(TSOSessionConfig::class.java, any()) }
      }
      // onDelete
      should("delete session from crudable") {
        every { crudableMock.delete(any()) } returns Optional.empty()

        tableModel.onDelete(crudableMock, TSOSessionDialogState())

        verify { crudableMock.delete(any()) }
      }
      // onUpdate
      should("update session in crudable if it exists") {
        val state = TSOSessionDialogState()
        every { crudableMock.update(any()) } returns Optional.of(state)

        val result = tableModel.onUpdate(crudableMock, state)

        verify { crudableMock.update(any()) }
        assertSoftly {
          result shouldBe true
        }
      }
      should("do not update session in crudable if it does not exist") {
        every { crudableMock.update(any()) } returns null

        val result = tableModel.onUpdate(crudableMock, TSOSessionDialogState())

        verify { crudableMock.update(any()) }
        assertSoftly {
          result shouldBe false
        }
      }
      // onAdd
      should("add session to crudable if it is returned") {
        val state = TSOSessionDialogState()
        every { crudableMock.add(any()) } returns Optional.of(state)

        val result = tableModel.onAdd(crudableMock, state)

        verify { crudableMock.add(any()) }
        assertSoftly {
          result shouldBe true
        }
      }
      should("do not add session to crudable if it is not returned") {
        every { crudableMock.add(any()) } returns null

        val result = tableModel.onAdd(crudableMock, TSOSessionDialogState())

        verify { crudableMock.add(any()) }
        assertSoftly {
          result shouldBe false
        }
      }
      // set
      should("set row to table model") {
        tableModel[0] = TSOSessionDialogState()

        verify { crudableMock.getByUniqueKey(ConnectionConfig::class.java, any<String>()) }
      }
      should("set row to table model when connection config is not received") {
        every { crudableMock.getByUniqueKey(ConnectionConfig::class.java, any<String>()) } returns Optional.empty()

        tableModel[0] = TSOSessionDialogState()

        verify { crudableMock.getByUniqueKey(ConnectionConfig::class.java, any<String>()) }
      }
    }

    context("TSOSessionDialog") {

      lateinit var dialog: TSOSessionDialog

      val crudableMock = mockk<Crudable>()
      every { crudableMock.getAll(ConnectionConfig::class.java) } answers {
        Stream.of(ConnectionConfig())
      }
      every {
        crudableMock.getByUniqueKey(ConnectionConfig::class.java, any<String>())
      } returns Optional.of(ConnectionConfig())

      val createCenterPanelMethod = DialogWrapper::class.java.getDeclaredMethod("createCenterPanel")
      createCenterPanelMethod.isAccessible = true

      val codepageComboBoxModelField = TSOSessionDialog::class.java.getDeclaredField("codepageComboBoxModel")
      codepageComboBoxModelField.isAccessible = true

      val doValidateMethod = DialogWrapper::class.java.getDeclaredMethod("doValidate")
      doValidateMethod.isAccessible = true

      val resetToDefaultMethod = TSOSessionDialog::class.java.getDeclaredMethod("resetToDefault")
      resetToDefaultMethod.isAccessible = true

      val sessionNameField = TSOSessionDialog::class.java.getDeclaredField("sessionNameField")
      sessionNameField.isAccessible = true

      val logonProcField = TSOSessionDialog::class.java.getDeclaredField("logonProcField")
      logonProcField.isAccessible = true

      val getPreferredFocusedComponentMethod =
        DialogWrapper::class.java.getDeclaredMethod("getPreferredFocusedComponent")
      getPreferredFocusedComponentMethod.isAccessible = true

      beforeEach {
        mockkStatic(::initialize)
        every { initialize(any()) } returns Unit

        dialog = runBlocking {
          withContext(Dispatchers.EDT) {
            TSOSessionDialog(crudableMock, TSOSessionDialogState())
          }
        }

        mockkStatic("eu.ibagroup.formainframe.utils.ValidationFunctionsKt")
        every { validateForBlank(any()) } returns null
        every { validateTsoSessionName(any(), any(), any()) } returns null
        every { validateConnectionSelection(any()) } returns null
        every { validateForPositiveInteger(any()) } returns null
        every { validateForPositiveLong(any()) } returns null
      }

      afterEach {
        unmockkAll()
      }

      // createCenterPanel
      should("create panel") {
        val panel = createCenterPanelMethod.invoke(dialog) as? JBScrollPane

        assertSoftly {
          panel shouldNotBe null
        }
      }
      should("create panel when connection config is not received") {
        every {
          crudableMock.getByUniqueKey(ConnectionConfig::class.java, any<String>())
        } returns Optional.empty()

        val panel = createCenterPanelMethod.invoke(dialog) as? JBScrollPane

        assertSoftly {
          panel shouldNotBe null
        }
      }
      should("create panel when no connection configs are found") {
        every {
          crudableMock.getByUniqueKey(ConnectionConfig::class.java, any<String>())
        } returns Optional.empty()
        every { crudableMock.getAll(ConnectionConfig::class.java) } answers {
          Stream.of()
        }

        val panel = createCenterPanelMethod.invoke(dialog) as? JBScrollPane

        assertSoftly {
          panel shouldNotBe null
        }
      }
      // doValidate
      should("validate panel") {
        val result = doValidateMethod.invoke(dialog)

        assertSoftly {
          result shouldBe null
        }
      }
      should("validate panel if fields are empty") {
        val exceptedMessage = "This field must not be blank"
        every { validateForBlank(any()) } returns ValidationInfo(exceptedMessage)

        val result = doValidateMethod.invoke(dialog) as? ValidationInfo

        assertSoftly {
          result?.message shouldBe exceptedMessage
        }
      }
      should("validate panel if session name is not empty") {
        val state = TSOSessionDialogState(name = "sessionName")
        dialog = runBlocking {
          withContext(Dispatchers.EDT) {
            TSOSessionDialog(crudableMock, state)
          }
        }

        val result = doValidateMethod.invoke(dialog)

        assertSoftly {
          result shouldBe null
        }
      }
      // resetToDefault
      should("reset field values to default") {
        val expectedSessionName = "sessionName"
        val defaultLogonProc = "DBSPROCC"

        val state = TSOSessionDialogState(name = expectedSessionName, logonProcedure = "logonProc")
        dialog = runBlocking {
          withContext(Dispatchers.EDT) {
            TSOSessionDialog(crudableMock, state)
          }
        }

        createCenterPanelMethod.invoke(dialog)
        resetToDefaultMethod.invoke(dialog)

        val sessionName = sessionNameField.get(dialog) as? JTextField
        val logonProc = logonProcField.get(dialog) as? JTextField

        assertSoftly {
          sessionName?.text shouldBe expectedSessionName
          logonProc?.text shouldBe defaultLogonProc
        }
      }
      // getPreferredFocusedComponent
      should("get preferred focused component") {
        val preferredFocusedComponent = getPreferredFocusedComponentMethod.invoke(dialog)

        val sessionName = sessionNameField.get(dialog) as? JTextField

        assertSoftly {
          preferredFocusedComponent shouldBe sessionName
        }
      }
      should("get preferred focused component when main panel focused component is null") {
        val mainPanel = spyk(DialogPanel())
        every { mainPanel.preferredFocusedComponent } returns null

        mockkStatic(::panel)
        every { panel(any()) } returns mainPanel

        dialog = runBlocking {
          withContext(Dispatchers.EDT) {
            TSOSessionDialog(crudableMock, TSOSessionDialogState())
          }
        }

        createCenterPanelMethod.invoke(dialog)

        val preferredFocusedComponent = getPreferredFocusedComponentMethod.invoke(dialog)

        assertSoftly {
          preferredFocusedComponent shouldBe null
        }
      }

    }
  }

  context("TSOSessionConfig") {

    // equals
    should("check whether two files are equal") {
      val tsoSessionConfigA = TSOSessionConfig()
      val tsoSessionConfigB = TSOSessionConfig()

      val resultA = tsoSessionConfigA.equals(tsoSessionConfigA)
      val resultB = tsoSessionConfigA.equals(tsoSessionConfigB)

      assertSoftly {
        resultA shouldBe true
        resultB shouldBe true
      }
    }
    should("check whether two files are not equal") {
      val tsoSessionConfigA = TSOSessionConfig()
      val tsoSessionConfigB = TSOSessionConfig()

      tsoSessionConfigB.name = "name"
      val resultA = tsoSessionConfigA.equals(tsoSessionConfigB)

      tsoSessionConfigA.name = "name"
      tsoSessionConfigB.connectionConfigUuid = "connectionConfigUuid"
      val resultB = tsoSessionConfigA.equals(tsoSessionConfigB)

      tsoSessionConfigA.connectionConfigUuid = "connectionConfigUuid"
      tsoSessionConfigB.logonProcedure = "logonProcedure"
      val resultC = tsoSessionConfigA.equals(tsoSessionConfigB)

      tsoSessionConfigA.logonProcedure = "logonProcedure"
      tsoSessionConfigB.charset = "charset"
      val resultD = tsoSessionConfigA.equals(tsoSessionConfigB)

      tsoSessionConfigA.charset = "charset"
      tsoSessionConfigB.codepage = TsoCodePage.IBM_1025
      val resultE = tsoSessionConfigA.equals(tsoSessionConfigB)

      tsoSessionConfigA.codepage = TsoCodePage.IBM_1025
      tsoSessionConfigB.rows = 24
      val resultF = tsoSessionConfigA.equals(tsoSessionConfigB)

      tsoSessionConfigA.rows = 24
      tsoSessionConfigB.columns = 80
      val resultG = tsoSessionConfigA.equals(tsoSessionConfigB)

      tsoSessionConfigA.columns = 80
      tsoSessionConfigB.accountNumber = "accountNumber"
      val resultH = tsoSessionConfigA.equals(tsoSessionConfigB)

      tsoSessionConfigA.accountNumber = "accountNumber"
      tsoSessionConfigB.userGroup = "userGroup"
      val resultI = tsoSessionConfigA.equals(tsoSessionConfigB)

      tsoSessionConfigA.userGroup = "userGroup"
      tsoSessionConfigB.regionSize = 64000
      val resultJ = tsoSessionConfigA.equals(tsoSessionConfigB)

      tsoSessionConfigA.regionSize = 64000
      tsoSessionConfigB.timeout = 10
      val resultK = tsoSessionConfigA.equals(tsoSessionConfigB)

      tsoSessionConfigA.timeout = 10L
      tsoSessionConfigB.maxAttempts = 3
      val resultL = tsoSessionConfigA.equals(tsoSessionConfigB)

      assertSoftly {
        resultA shouldBe false
        resultB shouldBe false
        resultC shouldBe false
        resultD shouldBe false
        resultE shouldBe false
        resultF shouldBe false
        resultG shouldBe false
        resultH shouldBe false
        resultI shouldBe false
        resultJ shouldBe false
        resultK shouldBe false
        resultL shouldBe false
      }
    }
    // hashCode
    should("check hashcode for equality") {
      val tsoSessionConfigA = TSOSessionConfig()
      val tsoSessionConfigB = TSOSessionConfig()
      val hashCodeA = tsoSessionConfigA.hashCode()
      val hashCodeB = tsoSessionConfigB.hashCode()

      assertSoftly {
        hashCodeA shouldBe hashCodeB
      }
    }
    // toString

    // TSOSessionConfig.toDialogState
    should("convert TSO session config to dialog state") {
      val tsoSessionConfig = TSOSessionConfig(
        "uuid",
        "name",
        "connectionConfigUuid",
        "logonProcedure",
        "charset",
        TsoCodePage.IBM_1025,
        24,
        80,
        "accountNumber",
        "userGroup",
        64000,
        10L,
        3
      )

      val tsoDialogState = tsoSessionConfig.toDialogState()

      assertSoftly {
        tsoSessionConfig.uuid shouldBe tsoDialogState.uuid
        tsoSessionConfig.name shouldBe tsoDialogState.name
        tsoSessionConfig.connectionConfigUuid shouldBe tsoDialogState.connectionConfigUuid
        tsoSessionConfig.logonProcedure shouldBe tsoDialogState.logonProcedure
        tsoSessionConfig.charset shouldBe tsoDialogState.charset
        tsoSessionConfig.codepage shouldBe tsoDialogState.codepage
        tsoSessionConfig.rows shouldBe tsoDialogState.rows
        tsoSessionConfig.columns shouldBe tsoDialogState.columns
        tsoSessionConfig.accountNumber shouldBe tsoDialogState.accountNumber
        tsoSessionConfig.userGroup shouldBe tsoDialogState.userGroup
        tsoSessionConfig.regionSize shouldBe tsoDialogState.regionSize
        tsoSessionConfig.timeout shouldBe tsoDialogState.timeout
        tsoSessionConfig.maxAttempts shouldBe tsoDialogState.maxAttempts
      }

    }

  }

  context("TSOSessionConfigDeclaration") {

    val crudableMock = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })
    every { crudableMock.getAll(TSOSessionConfig::class.java) } answers {
      Stream.of(TSOSessionConfig())
    }

    val configDeclaration = TSOSessionConfigDeclaration(crudableMock)

    // ConfigDecider.canUpdate
    should("check if the config can be updated") {
      val decider = configDeclaration.getDecider()

      val sessionConfigA = TSOSessionConfig()
      val sessionConfigB = TSOSessionConfig()
      val resultA = decider.canUpdate(sessionConfigA, sessionConfigB)

      sessionConfigA.name = "name"
      val resultB = decider.canUpdate(sessionConfigA, sessionConfigB)

      sessionConfigB.name = "name"
      val resultC = decider.canUpdate(sessionConfigA, sessionConfigB)

      assertSoftly {
        resultA shouldBe true
        resultB shouldBe false
        resultC shouldBe true
      }
    }
    // getConfigurable
    should("get configurable") {
      val configurable = configDeclaration.getConfigurable()

      assertSoftly {
        configurable shouldNotBe null
      }
    }
  }

})
