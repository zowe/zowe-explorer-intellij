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

import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.project.Project
import com.intellij.util.SmartList
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.testutils.setPrivateFieldValue
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystemModel
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import org.zowe.kotlinsdk.Dataset
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.HasMigrated
import kotlin.reflect.KFunction

class RemoteDatasetAttributesServiceTestSpec : ShouldSpec({
  context("dataops module: attributes/RemoteDatasetAttributesService") {
    context("getOrCreateVirtualFile") {
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
      }

      afterEach {
        unmockkAll()
        clearAllMocks()
      }

      should("get existing virtual file by the provided attributes") {
        var isUpdateAttributesTriggered = false

        val dsInfo = Dataset("dsTestName", volumeSerial = "dsTestVolser")
        val remoteDsAttrs = RemoteDatasetAttributes(dsInfo, "test", SmartList())
        val remoteDsAttrsIncorrect = RemoteDatasetAttributes(dsInfo, "test_wrong", SmartList())

        val mfVFileMock = mockk<MFVirtualFile>()
        every { mfVFileMock.isValid } returns true

        every { fsModelMock.findOrCreate(any(), any(), any(), any()) } returns mfVFileMock

        val attributesToFileMapMock = hashMapOf(remoteDsAttrs to mfVFileMock, remoteDsAttrsIncorrect to mockk())
        val fileToAttributesMapMock =
          hashMapOf(mfVFileMock to remoteDsAttrs, mockk<MFVirtualFile>() to remoteDsAttrsIncorrect)
        val remoteDsAttrsService = spyk(RemoteDatasetAttributesService(dataOpsManager))
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "attributesToFileMap",
          attributesToFileMapMock
        )
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "fileToAttributesMap",
          fileToAttributesMapMock
        )

        val sendTopicRef: (Topic<AttributesListener>, Project) -> AttributesListener = ::sendTopic
        mockkStatic(sendTopicRef as KFunction<*>)
        every { sendTopic(AttributesService.ATTRIBUTES_CHANGED, any<ComponentManager>()) } answers {
          val attributesListenerMock = mockk<AttributesListener>()
          every {
            attributesListenerMock.onUpdate(any(), any(), any())
          } answers {
            isUpdateAttributesTriggered = true
          }
          attributesListenerMock
        }

        val returned = remoteDsAttrsService.getOrCreateVirtualFile(remoteDsAttrs)

        assertSoftly { isUpdateAttributesTriggered shouldBe true }
        assertSoftly { returned shouldBe mfVFileMock }
      }
      should("create and return virtual file for the provided attributes as it is not yet exist") {
        var isCreateAttributesTriggered = false

        val dsInfo = Dataset("dsTestName", volumeSerial = "dsTestVolser")
        val remoteDsAttrs = RemoteDatasetAttributes(dsInfo, "test", SmartList())
        val remoteDsAttrsIncorrect = RemoteDatasetAttributes(dsInfo, "test_wrong", SmartList())

        val mfVFileMock = mockk<MFVirtualFile>()
        every { mfVFileMock.name } returns "dsTestName"
        every { mfVFileMock.path } returns "test/Data Sets/dsTestVolser/dsTestName/"

        every { fsModelMock.findOrCreate(any(), any(), any(), any()) } returns mfVFileMock

        val attributesToFileMapMock = hashMapOf(remoteDsAttrsIncorrect to mockk<MFVirtualFile>())
        val fileToAttributesMapMock = hashMapOf(mockk<MFVirtualFile>() to remoteDsAttrsIncorrect)
        val remoteDsAttrsService = spyk(RemoteDatasetAttributesService(dataOpsManager))
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "attributesToFileMap",
          attributesToFileMapMock
        )
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "fileToAttributesMap",
          fileToAttributesMapMock
        )

        val sendTopicRef: (Topic<AttributesListener>, Project) -> AttributesListener = ::sendTopic
        mockkStatic(sendTopicRef as KFunction<*>)
        every { sendTopic(AttributesService.ATTRIBUTES_CHANGED, any<ComponentManager>()) } answers {
          val attributesListenerMock = mockk<AttributesListener>()
          every {
            attributesListenerMock.onCreate(any(), any())
          } answers {
            isCreateAttributesTriggered = true
          }
          attributesListenerMock
        }

        val returned = remoteDsAttrsService.getOrCreateVirtualFile(remoteDsAttrs)

        assertSoftly { isCreateAttributesTriggered shouldBe true }
        assertSoftly { returned.name shouldBeEqual mfVFileMock.name }
        assertSoftly { returned.path shouldBeEqual mfVFileMock.path }
      }
      should("create and return migrated virtual file for the provided attributes as it is not yet exist") {
        var isCreateAttributesTriggered = false

        val dsInfo = Dataset("dsTestName")
        val remoteDsAttrs = RemoteDatasetAttributes(dsInfo, "test", SmartList())
        val remoteDsAttrsIncorrect = RemoteDatasetAttributes(dsInfo, "test_wrong", SmartList())

        val mfVFileMock = mockk<MFVirtualFile>()
        every { mfVFileMock.name } returns "dsTestName"
        every { mfVFileMock.path } returns "test/Data Sets/dsTestVolser/dsTestName/"

        every { fsModelMock.findOrCreate(any(), any(), any(), any()) } returns mfVFileMock

        val attributesToFileMapMock = hashMapOf(remoteDsAttrsIncorrect to mockk<MFVirtualFile>())
        val fileToAttributesMapMock = hashMapOf(mockk<MFVirtualFile>() to remoteDsAttrsIncorrect)
        val remoteDsAttrsService = spyk(RemoteDatasetAttributesService(dataOpsManager))
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "attributesToFileMap",
          attributesToFileMapMock
        )
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "fileToAttributesMap",
          fileToAttributesMapMock
        )

        val sendTopicRef: (Topic<AttributesListener>, Project) -> AttributesListener = ::sendTopic
        mockkStatic(sendTopicRef as KFunction<*>)
        every { sendTopic(AttributesService.ATTRIBUTES_CHANGED, any<ComponentManager>()) } answers {
          val attributesListenerMock = mockk<AttributesListener>()
          every {
            attributesListenerMock.onCreate(any(), any())
          } answers {
            isCreateAttributesTriggered = true
          }
          attributesListenerMock
        }

        val returned = remoteDsAttrsService.getOrCreateVirtualFile(remoteDsAttrs)

        assertSoftly { isCreateAttributesTriggered shouldBe true }
        assertSoftly { returned.name shouldBeEqual mfVFileMock.name }
        assertSoftly { returned.path shouldBeEqual mfVFileMock.path }
      }
      should("create new virtual file by the provided attributes as the old one is not valid") {
        var isCreateAttributesTriggered = false

        val dsInfo = Dataset("dsTestName", volumeSerial = "dsTestVolser")
        val remoteDsAttrs = RemoteDatasetAttributes(dsInfo, "test", SmartList())
        val remoteDsAttrsIncorrect = RemoteDatasetAttributes(dsInfo, "test_wrong", SmartList())

        val mfVFileMock = mockk<MFVirtualFile>()
        every { mfVFileMock.isValid } returns false

        every {
          fsModelMock.findOrCreate(any(), any(), any(), any())
        } answers {
          mfVFileMock
        }

        val attributesToFileMapMock = hashMapOf(remoteDsAttrs to mfVFileMock, remoteDsAttrsIncorrect to mockk())
        val fileToAttributesMapMock =
          hashMapOf(mfVFileMock to remoteDsAttrs, mockk<MFVirtualFile>() to remoteDsAttrsIncorrect)
        val remoteDsAttrsService = spyk(RemoteDatasetAttributesService(dataOpsManager))
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "attributesToFileMap",
          attributesToFileMapMock
        )
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "fileToAttributesMap",
          fileToAttributesMapMock
        )
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "subDirectory",
          mockk<MFVirtualFile>()
        )

        val sendTopicRef: (Topic<AttributesListener>, Project) -> AttributesListener = ::sendTopic
        mockkStatic(sendTopicRef as KFunction<*>)
        every { sendTopic(AttributesService.ATTRIBUTES_CHANGED, any<ComponentManager>()) } answers {
          val attributesListenerMock = mockk<AttributesListener>()
          every {
            attributesListenerMock.onCreate(any(), any())
          } answers {
            isCreateAttributesTriggered = true
          }
          attributesListenerMock
        }

        val returned = remoteDsAttrsService.getOrCreateVirtualFile(remoteDsAttrs)

        assertSoftly { isCreateAttributesTriggered shouldBe true }
        assertSoftly { returned shouldBe mfVFileMock }
      }
      should("create new virtual file by the provided attributes as the old attributes do not exist") {
        var isCreateAttributesTriggered = false

        val dsInfo = Dataset("dsTestName", volumeSerial = "dsTestVolser")
        val remoteDsAttrs = RemoteDatasetAttributes(dsInfo, "test", SmartList())

        val mfVFileMock = mockk<MFVirtualFile>()
        every { mfVFileMock.isValid } returns true

        every { fsModelMock.findOrCreate(any(), any(), any(), any()) } returns mfVFileMock

        val attributesToFileMapMock = hashMapOf(remoteDsAttrs to mfVFileMock)
        val fileToAttributesMapMock = hashMapOf(mfVFileMock to null)
        val remoteDsAttrsService = spyk(RemoteDatasetAttributesService(dataOpsManager))
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "attributesToFileMap",
          attributesToFileMapMock
        )
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "fileToAttributesMap",
          fileToAttributesMapMock
        )

        val sendTopicRef: (Topic<AttributesListener>, Project) -> AttributesListener = ::sendTopic
        mockkStatic(sendTopicRef as KFunction<*>)
        every { sendTopic(AttributesService.ATTRIBUTES_CHANGED, any<ComponentManager>()) } answers {
          val attributesListenerMock = mockk<AttributesListener>()
          every {
            attributesListenerMock.onCreate(any(), any())
          } answers {
            isCreateAttributesTriggered = true
          }
          attributesListenerMock
        }

        val returned = remoteDsAttrsService.getOrCreateVirtualFile(remoteDsAttrs)

        assertSoftly { isCreateAttributesTriggered shouldBe true }
        assertSoftly { returned shouldBe mfVFileMock }
      }
    }
    context("clearAttributes") {
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
      }

      afterEach {
        unmockkAll()
        clearAllMocks()
      }

      should("clear attributes for the provided virtual file") {
        var isDeleteAttributesTriggered = false

        val dsInfo = Dataset("dsTestName", volumeSerial = "dsTestVolser")
        val remoteDsAttrs = RemoteDatasetAttributes(dsInfo, "test", SmartList())
        val mfVFileMock = mockk<MFVirtualFile>()
        val attributesToFileMapMock = hashMapOf(remoteDsAttrs to mfVFileMock)
        val fileToAttributesMapMock = hashMapOf(mfVFileMock to remoteDsAttrs)

        val remoteDsAttrsService = spyk(RemoteDatasetAttributesService(dataOpsManager))

        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "attributesToFileMap",
          attributesToFileMapMock
        )
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "fileToAttributesMap",
          fileToAttributesMapMock
        )

        val sendTopicRef: (Topic<AttributesListener>, Project) -> AttributesListener = ::sendTopic
        mockkStatic(sendTopicRef as KFunction<*>)
        every { sendTopic(AttributesService.ATTRIBUTES_CHANGED, any<ComponentManager>()) } answers {
          val attributesListenerMock = mockk<AttributesListener>()
          every {
            attributesListenerMock.onDelete(any(), any())
          } answers {
            val fileToClearAttributes = secondArg<MFVirtualFile>()
            assertSoftly { fileToClearAttributes shouldBe mfVFileMock }
            isDeleteAttributesTriggered = true
          }
          attributesListenerMock
        }

        remoteDsAttrsService.clearAttributes(mfVFileMock)

        assertSoftly { isDeleteAttributesTriggered shouldBe true }
      }
      should("not clear attributes for the provided virtual file as there is no attributes for it") {
        var isDeleteAttributesTriggered = false

        val dsInfo = Dataset("dsTestName", volumeSerial = "dsTestVolser")
        val remoteDsAttrs = RemoteDatasetAttributes(dsInfo, "test", SmartList())
        val mfVFileMock = mockk<MFVirtualFile>()
        val mfVFileMockOther = mockk<MFVirtualFile>()
        val fileToAttributesMapMock = hashMapOf(mfVFileMockOther to remoteDsAttrs)

        val remoteDsAttrsService = spyk(RemoteDatasetAttributesService(dataOpsManager))

        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "fileToAttributesMap",
          fileToAttributesMapMock
        )

        val sendTopicRef: (Topic<AttributesListener>, Project) -> AttributesListener = ::sendTopic
        mockkStatic(sendTopicRef as KFunction<*>)
        every { sendTopic(AttributesService.ATTRIBUTES_CHANGED, any<ComponentManager>()) } answers {
          val attributesListenerMock = mockk<AttributesListener>()
          every {
            attributesListenerMock.onDelete(any(), any())
          } answers {
            val fileToClearAttributes = secondArg<MFVirtualFile>()
            assertSoftly { fileToClearAttributes shouldBe mfVFileMock }
            isDeleteAttributesTriggered = true
          }
          attributesListenerMock
        }

        remoteDsAttrsService.clearAttributes(mfVFileMock)

        assertSoftly { isDeleteAttributesTriggered shouldBe false }
      }
    }
    context("updateAttributes") {
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
      }

      afterEach {
        unmockkAll()
        clearAllMocks()
      }

      should("update attributes for the provided virtual file") {
        var isUpdateAttributesTriggered = false
        var isMoveCalled = false
        var isRenameCalled = false

        val oldDsInfo = Dataset("dsTestName", volumeSerial = "dsTestVolser")
        val newDsInfo = Dataset(
          "dsTestNameNew",
          volumeSerial = "dsTestVolserNew",
          migrated = HasMigrated.NO,
          datasetOrganization = DatasetOrganization.PO
        )
        val oldRemoteDsAttrs = RemoteDatasetAttributes(oldDsInfo, "test", SmartList())
        val newRemoteDsAttrs = RemoteDatasetAttributes(newDsInfo, "test", SmartList())

        val mfVFileMock = mockk<MFVirtualFile>()
        every {
          mfVFileMock.move(any(), any())
        } answers {
          isMoveCalled = true
        }
        every {
          mfVFileMock.rename(any(), any())
        } answers {
          isRenameCalled = true
        }

        val attributesToFileMapMock = hashMapOf(oldRemoteDsAttrs to mfVFileMock)
        val fileToAttributesMapMock = hashMapOf(mfVFileMock to oldRemoteDsAttrs)

        val remoteDsAttrsService = spyk(RemoteDatasetAttributesService(dataOpsManager))

        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "attributesToFileMap",
          attributesToFileMapMock
        )
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "fileToAttributesMap",
          fileToAttributesMapMock
        )
        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "subDirectory",
          mockk<MFVirtualFile>()
        )

        val sendTopicRef: (Topic<AttributesListener>, Project) -> AttributesListener = ::sendTopic
        mockkStatic(sendTopicRef as KFunction<*>)
        every { sendTopic(AttributesService.ATTRIBUTES_CHANGED, any<ComponentManager>()) } answers {
          val attributesListenerMock = mockk<AttributesListener>()
          every {
            attributesListenerMock.onUpdate(any(), any(), any())
          } answers {
            val newAttributesActual = secondArg<RemoteDatasetAttributes>()
            assertSoftly { newAttributesActual shouldBe newRemoteDsAttrs }
            isUpdateAttributesTriggered = true
          }
          attributesListenerMock
        }
        every { fsModelMock.findOrCreate(any(), any(), any(), any()) } returns mfVFileMock
        every { fsModelMock.changeFileType(any(), any(), any()) } just Runs

        remoteDsAttrsService.updateAttributes(mfVFileMock, newRemoteDsAttrs)

        assertSoftly { isUpdateAttributesTriggered shouldBe true }
        assertSoftly { isMoveCalled shouldBe true }
        assertSoftly { isRenameCalled shouldBe true }
      }
      should("not update attributes for the provided virtual file as there is no attributes for it") {
        var isUpdateAttributesTriggered = false

        val dsInfo = Dataset("dsTestName", volumeSerial = "dsTestVolser")
        val remoteDsAttrs = RemoteDatasetAttributes(dsInfo, "test", SmartList())
        val mfVFileMock = mockk<MFVirtualFile>()
        val mfVFileMockOther = mockk<MFVirtualFile>()
        val fileToAttributesMapMock = hashMapOf(mfVFileMockOther to remoteDsAttrs)

        val remoteDsAttrsService = spyk(RemoteDatasetAttributesService(dataOpsManager))

        setPrivateFieldValue(
          remoteDsAttrsService,
          MFRemoteAttributesServiceBase::class.java,
          "fileToAttributesMap",
          fileToAttributesMapMock
        )

        val sendTopicRef: (Topic<AttributesListener>, Project) -> AttributesListener = ::sendTopic
        mockkStatic(sendTopicRef as KFunction<*>)
        every { sendTopic(AttributesService.ATTRIBUTES_CHANGED, any<ComponentManager>()) } answers {
          val attributesListenerMock = mockk<AttributesListener>()
          every {
            attributesListenerMock.onUpdate(any(), any(), any())
          } answers {
            isUpdateAttributesTriggered = true
          }
          attributesListenerMock
        }

        remoteDsAttrsService.updateAttributes(mfVFileMock, mockk<RemoteDatasetAttributes>())

        assertSoftly { isUpdateAttributesTriggered shouldBe false }
      }
    }
  }
})