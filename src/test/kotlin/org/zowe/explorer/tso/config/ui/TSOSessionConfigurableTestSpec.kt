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

package org.zowe.explorer.tso.config.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.DslConfigurableBase
import com.intellij.openapi.ui.DialogPanel
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import org.zowe.explorer.common.ui.ValidatingTableView
import org.zowe.explorer.config.ConfigSandbox
import org.zowe.explorer.config.SandboxListener
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.testutils.getPrivateFieldValue
import org.zowe.explorer.testutils.setPrivateFieldValue
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.tso.config.ui.table.TSOSessionTableModel
import org.zowe.explorer.utils.sendTopic
import java.awt.event.MouseAdapter
import java.util.*

class TSOSessionConfigurableTestSpec : AppInitShouldSpec("tso/config/ui/TSOSessionConfigurable", {
  val registerMouseListenerMethod = TSOSessionConfigurable::class.java.getDeclaredMethod("registerMouseListener")
  registerMouseListenerMethod.isAccessible = true
  val addSandboxListenerMethod = TSOSessionConfigurable::class.java.getDeclaredMethod("addSandboxListener")
  addSandboxListenerMethod.isAccessible = true
  val addSessionMethod = TSOSessionConfigurable::class.java.getDeclaredMethod("addSession")
  addSessionMethod.isAccessible = true

  mockkConstructor(TSOSessionDialog::class)

  lateinit var configSandboxServiceMock: ConfigSandbox

  beforeEach {
    configSandboxServiceMock = ConfigSandbox.getService()
    every { configSandboxServiceMock.apply(any<Class<*>>()) } returns mockk()
    every { configSandboxServiceMock.rollback(any<Class<*>>()) } returns mockk()
    every { configSandboxServiceMock.crudable } returns mockk {
      every {
        getAll(any<Class<*>>())
      } answers {
        emptyList<Any>().stream()
      }
      every { getByUniqueKey(any<Class<*>>(), any<Any>()) } returns Optional.ofNullable(null)
      every { nextUniqueValue<TSOSessionConfig, String>(TSOSessionConfig::class.java) } returns "test"
    }
  }

  context("createPanel") {
    val tsoSessionConfigurable = spyk<TSOSessionConfigurable>()
    setPrivateFieldValue(
      tsoSessionConfigurable,
      "disposable",
      mockk<Disposable>(),
      DslConfigurableBase::class.java
    )

    should("create a new panel and set it to the instance as the current") {
      val result = tsoSessionConfigurable.createPanel()

      val initPanel = getPrivateFieldValue(tsoSessionConfigurable, "panel")

      result.apply()

      assertSoftly { result shouldBeSameInstanceAs initPanel }
    }
  }

  context("registerMouseListener") {
    var didSetTableModel = false
    var didTriggerEditSession = false

    val tableViewMock = mockk<ValidatingTableView<TSOSessionDialogState>> {
      every { selectedRow } returns 1234
    }

    val tableModelMock = mockk<TSOSessionTableModel> {
      every {
        set(1234, any())
      } answers {
        didSetTableModel = true
      }
    }

    val tsoSessionConfigurable = spyk<TSOSessionConfigurable>()
    setPrivateFieldValue(tsoSessionConfigurable, "table", tableViewMock)
    setPrivateFieldValue(tsoSessionConfigurable, "tableModel", tableModelMock)

    beforeEach {
      didSetTableModel = false
      didTriggerEditSession = false

      every {
        tableViewMock.selectedObject
      } answers {
        didTriggerEditSession = true
        mockk()
      }
    }

    should("react on a mouse double click and set a new edited session state after it is edited") {
      every { anyConstructed<TSOSessionDialog>().showAndGet() } returns true

      val mouseAdapter = registerMouseListenerMethod.invoke(tsoSessionConfigurable) as MouseAdapter
      ApplicationManager.getApplication().invokeAndWait {
        mouseAdapter.mouseClicked(mockk { every { clickCount } returns 2 })
      }

      assertSoftly { didTriggerEditSession shouldBe true }
      assertSoftly { didSetTableModel shouldBe true }
    }
    should("react on a mouse double click and not set a new edited session state cause the dialog is canceled") {
      every { anyConstructed<TSOSessionDialog>().showAndGet() } returns false

      val mouseAdapter = registerMouseListenerMethod.invoke(tsoSessionConfigurable) as MouseAdapter
      ApplicationManager.getApplication().invokeAndWait {
        mouseAdapter.mouseClicked(mockk { every { clickCount } returns 2 })
      }

      assertSoftly { didTriggerEditSession shouldBe true }
      assertSoftly { didSetTableModel shouldBe false }
    }
    should("react on a mouse double click and not make any updates cause there is no selected object in the table") {
      every {
        tableViewMock.selectedObject
      } answers {
        didTriggerEditSession = true
        null
      }

      val mouseAdapter = registerMouseListenerMethod.invoke(tsoSessionConfigurable) as MouseAdapter
      mouseAdapter.mouseClicked(mockk { every { clickCount } returns 2 })

      assertSoftly { didTriggerEditSession shouldBe true }
      assertSoftly { didSetTableModel shouldBe false }
    }
    should("not react on a mouse single click") {
      val mouseAdapter = registerMouseListenerMethod.invoke(tsoSessionConfigurable) as MouseAdapter
      mouseAdapter.mouseClicked(mockk { every { clickCount } returns 1 })

      assertSoftly { didTriggerEditSession shouldBe false }
      assertSoftly { didSetTableModel shouldBe false }
    }
  }

  context("apply + reset") {
    var didUpdateUI = false

    val panelMock = mockk<DialogPanel> {
      every {
        updateUI()
      } answers {
        didUpdateUI = true
      }
    }

    val tsoSessionConfigurable = spyk<TSOSessionConfigurable>()
    setPrivateFieldValue(tsoSessionConfigurable, "panel", panelMock)

    beforeEach {
      didUpdateUI = false
    }

    should("update UI when there is a modification made and applied") {
      every { configSandboxServiceMock.isModified(any<Class<*>>()) } returns true
      tsoSessionConfigurable.apply()
      assertSoftly { didUpdateUI shouldBe true }
    }
    should("not update UI when there is not modifications made and applied") {
      every { configSandboxServiceMock.isModified(any<Class<*>>()) } returns false
      tsoSessionConfigurable.apply()
      assertSoftly { didUpdateUI shouldBe false }
    }
    should("update UI when there is a modification made and reset") {
      every { configSandboxServiceMock.isModified(any<Class<*>>()) } returns true
      tsoSessionConfigurable.reset()
      assertSoftly { didUpdateUI shouldBe true }
    }
    should("update UI when there is a modification made and reset") {
      every { configSandboxServiceMock.isModified(any<Class<*>>()) } returns false
      tsoSessionConfigurable.reset()
      assertSoftly { didUpdateUI shouldBe false }
    }
  }

  context("reload listener") {
    var didTriggerReinitialize = false

    val tableModelMock = mockk<TSOSessionTableModel> {
      every {
        reinitialize()
      } answers {
        didTriggerReinitialize = true
      }
    }

    val tsoSessionConfigurable = spyk<TSOSessionConfigurable>()
    setPrivateFieldValue(tsoSessionConfigurable, "tableModel", tableModelMock)
    setPrivateFieldValue(
      tsoSessionConfigurable,
      "disposable",
      mockk<Disposable>(),
      DslConfigurableBase::class.java
    )

    beforeEach {
      didTriggerReinitialize = false
    }

    should("reinitialize the table when there is a TSO session config is reloaded") {
      addSandboxListenerMethod.invoke(tsoSessionConfigurable)
      sendTopic(SandboxListener.TOPIC).reload(TSOSessionConfig::class.java)
      assertSoftly { didTriggerReinitialize shouldBe true }
    }
    should("do nothing with the table when there is another config is reloaded") {
      addSandboxListenerMethod.invoke(tsoSessionConfigurable)
      sendTopic(SandboxListener.TOPIC).reload(ConnectionConfig::class.java)
      assertSoftly { didTriggerReinitialize shouldBe false }
    }
  }

  context("addSession") {
    var didAddRow = false

    val tableModelMock = mockk<TSOSessionTableModel> {
      every {
        addRow(any())
      } answers {
        didAddRow = true
      }
    }

    val tsoSessionConfigurable = spyk<TSOSessionConfigurable>()
    setPrivateFieldValue(tsoSessionConfigurable, "tableModel", tableModelMock)

    beforeEach {
      didAddRow = false
    }

    should("add new table row when the dialog is closed with Ok button") {
      every { anyConstructed<TSOSessionDialog>().showAndGet() } returns true
      ApplicationManager.getApplication().invokeAndWait {
        addSessionMethod.invoke(tsoSessionConfigurable)
      }
      assertSoftly { didAddRow shouldBe true }
    }
    should("add new table row when the dialog is canceled") {
      every { anyConstructed<TSOSessionDialog>().showAndGet() } returns false
      ApplicationManager.getApplication().invokeAndWait {
        addSessionMethod.invoke(tsoSessionConfigurable)
      }
      assertSoftly { didAddRow shouldBe false }
    }
  }
})
