/*
 * Copyright (c) 2020-2024 IBA Group.
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

import com.intellij.util.net.ssl.CertificateManager
import org.zowe.kotlinsdk.buildApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.kotlinsdk.buildApiWithBytesConverter
import okhttp3.ConnectionPool
import okhttp3.ConnectionSpec
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

/**
 * Class that implements z/OSMF API for sending requests.
 */
class ZosmfApiImpl : ZosmfApi {

  /**
   * Data class to describe z/OSMF URL address.
   */
  private data class ZosmfUrl(val url: String, val isAllowSelfSigned: Boolean)

  private var apis = hashMapOf<Class<out Any>, Pair<MutableMap<ZosmfUrl, Any>, MutableMap<ZosmfUrl, Any>>>()

  /**
   * Method for getting API by connection config.
   * @param apiClass class to represent API.
   * @param connectionConfig connection configuration to specify the system to work with.
   * @return API class object.
   */
  override fun <Api : Any> getApi(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api {
    return getApi(apiClass, connectionConfig.url, connectionConfig.isAllowSelfSigned)
  }

  /**
   * Method for getting API with bytes converter by connection config.
   * @param apiClass class to represent API.
   * @param connectionConfig connection configuration to specify the system to work with.
   * @return API class object with bytes converter.
   */
  override fun <Api : Any> getApiWithBytesConverter(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api {
    return getApi(apiClass, connectionConfig.url, connectionConfig.isAllowSelfSigned, true)
  }

  /**
   * Common method for getting API.
   * @param apiClass class to represent API.
   * @param url url address of the remote system.
   * @param isAllowSelfSigned whether to allow self-signed certificates.
   * @param useBytesConverter whether to use a byte converter.
   * @return prepared API class object.
   */
  @Suppress("UNCHECKED_CAST")
  override fun <Api : Any> getApi(
    apiClass: Class<out Api>,
    url: String,
    isAllowSelfSigned: Boolean,
    useBytesConverter: Boolean
  ): Api {
    val zosmfUrl = ZosmfUrl(url, isAllowSelfSigned)
    val defaultApi = Pair<MutableMap<ZosmfUrl, Any>, MutableMap<ZosmfUrl, Any>>(hashMapOf(), hashMapOf())
    if (!apis.containsKey(apiClass)) {
      synchronized(apis) {
        if (!apis.containsKey(apiClass)) {
          apis[apiClass] = defaultApi
        }
      }
    }
    val apiClassMap = apis[apiClass] ?: defaultApi
    synchronized(apiClassMap) {
      if (!useBytesConverter && !apiClassMap.first.containsKey(zosmfUrl)) {
        apiClassMap.first[zosmfUrl] = buildApi(zosmfUrl.url, getOkHttpClient(zosmfUrl.isAllowSelfSigned), apiClass)
      } else if (useBytesConverter && !apiClassMap.second.containsKey(zosmfUrl)) {
        apiClassMap.second[zosmfUrl] =
          buildApiWithBytesConverter(zosmfUrl.url, getOkHttpClient(zosmfUrl.isAllowSelfSigned), apiClass)
      }
    }
    return if (!useBytesConverter) apiClassMap.first[zosmfUrl] as Api else apiClassMap.second[zosmfUrl] as Api
  }
}

/**
 * Returns [OkHttpClient] depending on whether self-signed certificates are allowed or not
 * @param isAllowSelfSigned whether to allow self-signed certificates.
 * @return safe or unsafe [OkHttpClient] object.
 */
private fun getOkHttpClient(isAllowSelfSigned: Boolean): OkHttpClient {
  return if (isAllowSelfSigned) {
    unsafeOkHttpClient
  } else {
    safeOkHttpClient
  }
}

private val unsafeOkHttpClient by lazy { buildUnsafeClient() }
private val safeOkHttpClient by lazy { buildSafeClient() }

/**
 * Build an unsafe HTTP client that will allow the use of self-signed certificates
 * @return unsafe [OkHttpClient] object
 */
private fun buildUnsafeClient(): OkHttpClient {
  val trustAllCerts: Array<TrustManager> = arrayOf(
    object : X509TrustManager {
      @Throws(CertificateException::class)
      override fun checkClientTrusted(
        chain: Array<X509Certificate?>?,
        authType: String?
      ) { /* Unsafe client does not need a trust check */ }

      @Throws(CertificateException::class)
      override fun checkServerTrusted(
        chain: Array<X509Certificate?>?,
        authType: String?
      ) { /* Unsafe client does not need a trust check */ }

      override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf()
      }
    }
  )
  val sslContext = SSLContext.getInstance("TLSv1.2")
  sslContext.init(null, trustAllCerts, SecureRandom())
  val sslSocketFactory = sslContext.socketFactory
  return OkHttpClient.Builder()
    .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
    .hostnameVerifier { _, _ -> true } //NOSONAR User is fully responsible for this functionality to take place
    .setupClient()
    .build()
}

/**
 * Build a safe HTTP client that will reuse all allowed secured trusted certificates across the client's system
 * (as well as those uploaded into IntelliJ's server certificates store)
 * @return safe [OkHttpClient] object
 */
private fun buildSafeClient(): OkHttpClient {
  val trustManager = CertificateManager.getInstance().trustManager
  val sslContext = CertificateManager.getInstance().sslContext
  return OkHttpClient.Builder()
    .sslSocketFactory(sslContext.socketFactory, trustManager)
    .setupClient()
    .build()
}

/** Set up an HTTP client. Adds the necessary headers. Configures connection specs */
private fun OkHttpClient.Builder.setupClient(): OkHttpClient.Builder {
  return addThreadPool()
    .addInterceptor {
      it.request()
        .newBuilder()
        .addHeader("X-CSRF-ZOSMF-HEADER", "")
        .build()
        .let { request ->
          it.proceed(request)
        }
    }
    .connectionSpecs(
      mutableListOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT) //NOSONAR User is fully responsible for this functionality to take place
    )
}

/** Connection pool is initialized and the connection parameters are set */
private fun OkHttpClient.Builder.addThreadPool(): OkHttpClient.Builder {
  readTimeout(5, TimeUnit.MINUTES)
  connectTimeout(5, TimeUnit.MINUTES)
  connectionPool(ConnectionPool(100, 5, TimeUnit.MINUTES))
  dispatcher(Dispatcher().apply {
    maxRequests = 100
    maxRequestsPerHost = 100
  })
  return this
}
