/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.api

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.testutils.MockkAwareShouldSpec
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession

class ConfirmingHostnameVerifierTestSpec : MockkAwareShouldSpec({
  context("ConfirmingHostnameVerifier") {
    should("accept hostname when default verifier passes") {
      val defaultVerifier = mockk<HostnameVerifier>()
      every { defaultVerifier.verify("example.com", any()) } returns true

      mockkStatic(HttpsURLConnection::class)
      every { HttpsURLConnection.getDefaultHostnameVerifier() } returns defaultVerifier

      val session = mockk<SSLSession>()
      val verifier = ConfirmingHostnameVerifier()

      verifier.verify("example.com", session) shouldBe true
    }

    should("prompt user and accept hostname when user clicks Continue") {
      val defaultVerifier = mockk<HostnameVerifier>()
      every { defaultVerifier.verify(any(), any()) } returns false

      mockkStatic(HttpsURLConnection::class)
      every { HttpsURLConnection.getDefaultHostnameVerifier() } returns defaultVerifier

      val appMock = mockk<Application>()
      every { appMock.invokeAndWait(any()) } answers {
        firstArg<Runnable>().run()
      }
      mockkStatic(ApplicationManager::class)
      every { ApplicationManager.getApplication() } returns appMock

      val session = mockk<SSLSession>()
      every { session.peerCertificates } returns arrayOf()

      mockkStatic(Messages::class)
      every {
        Messages.showYesNoDialog(any<String>(), any(), any(), any(), any())
      } returns Messages.YES

      val verifier = ConfirmingHostnameVerifier()
      verifier.verify("mismatch.example.com", session) shouldBe true
    }

    should("prompt user and reject hostname when user clicks Abort") {
      val defaultVerifier = mockk<HostnameVerifier>()
      every { defaultVerifier.verify(any(), any()) } returns false

      mockkStatic(HttpsURLConnection::class)
      every { HttpsURLConnection.getDefaultHostnameVerifier() } returns defaultVerifier

      val appMock = mockk<Application>()
      every { appMock.invokeAndWait(any()) } answers {
        firstArg<Runnable>().run()
      }
      mockkStatic(ApplicationManager::class)
      every { ApplicationManager.getApplication() } returns appMock

      val session = mockk<SSLSession>()
      every { session.peerCertificates } returns arrayOf()

      mockkStatic(Messages::class)
      every {
        Messages.showYesNoDialog(any<String>(), any(), any(), any(), any())
      } returns Messages.NO

      val verifier = ConfirmingHostnameVerifier()
      verifier.verify("mismatch.example.com", session) shouldBe false
    }

    should("remember accepted hostname and not prompt again") {
      val defaultVerifier = mockk<HostnameVerifier>()
      every { defaultVerifier.verify(any(), any()) } returns false

      mockkStatic(HttpsURLConnection::class)
      every { HttpsURLConnection.getDefaultHostnameVerifier() } returns defaultVerifier

      val appMock = mockk<Application>()
      every { appMock.invokeAndWait(any()) } answers {
        firstArg<Runnable>().run()
      }
      mockkStatic(ApplicationManager::class)
      every { ApplicationManager.getApplication() } returns appMock

      val session = mockk<SSLSession>()
      every { session.peerCertificates } returns arrayOf()

      mockkStatic(Messages::class)
      every {
        Messages.showYesNoDialog(any<String>(), any(), any(), any(), any())
      } returns Messages.YES

      val verifier = ConfirmingHostnameVerifier()
      verifier.verify("remembered.example.com", session) shouldBe true
      verifier.verify("remembered.example.com", session) shouldBe true

      verify(exactly = 1) {
        Messages.showYesNoDialog(any<String>(), any(), any(), any(), any())
      }
    }

    should("remember rejected hostname and not prompt again") {
      val defaultVerifier = mockk<HostnameVerifier>()
      every { defaultVerifier.verify(any(), any()) } returns false

      mockkStatic(HttpsURLConnection::class)
      every { HttpsURLConnection.getDefaultHostnameVerifier() } returns defaultVerifier

      val appMock = mockk<Application>()
      every { appMock.invokeAndWait(any()) } answers {
        firstArg<Runnable>().run()
      }
      mockkStatic(ApplicationManager::class)
      every { ApplicationManager.getApplication() } returns appMock

      val session = mockk<SSLSession>()
      every { session.peerCertificates } returns arrayOf()

      mockkStatic(Messages::class)
      every {
        Messages.showYesNoDialog(any<String>(), any(), any(), any(), any())
      } returns Messages.NO

      val verifier = ConfirmingHostnameVerifier()
      verifier.verify("rejected.example.com", session) shouldBe false
      verifier.verify("rejected.example.com", session) shouldBe false

      verify(exactly = 1) {
        Messages.showYesNoDialog(any<String>(), any(), any(), any(), any())
      }
    }
  }
})