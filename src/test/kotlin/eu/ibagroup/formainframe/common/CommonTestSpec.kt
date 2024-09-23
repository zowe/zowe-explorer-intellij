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

package eu.ibagroup.formainframe.common

import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.util.Properties

class CommonTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("common module: ui") {
    // ValidatingCellRenderer.getTableCellRendererComponent
    should("get table cell renderer") {}
    // ValidatingCellEditor.getTableCellEditorComponent
    should("get table cell editor") {}
    // treeUtils.makeNodeDataFromTreePath
    should("make node data from tree path") {}
    // treeUtils.getVirtualFile
    should("get virtual file from tree path") {}
    should("not get virtual file from tree path if it cannot be casted") {}
    // StatefulDialog.showUntilDone
    should("show dialog until it is fulfilled") {}
  }
  context("common module: SettingsPropertyManager") {
    val propertyName = "debug.mode"

    mockkConstructor(Properties::class)

    // isDebugModeEnabled
    should("debug mode enabled") {
      every { anyConstructed<Properties>().getProperty(propertyName) } returns "true"
      val debugMode = isDebugModeEnabled()

      assertSoftly {
        debugMode shouldBe true
      }
    }
    should("debug mode disabled") {
      every { anyConstructed<Properties>().getProperty(propertyName) } returns "false"
      val debugMode = isDebugModeEnabled()

      assertSoftly {
        debugMode shouldBe false
      }
    }
    should("debug mode property not found") {
      every { anyConstructed<Properties>().getProperty(propertyName) } returns null
      val debugMode = isDebugModeEnabled()

      assertSoftly {
        debugMode shouldBe false
      }
    }
    should("debug mode property contains a non-boolean value") {
      every { anyConstructed<Properties>().getProperty(propertyName) } returns "123"
      val debugMode = isDebugModeEnabled()

      assertSoftly {
        debugMode shouldBe false
      }
    }

    unmockkAll()
  }
})
