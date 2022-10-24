/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer

import io.kotest.core.spec.style.ShouldSpec

class ExplorerTestSpec : ShouldSpec({
  context("explorer module: FilesWorkingSetImpl") {
    // addUssPath
    should("add USS path to a config") {}
    // deleteUssPath
    should("delete USS path from a config") {}
  }
  context("explorer module: ui/FileExplorerViewDropTarget") {
    // drop
    should("perform paste from project files to the mainframe files") {}
    should("perform paste from mainframe files to the project files") {}
    should("perform paste from mainframe z/OS datasets to the USS files") {}
    should("perform paste from mainframe USS files to the datasets") {}
    should("perform paste from mainframe USS files of the first mainframe to the USS files of the other") {}
    // update
    should("highlight places where paste is possible") {}
  }
  context("explorer module: ui/ExplorerPasteProvider") {
    // performPaste
    should("perform paste without conflicts") {}
    should("perform paste accepting conflicts") {}
    should("perform paste declining conflicts") {}
  }
  context("explorer module: ui/UssFileNode") {
    context("navigate") {
//      val requestFocus = true
//
//      mockkObject(MFVirtualFileSystem)
//      every { MFVirtualFileSystem.instance } returns mockk()
//
//      val fileMock = mockk<MFVirtualFile>()
//
//      val projectMock = mockk<Project>()
//
//      val treeStructureMock = mockk<ExplorerTreeStructureBase>()
//      every { treeStructureMock.registerNode(any()) } returns mockk()
//
//      mockkStatic(TreeAnchorizer::class)
//      every { TreeAnchorizer.getService().createAnchor(any()) } returns mockk()
//      mockkObject(UIComponentManager)
//      every { UIComponentManager.INSTANCE.getExplorerContentProvider<Explorer<WorkingSet<UssFileNode>>>(any()) } returns mockk()
//      val explorerTreeNodeMock = mockk<ExplorerTreeNode<*>>()
//
//      val explorerUnitMock = mockk<ExplorerUnit>()
//      every { explorerUnitMock.explorer } returns mockk()
//
//      val ussFileNode = UssFileNode(
//        fileMock,
//        projectMock,
//        explorerTreeNodeMock,
//        explorerUnitMock,
//        treeStructureMock
//      )
//
//      should("perform navigate on file") {
//        ussFileNode.navigate(requestFocus)
//      }
      should("perform navigate on file with failure due to permission denied") {}
      should("perform navigate on file with failure due to error") {}
    }
  }
  context("explorer module: actions/RenameAction") {
    // actionPerformed
    should("perform rename on dataset") {}
    should("perform rename on dataset member") {}
    should("perform rename on USS file") {}
    should("perform rename on USS directory") {}
  }
  context("explorer module: actions/PurgeJobAction") {
    // actionPerformed
    should("perform purge on job successfully") {}
    should("perform purge on job with error") {}
  }
  context("explorer module: actions/GetJobPropertiesAction") {
    // actionPerformed
    should("get job properties") {}
    should("get spool file properties") {}
  }
  context("explorer module: actions/GetFilePropertiesAction") {
    // actionPerformed
    should("get dataset properties") {}
    should("get dataset member properties") {}
    should("get USS file properties") {}
  }
  context("explorer module: actions/ForceRenameAction") {
    // actionPerformed
    should("perform force rename on USS file") {}
    should("not rename dataset") {}
  }
  context("explorer module: actions/ChangeContentModeAction") {
    // isSelected
    should("check if the 'Use binary mode' selected for a file") {}
    // setSelected
    should("select 'Use binary mode' for a file") {}
    should("unselect 'Use binary mode' for a file") {}
  }
  context("explorer module: actions/AllocateDatasetAction") {
    // actionPerformed
    should("perform dataset allocate with default parameters") {}
  }
  context("explorer module: actions/AddMemberAction") {
    // actionPerformed
    should("perform dataset member allocate with default parameters") {}
  }
})
