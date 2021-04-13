package eu.ibagroup.formainframe.config.connect

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.service
import eu.ibagroup.formainframe.utils.sendTopic

private fun createCredentialAttributes(key: String): CredentialAttributes {
  return CredentialAttributes(generateServiceName("MySystem", key))
}

class CredentialServiceImpl : CredentialService {

  private fun getCredentials(connectionConfigUuid: String): Credentials? {
    return service<PasswordSafe>().get(createCredentialAttributes(connectionConfigUuid))
  }

  override fun getUsernameByKey(connectionConfigUuid: String): String? {
    val credentials = getCredentials(connectionConfigUuid)
    return credentials?.userName?.toUpperCase()
  }

  override fun getPasswordByKey(connectionConfigUuid: String): String? {
    val credentials = getCredentials(connectionConfigUuid)
    return credentials?.getPasswordAsString()
  }

  override fun setCredentials(connectionConfigUuid: String, username: String, password: String) {
    val credentialAttributes = createCredentialAttributes(connectionConfigUuid)
    val credentials = Credentials(username, password)
    service<PasswordSafe>().set(credentialAttributes, credentials)
    sendTopic(CREDENTIALS_CHANGED).onChanged(connectionConfigUuid)
  }

  override fun clearCredentials(connectionConfigUuid: String) {
    val credentialAttributes = createCredentialAttributes(connectionConfigUuid)
    service<PasswordSafe>().set(credentialAttributes, null)
  }

}