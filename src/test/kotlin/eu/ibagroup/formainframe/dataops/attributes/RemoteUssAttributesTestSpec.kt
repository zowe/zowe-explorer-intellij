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
 */

package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.formainframe.dataops.content.synchronizer.DEFAULT_BINARY_CHARSET
import eu.ibagroup.formainframe.dataops.operations.TsoOperation
import eu.ibagroup.formainframe.dataops.operations.TsoOperationMode
import eu.ibagroup.formainframe.testutils.testAppFixture
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestCredentialsServiceImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.kotlinsdk.*

class RemoteUssAttributesTestSpec : ShouldSpec({

  afterSpec {
    unmockkAll()
    clearAllMocks()
  }

  context("dataops/attributes/RemoteUssAttributesTestSpec") {
    val testUsername = "TSTUSR"
    val testOwner = "TSTOWNR"
    val testRootPath = "test_root_path"
    val testUrl = "test_url"

    lateinit var connectionConfig: ConnectionConfig
    lateinit var ussFile: UssFile

    val credentialServiceMock: CredentialService
    if (testAppFixture == null) {
      credentialServiceMock = mockk<CredentialService>()
      every { credentialServiceMock.getUsernameByKey(any<String>()) } returns testUsername

      mockkStatic(ApplicationManager::getApplication)
      every { ApplicationManager.getApplication() } returns mockk {
        every { getService(CredentialService::class.java) } returns credentialServiceMock
      }
    } else {
      credentialServiceMock = CredentialService.getService() as TestCredentialsServiceImpl
      credentialServiceMock.testInstance = object : TestCredentialsServiceImpl() {
        override fun getUsernameByKey(connectionConfigUuid: String): String {
          return testUsername
        }
      }
    }

    beforeEach {
      if (testAppFixture == null) {
        every { credentialServiceMock.getUsernameByKey(any<String>()) } returns testUsername
      } else {
        (credentialServiceMock as TestCredentialsServiceImpl).testInstance = object : TestCredentialsServiceImpl() {
          override fun getUsernameByKey(connectionConfigUuid: String): String {
            return testUsername
          }
        }
      }

      ussFile = mockk<UssFile> {
        every { name } returns "test_name"
        every { isDirectory } returns false
        every { fileMode } returns mockk<FileMode>()
        every { size } returns 0L
        every { uid } returns 0L
        every { user } returns testUsername
        every { gid } returns 0L
        every { groupId } returns "test_group_id"
        every { modificationTime } returns "test_modification_time"
        every { target } returns "test_target"
      }
      connectionConfig = mockk<ConnectionConfig> {
        every { uuid } returns "test_uuid"
        every { owner } returns testOwner
      }
    }

    context("constructPath") {
      should("construct a USS path of the file as a root path when the file name is empty") {
        every { ussFile.name } returns ""

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.path shouldBe testRootPath }
      }
      should("construct a USS path of the file as a root path when the file name is a current directory") {
        every { ussFile.name } returns CURRENT_DIR_NAME

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.path shouldBe testRootPath }
      }
      should("construct a USS path of the file as a root path + the file name when the file name is present, not equals to the current dir name and the root path is a USS delimiter") {
        val remoteUssAttributes = RemoteUssAttributes(USS_DELIMITER, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.path shouldBe "$USS_DELIMITER${ussFile.name}" }
      }
      should("construct a USS path of the file as a root path + a USS delimiter + the file name in every other case") {
        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.path shouldBe "$testRootPath$USS_DELIMITER${ussFile.name}" }
      }
    }

    context("RemoteUssAttributes.isSymlink") {
      should("return true if the USS file is a symlink") {
        every { ussFile.size } returns null

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isSymlink shouldBe true }
      }
      should("return false if the USS file is not a symlink") {
        every { ussFile.target } returns null

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isSymlink shouldBe false }
      }
    }

    context("RemoteUssAttributes.isWritable") {
      should("return true if the USS file mode for owner is write and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.WRITE.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isWritable shouldBe true }
      }
      should("return true if the USS file mode for owner is write and the owner is not present but the username is present") {
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.WRITE.mode
        }
        every { connectionConfig.owner } returns ""

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isWritable shouldBe true }
      }
      should("return true if the USS file mode for all users is write and the owner is not present and the username is not the same as the USS file's") {
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { all } returns FileModeValue.WRITE.mode
        }
        every { connectionConfig.owner } returns ""
        if (testAppFixture == null) {
          every { credentialServiceMock.getUsernameByKey(any<String>()) } returns "TSTFAKE"
        } else {
          (credentialServiceMock as TestCredentialsServiceImpl).testInstance = object : TestCredentialsServiceImpl() {
            override fun getUsernameByKey(connectionConfigUuid: String): String {
              return "TSTFAKE"
            }
          }
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isWritable shouldBe true }
      }
      should("return false if the USS file mode is not present") {
        val remoteUssAttributes = RemoteUssAttributes(
          path = testRootPath,
          isDirectory = ussFile.isDirectory,
          fileMode = null,
          url = testUrl,
          requesters = mutableListOf(UssRequester(connectionConfig)),
          length = ussFile.size ?: 0L,
          uid = ussFile.uid,
          owner = ussFile.user,
          gid = ussFile.gid,
          groupId = ussFile.groupId,
          modificationTime = ussFile.modificationTime,
          symlinkTarget = ussFile.target,
          charset = DEFAULT_BINARY_CHARSET
        )

        assertSoftly { remoteUssAttributes.isWritable shouldBe false }
      }
      should("return true if the USS file mode for owner is write-execute and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.WRITE_EXECUTE.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isWritable shouldBe true }
      }
      should("return true if the USS file mode for owner is read-write and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.READ_WRITE.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isWritable shouldBe true }
      }
      should("return true if the USS file mode for owner is read-write-execute and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.READ_WRITE_EXECUTE.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isWritable shouldBe true }
      }
    }
    context("RemoteUssAttributes.isReadable") {
      should("return true if the USS file mode for owner is read and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.READ.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isReadable shouldBe true }
      }
      should("return true if the USS file mode for owner is read-write and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.READ_WRITE.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isReadable shouldBe true }
      }
      should("return true if the USS file mode for owner is read-execute and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.READ_EXECUTE.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isReadable shouldBe true }
      }
      should("return true if the USS file mode for owner is read-write-execute and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.READ_WRITE_EXECUTE.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isReadable shouldBe true }
      }
      should("return false if the USS file mode for owner is none and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.NONE.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isReadable shouldBe false }
      }
    }
    context("RemoteUssAttributes.isExecutable") {
      should("return true if the USS file mode for owner is execute and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.EXECUTE.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isExecutable shouldBe true }
      }
      should("return true if the USS file mode for owner is read-execute and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.READ_EXECUTE.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isExecutable shouldBe true }
      }
      should("return true if the USS file mode for owner is write-execute and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.WRITE_EXECUTE.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isExecutable shouldBe true }
      }
      should("return true if the USS file mode for owner is read-write-execute and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.READ_WRITE_EXECUTE.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isExecutable shouldBe true }
      }
      should("return false if the USS file mode for owner is write and the owner is present") {
        every { ussFile.user } returns testOwner
        every { ussFile.fileMode } returns mockk<FileMode> {
          every { owner } returns FileModeValue.WRITE.mode
        }

        val remoteUssAttributes = RemoteUssAttributes(testRootPath, ussFile, testUrl, connectionConfig)

        assertSoftly { remoteUssAttributes.isExecutable shouldBe false }
      }
    }
  }
})
