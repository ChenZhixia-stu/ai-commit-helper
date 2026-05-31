package com.ai.commithelper.config;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

/**
 * Stores the DeepSeek API key in IntelliJ PasswordSafe.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public final class ApiKeyStore {

    private static final String SERVICE_NAME = "AI Commit Helper DeepSeek API Key";
    private static final String USER_NAME = "deepseek";

    private ApiKeyStore() {
    }

    /**
     * Reads the configured API key.
     *
     * @return API key, or an empty string when missing
     */
    public static String getApiKey() {
        Credentials credentials = PasswordSafe.getInstance().get(credentials());
        if (credentials == null || credentials.getPasswordAsString() == null) {
            return "";
        }
        return credentials.getPasswordAsString();
    }

    /**
     * Stores or clears the API key.
     *
     * @param apiKey key value
     */
    public static void setApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            PasswordSafe.getInstance().set(credentials(), null);
            return;
        }
        PasswordSafe.getInstance().set(credentials(), new Credentials(USER_NAME, apiKey.trim()));
    }

    /**
     * Returns whether a non-empty API key exists.
     *
     * @return true if configured
     */
    public static boolean hasApiKey() {
        return !getApiKey().trim().isEmpty();
    }

    private static CredentialAttributes credentials() {
        return new CredentialAttributes(SERVICE_NAME);
    }
}
