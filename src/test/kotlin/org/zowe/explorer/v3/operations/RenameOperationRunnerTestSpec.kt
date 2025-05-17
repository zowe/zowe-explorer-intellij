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

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.v3.ConnectionConfigOldStruct
import org.zowe.explorer.v3.Requester
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.runInEdtAndWait
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.annotations.ZVersion
import retrofit2.Response
import kotlin.reflect.KFunction

class RenameOperationRunnerTestSpec : AppInitShouldSpec("v3/operations/RenameOperationRunner", {
  context("v3/operations/RenameOperationRunner") {
    val newName = "test_new_name"

    context("canRun") {
      should("test canRun returns true as provided attributes are USS attributes") {
        val file = mockk<VirtualFile>()
        val attributes = mockk<RemoteUssAttributes>()
        val origin = mockk<Requester<ConnectionConfigOldStruct>>()
        val operationData = RenameOperationData(file, attributes, newName, origin)
        val renameOperationRunner = RenameOperationRunner<ConnectionConfigOldStruct>()

        val result = renameOperationRunner.canRun(operationData)

        assertSoftly { result shouldBe true }
      }
    }
    context("run") {
      var didMoveUssFileCall = false
      var didCallExecute = false
      var didFileRename = false
      var isCallExceptionRaised = false

      val progressIndicator = mockk<ProgressIndicator> {
        every { checkCanceled() } answers { }
      }

      val file = mockk<VirtualFile> {
        every {
          rename(any(), any())
        } answers {
          didFileRename = true
        }
      }
      val attributes = mockk<RemoteUssAttributes> {
        every { parentDirPath } returns "test_parent_dir_path"
        every { path } returns "test_path"
      }
      val origin = mockk<Requester<ConnectionConfigOldStruct>> {
        every { connectionConfig } returns mockk {
          every { uuid } returns "test_uuid"
          every { name } returns "test_old_name"
          every { url } returns "test_url"
          every { isAllowSelfSigned } returns true
          every { zVersion } returns ZVersion.ZOS_3_1
          every { owner } returns "TSTOWNR"
        }
      }
      val operationData = RenameOperationData(file, attributes, newName, origin)

      val callExceptionFun: (Response<*>, String) -> CallException = ::CallException
      mockkStatic(callExceptionFun as KFunction<*>)
      every {
        callExceptionFun(any<Response<*>>(), any<String>())
      } answers {
        isCallExceptionRaised = true
        mockk {
          every { headMessage } returns secondArg()
        }
      }

      val responseMock = mockk<Response<Void>>()

      val zosmfApi = ZosmfApi.getService()
      every { zosmfApi.getApi(DataAPI::class.java, any()) } returns mockk {
        every {
          moveUssFile(authorizationToken = any(), body = any(), filePath = any())
        } answers {
          didMoveUssFileCall = true
          mockk {
            every {
              execute()
            } answers {
              didCallExecute = true
              responseMock
            }
          }
        }
      }

      val credentialService = CredentialService.getService()
      every { credentialService.getUsernameByKey(any<String>()) } returns "test"
      every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

      mockkStatic(::runInEdtAndWait)

      beforeEach {
        every { runInEdtAndWait(any()) } answers { callOriginal() }
      }

      beforeEach {
        didMoveUssFileCall = false
        didCallExecute = false
        didFileRename = false
        isCallExceptionRaised = false
      }

      should("run operation successfully for USS entities") {
        every { responseMock.isSuccessful } returns true

        val renameOperationRunner = RenameOperationRunner<ConnectionConfigOldStruct>()

        renameOperationRunner.run(operationData, progressIndicator)

        assertSoftly { didMoveUssFileCall shouldBe true }
        assertSoftly { didCallExecute shouldBe true }
        assertSoftly { didFileRename shouldBe true }
      }

      should("process non-successful response for run operation") {
        every { responseMock.isSuccessful } returns false

        val renameOperationRunner = RenameOperationRunner<ConnectionConfigOldStruct>()

        val exception = assertThrows<CallException> { renameOperationRunner.run(operationData, progressIndicator) }

        assertSoftly { exception.headMessage shouldContain "Unable to rename the selected file or directory" }
        assertSoftly { didMoveUssFileCall shouldBe true }
        assertSoftly { didCallExecute shouldBe true }
        assertSoftly { didFileRename shouldBe false }
        assertSoftly { isCallExceptionRaised shouldBe true }
      }

      should("fail the run operation with RuntimeException") {
        every { responseMock.isSuccessful } returns true
        every { runInEdtAndWait(any()) } throws Exception("Test exception")

        val renameOperationRunner = RenameOperationRunner<ConnectionConfigOldStruct>()

        val exception = assertThrows<Exception> { renameOperationRunner.run(operationData, progressIndicator) }

        assertSoftly { exception.message shouldContain "Test exception" }
        assertSoftly { didMoveUssFileCall shouldBe true }
        assertSoftly { didCallExecute shouldBe true }
        assertSoftly { didFileRename shouldBe false }
        assertSoftly { isCallExceptionRaised shouldBe false }
      }
    }
  }
})
