/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Query
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.fetch.DatasetFileFetchProvider
import org.zowe.explorer.dataops.fetch.FileFetchProvider
import org.zowe.explorer.explorer.FileExplorer
import org.zowe.explorer.explorer.FilesWorkingSetImpl
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.utils.service
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import java.time.LocalDateTime

class FileFetchNodeTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
  }

  context("refresh date test spec") {

    val dataOpsManagerService =
      ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
    val datasetFileFetchProvider = mockk<DatasetFileFetchProvider>()

    val queryMock = mockk<RemoteQuery<ConnectionConfig, DSMask, Unit>>()
    val lastRefreshDate = LocalDateTime.of(2023, 12, 30, 10, 0, 0)

    val presentationMock = mockk<PresentationData>()
    val mockedMask = mockk<DSMask>()
    val mockedProject = mockk<Project>()
    val mockedExplorerTreeNodeParent = mockk<FilesWorkingSetNode>()
    val mockedWorkingSet = mockk<FilesWorkingSetImpl>()
    val mockedExplorer = mockk<FileExplorer>()
    val mockedExplorerTreeStructure = mockk<ExplorerTreeStructureBase>()

    every { mockedWorkingSet.explorer } returns mockedExplorer
    every { mockedExplorerTreeStructure.registerNode(any()) } just Runs
    every { mockedWorkingSet.connectionConfig } returns mockk()

    dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
      @Suppress("UNCHECKED_CAST")
      override fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> getFileFetchProvider(
        requestClass: Class<out R>,
        queryClass: Class<out Query<*, *>>,
        vFileClass: Class<out File>
      ): FileFetchProvider<R, Q, File> {
        return datasetFileFetchProvider as FileFetchProvider<R, Q, File>
      }
    }

    val classUnderTest =
      DSMaskNode(mockedMask, mockedProject, mockedExplorerTreeNodeParent, mockedWorkingSet, mockedExplorerTreeStructure)

    context("updateRefreshDateAndTime") {
      should("should update node presentation with correct refresh date and time given valid query") {
        //given
        val text = " latest refresh: 30 DECEMBER 10:00:00"
        every { presentationMock.addText(any(), any()) } just Runs
        every { datasetFileFetchProvider.getRealQueryInstance(any()) } returns queryMock
        every { datasetFileFetchProvider.findCacheRefreshDateIfPresent(any()) } returns lastRefreshDate

        //when
        classUnderTest.updateRefreshDateAndTime(presentationMock)

        //then
        verify(exactly = 1) { datasetFileFetchProvider.findCacheRefreshDateIfPresent(queryMock) }
        verify(exactly = 1) { presentationMock.addText(text, SimpleTextAttributes.GRAYED_ATTRIBUTES) }
        clearMocks(presentationMock, verificationMarks = true)

      }

      should("should update node presentation with correct refresh date and time given valid query if no real instance found") {
        //given
        val text = " latest refresh: 30 DECEMBER 10:00:00"
        every { presentationMock.addText(any(), any()) } just Runs
        every { datasetFileFetchProvider.getRealQueryInstance(any()) } returns null
        every { datasetFileFetchProvider.findCacheRefreshDateIfPresent(any()) } returns lastRefreshDate

        //when
        classUnderTest.updateRefreshDateAndTime(presentationMock)

        //then
        verify(exactly = 1) { presentationMock.addText(text, SimpleTextAttributes.GRAYED_ATTRIBUTES) }
        clearMocks(presentationMock, verificationMarks = true)
      }

      should("should not update presentation for node if no refresh date found") {
        //given
        every { datasetFileFetchProvider.getRealQueryInstance(any()) } returns null
        every { datasetFileFetchProvider.findCacheRefreshDateIfPresent(any()) } returns null

        //when
        classUnderTest.updateRefreshDateAndTime(presentationMock)

        //then
        verify { presentationMock wasNot Called }
        clearMocks(presentationMock, verificationMarks = true)
      }

      should("should update node presentation with Out-Of-Sync text if no valid query") {
        //given
        val text = " Out of sync"
        every { mockedWorkingSet.connectionConfig } returns null
        every { presentationMock.addText(any(), any()) } just Runs
        every { datasetFileFetchProvider.getRealQueryInstance(any()) } returns null

        //when
        classUnderTest.updateRefreshDateAndTime(presentationMock)

        //then
        verify(exactly = 1) { presentationMock.addText(text, SimpleTextAttributes.GRAYED_ATTRIBUTES) }
      }
    }
    unmockkAll()
  }
})
