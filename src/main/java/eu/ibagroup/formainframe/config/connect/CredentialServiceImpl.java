package eu.ibagroup.formainframe.config.connect;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CredentialServiceImpl implements CredentialService {

  @Override
  public @Nullable String getUsernameByKey(@NotNull String connectionConfigUuid) {
    final CredentialAttributes credentialAttributes = createCredentialAttributes(connectionConfigUuid);
    final Credentials credentials = PasswordSafe.getInstance().get(credentialAttributes);
    String username = null;
    if (credentials != null) {
      username = credentials.getUserName();
      if (username != null) {
        username = username.toUpperCase();
      }
    }
    return username;
  }

  @Override
  public @Nullable String getPasswordByKey(@NotNull String connectionConfigUuid) {
    final CredentialAttributes credentialAttributes = createCredentialAttributes(connectionConfigUuid);
    final Credentials credentials = PasswordSafe.getInstance().get(credentialAttributes);
    String password = null;
    if (credentials != null) {
      password = credentials.getPasswordAsString();
    }
    return password;
  }

  @Override
  public void setCredentials(@NotNull String connectionConfigUuid, @NotNull String username, @NotNull String password) {
    final CredentialAttributes credentialAttributes = createCredentialAttributes(connectionConfigUuid);
    final Credentials credentials = new Credentials(username, password);
    PasswordSafe.getInstance().set(credentialAttributes, credentials);
    ApplicationManager.getApplication()
        .getMessageBus()
        .syncPublisher(CredentialServiceKt.CREDENTIALS_CHANGED)
        .onChanged(connectionConfigUuid);
  }

  private static @NotNull CredentialAttributes createCredentialAttributes(String key) {
    return new CredentialAttributes(CredentialAttributesKt.generateServiceName("MySystem", key));
  }

  @Override
  public void clearCredentials(@NotNull String connectionConfigUuid) {
    final CredentialAttributes credentialAttributes = createCredentialAttributes(connectionConfigUuid);
    PasswordSafe.getInstance().set(credentialAttributes, null);
  }
}
