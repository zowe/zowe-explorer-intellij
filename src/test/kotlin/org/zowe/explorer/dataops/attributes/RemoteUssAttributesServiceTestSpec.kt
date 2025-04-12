/*
 * Copyright (c) 2020 IBA Group.
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

package org.zowe.explorer.dataops.attributes

import com.intellij.openapi.application.ApplicationManager
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.explorer.vfs.MFVirtualFileSystem
import org.zowe.explorer.vfs.MFVirtualFileSystemModel
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.kotlinsdk.UssFile
import org.zowe.kotlinsdk.XIBMDataType
import org.zowe.kotlinsdk.annotations.ZVersion

class RemoteUssAttributesServiceTestSpec : AppInitShouldSpec("dataops/attributes/RemoteUssAttributesService", {
  context("all functions") {
    lateinit var fsModelMock: MFVirtualFileSystemModel
    lateinit var fsRootMock: MFVirtualFile
    lateinit var mfVFileSystemMock: MFVirtualFileSystem

    val dataOpsManager = DataOpsManager.getService()
    every { dataOpsManager.componentManager } returns mockk {
      every { messageBus } returns ApplicationManager.getApplication().messageBus
    }

    beforeEach {
      fsModelMock = mockk<MFVirtualFileSystemModel>()
      fsRootMock = mockk<MFVirtualFile>()
      mfVFileSystemMock = mockk<MFVirtualFileSystem> {
        every { model } returns fsModelMock
        every { root } returns fsRootMock
      }

      mockkObject(MFVirtualFileSystem)
      every { MFVirtualFileSystem.instance } returns mfVFileSystemMock
    }

    should("create USS file") {
      var isCreateChildWithAttributesCalled = false
      val remoteUssAttributesService = spyk(RemoteUssAttributesService(dataOpsManager))

      val attributes = RemoteUssAttributes(
        "test",
        UssFile(),
        "test",
        ConnectionConfig(
          uuid = "test",
          name = "test",
          url = "test",
          isAllowSelfSigned = true,
          zVersion = ZVersion.ZOS_2_3,
          owner = "TSTOWNR"
        )
      )

      val mfVirtualFileMock = mockk<MFVirtualFile>(relaxUnitFun = true) {
        every { isReadable } returns true
      }
      every { mfVirtualFileMock.findChild(any()) } returns mfVirtualFileMock

      every { remoteUssAttributesService.getVirtualFile(any()) } returns null
      every { fsRootMock.findChild(any()) } returns null
      every { fsModelMock.findOrCreate(any(), any(), any(), any()) } answers { callOriginal() }
      every { fsModelMock.createChildWithAttributes(any(), any(), any(), any(), any()) } answers {
        isCreateChildWithAttributesCalled = true
        mfVirtualFileMock
      }

      val createdUSSFile = remoteUssAttributesService.getOrCreateVirtualFile(attributes)

      assertSoftly {
        isCreateChildWithAttributesCalled shouldBe true
        createdUSSFile shouldNotBe null
        createdUSSFile shouldBe mfVirtualFileMock
        remoteUssAttributesService.getAttributes(createdUSSFile) shouldBe attributes
      }
    }
    should("get existing USS file") {
      var isGetVirtualFileCalled = false
      val remoteUssAttributesService = spyk(RemoteUssAttributesService(dataOpsManager))

      val attributes = RemoteUssAttributes(
        "test",
        UssFile(),
        "test",
        ConnectionConfig(
          uuid = "test",
          name = "test",
          url = "test",
          isAllowSelfSigned = true,
          zVersion = ZVersion.ZOS_2_3,
          owner = "TSTOWNR"
        )
      )

      val mfVirtualFileMock = mockk<MFVirtualFile>()
      every { mfVirtualFileMock.isValid } returns true
      every { mfVirtualFileMock.isReadable } returns true
      every { mfVirtualFileMock.isReadable = any() } just runs
      every { mfVirtualFileMock.findChild(any()) } returns mfVirtualFileMock

      every { remoteUssAttributesService.getVirtualFile(any()) } returns null
      every { fsRootMock.findChild(any()) } returns null
      every { fsModelMock.findOrCreate(any(), any(), any(), any()) } answers { callOriginal() }
      every { fsModelMock.createChildWithAttributes(any(), any(), any(), any(), any()) } returns mfVirtualFileMock

      val createdUSSFile = remoteUssAttributesService.getOrCreateVirtualFile(attributes)

      every { remoteUssAttributesService.getVirtualFile(any()) } answers {
        isGetVirtualFileCalled = true
        callOriginal()
      }
      every { fsModelMock.setWritable(any(), any()) } just runs

      val existingUSSFile = remoteUssAttributesService.getOrCreateVirtualFile(attributes)

      assertSoftly {
        isGetVirtualFileCalled shouldBe true
        existingUSSFile shouldBe createdUSSFile
      }
    }
    should("reassign attributes after folder is renamed") {
      var isFileRenameCalled = false
      val remoteUssAttributesService = spyk(RemoteUssAttributesService(dataOpsManager))

      val attributes = RemoteUssAttributes(
        "test",
        UssFile(),
        "test",
        ConnectionConfig(
          uuid = "test",
          name = "test",
          url = "test",
          isAllowSelfSigned = true,
          zVersion = ZVersion.ZOS_2_3,
          owner = "TSTOWNR"
        )
      )

      val mfVirtualFileMock = mockk<MFVirtualFile>()
      every { mfVirtualFileMock.findChild(any()) } returns mfVirtualFileMock
      every { mfVirtualFileMock.isValid } returns true
      every { mfVirtualFileMock.isReadable } returns true
      every { mfVirtualFileMock.isReadable = any() } just runs
      every { mfVirtualFileMock.name } returns "test"
      every { mfVirtualFileMock.fileSystem } returns mfVFileSystemMock
      every { mfVirtualFileMock.rename(any(), any()) } answers {
        isFileRenameCalled = true
      }
      every { mfVFileSystemMock.isValidName(any()) } returns true
      every { mfVFileSystemMock.renameFile(any(), any(), any()) } answers { callOriginal() }

      every { remoteUssAttributesService.getVirtualFile(any()) } returns null
      every { fsRootMock.findChild(any()) } returns null
      every { fsModelMock.findOrCreate(any(), any(), any(), any()) } answers { callOriginal() }
      every { fsModelMock.createChildWithAttributes(any(), any(), any(), any(), any()) } returns mfVirtualFileMock

      val createdUSSFile = remoteUssAttributesService.getOrCreateVirtualFile(attributes)

      every { remoteUssAttributesService.getVirtualFile(any()) } answers { callOriginal() }
      every { fsModelMock.setWritable(any(), any()) } just runs

      val updatedAttributes = RemoteUssAttributes(
        "test1",
        UssFile(),
        "test",
        ConnectionConfig(
          uuid = "test",
          name = "test",
          url = "test",
          isAllowSelfSigned = true,
          zVersion = ZVersion.ZOS_2_3,
          owner = "TSTOWNR"
        )
      )

      remoteUssAttributesService.updateAttributes(createdUSSFile, updatedAttributes)

      val actual = remoteUssAttributesService.getAttributes(createdUSSFile)

      assertSoftly {
        isFileRenameCalled shouldBe true
        actual shouldNotBe null
        actual shouldNotBe attributes
        actual shouldBe updatedAttributes
      }
    }
    should("reassign attributes after folder path is changed") {
      var isFileMoveCalled = false
      val remoteUssAttributesService = spyk(RemoteUssAttributesService(dataOpsManager))

      val attributes = RemoteUssAttributes(
        "test",
        UssFile(),
        "test",
        ConnectionConfig(
          uuid = "test",
          name = "test",
          url = "test",
          isAllowSelfSigned = true,
          zVersion = ZVersion.ZOS_2_3,
          owner = "TSTOWNR"
        )
      )

      val mfVirtualFileMock = mockk<MFVirtualFile>()
      every { mfVirtualFileMock.findChild(any()) } returns mfVirtualFileMock
      every { mfVirtualFileMock.isValid } returns true
      every { mfVirtualFileMock.isReadable } returns true
      every { mfVirtualFileMock.isReadable = any() } just runs
      every { mfVirtualFileMock.name } returns "test"
      every { mfVirtualFileMock.fileSystem } returns mfVFileSystemMock
      every { mfVirtualFileMock.move(any(), any()) } answers {
        isFileMoveCalled = true
      }
      every { mfVFileSystemMock.isValidName(any()) } returns true
      every { mfVFileSystemMock.moveFile(any(), any(), any()) } answers { callOriginal() }

      every { remoteUssAttributesService.getVirtualFile(any()) } returns null
      every { fsRootMock.findChild(any()) } returns null
      every { fsModelMock.findOrCreate(any(), any(), any(), any()) } answers { callOriginal() }
      every { fsModelMock.createChildWithAttributes(any(), any(), any(), any(), any()) } returns mfVirtualFileMock

      val createdUSSFile = remoteUssAttributesService.getOrCreateVirtualFile(attributes)

      every { remoteUssAttributesService.getVirtualFile(any()) } answers { callOriginal() }
      every { fsModelMock.setWritable(any(), any()) } just runs

      val updatedAttributes = RemoteUssAttributes(
        "/test1/test",
        UssFile(),
        "test",
        ConnectionConfig(
          uuid = "test",
          name = "test",
          url = "test",
          isAllowSelfSigned = true,
          zVersion = ZVersion.ZOS_2_3,
          owner = "TSTOWNR"
        )
      )

      remoteUssAttributesService.updateAttributes(createdUSSFile, updatedAttributes)

      val actual = remoteUssAttributesService.getAttributes(createdUSSFile)

      assertSoftly {
        isFileMoveCalled shouldBe true
        actual shouldNotBe null
        actual shouldNotBe attributes
        actual shouldBe updatedAttributes
      }
    }
    should("update writable flag when content is binary") {
      val remoteUssAttributesService = spyk(RemoteUssAttributesService(dataOpsManager))

      val attributes = RemoteUssAttributes(
        "test",
        UssFile(mode = "drwxrwxrwx"),
        "test",
        ConnectionConfig(
          uuid = "test",
          name = "test",
          url = "test",
          isAllowSelfSigned = true,
          zVersion = ZVersion.ZOS_2_3,
          owner = "TSTOWNR"
        )
      )

      val mfVirtualFileMock = mockk<MFVirtualFile>()
      every { mfVirtualFileMock.findChild(any()) } returns mfVirtualFileMock
      every { mfVirtualFileMock.isValid } returns true
      every { mfVirtualFileMock.isReadable } returns true
      every { mfVirtualFileMock.isReadable = any() } just runs
      every { mfVirtualFileMock.isWritable = any<Boolean>() } answers {
        val setWritable = firstArg<Boolean>()
        every {
          mfVirtualFileMock.isWritable
        } returns setWritable
      }

      every { remoteUssAttributesService.getVirtualFile(any()) } returns null
      every { fsRootMock.findChild(any()) } returns null
      every { fsModelMock.findOrCreate(any(), any(), any(), any()) } answers { callOriginal() }
      every { fsModelMock.createChildWithAttributes(any(), any(), any(), any(), any()) } returns mfVirtualFileMock

      val createdUSSFile = remoteUssAttributesService.getOrCreateVirtualFile(attributes)

      val updatedAttributes = RemoteUssAttributes(
        "test",
        UssFile(),
        "test",
        ConnectionConfig(
          uuid = "test",
          name = "test",
          url = "test",
          isAllowSelfSigned = true,
          zVersion = ZVersion.ZOS_2_3,
          owner = "TSTOWNR"
        )
      )

      remoteUssAttributesService.updateWritableFlagAfterContentChanged(createdUSSFile, updatedAttributes)

      assertSoftly {
        createdUSSFile.isWritable shouldBe false
      }
    }
    should("update writable flag from provided attributes") {
      val remoteUssAttributesService = spyk(RemoteUssAttributesService(dataOpsManager))

      val attributes = RemoteUssAttributes(
        "test",
        UssFile(),
        "test",
        ConnectionConfig(
          uuid = "test",
          name = "test",
          url = "test",
          isAllowSelfSigned = true,
          zVersion = ZVersion.ZOS_2_3,
          owner = "TSTOWNR"
        )
      )

      val mfVirtualFileMock = mockk<MFVirtualFile>()
      every { mfVirtualFileMock.isValid } returns true
      every { mfVirtualFileMock.isReadable } returns true
      every { mfVirtualFileMock.isReadable = any() } just runs
      every { mfVirtualFileMock.isWritable = any<Boolean>() } answers {
        val setWritable = firstArg<Boolean>()
        every {
          mfVirtualFileMock.isWritable
        } returns setWritable
      }
      every { mfVirtualFileMock.findChild(any()) } returns null

      every { remoteUssAttributesService.getVirtualFile(any()) } returns null
      every { fsRootMock.findChild(any()) } returns null
      every {
        fsModelMock.findOrCreate(any(), any(), any(), any())
      } answers {
        callOriginal()
      }
      every { fsModelMock.createChildWithAttributes(any(), any(), any(), any(), any()) } returns mfVirtualFileMock

      val createdUSSFile = remoteUssAttributesService.getOrCreateVirtualFile(attributes)

      val updatedAttributes = RemoteUssAttributes(
        "test",
        UssFile(mode = "drwxrwxrwx"),
        "test",
        ConnectionConfig(
          uuid = "test",
          name = "test",
          url = "test",
          isAllowSelfSigned = true,
          zVersion = ZVersion.ZOS_2_3,
          owner = "TSTOWNR"
        )
      )
      updatedAttributes.contentMode = XIBMDataType(XIBMDataType.Type.TEXT)

      remoteUssAttributesService.updateWritableFlagAfterContentChanged(createdUSSFile, updatedAttributes)

      assertSoftly {
        createdUSSFile.isWritable shouldBe updatedAttributes.isWritable
      }
    }
  }
})
