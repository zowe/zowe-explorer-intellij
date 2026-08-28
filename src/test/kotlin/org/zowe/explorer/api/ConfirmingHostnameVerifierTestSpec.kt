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
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.testutils.MockkAwareShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.kotlinsdk.annotations.ZVersion
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Optional
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSession
import javax.security.auth.x500.X500Principal

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

    should("load the previously accepted hostnames from the config service and not prompt for them") {
      val defaultVerifier = mockk<HostnameVerifier>()
      every { defaultVerifier.verify(any(), any()) } returns false

      mockkStatic(HttpsURLConnection::class)
      every { HttpsURLConnection.getDefaultHostnameVerifier() } returns defaultVerifier

      val acceptedConfig = ConnectionConfig(
        "accepted_uuid", "accepted", "https://accepted.example.com:1234", true, ZVersion.ZOS_2_4,
        isHostnameVerified = true
      )
      val notVerifiedConfig = ConnectionConfig(
        "not_verified_uuid", "not_verified", "https://not.verified.example.com:1234", true, ZVersion.ZOS_2_4,
        isHostnameVerified = false
      )
      val malformedUrlConfig = ConnectionConfig(
        "malformed_uuid", "malformed", "not_a_url", true, ZVersion.ZOS_2_4,
        isHostnameVerified = true
      )
      val strictConfig = ConnectionConfig(
        "strict_uuid", "strict", "https://strict.example.com:1234", false, ZVersion.ZOS_2_4,
        isHostnameVerified = true
      )

      val crudableMock = mockk<Crudable> {
        every {
          getAll(ConnectionConfig::class.java)
        } answers {
          listOf(acceptedConfig, notVerifiedConfig, malformedUrlConfig, strictConfig).stream()
        }
      }
      mockkObject(ConfigService.Companion)
      every { ConfigService.getService() } returns mockk { every { crudable } returns crudableMock }

      mockkStatic(Messages::class)

      val session = mockk<SSLSession>()
      val verifier = ConfirmingHostnameVerifier()

      verifier.verify("accepted.example.com", session) shouldBe true

      verify(exactly = 0) {
        Messages.showYesNoDialog(any<String>(), any(), any(), any(), any())
      }
    }

    should("persist the hostname decision for the matching self-signed connection configs") {
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

      val matchingConfig = ConnectionConfig(
        "matching_uuid", "matching", "https://persisted.example.com:1234", true, ZVersion.ZOS_2_4
      )
      val otherHostConfig = ConnectionConfig(
        "other_uuid", "other", "https://other.example.com:1234", true, ZVersion.ZOS_2_4
      )
      val strictConfig = ConnectionConfig(
        "strict_uuid", "strict", "https://persisted.example.com:1234", false, ZVersion.ZOS_2_4
      )

      val updatedConfigs = mutableListOf<ConnectionConfig>()
      val crudableMock = mockk<Crudable> {
        every {
          getAll(ConnectionConfig::class.java)
        } answers {
          listOf(matchingConfig, otherHostConfig, strictConfig).stream()
        }
        every { update(any<ConnectionConfig>()) } answers {
          updatedConfigs.add(firstArg())
          Optional.empty()
        }
      }
      mockkObject(ConfigService.Companion)
      every { ConfigService.getService() } returns mockk { every { crudable } returns crudableMock }

      val x509Cert = mockk<X509Certificate> {
        every { subjectX500Principal } returns X500Principal("CN=test.com")
      }
      val otherCert = mockk<Certificate> {
        every { type } returns "X.509"
      }
      val session = mockk<SSLSession> {
        every { peerCertificates } returns arrayOf(x509Cert, otherCert)
      }

      mockkStatic(Messages::class)
      every {
        Messages.showYesNoDialog(any<String>(), any(), any(), any(), any())
      } returns Messages.YES

      val verifier = ConfirmingHostnameVerifier()
      verifier.verify("persisted.example.com", session) shouldBe true

      updatedConfigs shouldBe listOf(matchingConfig)
      matchingConfig.isHostnameVerified shouldBe true
      otherHostConfig.isHostnameVerified shouldBe false
      strictConfig.isHostnameVerified shouldBe false
    }

    should("prompt with the 'unavailable' certificate details when the peer certificates cannot be resolved") {
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

      val session = mockk<SSLSession> {
        every { peerCertificates } throws SSLPeerUnverifiedException("no certificates")
      }

      var messageActual = ""
      mockkStatic(Messages::class)
      every {
        Messages.showYesNoDialog(any<String>(), any(), any(), any(), any())
      } answers {
        messageActual = firstArg()
        Messages.NO
      }

      val verifier = ConfirmingHostnameVerifier()
      verifier.verify("unavailable.example.com", session) shouldBe false

      messageActual.contains("unavailable") shouldBe true
    }
  }

  context("api module: extractHostname") {
    should("extract the hostname from a valid URL") {
      extractHostname("https://test.com:1234/base/path") shouldBe "test.com"
    }

    should("return null for a malformed URL") {
      extractHostname("not_a_url") shouldBe null
    }
  }
})