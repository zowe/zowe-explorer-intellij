/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops

import io.kotest.core.spec.style.ShouldSpec

class OperationsTestSpec : ShouldSpec({
  context("dataops module: operations/RenameOperationRunner") {
    // run
    should("perform USS file rename") {}
    should("perform dataset rename") {}
    should("perform dataset member rename") {}
  }
  context("dataops module: operations/mover") {
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
    // RemoteToLocalFileMover.canRun
    should("return true when we try to move USS file to a local folder") {}
    should("return false when we try to move USS file to a local file") {}
    // RemoteToLocalFileMover.run
    should("move a remote USS binary file to a local folder") {}
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
  }
  context("dataops module: operations/jobs") {
    // SubmitOperationRunner.run
    should("submit job") {}
  }
})
