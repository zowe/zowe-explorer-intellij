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

class FetchTestSpec : ShouldSpec({
  context("dataops module: fetch") {
    // SpoolFileFetchProvider.reload
    should("reload files cache") {}
    should("reload files cache with failure") {}
    // DatasetFileFetchProvider.cleanupUnusedFile
    should("clean up files attributes") {}
  }
})
