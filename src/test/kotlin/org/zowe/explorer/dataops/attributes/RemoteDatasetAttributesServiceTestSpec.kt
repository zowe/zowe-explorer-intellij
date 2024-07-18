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

package org.zowe.explorer.dataops.attributes

import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.project.Project
import com.intellij.util.SmartList
import com.intellij.util.messages.Topic
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.testutils.mockPrivateField
import org.zowe.explorer.utils.sendTopic
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.explorer.vfs.MFVirtualFileSystem
import org.zowe.explorer.vfs.MFVirtualFileSystemModel
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import org.zowe.kotlinsdk.Dataset
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
        mockPrivateField(
          remoteDsAttrsService,
          remoteDsAttrsService.javaClass.superclass, // MFRemoteAttributesServiceBase
          "attributesToFileMap",
          attributesToFileMapMock
        )
        mockPrivateField(
          remoteDsAttrsService,
          remoteDsAttrsService.javaClass.superclass, // MFRemoteAttributesServiceBase
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
        every { mfVFileMock.path } returns "For Mainframe/test/Data Sets/dsTestVolser/dsTestName/"

        every { fsModelMock.findOrCreate(any(), any(), any(), any()) } returns mfVFileMock

        val attributesToFileMapMock = hashMapOf(remoteDsAttrsIncorrect to mockk<MFVirtualFile>())
        val fileToAttributesMapMock = hashMapOf(mockk<MFVirtualFile>() to remoteDsAttrsIncorrect)
        val remoteDsAttrsService = spyk(RemoteDatasetAttributesService(dataOpsManager))
        mockPrivateField(
          remoteDsAttrsService,
          remoteDsAttrsService.javaClass.superclass, // MFRemoteAttributesServiceBase
          "attributesToFileMap",
          attributesToFileMapMock
        )
        mockPrivateField(
          remoteDsAttrsService,
          remoteDsAttrsService.javaClass.superclass, // MFRemoteAttributesServiceBase
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
//      should("create new virtual file by the provided attributes as the old one is not valid") {
//        var isUpdateAttributesTriggered = false
//
//        val dsInfo = Dataset("dsTestName", volumeSerial = "dsTestVolser")
//        val remoteDsAttrs = RemoteDatasetAttributes(dsInfo, "test", SmartList())
//        val remoteDsAttrsIncorrect = RemoteDatasetAttributes(dsInfo, "test_wrong", SmartList())
//
//        val mfVFileMock = mockk<MFVirtualFile>()
//        every { mfVFileMock.isValid } returns false
//
//        every {
//          fsModelMock.findOrCreate(any(), any(), any(), any())
//        } answers {
//          // TODO: continue from here
//          mfVFileMock
//        }
//
//        val attributesToFileMapMock = hashMapOf(remoteDsAttrs to mfVFileMock, remoteDsAttrsIncorrect to mockk())
//        val fileToAttributesMapMock =
//          hashMapOf(mfVFileMock to remoteDsAttrs, mockk<MFVirtualFile>() to remoteDsAttrsIncorrect)
//        val remoteDsAttrsService = spyk(RemoteDatasetAttributesService(dataOpsManager))
//        mockPrivateField(
//          remoteDsAttrsService,
//          remoteDsAttrsService.javaClass.superclass, // MFRemoteAttributesServiceBase
//          "attributesToFileMap",
//          attributesToFileMapMock
//        )
//        mockPrivateField(
//          remoteDsAttrsService,
//          remoteDsAttrsService.javaClass.superclass, // MFRemoteAttributesServiceBase
//          "fileToAttributesMap",
//          fileToAttributesMapMock
//        )
//
//        val sendTopicRef: (Topic<AttributesListener>, Project) -> AttributesListener = ::sendTopic
//        mockkStatic(sendTopicRef as KFunction<*>)
//        every { sendTopic(AttributesService.ATTRIBUTES_CHANGED, any<ComponentManager>()) } answers {
//          val attributesListenerMock = mockk<AttributesListener>()
//          every {
//            attributesListenerMock.onUpdate(any(), any(), any())
//          } answers {
//            isUpdateAttributesTriggered = true
//          }
//          attributesListenerMock
//        }
//
//        val returned = remoteDsAttrsService.getOrCreateVirtualFile(remoteDsAttrs)
//
//        assertSoftly { isUpdateAttributesTriggered shouldBe true }
//        assertSoftly { returned shouldBe mfVFileMock }
//      }
    }

    // RemoteDatasetAttributesService.mergeAttributes
    // RemoteDatasetAttributesService.continuePathChain
    // RemoteDatasetAttributesService.reassignAttributesAfterUrlFolderRenaming
    // RemoteDatasetAttributesService.reassignAttributesAfterUrlFolderRenaming
  }
})
