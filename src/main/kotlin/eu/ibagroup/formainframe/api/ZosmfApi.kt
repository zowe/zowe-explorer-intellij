package eu.ibagroup.formainframe.api

import com.intellij.openapi.application.ApplicationManager
import eu.ibagroup.formainframe.config.connect.ConnectionConfig

interface ZosmfApi {

  companion object {
    @JvmStatic
    val instance: ZosmfApi
      get() = ApplicationManager.getApplication().getService(ZosmfApi::class.java)
  }

  fun <Api : Any> getApi(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api

  fun <Api : Any> getApi(apiClass: Class<out Api>, url: String, isAllowSelfSigned: Boolean): Api

}

inline fun <reified Api : Any> api(connectionConfig: ConnectionConfig): Api {
  return ZosmfApi.instance.getApi(Api::class.java, connectionConfig)
}

inline fun <reified Api : Any> api(url: String, isAllowSelfSigned: Boolean): Api {
  return ZosmfApi.instance.getApi(Api::class.java, url, isAllowSelfSigned)
}