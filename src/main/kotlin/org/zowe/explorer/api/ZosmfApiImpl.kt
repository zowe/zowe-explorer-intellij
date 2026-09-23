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

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.util.net.ssl.CertificateManager
import okhttp3.ConnectionPool
import okhttp3.ConnectionSpec
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.utils.crudable.getByUniqueKey
import org.zowe.kotlinsdk.buildApi
import org.zowe.kotlinsdk.buildApiWithBytesConverter
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

/**
 * Class that implements z/OSMF API for sending requests.
 */
class ZosmfApiImpl : ZosmfApi {

  /**
   * Data class to describe z/OSMF URL address.
   */
  private data class ZosmfUrl(val url: String, val isAllowSelfSigned: Boolean, val isAllowCleartext: Boolean)

  private var apis = hashMapOf<Class<out Any>, Pair<MutableMap<ZosmfUrl, Any>, MutableMap<ZosmfUrl, Any>>>()

  /**
   * Method for getting API by connection config.
   * @param apiClass class to represent API.
   * @param connectionConfig connection configuration to specify the system to work with.
   * @return API class object.
   */
  override fun <Api : Any> getApi(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api {
    return getApi(
      apiClass,
      connectionConfig.url,
      connectionConfig.isAllowSelfSigned,
      resolveCleartextUsage(connectionConfig)
    )
  }

  /**
   * Method for getting API with bytes converter by connection config.
   * @param apiClass class to represent API.
   * @param connectionConfig connection configuration to specify the system to work with.
   * @return API class object with bytes converter.
   */
  override fun <Api : Any> getApiWithBytesConverter(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api {
    return getApi(
      apiClass,
      connectionConfig.url,
      connectionConfig.isAllowSelfSigned,
      resolveCleartextUsage(connectionConfig),
      true
    )
  }

  /**
   * Resolve whether the unencrypted HTTP transport is permitted for the connection.
   * The connections with an "https" URL never need it. For a connection with an "http" URL the user is asked
   * for the consent on every usage, unless the consent is already stored in the connection config.
   * @param connectionConfig connection configuration to check.
   * @throws IllegalStateException if the user declined the unencrypted HTTP transport usage.
   * @return true if the connection is allowed to use unencrypted HTTP transport, false if it does not need it.
   */
  private fun resolveCleartextUsage(connectionConfig: ConnectionConfig): Boolean {
    if (!connectionConfig.url.startsWith("http://", true)) return false
    if (!confirmCleartextUsage(connectionConfig)) {
      throw IllegalStateException(
        "The connection \"${connectionConfig.name}\" uses an unencrypted HTTP URL. " +
          "Provide an HTTPS URL for the connection to be able to work with it."
      )
    }
    return true
  }

  /**
   * Common method for getting API.
   * @param apiClass class to represent API.
   * @param url url address of the remote system.
   * @param isAllowSelfSigned whether to allow self-signed certificates.
   * @param isAllowCleartext whether the connection is explicitly allowed to use unencrypted HTTP transport.
   * @param useBytesConverter whether to use a byte converter.
   * @return prepared API class object.
   */
  @Suppress("UNCHECKED_CAST")
  override fun <Api : Any> getApi(
    apiClass: Class<out Api>,
    url: String,
    isAllowSelfSigned: Boolean,
    isAllowCleartext: Boolean,
    useBytesConverter: Boolean
  ): Api {
    val zosmfUrl = ZosmfUrl(url, isAllowSelfSigned, isAllowCleartext)
    val defaultApi = Pair<MutableMap<ZosmfUrl, Any>, MutableMap<ZosmfUrl, Any>>(hashMapOf(), hashMapOf())
    if (!apis.containsKey(apiClass)) {
      synchronized(apis) {
        if (!apis.containsKey(apiClass)) {
          apis[apiClass] = defaultApi
        }
      }
    }
    val apiClassMap = apis[apiClass] ?: defaultApi
    val client = getOkHttpClient(isAllowSelfSigned, isAllowCleartext)
    synchronized(apiClassMap) {
      if (!useBytesConverter && !apiClassMap.first.containsKey(zosmfUrl)) {
        apiClassMap.first[zosmfUrl] = buildApi(zosmfUrl.url, client, apiClass)
      } else if (useBytesConverter && !apiClassMap.second.containsKey(zosmfUrl)) {
        apiClassMap.second[zosmfUrl] =
          buildApiWithBytesConverter(zosmfUrl.url, client, apiClass)
      }
    }
    return if (!useBytesConverter) apiClassMap.first[zosmfUrl] as Api else apiClassMap.second[zosmfUrl] as Api
  }
}

/**
 * [HostnameVerifier] that prompts the user to accept a hostname mismatch.
 * Accepted/rejected hostnames are persisted in [ConnectionConfig.isHostnameVerified]
 * and cached in memory for the IDE session.
 */
internal class ConfirmingHostnameVerifier : HostnameVerifier {

  private val acceptedHostnames = ConcurrentHashMap.newKeySet<String>()
  private val rejectedHostnames = ConcurrentHashMap.newKeySet<String>()
  private val defaultVerifier = javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier()

  init {
    loadPersistedHostnames()
  }

  override fun verify(hostname: String, session: SSLSession): Boolean {
    if (defaultVerifier.verify(hostname, session)) return true
    if (acceptedHostnames.contains(hostname)) return true
    if (rejectedHostnames.contains(hostname)) return false

    var accepted = false
    ApplicationManager.getApplication().invokeAndWait {
      val peerCerts = try {
        session.peerCertificates.joinToString(separator = "\n") { cert ->
          if (cert is java.security.cert.X509Certificate) {
            "Subject: ${cert.subjectX500Principal}"
          } else {
            cert.type
          }
        }
      } catch (_: Exception) {
        "unavailable"
      }

      val result = Messages.showYesNoDialog(
        "The certificate for this server does not match the hostname \"$hostname\".\n\n" +
          "Certificate details:\n$peerCerts\n\n" +
          "This could indicate a man-in-the-middle attack.\n" +
          "Do you want to continue connecting?",
        "Hostname Verification Failed",
        "Continue",
        "Abort",
        Messages.getWarningIcon()
      )
      accepted = result == Messages.YES
    }

    if (accepted) {
      acceptedHostnames.add(hostname)
    } else {
      rejectedHostnames.add(hostname)
    }
    persistHostnameDecision(hostname, accepted)
    return accepted
  }

  private fun loadPersistedHostnames() {
    try {
      val crudable = ConfigService.getService().crudable
      crudable.getAll<ConnectionConfig>()
        .filter { it.isAllowSelfSigned && it.isHostnameVerified }
        .forEach { config ->
          extractHostname(config.url)?.let { acceptedHostnames.add(it) }
        }
    } catch (_: Exception) {
      // ConfigService may not be available yet during early initialization
    }
  }

  private fun persistHostnameDecision(hostname: String, accepted: Boolean) {
    try {
      val crudable = ConfigService.getService().crudable
      crudable.getAll<ConnectionConfig>()
        .filter { it.isAllowSelfSigned && extractHostname(it.url) == hostname }
        .forEach { config ->
          config.isHostnameVerified = accepted
          crudable.update(config)
        }
    } catch (_: Exception) {
      // ConfigService may not be available
    }
  }
}

/**
 * Ask the user whether the connection is allowed to send the requests over the unencrypted HTTP transport.
 * The user is asked on every usage of such a connection until the "Don't ask again" checkbox is selected
 * together with the "Proceed" choice, which stores the consent in the connection config.
 * @param connectionConfig connection configuration the consent is asked for.
 * @return true if the unencrypted HTTP transport usage is allowed, false otherwise.
 */
internal fun confirmCleartextUsage(connectionConfig: ConnectionConfig): Boolean {
  if (connectionConfig.isAllowCleartext) return true

  var accepted = false
  ApplicationManager.getApplication().invokeAndWait {
    val choice = Messages.showDialog(
      "The connection \"${connectionConfig.name}\" uses an unencrypted HTTP URL: ${connectionConfig.url}.\n" +
        "The username and the password are sent with every request, so anyone on the network can read them,\n" +
        "and the data you send to the mainframe can be modified on the way.\n\n" +
        "Do you want to proceed anyway?",
      "Attempt to Use an Unencrypted Connection",
      arrayOf(
        "Back to Safety",
        "Proceed"
      ),
      0,
      AllIcons.General.WarningDialog,
      object : DialogWrapper.DoNotAskOption {
        override fun isToBeShown(): Boolean = true

        override fun setToBeShown(toBeShown: Boolean, exitCode: Int) {
          if (!toBeShown && exitCode == 1) {
            rememberCleartextUsage(connectionConfig)
          }
        }

        override fun canBeHidden(): Boolean = true

        override fun shouldSaveOptionsOnCancel(): Boolean = false

        override fun getDoNotShowMessage(): String = "Don't ask again for this connection"
      }
    )
    accepted = choice == 1
  }
  return accepted
}

/**
 * Store the unencrypted HTTP transport usage consent in the connection config, so that it is not asked again.
 * @param connectionConfig connection configuration to store the consent for.
 */
private fun rememberCleartextUsage(connectionConfig: ConnectionConfig) {
  connectionConfig.isAllowCleartext = true
  try {
    val crudable = ConfigService.getService().crudable
    crudable.getByUniqueKey<ConnectionConfig>(connectionConfig.uuid)
      ?.let { storedConfig ->
        storedConfig.isAllowCleartext = true
        crudable.update(storedConfig)
      }
  } catch (_: Exception) {
    // ConfigService may not be available
  }
}

/**
 * Extracts hostname from a URL string.
 * @param url the URL string to extract hostname from.
 * @return the hostname, or null if the URL is malformed.
 */
internal fun extractHostname(url: String): String? {
  return try {
    URL(url).host
  } catch (_: Exception) {
    null
  }
}

/**
 * Returns [OkHttpClient] depending on whether self-signed certificates and unencrypted HTTP transport are allowed.
 * All the clients delegate certificate trust to IntelliJ's [CertificateManager].
 * The self-signed variants use [ConfirmingHostnameVerifier] that prompts the user
 * when the certificate hostname does not match.
 * The cleartext variants are provided only for the connections that explicitly opted in for the unencrypted
 * transport, so that a TLS protected connection can never be downgraded to the plain HTTP one.
 * @param isAllowSelfSigned whether to allow self-signed certificates.
 * @param isAllowCleartext whether the connection is explicitly allowed to use unencrypted HTTP transport.
 * @return [OkHttpClient] object.
 */
private fun getOkHttpClient(isAllowSelfSigned: Boolean, isAllowCleartext: Boolean): OkHttpClient {
  return if (isAllowCleartext) {
    if (isAllowSelfSigned) selfSignedCleartextClient else strictCleartextClient
  } else {
    if (isAllowSelfSigned) selfSignedClient else strictClient
  }
}

private val selfSignedClient by lazy {
  buildClient(relaxedHostnameVerification = true, allowCleartext = false)
}
private val strictClient by lazy {
  buildClient(relaxedHostnameVerification = false, allowCleartext = false)
}
private val selfSignedCleartextClient by lazy {
  buildClient(relaxedHostnameVerification = true, allowCleartext = true)
}
private val strictCleartextClient by lazy {
  buildClient(relaxedHostnameVerification = false, allowCleartext = true)
}

/**
 * Build an HTTP client that delegates certificate trust to IntelliJ's [CertificateManager].
 * @param relaxedHostnameVerification when true, uses [ConfirmingHostnameVerifier] instead of strict verification.
 * @param allowCleartext when true, the unencrypted HTTP transport is permitted for the client.
 * @return [OkHttpClient] object.
 */
private fun buildClient(relaxedHostnameVerification: Boolean, allowCleartext: Boolean): OkHttpClient {
  val trustManager = CertificateManager.getInstance().trustManager
  val sslContext = CertificateManager.getInstance().sslContext
  return OkHttpClient.Builder()
    .sslSocketFactory(sslContext.socketFactory, trustManager)
    .apply {
      if (relaxedHostnameVerification) {
        hostnameVerifier(ConfirmingHostnameVerifier())
      }
    }
    .setupClient(allowCleartext)
    .build()
}

/**
 * Set up an HTTP client. Adds the necessary headers. Configures connection specs
 * @param allowCleartext when true, [ConnectionSpec.CLEARTEXT] is added to the client connection specs.
 */
private fun OkHttpClient.Builder.setupClient(allowCleartext: Boolean): OkHttpClient.Builder {
  val connectionSpecs = mutableListOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS)
  if (allowCleartext) {
    connectionSpecs.add(ConnectionSpec.CLEARTEXT) //NOSONAR User is fully responsible for this functionality to take place
  }
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
    .connectionSpecs(connectionSpecs)
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