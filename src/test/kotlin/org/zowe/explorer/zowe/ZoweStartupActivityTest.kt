/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.zowe

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.zowe.service.ZoweConfigService
import org.zowe.explorer.zowe.service.ZoweConfigServiceImpl
import org.zowe.explorer.zowe.service.ZoweConfigState
import org.zowe.explorer.zowe.service.ZoweConfigType
import javax.swing.Icon
import kotlin.reflect.KFunction


class ZoweStartupActivityTest : WithApplicationShouldSpec({
  var isDialogCalled = false
  var isConnDeleted = false

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  beforeEach {
    isDialogCalled = false
    isConnDeleted = false
  }

  context("ZoweStartupActivity") {
    val mockedProject = mockk<Project>(relaxed = true)
    every { mockedProject.basePath } returns "test"
    every { mockedProject.name } returns "testProj"
    val mockedZoweConfigService = spyk(ZoweConfigServiceImpl(mockedProject), recordPrivateCalls = true)
    every { mockedProject.service<ZoweConfigService>() } returns mockedZoweConfigService
    every { mockedZoweConfigService.getZoweConfigState(type = any<ZoweConfigType>()) } returns ZoweConfigState.NEED_TO_ADD
    every { mockedZoweConfigService.deleteZoweConfig(type = any<ZoweConfigType>()) } answers {
      isConnDeleted = true
    }
    var ret = mutableListOf(0)
    val showDialogRef: (Project, String, String, Array<String>, Int, Icon) -> Int = Messages::showDialog
    mockkStatic(showDialogRef as KFunction<*>)
    every {
      Messages.showDialog(any<Project>(), any<String>(), any<String>(), any<Array<String>>(), any<Int>(), any<Icon>())
    } answers {
      isDialogCalled = true
      ret[0]
    }

    should("showDialogForDeleteZoweConfigIfNeeded NEED_TO_ADD") {
      showDialogForDeleteZoweConfigIfNeeded(mockedProject, ZoweConfigType.LOCAL)
      isDialogCalled shouldBe false
    }

    should("showDialogForDeleteZoweConfigIfNeeded NOT_EXISTS") {
      every { mockedZoweConfigService.getZoweConfigState(type = any<ZoweConfigType>()) } returns ZoweConfigState.NOT_EXISTS
      showDialogForDeleteZoweConfigIfNeeded(mockedProject, ZoweConfigType.LOCAL)
      isDialogCalled shouldBe false
    }

    should("showDialogForDeleteZoweConfigIfNeeded SYNCHRONIZED") {
      every { mockedZoweConfigService.getZoweConfigState(type = any<ZoweConfigType>()) } returns ZoweConfigState.SYNCHRONIZED
      showDialogForDeleteZoweConfigIfNeeded(mockedProject, ZoweConfigType.LOCAL)
      isDialogCalled shouldBe true
      isConnDeleted shouldBe true
    }

    should("showDialogForDeleteZoweConfigIfNeeded SYNCHRONIZED Global Keep conn") {
      ret[0] = 1
      showDialogForDeleteZoweConfigIfNeeded(mockedProject, ZoweConfigType.GLOBAL)
      isDialogCalled shouldBe true
      isConnDeleted shouldBe false
    }

  }

})