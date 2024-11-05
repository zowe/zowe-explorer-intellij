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
 */

package org.zowe.explorer.api

import com.intellij.util.net.ssl.CertificateManager
import com.intellij.util.net.ssl.ConfirmingTrustManager
import org.zowe.explorer.config.connect.ConnectionConfig
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import okhttp3.*
import org.zowe.kotlinsdk.buildApi
import org.zowe.kotlinsdk.buildApiWithBytesConverter
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.reflect.KFunction

class ZosmfApiImplTestSpec : ShouldSpec({
  afterSpec {
    unmockkAll()
    clearAllMocks()
  }

  context("api module: ZosmfApiImpl") {
    var sslFactoryActual: SSLSocketFactory? = null
    var trustManagerActual: TrustManager? = null

    val safeTrustManagerMock = mockk<ConfirmingTrustManager>()
    val safeSslContextMock = mockk<SSLContext> {
      every { socketFactory } returns mockk()
    }

    val unsafeSslContextMock = mockk<SSLContext> {
      every { init(any(), any(), any()) } answers {}
      every { socketFactory } returns mockk()
    }

    val sslContextGetInstanceMock: (String) -> SSLContext = SSLContext::getInstance
    mockkStatic(sslContextGetInstanceMock as KFunction<*>)
    every { sslContextGetInstanceMock("TLSv1.2") } returns unsafeSslContextMock

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
      every { addInterceptor(any<Interceptor>()) } returns this
      every { connectionSpecs(any<List<ConnectionSpec>>()) } returns this
      every { hostnameVerifier(any()) } returns this
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
    }

    should("check that getApi returns safe OkHttpClient without bytes converter when self-signed certificates are not allowed") {
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
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val resutlActual = zosmfApiImpl.getApi(Any::class.java, connectionConfig)

      assertSoftly { buildApiFunResultMockk shouldBe resutlActual }
      assertSoftly { sslFactoryActual shouldNotBe null }
      assertSoftly { trustManagerActual shouldNotBe null }
      assertSoftly { sslFactoryActual shouldBe safeSslContextMock.socketFactory }
      assertSoftly { trustManagerActual shouldBe safeTrustManagerMock }
    }
    should("check that getApi returns already initialized safe OkHttpClient without bytes converter when self-signed certificates are not allowed") {
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
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val resutlActual1 = zosmfApiImpl.getApi(Any::class.java, connectionConfig)
      val resutlActual2 = zosmfApiImpl.getApi(Any::class.java, connectionConfig)

      assertSoftly { buildApiFunResultMockk shouldBe resutlActual1 }
      assertSoftly { buildApiFunResultMockk shouldBe resutlActual2 }
      assertSoftly { resutlActual1 shouldBe resutlActual2 }
      // Lazy is not initialized the second time
      assertSoftly { sslFactoryActual shouldBe null }
      assertSoftly { trustManagerActual shouldBe null }
    }
    should("check that getApi returns safe OkHttpClient with bytes converter when self-signed certificates are not allowed") {
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
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val resutlActual = zosmfApiImpl.getApiWithBytesConverter(Any::class.java, connectionConfig)

      assertSoftly { buildApiWithBytesConverterFunResultMockk shouldBe resutlActual }
      // Lazy is not initialized the second time
      assertSoftly { sslFactoryActual shouldBe null }
      assertSoftly { trustManagerActual shouldBe null }
    }
    should("check that getApi returns unsafe OkHttpClient without bytes converter when self-signed certificates are allowed") {
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
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val resutlActual = zosmfApiImpl.getApi(Any::class.java, connectionConfig)

      assertSoftly { buildApiFunResultMockk shouldBe resutlActual }
      assertSoftly { sslFactoryActual shouldNotBe null }
      assertSoftly { trustManagerActual shouldNotBe null }
      assertSoftly { sslFactoryActual shouldBe unsafeSslContextMock.socketFactory }
      assertSoftly { trustManagerActual shouldNotBe safeTrustManagerMock }
    }
    should("check that getApi returns already initialized unsafe OkHttpClient without bytes converter when self-signed certificates are allowed") {
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
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val resutlActual1 = zosmfApiImpl.getApi(Any::class.java, connectionConfig)
      val resutlActual2 = zosmfApiImpl.getApi(Any::class.java, connectionConfig)

      assertSoftly { buildApiFunResultMockk shouldBe resutlActual1 }
      assertSoftly { buildApiFunResultMockk shouldBe resutlActual2 }
      assertSoftly { resutlActual1 shouldBe resutlActual2 }
      // Lazy is not initialized the second time
      assertSoftly { sslFactoryActual shouldBe null }
      assertSoftly { trustManagerActual shouldBe null }
    }
    should("check that getApi returns unsafe OkHttpClient with bytes converter when self-signed certificates are allowed") {
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
      }

      val zosmfApiImpl = ZosmfApiImpl()
      val resutlActual = zosmfApiImpl.getApiWithBytesConverter(Any::class.java, connectionConfig)

      assertSoftly { buildApiWithBytesConverterFunResultMockk shouldBe resutlActual }
      // Lazy is not initialized the second time
      assertSoftly { sslFactoryActual shouldBe null }
      assertSoftly { trustManagerActual shouldBe null }
    }
  }
})
