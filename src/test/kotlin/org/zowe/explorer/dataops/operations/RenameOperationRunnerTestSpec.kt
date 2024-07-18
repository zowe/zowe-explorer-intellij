/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
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
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.testutils.testServiceImpl.TestZosmfApiImpl
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.zowe.kotlinsdk.CopyDataZOS
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.Dataset
import org.zowe.kotlinsdk.FilePath
import org.zowe.kotlinsdk.Member
import org.zowe.kotlinsdk.MoveUssFile
import org.zowe.kotlinsdk.RenameData
import retrofit2.Response

class RenameOperationRunnerTestSpec : WithApplicationShouldSpec({

  beforeSpec {
    clearAllMocks()
  }

  context("RenameOperationRunner common spec") {

    val dataOpsManager = service<DataOpsManager>() as TestDataOpsManagerImpl
    val dataApi = mockk<DataAPI>()
    val zosmfApi = service<ZosmfApi>() as TestZosmfApiImpl
    zosmfApi.testInstance = object : TestZosmfApiImpl() {
      override fun <Api : Any> getApi(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api {
        @Suppress("UNCHECKED_CAST")
        return dataApi as Api
      }
    }

    val classUnderTest = spyk(RenameOperationRunner(dataOpsManager))

    context("canRun") {

      val operation = mockk<RenameOperation>()
      every { operation.attributes }.returnsMany(
        mockk<RemoteMemberAttributes>(),
        mockk<RemoteDatasetAttributes>(),
        mockk<RemoteUssAttributes>(),
        mockk<FileAttributes>()
      )

      should("returnTrue_whenCanRun_givenRemoteMemberAttributes") {
        // given

        // when
        val canRun = classUnderTest.canRun(operation)

        // then
        assertSoftly {
          canRun shouldBe true
        }
      }

      should("returnTrue_whenCanRun_givenRemoteDatasetAttributes") {
        // given

        // when
        val canRun = classUnderTest.canRun(operation)

        // then
        assertSoftly {
          canRun shouldBe true
        }
      }

      should("returnTrue_whenCanRun_givenRemoteUssAttributes") {
        // given

        // when
        val canRun = classUnderTest.canRun(operation)

        // then
        assertSoftly {
          canRun shouldBe true
        }
      }

      should("returnFalse_whenCanRun_givenFileAttributes") {
        // given

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
      val progressIndicator = mockk<ProgressIndicator>()
      val datasetRequester = mockk<MaskedRequester>()
      val ussRequester = mockk<UssRequester>()
      val connectionConfig = mockk<ConnectionConfig>()

      mockkStatic("org.zowe.explorer.config.connect.CredentialServiceKt")
      every { connectionConfig.authToken } returns "TEST_TOKEN"
      every { datasetRequester.connectionConfig } returns connectionConfig
      every { ussRequester.connectionConfig } returns connectionConfig
      every { progressIndicator.checkCanceled() } just Runs

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
        val datasetAttributes = mockk<RemoteDatasetAttributes>()
        val mfFile = mockk<MFVirtualFile>()
        val apiResponse = mockk<Response<Void>>()

        every { datasetAttributes.requesters } returns mutableListOf(datasetRequester)
        every { datasetAttributes.name } returns "OLD_FILE_NAME"
        every { operation.file } returns mfFile
        every { mfFile.rename(any(), any()) } just Runs
        every { operation.attributes } returns datasetAttributes
        every { operation.newName } returns "NEW_FILE_NAME"
        every { apiResponse.isSuccessful } returns true

        every {
          dataApi.renameDataset(any(), any(), any(), any()).cancelByIndicator(progressIndicator).execute()
        } returns apiResponse

        // when
        classUnderTest.run(operation, progressIndicator)

        // then
        verify(exactly = 1) {
          dataApi.renameDataset(
            "TEST_TOKEN",
            null,
            RenameData(
              fromDataset = RenameData.FromDataset("OLD_FILE_NAME")
            ),
            "NEW_FILE_NAME"
          )
        }
        verify(exactly = 1) { mfFile.rename(classUnderTest, "NEW_FILE_NAME") }
      }

      should("throw CallException while running rename operation on RemoteDatasetAttributes, if response wan not successful") {
        // given
        val apiResponse = mockk<Response<Void>>()
        every { apiResponse.code() } returns 404
        every { apiResponse.isSuccessful } returns false

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
        val memberAttributes = mockk<RemoteMemberAttributes>()
        val parentAttributes = mockk<RemoteDatasetAttributes>()
        val datasetMock = mockk<Dataset>()
        val memberMock = mockk<Member>()
        val parentFile = mockk<MFVirtualFile>()
        val apiResponse = mockk<Response<Void>>()

        every { datasetMock.name } returns "DATASET_NAME"
        every { parentAttributes.datasetInfo } returns datasetMock
        every { memberAttributes.parentFile } returns parentFile
        every { parentAttributes.requesters } returns mutableListOf(datasetRequester)
        every { memberMock.name } returns "MEMBER_NAME"
        every { memberAttributes.info } returns memberMock
        every { operation.attributes } returns memberAttributes
        every { operation.requester } returns mockk<DuplicateMemberAction>()
        every { operation.newName } returns "NEW_MEMBER_NAME"
        every { apiResponse.isSuccessful } returns true

        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
            return parentAttributes
          }
        }

        every {
          dataApi.copyToDatasetMember(any(), any(), any(), any(), any(), any()).cancelByIndicator(progressIndicator)
            .execute()
        } returns apiResponse

        // when
        classUnderTest.run(operation, progressIndicator)

        // then
        verify(exactly = 1) {
          dataApi.copyToDatasetMember(
            "TEST_TOKEN",
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
        val apiResponse = mockk<Response<Void>>()
        every { apiResponse.code() } returns 404
        every { apiResponse.isSuccessful } returns false

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
        val apiResponse = mockk<Response<Void>>()
        val memberFile = mockk<MFVirtualFile>()

        every { operation.requester } returns mockk<RenameAction>()
        every { operation.file } returns memberFile
        every { memberFile.rename(any(), any()) } just Runs
        every { apiResponse.isSuccessful } returns true

        every {
          dataApi.renameDatasetMember(any(), any(), any(), any(), any()).cancelByIndicator(progressIndicator).execute()
        } returns apiResponse

        // when
        classUnderTest.run(operation, progressIndicator)

        // then
        verify(exactly = 1) {
          dataApi.renameDatasetMember(
            "TEST_TOKEN",
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
        val apiResponse = mockk<Response<Void>>()
        every { apiResponse.code() } returns 404
        every { apiResponse.isSuccessful } returns false

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
        val ussAttributes = mockk<RemoteUssAttributes>()
        val ussFile = mockk<MFVirtualFile>()
        val apiResponse = mockk<Response<Void>>()

        every { ussAttributes.parentDirPath } returns "PARENT_PATH"
        every { ussAttributes.path } returns "TEST_FILE_PATH"
        every { ussAttributes.requesters } returns mutableListOf(ussRequester)
        every { operation.attributes } returns ussAttributes
        every { operation.newName } returns "NEW_FILE_NAME"
        every { operation.file } returns ussFile
        every { ussFile.rename(any(), any()) } just Runs
        every { apiResponse.isSuccessful } returns true

        every {
          dataApi.moveUssFile(any(), any(), any(), any()).cancelByIndicator(progressIndicator).execute()
        } returns apiResponse

        // when
        classUnderTest.run(operation, progressIndicator)

        // then
        verify(exactly = 1) {
          dataApi.moveUssFile(
            "TEST_TOKEN",
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
        val apiResponse = mockk<Response<Void>>()
        every { apiResponse.code() } returns 404
        every { apiResponse.isSuccessful } returns false

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
