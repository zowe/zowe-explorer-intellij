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

package org.zowe.explorer.config.ws.ui.files

import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.ui.FilesWorkingSetDialogState
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.runInEdtAndWait
import java.util.*
import java.util.stream.Stream
import javax.swing.JComponent

class FilesWorkingSetDialogTestSpec : AppInitShouldSpec("config/ws/ui/files/FilesWorkingSetDialog", {
  context("init") {
    lateinit var crudableMockk: Crudable

    beforeEach {
      crudableMockk = mockk<Crudable> {
        every { getAll(ConnectionConfig::class.java) } answers { Stream.of() }
        every { getByUniqueKey(ConnectionConfig::class.java, any<String>()) } returns Optional.of(ConnectionConfig())
      }

      mockkConstructor(DialogPanel::class)
      every { anyConstructed<DialogPanel>().registerValidators(any(), any()) } answers {
        val componentValidityChangedCallback = secondArg<(Map<JComponent, ValidationInfo>) -> Unit>()
        componentValidityChangedCallback(mapOf())
      }
    }

    should("check that OK action is enabled if validation map is empty") {
      runInEdtAndWait {
        val dialog = FilesWorkingSetDialog(crudableMockk, FilesWorkingSetDialogState())

        verify { anyConstructed<DialogPanel>().registerValidators(any(), any()) }
        assertSoftly { dialog.isOKActionEnabled shouldBe true }
      }
    }
  }
})
