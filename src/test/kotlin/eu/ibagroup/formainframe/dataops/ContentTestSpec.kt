/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops

import io.kotest.core.spec.style.ShouldSpec

class ContentTestSpec : ShouldSpec({
  context("dataops module: content/synchronizer") {
    // syncUtils.removeLastNewLine
    should("remove last blank line") {}
    should("not remove last non-blank line") {}
    // SyncAction.actionPerformed
    should("synchronize the file with the remote file") {}
    // MemberContentSynchronizer.fetchRemoteContentBytes
    should("fetch remote content bytes") {}
    // MemberContentSynchronizer.uploadNewContent
    should("upload new content to the mainframe") {}
    // DocumentedSyncProvider.putInitialContent
    should("put initial file content when the file is read-only") {}
  }
  context("dataops module: content/adapters") {
    // SeqDatasetContentAdapter.adaptContentToMainframe
    should("adapt content for the dataset with variable length") {}
    should("adapt content for the dataset with variable print length") {}
    should("adapt content for the dataset with fixed length") {}
    // SeqDatasetContentAdapter.adaptContentFromMainframe
    should("adapt content for the dataset from mainframe with variable print length") {}
  }
})
