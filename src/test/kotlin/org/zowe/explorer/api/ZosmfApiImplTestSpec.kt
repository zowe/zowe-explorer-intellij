/*
 * Copyright (c) 2024 IBA Group.
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

package org.zowe.explorer.api

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.util.net.ssl.CertificateManager
import com.intellij.util.net.ssl.ConfirmingTrustManager
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import okhttp3.*
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.testutils.MockkAwareShouldSpec
import org.zowe.kotlinsdk.buildApi
import org.zowe.kotlinsdk.buildApiWithBytesConverter
import java.util.concurrent.TimeUnit
import javax.net.ssl.*
import javax.swing.Icon

class ZosmfApiImplTestSpec : MockkAwareShouldSpec({
  context("api module: ZosmfApiImpl") {
    var sslFactoryActual: SSLSocketFactory? = null
    var trustManagerActual: TrustManager? = null
    var hostnameVerifierActual: HostnameVerifier? = null
    var interceptorActual: Interceptor? = null
    val connectionSpecsActual = mutableListOf<List<ConnectionSpec>>()

    val safeTrustManagerMock = mockk<ConfirmingTrustManager>()
    val safeSslContextMock = mockk<SSLContext> {
      every { socketFactory } returns mockk()
    }

    val certManagerMock = mockk<CertificateManager> {
      every { trustManager } returns safeTrustManagerMock
      every { sslContext } returns safeSslContextMock
    }

    mockkObject(CertificateManager.Companion)
    every { CertificateManager.getInstance() } returns certManagerMock

    val okHttpClientBuilderMock = mockk<OkHttpClient.Builder> {
      every { readTimeout(any(), TimeUnit.MINUTES) } returns this
      every { connectTimeout(any(), TimeUnit.MINUTES) } returns this
      every { connectionPool(any<ConnectionPool>()) } returns this
      every { dispatcher(any<Dispatcher>()) } returns this
      every { addInterceptor(any<Interceptor>()) } answers {
        interceptorActual = firstArg()
        this@mockk
      }
      every { connectionSpecs(any<List<ConnectionSpec>>()) } answers {
        connectionSpecsActual.add(firstArg())
        this@mockk
      }
      every { hostnameVerifier(any()) } answers {
        hostnameVerifierActual = firstArg()
        this@mockk
      }
      every { build() } returns mockk()
    }

    val buildApiFunResultMockk = mockk<Any>()
    val buildApiWithBytesConverterFunResultMockk = mockk<Any>()

    mockkStatic("org.zowe.kotlinsdk.ApiKt")
    every { buildApi(any<String>(), any<OkHttpClient>(), any<Class<Any>>()) } returns buildApiFunResultMockk
    every {
      buildApiWithBytesConverter(any<String>(), any<OkHttpClient>(), any<Class<Any>>())
    } answers {
      buildApiWithBytesConverterFunResultMockk
    }

    beforeEach {
      sslFactoryActual = null
      trustManagerActual = null
      hostnameVerifierActual = null
      connectionSpecsActual.clear()
    }

    should("getApi uses CertificateManager trust when self-signed certificates are not allowed") {
      mockkConstructor(OkHttpClient.Builder::class)
      every {
        anyConstructed<OkHttpClient.Builder>()
          .sslSocketFactory(any<SSLSocketFactory>(), any<X509TrustManager>())
      } answers {
        sslFactoryActual = firstArg()
        trustManagerActual = secondArg()
        okHttpClientBuilderMock
      }

      val connectionConfig = mockk<ConnectionConfig> {
        every { url } returns "test"
        every { isAllowSelfSigned } returns false
        every { isAllowCleartext } returns false
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val resultActual = zosmfApiImpl.getApi(Any::class.java, connectionConfig)

      assertSoftly { buildApiFunResultMockk shouldBe resultActual }
      assertSoftly { sslFactoryActual shouldNotBe null }
      assertSoftly { trustManagerActual shouldNotBe null }
      assertSoftly { sslFactoryActual shouldBe safeSslContextMock.socketFactory }
      assertSoftly { trustManagerActual shouldBe safeTrustManagerMock }
      assertSoftly { hostnameVerifierActual shouldBe null }
      assertSoftly { connectionSpecsActual.size shouldBe 1 }
      assertSoftly { connectionSpecsActual.first().contains(ConnectionSpec.CLEARTEXT) shouldBe false }
    }

    should("getApi permits cleartext transport only when the connection explicitly allows it") {
      mockkConstructor(OkHttpClient.Builder::class)
      every {
        anyConstructed<OkHttpClient.Builder>()
          .sslSocketFactory(any<SSLSocketFactory>(), any<X509TrustManager>())
      } answers {
        sslFactoryActual = firstArg()
        trustManagerActual = secondArg()
        okHttpClientBuilderMock
      }

      val connectionConfig = mockk<ConnectionConfig> {
        every { url } returns "http://test"
        every { isAllowSelfSigned } returns false
        every { isAllowCleartext } returns true
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val resultActual = zosmfApiImpl.getApi(Any::class.java, connectionConfig)

      assertSoftly { buildApiFunResultMockk shouldBe resultActual }
      assertSoftly { connectionSpecsActual.size shouldBe 1 }
      assertSoftly { connectionSpecsActual.first().contains(ConnectionSpec.CLEARTEXT) shouldBe true }
    }

    should("getApi throws an exception when the user declines the cleartext transport usage") {
      mockkConstructor(OkHttpClient.Builder::class)
      every {
        anyConstructed<OkHttpClient.Builder>()
          .sslSocketFactory(any<SSLSocketFactory>(), any<X509TrustManager>())
      } answers {
        sslFactoryActual = firstArg()
        trustManagerActual = secondArg()
        okHttpClientBuilderMock
      }

      val appMock = mockk<Application>()
      every { appMock.invokeAndWait(any()) } answers { firstArg<Runnable>().run() }
      mockkStatic(ApplicationManager::class)
      every { ApplicationManager.getApplication() } returns appMock

      mockkStatic(Messages::class)
      every {
        Messages.showDialog(
          any<String>(),
          any<String>(),
          any<Array<String>>(),
          any<Int>(),
          any<Icon>(),
          any<DialogWrapper.DoNotAskOption>()
        )
      } returns 0

      val connectionConfig = mockk<ConnectionConfig> {
        every { uuid } returns "test_uuid"
        every { name } returns "test_connection"
        every { url } returns "http://test"
        every { isAllowSelfSigned } returns false
        every { isAllowCleartext } returns false
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val exceptionActual = shouldThrow<IllegalStateException> {
        zosmfApiImpl.getApi(Any::class.java, connectionConfig)
      }

      assertSoftly { (exceptionActual.message?.contains("unencrypted HTTP URL") == true) shouldBe true }
    }

    should("getApi returns already initialized OkHttpClient when called again with same connection") {
      mockkConstructor(OkHttpClient.Builder::class)
      every {
        anyConstructed<OkHttpClient.Builder>()
          .sslSocketFactory(any<SSLSocketFactory>(), any<X509TrustManager>())
      } answers {
        sslFactoryActual = firstArg()
        trustManagerActual = secondArg()
        okHttpClientBuilderMock
      }

      val connectionConfig = mockk<ConnectionConfig> {
        every { url } returns "test"
        every { isAllowSelfSigned } returns false
        every { isAllowCleartext } returns false
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val resultActual1 = zosmfApiImpl.getApi(Any::class.java, connectionConfig)
      val resultActual2 = zosmfApiImpl.getApi(Any::class.java, connectionConfig)

      assertSoftly { buildApiFunResultMockk shouldBe resultActual1 }
      assertSoftly { buildApiFunResultMockk shouldBe resultActual2 }
      assertSoftly { resultActual1 shouldBe resultActual2 }
      assertSoftly { sslFactoryActual shouldBe null }
      assertSoftly { trustManagerActual shouldBe null }
    }

    should("getApiWithBytesConverter uses CertificateManager trust when self-signed certificates are not allowed") {
      mockkConstructor(OkHttpClient.Builder::class)
      every {
        anyConstructed<OkHttpClient.Builder>()
          .sslSocketFactory(any<SSLSocketFactory>(), any<X509TrustManager>())
      } answers {
        sslFactoryActual = firstArg()
        trustManagerActual = secondArg()
        okHttpClientBuilderMock
      }

      val connectionConfig = mockk<ConnectionConfig> {
        every { url } returns "test"
        every { isAllowSelfSigned } returns false
        every { isAllowCleartext } returns false
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val resultActual = zosmfApiImpl.getApiWithBytesConverter(Any::class.java, connectionConfig)

      assertSoftly { buildApiWithBytesConverterFunResultMockk shouldBe resultActual }
      assertSoftly { sslFactoryActual shouldBe null }
      assertSoftly { trustManagerActual shouldBe null }
    }

    should("getApi uses CertificateManager trust with ConfirmingHostnameVerifier when self-signed certificates are allowed") {
      mockkConstructor(OkHttpClient.Builder::class)
      every {
        anyConstructed<OkHttpClient.Builder>()
          .sslSocketFactory(any<SSLSocketFactory>(), any<X509TrustManager>())
      } answers {
        sslFactoryActual = firstArg()
        trustManagerActual = secondArg()
        okHttpClientBuilderMock
      }

      val connectionConfig = mockk<ConnectionConfig> {
        every { url } returns "test"
        every { isAllowSelfSigned } returns true
        every { isAllowCleartext } returns false
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val resultActual = zosmfApiImpl.getApi(Any::class.java, connectionConfig)

      assertSoftly { buildApiFunResultMockk shouldBe resultActual }
      assertSoftly { sslFactoryActual shouldNotBe null }
      assertSoftly { trustManagerActual shouldNotBe null }
      assertSoftly { sslFactoryActual shouldBe safeSslContextMock.socketFactory }
      assertSoftly { trustManagerActual shouldBe safeTrustManagerMock }
      assertSoftly { hostnameVerifierActual shouldNotBe null }
      assertSoftly { (hostnameVerifierActual is ConfirmingHostnameVerifier) shouldBe true }
    }

    should("getApi returns already initialized OkHttpClient when called again with self-signed allowed") {
      mockkConstructor(OkHttpClient.Builder::class)
      every {
        anyConstructed<OkHttpClient.Builder>()
          .sslSocketFactory(any<SSLSocketFactory>(), any<X509TrustManager>())
      } answers {
        sslFactoryActual = firstArg()
        trustManagerActual = secondArg()
        okHttpClientBuilderMock
      }

      val connectionConfig = mockk<ConnectionConfig> {
        every { url } returns "test"
        every { isAllowSelfSigned } returns true
        every { isAllowCleartext } returns false
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val resultActual1 = zosmfApiImpl.getApi(Any::class.java, connectionConfig)
      val resultActual2 = zosmfApiImpl.getApi(Any::class.java, connectionConfig)

      assertSoftly { buildApiFunResultMockk shouldBe resultActual1 }
      assertSoftly { buildApiFunResultMockk shouldBe resultActual2 }
      assertSoftly { resultActual1 shouldBe resultActual2 }
      assertSoftly { sslFactoryActual shouldBe null }
      assertSoftly { trustManagerActual shouldBe null }
    }

    should("getApiWithBytesConverter uses CertificateManager trust with ConfirmingHostnameVerifier when self-signed certificates are allowed") {
      mockkConstructor(OkHttpClient.Builder::class)
      every {
        anyConstructed<OkHttpClient.Builder>()
          .sslSocketFactory(any<SSLSocketFactory>(), any<X509TrustManager>())
      } answers {
        sslFactoryActual = firstArg()
        trustManagerActual = secondArg()
        okHttpClientBuilderMock
      }

      val connectionConfig = mockk<ConnectionConfig> {
        every { url } returns "test"
        every { isAllowSelfSigned } returns true
        every { isAllowCleartext } returns false
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val resultActual = zosmfApiImpl.getApiWithBytesConverter(Any::class.java, connectionConfig)

      assertSoftly { buildApiWithBytesConverterFunResultMockk shouldBe resultActual }
      assertSoftly { sslFactoryActual shouldBe null }
      assertSoftly { trustManagerActual shouldBe null }
    }

    should("the registered interceptor adds the CSRF header to the request") {
      var requestActual: Request? = null
      val responseMock = mockk<Response>()
      val chainMock = mockk<Interceptor.Chain> {
        every { request() } returns Request.Builder().url("https://test.com").build()
        every { proceed(any<Request>()) } answers {
          requestActual = firstArg()
          responseMock
        }
      }

      val resultActual = interceptorActual?.intercept(chainMock)

      assertSoftly { resultActual shouldBe responseMock }
      assertSoftly { requestActual?.header("X-CSRF-ZOSMF-HEADER") shouldBe "" }
    }
  }

})