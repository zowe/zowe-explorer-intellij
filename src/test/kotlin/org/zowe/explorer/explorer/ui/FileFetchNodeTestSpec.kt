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

package org.zowe.explorer.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Query
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.fetch.DatasetFileFetchProvider
import org.zowe.explorer.dataops.fetch.FileFetchProvider
import org.zowe.explorer.explorer.FileExplorer
import org.zowe.explorer.explorer.FilesWorkingSetImpl
import io.mockk.*
import org.zowe.explorer.explorer.UIComponentManager
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.vfs.MFVirtualFile
import java.time.LocalDateTime

class FileFetchNodeTestSpec : AppInitShouldSpec("explorer/ui/FileFetchNode", {
  context("refresh date test spec") {
    var addTextCalledCount = 0

    val uiComponentManager = UIComponentManager.getService()
    every { uiComponentManager.getExplorerContentProvider(any<Class<FileExplorer>>()) } returns mockk()

    val datasetFileFetchProvider = mockk<DatasetFileFetchProvider>()

    val queryMock = mockk<RemoteQuery<ConnectionConfig, DSMask, Unit>>()
    val lastRefreshDate = LocalDateTime.of(2023, 12, 30, 10, 0, 0)

    val mockedMask = mockk<DSMask>()
    val mockedProject = mockk<Project>()
    val mockedExplorerTreeNodeParent = mockk<FilesWorkingSetNode>()
    val mockedExplorer = mockk<FileExplorer>()
    val mockedWorkingSet = mockk<FilesWorkingSetImpl> {
      every { explorer } returns mockedExplorer
      every { connectionConfig } returns mockk()
    }
    val mockedExplorerTreeStructure = mockk<ExplorerTreeStructureBase> {
      every { registerNode(any()) } just Runs
    }

    lateinit var presentationMock: PresentationData

    val dataOpsManagerService = DataOpsManager.getService()
    every {
      dataOpsManagerService.getFileFetchProvider(DSMask::class.java, RemoteQuery::class.java, MFVirtualFile::class.java)
    } returns datasetFileFetchProvider as FileFetchProvider<DSMask, Query<DSMask, Unit>, MFVirtualFile>

    val classUnderTest =
      DSMaskNode(mockedMask, mockedProject, mockedExplorerTreeNodeParent, mockedWorkingSet, mockedExplorerTreeStructure)

    beforeEach {
      addTextCalledCount = 0

      every { datasetFileFetchProvider.getRealQueryInstance(any()) } returns queryMock
      every { datasetFileFetchProvider.findCacheRefreshDateIfPresent(any()) } returns lastRefreshDate

      presentationMock = mockk {
        every {
          addText(any<String>(), any<SimpleTextAttributes>())
        } answers {
          addTextCalledCount++
        }
      }
    }

    context("updateRefreshDateAndTime") {
      should("should update node presentation with correct refresh date and time given valid query") {
        //given
        val text = "refreshed: 30 DEC 10:00:00"

        //when
        classUnderTest.updateRefreshDateAndTime(presentationMock)

        //then
        verify(exactly = 1) { datasetFileFetchProvider.findCacheRefreshDateIfPresent(queryMock) }
        assertSoftly { addTextCalledCount shouldBe 2 }
      }

      should("should update node presentation with correct refresh date and time given valid query if no real instance found") {
        //given
        val text = "refreshed: 30 DEC 10:00:00"
        every { datasetFileFetchProvider.getRealQueryInstance(any()) } returns null

        //when
        classUnderTest.updateRefreshDateAndTime(presentationMock)

        //then
        assertSoftly { addTextCalledCount shouldBe 2 }
      }

      should("should not update presentation for node if no refresh date found") {
        //given
        every { datasetFileFetchProvider.getRealQueryInstance(any()) } returns null
        every { datasetFileFetchProvider.findCacheRefreshDateIfPresent(any()) } returns null

        //when
        classUnderTest.updateRefreshDateAndTime(presentationMock)

        //then
        assertSoftly { addTextCalledCount shouldBe 0 }
      }

      should("should update node presentation with Out-Of-Sync text if no valid query") {
        //given
        val text = "Out of sync"
        every { mockedWorkingSet.connectionConfig } returns null
        every { datasetFileFetchProvider.getRealQueryInstance(any()) } returns null

        //when
        classUnderTest.updateRefreshDateAndTime(presentationMock)

        //then
        assertSoftly { addTextCalledCount shouldBe 2 }
      }
    }
  }
})
