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

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.utils.crudable.Crudable
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.*
import java.util.stream.Stream
import javax.swing.JList
import javax.swing.ListCellRenderer

class SelectTSOSessionDialogTestSpec : AppInitShouldSpec("explorer/ui/SelectTSOSessionDialog", {
  context("all functions") {
    lateinit var dialog: SelectTSOSessionDialog

    val state = SelectTSOSessionDialogState(null)

    val crudableMock = mockk<Crudable>()
    every {
      crudableMock.getAll(TSOSessionConfig::class.java)
    } answers {
      Stream.of(TSOSessionConfig())
    }

    val createCenterPanelMethod = DialogWrapper::class.java.getDeclaredMethod("createCenterPanel")
    createCenterPanelMethod.isAccessible = true

    mockkStatic(::validateTsoSessionSelection)

    beforeEach {
      dialog = runWriteActionInEdtAndWait {
        SelectTSOSessionDialog(mockk(), crudableMock, state)
      }

      every { validateTsoSessionSelection(any(), any()) } returns null
    }

    // createCenterPanel
    should("create panel") {
      val panel = runWriteActionInEdtAndWait {
        createCenterPanelMethod.invoke(dialog) as? DialogPanel
      }

      assertSoftly { panel shouldNotBe null }
    }

    should("validate panel") {
      val panel = runWriteActionInEdtAndWait {
        createCenterPanelMethod.invoke(dialog) as? DialogPanel
      }
      panel?.registerValidators { }
      panel?.validateAll()

      verify { validateTsoSessionSelection(any(), any()) }
    }

    should("render TSO sessions combobox") {
      val panel = runWriteActionInEdtAndWait {
        createCenterPanelMethod.invoke(dialog) as? DialogPanel
      }

      val comboBox = panel?.components?.filterIsInstance<ComboBox<TSOSessionConfig>>()?.firstOrNull()

      @Suppress("UNCHECKED_CAST")
      val renderer = comboBox?.renderer as? ListCellRenderer<TSOSessionConfig>

      val comboBoxModel = CollectionComboBoxModel(listOf<TSOSessionConfig>())

      val component = renderer?.getListCellRendererComponent(
        JList(comboBoxModel),
        TSOSessionConfig(),
        0,
        true,
        true
      )

      assertSoftly {
        comboBox shouldNotBe null
        renderer shouldNotBe null
        component shouldNotBe null
      }
    }
  }
})
