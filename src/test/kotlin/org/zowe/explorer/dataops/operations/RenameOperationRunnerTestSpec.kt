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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.MaskedRequester
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.attributes.UssRequester
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.explorer.actions.DuplicateMemberAction
import org.zowe.explorer.explorer.actions.RenameAction
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.kotlinsdk.CopyDataZOS
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.Dataset
import org.zowe.kotlinsdk.FilePath
import org.zowe.kotlinsdk.Member
import org.zowe.kotlinsdk.MoveUssFile
import org.zowe.kotlinsdk.RenameData
import retrofit2.Response

class RenameOperationRunnerTestSpec : AppInitShouldSpec("dataops/operations/RenameOperationRunner", {
  context("common functions") {
    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()
    val dataApi = mockk<DataAPI>()
    val zosmfApi = ZosmfApi.getService()
    every { zosmfApi.getApi(DataAPI::class.java, any<ConnectionConfig>()) } returns dataApi
    val dataOpsManager = DataOpsManager.getService()

    val classUnderTest = spyk(RenameOperationRunner(dataOpsManager))

    context("canRun") {
      val operation = mockk<RenameOperation> {
        every {
          attributes
        }.returnsMany(
          mockk<RemoteMemberAttributes>(),
          mockk<RemoteDatasetAttributes>(),
          mockk<RemoteUssAttributes>(),
          mockk<FileAttributes>()
        )
      }

      should("returnTrue_whenCanRun_givenRemoteMemberAttributes") {
        // when
        val canRun = classUnderTest.canRun(operation)

        // then
        assertSoftly {
          canRun shouldBe true
        }
      }

      should("returnTrue_whenCanRun_givenRemoteDatasetAttributes") {
        // when
        val canRun = classUnderTest.canRun(operation)

        // then
        assertSoftly {
          canRun shouldBe true
        }
      }

      should("returnTrue_whenCanRun_givenRemoteUssAttributes") {
        // when
        val canRun = classUnderTest.canRun(operation)

        // then
        assertSoftly {
          canRun shouldBe true
        }
      }

      should("returnFalse_whenCanRun_givenFileAttributes") {
        // when
        val canRun = classUnderTest.canRun(operation)

        // then
        assertSoftly {
          canRun shouldBe false
        }
      }
    }

    context("run operation") {
      val operation = mockk<RenameOperation>()
      val progressIndicator = mockk<ProgressIndicator> {
        every { checkCanceled() } just Runs
      }
      val connectionConfigMock = mockk<ConnectionConfig> {
        every { uuid } returns "test_uuid"
      }
      val datasetRequester = mockk<MaskedRequester> {
        every { connectionConfig } returns connectionConfigMock
      }
      val ussRequester = mockk<UssRequester> {
        every { connectionConfig } returns connectionConfigMock
      }

      should("not run rename operation on invalid attributes given") {
        // given
        every { operation.attributes } returns mockk<FileAttributes>()

        // when
        classUnderTest.run(operation, progressIndicator)

        // then
        verify { progressIndicator wasNot Called }
      }

      should("run rename operation on RemoteDatasetAttributes given") {
        // given
        val mfFile = mockk<MFVirtualFile> {
          every { rename(any(), any()) } just Runs
        }
        val apiResponse = mockk<Response<Void>> {
          every { isSuccessful } returns true
        }
        val datasetAttributes = mockk<RemoteDatasetAttributes> {
          every { requesters } returns mutableListOf(datasetRequester)
          every { name } returns "OLD_FILE_NAME"
        }
        every { operation.file } returns mfFile
        every { operation.attributes } returns datasetAttributes
        every { operation.newName } returns "NEW_FILE_NAME"

        every {
          dataApi.renameDataset(any(), any(), any(), any()).cancelByIndicator(progressIndicator).execute()
        } returns apiResponse

        // when
        classUnderTest.run(operation, progressIndicator)

        // then
        verify(exactly = 1) {
          dataApi.renameDataset(
            any(),
            null,
            RenameData(fromDataset = RenameData.FromDataset("OLD_FILE_NAME")),
            "NEW_FILE_NAME"
          )
        }
        verify(exactly = 1) { mfFile.rename(classUnderTest, "NEW_FILE_NAME") }
      }

      should("throw CallException while running rename operation on RemoteDatasetAttributes, if response wan not successful") {
        // given
        val apiResponse = mockk<Response<Void>> {
          every { code() } returns 404
          every { isSuccessful } returns false
        }

        every {
          dataApi.renameDataset(any(), any(), any(), any()).cancelByIndicator(progressIndicator).execute()
        } returns apiResponse

        // when
        val exception = shouldThrowExactly<CallException> {
          classUnderTest.run(operation, progressIndicator)
        }

        // then
        assertSoftly {
          exception.message shouldBe "Unable to rename the selected dataset\nCode: 404"
        }
      }

      should("throw RuntimeException while running rename operation on RemoteDatasetAttributes, if any error happened during processing the request") {
        // given
        every {
          dataApi.renameDataset(any(), any(), any(), any()).cancelByIndicator(progressIndicator).execute()
        } answers {
          throw RuntimeException("TEST ERROR")
        }

        // when
        val exception = shouldThrowExactly<RuntimeException> {
          classUnderTest.run(operation, progressIndicator)
        }

        // then
        assertSoftly {
          exception.message shouldBe "java.lang.RuntimeException: TEST ERROR"
        }
      }

      should("run rename operation on RemoteMemberAttributes given DuplicateMember requester") {
        // given
        val datasetMock = mockk<Dataset> {
          every { name } returns "DATASET_NAME"
        }
        val parentAttributes = mockk<RemoteDatasetAttributes> {
          every { datasetInfo } returns datasetMock
          every { requesters } returns mutableListOf(datasetRequester)
        }
        val parentFileMock = mockk<MFVirtualFile>()
        val memberMock = mockk<Member> {
          every { name } returns "MEMBER_NAME"
        }
        val memberAttributes = mockk<RemoteMemberAttributes> {
          every { parentFile } returns parentFileMock
          every { info } returns memberMock
        }
        val apiResponse = mockk<Response<Void>> {
          every { isSuccessful } returns true
        }

        every { operation.attributes } returns memberAttributes
        every { operation.requester } returns mockk<DuplicateMemberAction>()
        every { operation.newName } returns "NEW_MEMBER_NAME"

        every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns parentAttributes

        every {
          dataApi.copyToDatasetMember(any(), any(), any(), any(), any(), any()).cancelByIndicator(progressIndicator)
            .execute()
        } returns apiResponse

        // when
        classUnderTest.run(operation, progressIndicator)

        // then
        verify(exactly = 1) {
          dataApi.copyToDatasetMember(
            any(),
            null,
            null,
            CopyDataZOS.CopyFromDataset(
              dataset = CopyDataZOS.CopyFromDataset.Dataset("DATASET_NAME", "MEMBER_NAME"),
              replace = true
            ),
            "DATASET_NAME",
            "NEW_MEMBER_NAME"
          )
        }
      }

      should("throw CallException while running rename operation on RemoteMemberAttributes, if response wan not successful for DuplicateMember requester") {
        // given
        val apiResponse = mockk<Response<Void>> {
          every { code() } returns 404
          every { isSuccessful } returns false
        }

        every {
          dataApi.copyToDatasetMember(any(), any(), any(), any(), any(), any()).cancelByIndicator(progressIndicator)
            .execute()
        } returns apiResponse

        // when
        val exception = shouldThrowExactly<CallException> {
          classUnderTest.run(operation, progressIndicator)
        }

        // then
        assertSoftly {
          exception.message shouldBe "Unable to duplicate the selected member\nCode: 404"
        }
      }

      should("run rename operation on RemoteMemberAttributes given non DuplicateMember requester") {
        // given
        val memberFile = mockk<MFVirtualFile> {
          every { rename(any(), any()) } just Runs
        }
        every { operation.requester } returns mockk<RenameAction>()
        every { operation.file } returns memberFile
        val apiResponse = mockk<Response<Void>> {
          every { isSuccessful } returns true
        }
        every {
          dataApi.renameDatasetMember(any(), any(), any(), any(), any()).cancelByIndicator(progressIndicator).execute()
        } returns apiResponse

        // when
        classUnderTest.run(operation, progressIndicator)

        // then
        verify(exactly = 1) {
          dataApi.renameDatasetMember(
            any(),
            null,
            RenameData(fromDataset = RenameData.FromDataset("DATASET_NAME", "MEMBER_NAME")),
            "DATASET_NAME",
            "NEW_MEMBER_NAME"
          )
        }
        verify(exactly = 1) { memberFile.rename(classUnderTest, "NEW_MEMBER_NAME") }
      }

      should("throw CallException while running rename operation on RemoteMemberAttributes, if response wan not successful for RenameAction requester") {
        // given
        val apiResponse = mockk<Response<Void>> {
          every { code() } returns 404
          every { isSuccessful } returns false
        }

        every {
          dataApi.renameDatasetMember(any(), any(), any(), any(), any()).cancelByIndicator(progressIndicator).execute()
        } returns apiResponse

        // when
        val exception = shouldThrowExactly<CallException> {
          classUnderTest.run(operation, progressIndicator)
        }

        // then
        assertSoftly {
          exception.message shouldBe "Unable to rename the selected member\nCode: 404"
        }
      }

      should("throw RuntimeException while running rename operation on RemoteMemberAttributes, if response wan not successful for RenameAction requester") {
        // given
        every {
          dataApi.renameDatasetMember(any(), any(), any(), any(), any()).cancelByIndicator(progressIndicator).execute()
        } answers {
          throw RuntimeException("TEST ERROR")
        }

        // when
        val exception = shouldThrowExactly<RuntimeException> {
          classUnderTest.run(operation, progressIndicator)
        }

        // then
        assertSoftly {
          exception.message shouldBe "java.lang.RuntimeException: TEST ERROR"
        }
      }

      should("run rename operation on RemoteUssAttributes given") {
        // given
        val ussAttributes = mockk<RemoteUssAttributes> {
          every { parentDirPath } returns "PARENT_PATH"
          every { path } returns "TEST_FILE_PATH"
          every { requesters } returns mutableListOf(ussRequester)
        }
        val ussFile = mockk<MFVirtualFile> {
          every { rename(any(), any()) } just Runs
        }
        val apiResponse = mockk<Response<Void>> {
          every { isSuccessful } returns true
        }

        every { operation.attributes } returns ussAttributes
        every { operation.newName } returns "NEW_FILE_NAME"
        every { operation.file } returns ussFile

        every {
          dataApi.moveUssFile(any(), any(), any(), any()).cancelByIndicator(progressIndicator).execute()
        } returns apiResponse

        // when
        classUnderTest.run(operation, progressIndicator)

        // then
        verify(exactly = 1) {
          dataApi.moveUssFile(
            any(),
            null,
            MoveUssFile(
              from = "TEST_FILE_PATH"
            ),
            filePath = FilePath("PARENT_PATH/NEW_FILE_NAME")
          )
        }
        verify(exactly = 1) { ussFile.rename(classUnderTest, "NEW_FILE_NAME") }
      }

      should("throw CallException while running rename operation on RemoteUssAttributes, if response wan not successful") {
        // given
        val apiResponse = mockk<Response<Void>> {
          every { code() } returns 404
          every { isSuccessful } returns false
        }

        every {
          dataApi.moveUssFile(any(), any(), any(), any()).cancelByIndicator(progressIndicator).execute()
        } returns apiResponse

        // when
        val exception = shouldThrowExactly<CallException> {
          classUnderTest.run(operation, progressIndicator)
        }

        // then
        assertSoftly {
          exception.message shouldBe "Unable to rename the selected file or directory\nCode: 404"
        }
      }

      should("throw RuntimeException while running rename operation on RemoteUssAttributes, if some error happened during request execution") {
        // given
        every {
          dataApi.moveUssFile(any(), any(), any(), any()).cancelByIndicator(progressIndicator).execute()
        } answers {
          throw RuntimeException("TEST ERROR")
        }

        // when
        val exception = shouldThrowExactly<RuntimeException> {
          classUnderTest.run(operation, progressIndicator)
        }

        // then
        assertSoftly {
          exception.message shouldBe "java.lang.RuntimeException: TEST ERROR"
        }
      }
    }
  }
})
