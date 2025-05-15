/*
 * Copyright (c) 2024 IBA Group.
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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.operations

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.ExtensionTestUtil
import org.zowe.explorer.telemetry.NotificationCompatibleException
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.v3.ConnectionConfigOldStruct
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import org.zowe.explorer.testutils.AppInitShouldSpec

class OperationsServiceTestSpec : AppInitShouldSpec("v3/operations/OperationsService", {
  context("all functions") {
    var didRenameOperationRun = false
    var isErrorNotificationTriggered = false

    val progressIndicator = mockk<ProgressIndicator>()
    val operationData = mockk<RenameOperationData<ConnectionConfigOldStruct>> {
      every { attributes } returns mockk()
    }

    val testAppEpName = ExtensionPointName<OperationRunner<*, *, *>>("org.zowe.explorer.operationRunnerV3")
    var testAppEpNameDisposable = Disposer.newDisposable()

    mockkObject(NotificationsService)
    every {
      NotificationsService.errorNotification(any())
    } answers {
      isErrorNotificationTriggered = true
    }

    beforeEach {
      didRenameOperationRun = false
      isErrorNotificationTriggered = false
      testAppEpNameDisposable = Disposer.newDisposable()
    }

    afterEach {
      Disposer.dispose(testAppEpNameDisposable)
    }

    mockkObject(OperationsService)
    every { OperationsService.getService() } returns spyk(OperationsService())

    should("test operation performed successfully") {
      val operationRunner = mockk<RenameOperationRunner<ConnectionConfigOldStruct>> {
        every { operationDataClass } returns RenameOperationData::class.java
        every { canRun(any()) } returns true
        every {
          run(any(), progressIndicator)
        } answers {
          didRenameOperationRun = true
        }
      }

      ExtensionTestUtil.maskExtensions(testAppEpName, listOf(operationRunner), testAppEpNameDisposable)

      val result = OperationsService.getService()
        .performOperation(
          operationData = operationData,
          progressIndicator = progressIndicator
        )

      assertSoftly { isErrorNotificationTriggered shouldBe false }
      assertSoftly { didRenameOperationRun shouldBe true }
      assertSoftly { result.isSuccess shouldBe true }
    }

    should("test operation is not run as operation runner is not found") {
      ExtensionTestUtil.maskExtensions(testAppEpName, listOf(), testAppEpNameDisposable)

      val result = OperationsService.getService()
        .performOperation(
          operationData = operationData,
          progressIndicator = progressIndicator
        )

      assertSoftly { isErrorNotificationTriggered shouldBe true }
      assertSoftly { didRenameOperationRun shouldBe false }
      assertSoftly { result.isFailure shouldBe true }
      result.onFailure {
        assertSoftly { (it is NotificationCompatibleException) shouldBe true }
        assertSoftly {
          (it as NotificationCompatibleException)
            .detailsShort shouldContain "Operation runner for operation-compatible"
        }
        assertSoftly { (it as NotificationCompatibleException).detailsShort shouldContain "is not found" }
      }
    }

    should("test operation is not run as the operation cannot be run with the provided data") {
      val operationRunner = mockk<RenameOperationRunner<ConnectionConfigOldStruct>> {
        every { operationDataClass } returns RenameOperationData::class.java
        every { canRun(any()) } returns false
        every {
          run(any(), progressIndicator)
        } answers {
          didRenameOperationRun = true
        }
      }

      ExtensionTestUtil.maskExtensions(testAppEpName, listOf(operationRunner), testAppEpNameDisposable)

      val result = OperationsService.getService()
        .performOperation(
          operationData = operationData,
          progressIndicator = progressIndicator
        )

      assertSoftly { isErrorNotificationTriggered shouldBe true }
      assertSoftly { didRenameOperationRun shouldBe false }
      assertSoftly { result.isFailure shouldBe true }
      result.onFailure {
        assertSoftly { (it is NotificationCompatibleException) shouldBe true }
        assertSoftly {
          (it as NotificationCompatibleException)
            .detailsShort shouldContain "cannot be run with the provided"
        }
      }
    }
  }
})
