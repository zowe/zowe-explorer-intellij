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
 */

package eu.ibagroup.formainframe.api

import com.google.gson.GsonBuilder
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import org.zowe.kotlinsdk.buildApi
import org.zowe.kotlinsdk.buildApiWithBytesConverter
import okhttp3.ConnectionPool
import okhttp3.ConnectionSpec
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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

private val gsonFactory = GsonConverterFactory.create(GsonBuilder().create())
private val scalarsConverterFactory = ScalarsConverterFactory.create()

/**
 * Connection pool is initialized and the connection parameters are set.
 */
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

val unsafeOkHttpClient by lazy { buildUnsafeClient() }
val safeOkHttpClient: OkHttpClient by lazy {
  OkHttpClient.Builder()
    .setupClient()
    .build()
}

/**
 * Setups http client. Adds the necessary headers. Configures connection specs.
 */
private fun OkHttpClient.Builder.setupClient(): OkHttpClient.Builder {
  return addThreadPool()
    .addInterceptor {
      it.request().newBuilder().addHeader("X-CSRF-ZOSMF-HEADER", "").build().let { request ->
        it.proceed(request)
      }
    }.connectionSpecs(mutableListOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
}

/**
 * Returns [OkHttpClient] depending on whether self-signed certificates are allowed or not.
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

/**
 * Method for building an unsafe http client that allows the use of self-signed certificates.
 * @throws RuntimeException if timeout is exceeded.
 * @return unsafe [OkHttpClient] object.
 */
private fun buildUnsafeClient(): OkHttpClient {
  return try {
    val trustAllCerts: Array<TrustManager> = arrayOf(
      object : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(
          chain: Array<X509Certificate?>?,
          authType: String?
        ) {
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(
          chain: Array<X509Certificate?>?,
          authType: String?
        ) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
          return arrayOf()
        }
      }
    )
    val sslContext = SSLContext.getInstance("TLSv1.2")
    sslContext.init(null, trustAllCerts, SecureRandom())
    val sslSocketFactory = sslContext.socketFactory
    val builder = OkHttpClient.Builder()
    builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
    builder.hostnameVerifier { _, _ -> true }
    builder.setupClient()
    builder.build()
  } catch (e: Exception) {
    throw RuntimeException(e)
  }
}
