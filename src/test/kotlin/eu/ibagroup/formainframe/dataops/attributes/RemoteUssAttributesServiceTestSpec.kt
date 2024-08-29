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
 */

package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystemModel
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.zowe.kotlinsdk.UssFile
import org.zowe.kotlinsdk.XIBMDataType
import kotlin.reflect.KFunction

class RemoteUssAttributesServiceTestSpec : ShouldSpec({
  context("dataops module: attributes/RemoteUssAttributes") {
    lateinit var fsModelMock: MFVirtualFileSystemModel
    lateinit var fsRootMock: MFVirtualFile
    lateinit var mfVFileSystemMock: MFVirtualFileSystem
    lateinit var dataOpsManager: DataOpsManager

    beforeEach {
      fsModelMock = mockk<MFVirtualFileSystemModel>()
      fsRootMock = mockk<MFVirtualFile>()
      mfVFileSystemMock = mockk<MFVirtualFileSystem>()
      dataOpsManager = mockk<DataOpsManager>()

      every { mfVFileSystemMock.model } returns fsModelMock
      every { mfVFileSystemMock.root } returns fsRootMock
      mockkObject(MFVirtualFileSystem)
      every { MFVirtualFileSystem.instance } returns mfVFileSystemMock
      every { dataOpsManager.componentManager } returns mockk()

      val sendTopicRef: (Topic<AttributesListener>, Project) -> AttributesListener = ::sendTopic
      mockkStatic(sendTopicRef as KFunction<*>)
      every { sendTopic(AttributesService.ATTRIBUTES_CHANGED, any<ComponentManager>()) } answers {
        val attributesListenerMock = mockk<AttributesListener>()
        every {
          attributesListenerMock.onUpdate(any(), any(), any())
        } just runs
        every {
          attributesListenerMock.onCreate(any(), any())
        } just runs
        attributesListenerMock
      }

      mockkStatic(ApplicationManager::getApplication)
      every { ApplicationManager.getApplication().assertWriteAccessAllowed() } just runs
      every { ApplicationManager.getApplication().invokeAndWait(any()) } answers {
        firstArg<Runnable>().run()
      }
      every { ApplicationManager.getApplication().runWriteAction<Any>(any()) } answers  {
        firstArg<Computable<() -> Unit>>().compute()
      }
    }

    afterEach {
      unmockkAll()
      clearAllMocks()
    }

    should("create USS file") {
      var isCreateChildWithAttributesCalled = false
      val remoteUssAttributesService = spyk(RemoteUssAttributesService(dataOpsManager))

      val attributes = RemoteUssAttributes(
        "test",
        UssFile(),
        "test",
        ConnectionConfig()
      )

      val mfVirtualFileMock = mockk<MFVirtualFile>()
      every { mfVirtualFileMock.findChild(any()) } returns mfVirtualFileMock
      every { mfVirtualFileMock.isReadable } returns true
      every { mfVirtualFileMock.isReadable = any() } just runs

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
        ConnectionConfig()
      )

      val mfVirtualFileMock = mockk<MFVirtualFile>()
      every { mfVirtualFileMock.findChild(any()) } returns mfVirtualFileMock
      every { mfVirtualFileMock.isValid } returns true
      every { mfVirtualFileMock.isReadable } returns true
      every { mfVirtualFileMock.isReadable = any() } just runs

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
        ConnectionConfig()
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
        ConnectionConfig()
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
        ConnectionConfig()
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
        ConnectionConfig()
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
        ConnectionConfig()
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
        ConnectionConfig()
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
        ConnectionConfig()
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
        UssFile(mode = "drwxrwxrwx"),
        "test",
        ConnectionConfig()
      )
      updatedAttributes.contentMode = XIBMDataType(XIBMDataType.Type.TEXT)

      remoteUssAttributesService.updateWritableFlagAfterContentChanged(createdUSSFile, updatedAttributes)

      assertSoftly {
        createdUSSFile.isWritable shouldBe updatedAttributes.isWritable
      }
    }
  }
})
