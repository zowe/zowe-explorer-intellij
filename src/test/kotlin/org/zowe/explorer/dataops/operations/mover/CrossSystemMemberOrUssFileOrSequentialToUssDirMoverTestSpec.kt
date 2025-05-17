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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.dataops.operations.mover

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.assertThrows
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.*
import org.zowe.explorer.dataops.content.synchronizer.ContentSynchronizer
import org.zowe.explorer.dataops.operations.DeleteOperation
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.setUssFileTag
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import org.zowe.kotlinsdk.XIBMDataType
import org.zowe.kotlinsdk.annotations.ZVersion
import retrofit2.Response
import java.nio.charset.StandardCharsets
import kotlin.reflect.KFunction

class CrossSystemMemberOrUssFileOrSequentialToUssDirMoverTestSpec : AppInitShouldSpec("dataops/operations/mover/CrossSystemMemberOrUssFileOrSequentialToUssDirMover", {
  context("all functions") {
    val connection1 = ConnectionConfig(
      "test_uuid_a",
      "test_name_a",
      "https://a.com",
      true,
      ZVersion.ZOS_2_3
    )
    val connection2 = ConnectionConfig(
      "test_uuid_b",
      "test_name_b",
      "https://b.com",
      true,
      ZVersion.ZOS_2_3
    )

    val psDataset = mockk<MFVirtualFile> {
      every { isDirectory } returns false
      every { name } returns "testPSDataset"
      every { contentsToByteArray() } returns "Test Content!!!".toByteArray()
      every { charset } returns StandardCharsets.UTF_8
    }
    val psDatasetAttributes = mockk<RemoteDatasetAttributes> {
      every { requesters } returns mutableListOf(MaskedRequester(connection1, DSMask()))
    }
    val pdsDataset = mockk<MFVirtualFile> {
      every { isDirectory } returns true
      every { name } returns "testPDSDataset"
    }
    val pdsDatasetAttributes = mockk<RemoteDatasetAttributes> {
      every { requesters } returns mutableListOf(MaskedRequester(connection1, DSMask()))
    }
    val pdsMember = mockk<MFVirtualFile> {
      every { isDirectory } returns false
      every { name } returns "testPDSMember"
      every { contentsToByteArray() } returns "Test Content!!!".toByteArray()
      every { charset } returns StandardCharsets.UTF_8
    }
    val pdsMemberAttributes = mockk<RemoteMemberAttributes> {
      every { parentFile } returns pdsDataset
    }
    val ussFile = mockk<MFVirtualFile> {
      every { isDirectory } returns false
      every { name } returns "testUssFile"
      every { contentsToByteArray() } returns "Test Content!!!".toByteArray()
      every { charset } returns StandardCharsets.UTF_8
      every { path } returns "/test/uss/file"
    }
    val ussFileAttributes = mockk<RemoteUssAttributes> {
      every { requesters } returns mutableListOf(UssRequester(connection1))
      every { isSymlink } returns false
      every { path } returns "/test/uss/file"
    }
    val ussFolder = mockk<MFVirtualFile> {
      every { isDirectory } returns true
      every { name } returns "testUssFolder"
      every { path } returns "/test/uss/folder"
    }
    val ussCrossSystemAttributes = mockk<RemoteUssAttributes> {
      every { requesters } returns mutableListOf(UssRequester(connection2))
      every { isSymlink } returns false
      every { path } returns "/test/uss/folder"
    }

    val dataOpsManager = DataOpsManager.getService()
    every {
      dataOpsManager.getAttributesService(RemoteDatasetAttributes::class.java, any<Class<MFVirtualFile>>())
    } returns mockk<RemoteDatasetAttributesService> {
      every { getAttributes(any<MFVirtualFile>()) } returns pdsDatasetAttributes
    }
    every { dataOpsManager.tryToGetAttributes(any<MFVirtualFile>()) } returns mockk()
    every {
      dataOpsManager.getContentSynchronizer(any<MFVirtualFile>())
    } returns mockk<ContentSynchronizer>(relaxUnitFun = true)

    context("canRun") {
      should("try to copy a PDS to a USS folder in a cross-system environment") {
        val operation = MoveCopyOperation(
          pdsDataset,
          pdsDatasetAttributes,
          ussFolder,
          ussCrossSystemAttributes,
          isMove = false,
          forceOverwriting = false,
          newName = null
        )

        val result = CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
          .canRun(operation)

        assertSoftly { result shouldBe false }
      }

      should("try to copy a PS dataset to a USS file in a cross-system environment") {
        val operation = MoveCopyOperation(
          psDataset,
          psDatasetAttributes,
          ussFile,
          ussCrossSystemAttributes,
          isMove = false,
          forceOverwriting = false,
          newName = null
        )

        val result = CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
          .canRun(operation)

        assertSoftly { result shouldBe false }
      }

      should("copy a PDS member to a USS folder in a cross-system environment") {
        val operation = MoveCopyOperation(
          pdsMember,
          pdsMemberAttributes,
          ussFolder,
          ussCrossSystemAttributes,
          isMove = false,
          forceOverwriting = false,
          newName = null
        )

        val result = CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
          .canRun(operation)

        assertSoftly { result shouldBe true }
      }

      should("copy a PS dataset to a USS folder in a cross-system environment") {
        val operation = MoveCopyOperation(
          psDataset,
          psDatasetAttributes,
          ussFolder,
          ussCrossSystemAttributes,
          isMove = false,
          forceOverwriting = false,
          newName = null
        )

        val result = CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
          .canRun(operation)

        assertSoftly { result shouldBe true }
      }

      should("try to copy a PS dataset to a USS folder in a single-system environment") {
        val ussSingleSystemAttributes = mockk<RemoteUssAttributes> {
          every { requesters } returns mutableListOf(UssRequester(connection1))
          every { isSymlink } returns false
        }

        val operation = MoveCopyOperation(
          psDataset,
          psDatasetAttributes,
          ussFolder,
          ussSingleSystemAttributes,
          isMove = false,
          forceOverwriting = false,
          newName = null
        )

        val result = CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
          .canRun(operation)

        assertSoftly { result shouldBe false }
      }

      should("copy a USS file to a USS folder in a cross-system environment") {
        val operation = MoveCopyOperation(
          ussFile,
          ussFileAttributes,
          ussFolder,
          ussCrossSystemAttributes,
          isMove = false,
          forceOverwriting = false,
          newName = null
        )

        val result = CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
          .canRun(operation)

        assertSoftly { result shouldBe true }
      }

      should("try to copy a plain non-mainframe folder to a USS folder in a cross-system environment") {
        val plainFolder = mockk<VirtualFile> { every { isDirectory } returns true }

        val operation = MoveCopyOperation(
          plainFolder,
          ussFileAttributes,
          ussFolder,
          ussCrossSystemAttributes,
          isMove = false,
          forceOverwriting = false,
          newName = null
        )

        val result = CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
          .canRun(operation)

        assertSoftly { result shouldBe false }
      }

      should("try to copy a PS dataset to a PDS in a cross-system environment") {
        val pdsCrossSystemAttributes = mockk<RemoteDatasetAttributes> {
          every { requesters } returns mutableListOf(MaskedRequester(connection2, DSMask()))
        }

        val operation = MoveCopyOperation(
          psDataset,
          psDatasetAttributes,
          pdsDataset,
          pdsCrossSystemAttributes,
          isMove = false,
          forceOverwriting = false,
          newName = null
        )

        val result = CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
          .canRun(operation)

        assertSoftly { result shouldBe false }
      }
    }

    context("run") {
      var didRunOperation = false
      var didDeleteSource = false

      val mockCallResponse = mockk<Response<Void>>()
      val apiMock = mockk<DataAPI> {
        every {
          writeToUssFile(
            authorizationToken = any<String>(),
            filePath = any<FilePath>(),
            body = any<ByteArray>(),
            xIBMDataType = any<XIBMDataType>()
          )
        } answers {
          didRunOperation = true
          mockk {
            every { execute() } returns mockCallResponse
          }
        }
      }

      val zosmfApi = ZosmfApi.getService()
      every { zosmfApi.getApiWithBytesConverter(DataAPI::class.java, any<ConnectionConfig>()) } returns apiMock

      val setUssFileTagFun: (String, String, ConnectionConfig) -> Unit = ::setUssFileTag
      mockkStatic(setUssFileTagFun as KFunction<*>)
      every { setUssFileTagFun(any<String>(), any<String>(), any<ConnectionConfig>()) } returns Unit

      every {
        dataOpsManager.performOperation(any<DeleteOperation>(), any<ProgressIndicator>())
      } answers {
        didDeleteSource = true
      }

      val credentialService = CredentialService.getService()
      every { credentialService.getUsernameByKey(any<String>()) } returns "test"
      every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

      beforeEach {
        didRunOperation = false
        didDeleteSource = false

        every { mockCallResponse.isSuccessful } returns true
      }

      should("copy a USS file to a USS folder in a cross-system environment") {
        val operation = MoveCopyOperation(
          ussFile,
          ussFileAttributes,
          ussFolder,
          ussCrossSystemAttributes,
          isMove = false,
          forceOverwriting = false,
          newName = null
        )

        CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
          .run(operation)

        assertSoftly {
          didRunOperation shouldBe true
          didDeleteSource shouldBe false
        }
      }

      should("move a USS file to a USS folder in a cross-system environment") {
        val operation = MoveCopyOperation(
          ussFile,
          ussFileAttributes,
          ussFolder,
          ussCrossSystemAttributes,
          isMove = true,
          forceOverwriting = false,
          newName = null
        )

        CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
          .run(operation)

        assertSoftly {
          didRunOperation shouldBe true
          didDeleteSource shouldBe true
        }
      }

      should("fail to move a USS file to a USS folder in a cross-system environment cause the source file is a symbolic link") {
        val ussFileSymlinkAttributes = mockk<RemoteUssAttributes> {
          every { requesters } returns mutableListOf(UssRequester(connection1))
          every { isSymlink } returns true
          every { path } returns "/test/uss/file"
          every { symlinkTarget } returns null
        }
        val operation = MoveCopyOperation(
          ussFile,
          ussFileSymlinkAttributes,
          ussFolder,
          ussCrossSystemAttributes,
          isMove = true,
          forceOverwriting = false,
          newName = null
        )

        val exception = assertThrows<IllegalArgumentException> {
          CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
            .run(operation)
        }

        assertSoftly {
          exception.message shouldContain "Impossible to move symlink."
          didRunOperation shouldBe false
          didDeleteSource shouldBe false
        }
      }

      should("copy a PS dataset to a USS folder in a cross-system environment") {
        val operation = MoveCopyOperation(
          psDataset,
          psDatasetAttributes,
          ussFolder,
          ussCrossSystemAttributes,
          isMove = false,
          forceOverwriting = false,
          newName = null
        )

        CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
          .run(operation)

        assertSoftly {
          didRunOperation shouldBe true
          didDeleteSource shouldBe false
        }
      }

      should("move a PDS member to a USS folder in a cross-system environment") {
        val operation = MoveCopyOperation(
          pdsMember,
          pdsMemberAttributes,
          ussFolder,
          ussCrossSystemAttributes,
          isMove = true,
          forceOverwriting = false,
          newName = null
        )

        CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
          .run(operation)

        assertSoftly {
          didRunOperation shouldBe true
          didDeleteSource shouldBe true
        }
      }
    }
  }
})
