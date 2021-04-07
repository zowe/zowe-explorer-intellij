package eu.ibagroup.formainframe.config.connect

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.CredentialsNotFoundForConnection
import okhttp3.Credentials

fun interface CredentialsListener {
  fun onChanged(connectionConfigUuid: String)
}

@JvmField
val CREDENTIALS_CHANGED = Topic.create("credentialChanges", CredentialsListener::class.java)

interface CredentialService {

  fun getUsernameByKey(connectionConfigUuid: String): String?
  fun getPasswordByKey(connectionConfigUuid: String): String?
  fun setCredentials(connectionConfigUuid: String, username: String, password: String)
  fun clearCredentials(connectionConfigUuid: String)

  companion object {
    @JvmStatic
    val instance: CredentialService
      get() = ApplicationManager.getApplication().getService(CredentialService::class.java)
  }

}

fun getInstance(): CredentialService = ApplicationManager.getApplication().getService(CredentialService::class.java)

fun username(connectionConfig: ConnectionConfig): String {
  return CredentialService.instance.getUsernameByKey(connectionConfig.uuid) ?: throw CredentialsNotFoundForConnection(
    connectionConfig
  )
}

fun password(connectionConfig: ConnectionConfig): String {
  return CredentialService.instance.getPasswordByKey(connectionConfig.uuid) ?: throw CredentialsNotFoundForConnection(
    connectionConfig
  )
}

val ConnectionConfig.token: String
  get() = Credentials.basic(username(this), password(this))