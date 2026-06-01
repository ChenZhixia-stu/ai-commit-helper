package com.ai.commithelper.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Global persistent settings for AI Commit Helper.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
@State(name = "AiCommitHelperSettings", storages = @Storage("aiCommitHelper.xml"))
public class AiCommitSettings implements PersistentStateComponent<AiCommitSettings.StateData> {

    public static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    public static final String DEFAULT_MODEL = "deepseek-v4-flash";
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    public static final int DEFAULT_MAX_DIFF_CHARS = 100000;
    public static final String DEFAULT_LANGUAGE = "中文";
    public static final String PRESET_DEFAULT = "Default";
    public static final String PRESET_COMPANY_NUMBERED = "Company Numbered";
    public static final String PRESET_CUSTOM = "Custom";
    public static final String DEFAULT_MESSAGE_TEMPLATE = "${title}\n\n${items.bullets}";
    public static final String COMPANY_NUMBERED_TEMPLATE = "[修改单编号]${changeId}\n"
            + "[缺陷编号]${bugId}\n"
            + "[修改说明]${description}\n"
            + "${title}\n"
            + "${items.numbered}";

    private StateData state = new StateData();

    /**
     * Returns the application-level settings service.
     *
     * @return settings instance
     */
    public static AiCommitSettings getInstance() {
        return ApplicationManager.getApplication().getService(AiCommitSettings.class);
    }

    @Nullable
    @Override
    public StateData getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull StateData state) {
        this.state = state;
        normalize();
    }

    /**
     * Applies sensible defaults for missing values.
     */
    public void normalize() {
        if (isBlank(state.baseUrl)) {
            state.baseUrl = DEFAULT_BASE_URL;
        }
        if (isBlank(state.model)) {
            state.model = DEFAULT_MODEL;
        }
        if (state.timeoutSeconds <= 0) {
            state.timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        }
        if (state.maxDiffChars <= 0) {
            state.maxDiffChars = DEFAULT_MAX_DIFF_CHARS;
        }
        if (isBlank(state.language)) {
            state.language = DEFAULT_LANGUAGE;
        }
        if (isBlank(state.messageTemplatePreset)) {
            state.messageTemplatePreset = PRESET_DEFAULT;
        }
        if (isBlank(state.messageTemplate)) {
            state.messageTemplate = DEFAULT_MESSAGE_TEMPLATE;
        }
        if (state.templateVariables == null) {
            state.templateVariables = "";
        }
    }

    /**
     * Returns the normalized base URL without a trailing slash.
     *
     * @return base URL
     */
    public String getBaseUrl() {
        normalize();
        return trimTrailingSlash(state.baseUrl.trim());
    }

    public void setBaseUrl(String baseUrl) {
        state.baseUrl = baseUrl;
        normalize();
    }

    public String getModel() {
        normalize();
        return state.model.trim();
    }

    public void setModel(String model) {
        state.model = model;
        normalize();
    }

    public int getTimeoutSeconds() {
        normalize();
        return state.timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        state.timeoutSeconds = timeoutSeconds;
        normalize();
    }

    public int getMaxDiffChars() {
        normalize();
        return state.maxDiffChars;
    }

    public void setMaxDiffChars(int maxDiffChars) {
        state.maxDiffChars = maxDiffChars;
        normalize();
    }

    public String getLanguage() {
        normalize();
        return state.language.trim();
    }

    public void setLanguage(String language) {
        state.language = language;
        normalize();
    }

    public String getMessageTemplatePreset() {
        normalize();
        return state.messageTemplatePreset.trim();
    }

    public void setMessageTemplatePreset(String messageTemplatePreset) {
        state.messageTemplatePreset = messageTemplatePreset;
        normalize();
    }

    public String getMessageTemplate() {
        normalize();
        return state.messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        state.messageTemplate = messageTemplate;
        normalize();
    }

    public String getTemplateVariables() {
        normalize();
        return state.templateVariables;
    }

    public void setTemplateVariables(String templateVariables) {
        state.templateVariables = templateVariables == null ? "" : templateVariables;
        normalize();
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Serializable settings data.
     *
     * @author AI Commit Helper
     * @since 1.0.0
     */
    public static class StateData {
        public String baseUrl = DEFAULT_BASE_URL;
        public String model = DEFAULT_MODEL;
        public int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        public int maxDiffChars = DEFAULT_MAX_DIFF_CHARS;
        public String language = DEFAULT_LANGUAGE;
        public String messageTemplatePreset = PRESET_DEFAULT;
        public String messageTemplate = DEFAULT_MESSAGE_TEMPLATE;
        public String templateVariables = "";
    }
}
