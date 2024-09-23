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

package eu.ibagroup.formainframe.dataops

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.LineSeparator
import eu.ibagroup.formainframe.api.ZosmfApi
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.MFRemoteFileAttributes
import eu.ibagroup.formainframe.dataops.attributes.MaskedRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributesService
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.attributes.Requester
import eu.ibagroup.formainframe.dataops.attributes.UssRequester
import eu.ibagroup.formainframe.dataops.content.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.operations.DeleteOperation
import eu.ibagroup.formainframe.dataops.operations.ZOSInfoOperation
import eu.ibagroup.formainframe.dataops.operations.ZOSInfoOperationRunner
import eu.ibagroup.formainframe.dataops.operations.mover.CrossSystemMemberOrUssFileOrSequentialToUssDirMover
import eu.ibagroup.formainframe.dataops.operations.mover.MoveCopyOperation
import eu.ibagroup.formainframe.dataops.operations.mover.RemoteToLocalFileMover
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestZosmfApiImpl
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.changeEncodingTo
import eu.ibagroup.formainframe.utils.setUssFileTag
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import org.zowe.kotlinsdk.InfoAPI
import org.zowe.kotlinsdk.InfoResponse
import org.zowe.kotlinsdk.XIBMDataType
import org.zowe.kotlinsdk.annotations.ZVersion
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.reflect.KFunction

inline fun mockkRequesters(
  attributes: MFRemoteFileAttributes<*, *>,
  connectionId: String,
  connectionConfigToRequester: (ConnectionConfig) -> Requester<*>
) {
  val connection = ConnectionConfig("ID$connectionId", connectionId, "URL$connectionId", true, ZVersion.ZOS_2_1)
  every { attributes.requesters } returns mutableListOf(connectionConfigToRequester(connection))
}

inline fun <reified S : FileAttributes, reified D : MFRemoteFileAttributes<*, *>> prepareBareOperation(
  isCrossystem: Boolean = false,
  sourceConnectionConfigToRequester: (ConnectionConfig) -> Requester<*>,
  destConnectionConfigToRequester: (ConnectionConfig) -> Requester<*>
): MoveCopyOperation {
  val sourceAttributes = mockk<S>()
  val destAttributes = mockk<D>()
  if (sourceAttributes is MFRemoteFileAttributes<*, *>) {
    mockkRequesters(sourceAttributes, "A", sourceConnectionConfigToRequester)
  }
  mockkRequesters(destAttributes, if (isCrossystem) "B" else "A", destConnectionConfigToRequester)
  return MoveCopyOperation(
    mockk<MFVirtualFile>(),
    sourceAttributes,
    mockk<MFVirtualFile>(),
    destAttributes,
    isMove = false,
    forceOverwriting = false,
    newName = null
  )
}

class OperationsTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("dataops module: operations/ZOSIntoOperationRunner") {
    val zosInfoOperationRunner = ZOSInfoOperationRunner()

    val zosmfApi = ZosmfApi.getService() as TestZosmfApiImpl
    zosmfApi.testInstance = mockk()

    val progressIndicatorMockk = mockk<ProgressIndicator>()
    val responseMockk = mockk<Response<InfoResponse>>()
    every {
      zosmfApi.testInstance.getApi(InfoAPI::class.java, any())
        .getSystemInfo()
        .cancelByIndicator(progressIndicatorMockk)
        .execute()
    } returns responseMockk

    val operationMockk = mockk<ZOSInfoOperation>()
    every { operationMockk.connectionConfig } returns mockk()

    val callExceptionRef: (Response<*>, String) -> CallException = ::CallException
    beforeEach {
      mockkStatic(callExceptionRef as KFunction<*>)
    }
    afterEach {
      unmockkAll()
    }

    // ZOSIntoOperationRunner.run
    should("perform Info operation with successful response") {
      val expected = InfoResponse(zosVersion = "2.3", zosmfHostname = "host", zosmfPort = "port")

      every { responseMockk.isSuccessful } returns true
      every { responseMockk.body() } returns expected

      val actual = zosInfoOperationRunner.run(operationMockk, progressIndicatorMockk)

      assertSoftly { actual shouldBe expected }
    }
    should("perform Info operation with unsuccessful response") {
      val expected = CallException(500, "An internal error has occurred")

      every { responseMockk.isSuccessful } returns false
      every { CallException(responseMockk, any()) } returns expected

      var actual: Throwable? = null
      runCatching {
        zosInfoOperationRunner.run(operationMockk, progressIndicatorMockk)
      }.onFailure {
        actual = it
      }

      assertSoftly { actual shouldBe expected }
    }
    should("perform Info operation when response body is null") {
      val expected = CallException(500, "Cannot parse z/OSMF info request body")

      every { responseMockk.isSuccessful } returns true
      every { responseMockk.body() } returns null
      every { CallException(responseMockk, any()) } returns expected

      var actual: Throwable? = null
      runCatching {
        zosInfoOperationRunner.run(operationMockk, progressIndicatorMockk)
      }.onFailure {
        actual = it
      }

      assertSoftly { actual shouldBe expected }
    }
  }
  context("dataops module: operations/RenameOperationRunner") {
    // run
    should("perform USS file rename") {}
    should("perform dataset rename") {}
    should("perform dataset member rename") {}
  }
  context("dataops module: operations/mover") {
    val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
    val zosmfApi = ZosmfApi.getService() as TestZosmfApiImpl
    beforeEach {
      dataOpsManager.testInstance = mockk()
      zosmfApi.testInstance = mockk()
    }
    // UssToUssFileMover.canRun
    should("return true when we try to move USS file to USS folder") {}
    should("return false when we try to move USS file to USS file") {}
    // UssFileToPdsMover.canRun
    should("return true when we try to move USS file to dataset") {}
    should("return false when we try to move USS file to dataset member") {}
    // UssFileToPdsMover.run
    should("perform move USS file to dataset") {}
    // SequentialToUssFolderMover.canRun
    should("return true when we try to move sequential dataset to USS folder") {}
    should("return false when we try to move sequential dataset to USS file") {}
    // SequentialToPdsMover.canRun
    should("return true when we try to move sequential dataset to the not sequential dataset") {}
    should("return false when we try to move sequential dataset to another sequential dataset") {}
    context("RemoteToLocalFileMover") {
      val remoteToLocalFileMover = spyk(RemoteToLocalFileMover(mockk()))

      val fileMockk = mockk<File>()
      val virtualFileMockk = mockk<VirtualFile>()
      val charsetMockk = mockk<Charset>()
      every { virtualFileMockk.charset } returns charsetMockk
      every { virtualFileMockk.detectedLineSeparator } returns LineSeparator.LF.separatorString

      var encodingChanged = false
      var lineSeparatorChanged = false

      beforeEach {
        encodingChanged = false
        lineSeparatorChanged = false

        mockkStatic(LocalFileSystem::getInstance)
        every { LocalFileSystem.getInstance().refreshAndFindFileByIoFile(fileMockk) } returns mockk {
          every { detectedLineSeparator = any<String>() } answers {
            lineSeparatorChanged = true
          }
        }

        mockkStatic(::changeEncodingTo)
        every { changeEncodingTo(any(), charsetMockk) } answers {
          encodingChanged = true
        }
      }
      afterEach {
        unmockkAll()
      }

      val setCreatedFileParamsRef = remoteToLocalFileMover::class.java.declaredMethods
        .first { it.name == "setCreatedFileParams" }
      setCreatedFileParamsRef.trySetAccessible()

      // canRun
      should("return true when we try to move USS file to a local folder") {}
      should("return false when we try to move USS file to a local file") {}
      // run
      should("move a remote USS binary file to a local folder") {}
      // setCreatedFileParams
      should("set parameters for created file in local system") {
        setCreatedFileParamsRef.invoke(remoteToLocalFileMover, fileMockk, virtualFileMockk)

        assertSoftly {
          encodingChanged shouldBe true
          lineSeparatorChanged shouldBe true
        }

      }
    }
    // RemoteToLocalDirectoryMoverFactory.canRun
    should("return true when we try to move USS folder to a local folder") {}
    should("return false when we try to move USS folder to a local file") {}
    // RemoteToLocalDirectoryMoverFactory.run
    should("move a remote USS folder to a local folder") {}
    // PdsToUssFolderMover.canRun
    should("return true when we try to move a PDS to a USS folder") {}
    should("return true when we try to move a PDS to a USS file") {}
    // MemberToUssFileMover.canRun
    should("return true when we try to move a dataset member to a USS folder") {}
    should("return false when we try to move a dataset member to a USS file") {}
    // MemberToPdsFileMover.canRun
    should("return true when we try to move a dataset member to a PDS") {}
    should("return false when we try to move a dataset member to a sequential dataset") {}
    // LocalFileToUssFileMover.canRun
    should("return true when we try to move a local file to a USS folder") {}
    should("return false when we try to move a local file to a USS file") {}
    // CrossSystemUssFileToUssFolderMover.canRun
    should("return true when we try to move a USS file from one system to a USS folder from the other system") {}
    should("return false when we try to move a USS file from one system to a USS file from the other system") {}
    // CrossSystenUssFileToUssFolderMover.run
    should("move a USS file from one system to a USS folder from the other system") {}
    // CrossSystemUssFileToPdsMover.canRun
    should("return true when we try to move a USS file from one system to a PDS from the other system") {}
    should("return false when we try to move a USS file from one system to a sequential dataset from the other system") {}
    // CrossSystemUssFileToPdsMover.run
    should("move a USS file from one system to a PDS from the other system") {}
    // CrossSystemUssDirMover.canRun
    should("return true when we try to move a USS folder from one system to a USS folder from the other system") {}
    should("return false when we try to move a USS folder from one system to a USS file the other system") {}
    // CrossSystemUssDirMover.run
    should("move a USS folder from one system to a USS folder from the other system") {}
    // CrossSystemPdsToUssDirMover.canRun
    should("return true when we try to move a PDS from one system to a USS folder from the other system") {}
    should("return false when we try to move a PDS from one system to a USS file from the other system") {}
    // CrossSystemPdsToUssDirMover.run
    should("move a PDS from one system to a USS folder from the other system") {}
    // CrossSystemMemberOrUssFileOrSequentialToUssDirMover.canRun
    should("return true/false when we try to move a UssFile/Member/PS from one system to a USS folder from the other system") {
      // SOURCE IS PDS
      // everything is ok.
      var op = prepareBareOperation<RemoteDatasetAttributes, RemoteUssAttributes>(
        true, { MaskedRequester(it, DSMask()) }, { UssRequester(it) }
      )
      every { op.source.isDirectory } returns false
      every { op.destination.isDirectory } returns true

      var result = CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager).canRun(op)
      assertSoftly { result shouldBe true }

      // destination is not a directory
      every { op.destination.isDirectory } returns false
      result = CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager).canRun(op)
      assertSoftly { result shouldBe false }

      // not a crosssystem copy
      op = prepareBareOperation<RemoteDatasetAttributes, RemoteUssAttributes>(
        false, { MaskedRequester(it, DSMask()) }, { UssRequester(it) }
      )
      every { op.source.isDirectory } returns false
      every { op.destination.isDirectory } returns true
      result = CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager).canRun(op)
      assertSoftly { result shouldBe false }

      // SOURCE IS MEMBER
      op = prepareBareOperation<RemoteMemberAttributes, RemoteUssAttributes>(
        false, { MaskedRequester(it, DSMask()) }, { UssRequester(it) }
      )

      every { (op.sourceAttributes as RemoteMemberAttributes).parentFile } returns mockk()
      val libraryAttributes = mockk<RemoteDatasetAttributes>()
      mockkRequesters(libraryAttributes, "B") { MaskedRequester(it, DSMask()) }

      val attributesService = mockk<RemoteDatasetAttributesService>()
      every { attributesService.getAttributes(any() as MFVirtualFile) } returns libraryAttributes
      every {
        dataOpsManager.testInstance.getAttributesService(libraryAttributes::class.java, any() as Class<MFVirtualFile>)
      } returns attributesService

      every { op.source.isDirectory } returns false
      every { op.destination.isDirectory } returns true

      result = CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager).canRun(op)
      assertSoftly { result shouldBe true }

      // SOURCE IS USS FILE
      op = prepareBareOperation<RemoteUssAttributes, RemoteUssAttributes>(
        true, { MaskedRequester(it, DSMask()) }, { UssRequester(it) }
      )
      every { op.source.isDirectory } returns false
      every { op.destination.isDirectory } returns true

      result = CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager).canRun(op)
      assertSoftly { result shouldBe true }

    }
    // CrossSystemMemberOrUssFileOrSequentialToUssDirMover.run
    should("move a PDS/Member/PS from one system to a USS folder from the other system") {
      var op = prepareBareOperation<RemoteUssAttributes, RemoteUssAttributes>(
        true, { MaskedRequester(it, DSMask()) }, { UssRequester(it) }
      )
      var sourceSynchronized = false
      var contentUploaded = false
      every { op.destination.name } returns "testDestination"
      every { (op.destinationAttributes as RemoteUssAttributes).path } returns "/path/to"
      every { op.destination.path } returns "/path/to"

      every { op.source.name } returns "testSource"
      if (op.sourceAttributes is RemoteUssAttributes) {
        every { (op.sourceAttributes as RemoteUssAttributes).isSymlink } returns false
      }
      every { op.source.charset } returns StandardCharsets.UTF_8
      every { op.source.contentsToByteArray() } answers {
        assertSoftly { sourceSynchronized shouldBe true }
        "Test Content!!!".toByteArray()
      }

      val contentSynchronizer = mockk<ContentSynchronizer>()
      every {
        contentSynchronizer.synchronizeWithRemote(any() as DocumentedSyncProvider, any() as ProgressIndicator)
      } answers {
        sourceSynchronized = true
      }
      every { dataOpsManager.getContentSynchronizer(any() as MFVirtualFile) } returns contentSynchronizer


      val api = mockk<DataAPI>()
      val call = mockk<Call<Void>>()
      every {
        api.writeToUssFile(
          authorizationToken = any() as String,
          filePath = any() as FilePath,
          body = any() as ByteArray,
          xIBMDataType = any() as XIBMDataType
        )
      } answers {
        assertSoftly {
          sourceSynchronized shouldBe true
          args[2].castOrNull<XIBMDataType>()?.type shouldBe XIBMDataType.Type.BINARY
          args[5].castOrNull<FilePath>()?.toString() shouldBe "path/to/testSource"
          args[6].castOrNull<ByteArray>() shouldBe op.source.contentsToByteArray()
        }
        contentUploaded = true
        call
      }
      val response = mockk<Response<Void>>()
      every { response.isSuccessful } returns true
      every { call.execute() } returns response
      val requesters = (op.destinationAttributes as RemoteUssAttributes).requesters
      every {
        zosmfApi.testInstance.getApiWithBytesConverter(DataAPI::class.java, requesters[0].connectionConfig)
      } returns api

      mockkStatic("eu.ibagroup.formainframe.utils.UssFileTagUtilsKt")
      every { setUssFileTag(any() as String, any() as String, any() as ConnectionConfig) } returns Unit

      // SUCCESSFUL CASE WITH USS FILE AS SOURCE
      CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager).run(op)
      assertSoftly {
        contentUploaded shouldBe true
      }

      // SUCCESSFUL CASE FOR MOVE OPERATION
      op = MoveCopyOperation(
        op.source,
        op.sourceAttributes,
        op.destination,
        op.destinationAttributes,
        true,
        op.forceOverwriting,
        op.newName
      )
      var sourceDeleted = false
      every {
        dataOpsManager.testInstance.performOperation(any() as DeleteOperation, any() as ProgressIndicator)
      } answers {
        assertSoftly {
          firstArg<DeleteOperation>().file shouldBe op.source
        }
        sourceDeleted = true
      }
      every { dataOpsManager.testInstance.tryToGetAttributes(any() as MFVirtualFile) } returns op.destinationAttributes
      CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager).run(op)
      assertSoftly { sourceDeleted shouldBe true }

      // FAIL CASE WHEN THE SOURCE USS FILE IS SYMLINK
      sourceSynchronized = false
      contentUploaded = false
      every { (op.sourceAttributes as RemoteUssAttributes).isSymlink } returns true
      every { (op.sourceAttributes as RemoteUssAttributes).symlinkTarget } returns "testTarget"
      val exception = shouldThrow<IllegalArgumentException> {
        CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager).run(op)
      }
      exception.message shouldContain "Impossible to move symlink."
      assertSoftly {
        sourceSynchronized shouldBe false
        contentUploaded shouldBe false
      }

      // SUCCESSFUL CASE WITH SEQUENTIAL DATASET AS SOURCE
      val sourceDatasetAttributes = mockk<RemoteDatasetAttributes>()
      op = MoveCopyOperation(
        op.source,
        sourceDatasetAttributes,
        op.destination,
        op.destinationAttributes,
        op.isMove,
        op.forceOverwriting,
        op.newName
      )
      CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager).run(op)
      assertSoftly {
        contentUploaded shouldBe true
      }

      // SUCCESSFUL CASE WITH MEMBER AS SOURCE
      val sourceMemberAttributes = mockk<RemoteMemberAttributes>()
      op = MoveCopyOperation(
        op.source,
        sourceMemberAttributes,
        op.destination,
        op.destinationAttributes,
        op.isMove,
        op.forceOverwriting,
        op.newName
      )
      CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager).run(op)
      assertSoftly {
        contentUploaded shouldBe true
      }
    }
  }
  context("dataops module: operations/jobs") {
    // SubmitOperationRunner.run
    should("submit job") {}
  }
})
