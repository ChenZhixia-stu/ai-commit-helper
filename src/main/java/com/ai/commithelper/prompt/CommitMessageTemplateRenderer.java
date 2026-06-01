package com.ai.commithelper.prompt;

import com.ai.commithelper.config.AiCommitSettings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders commit messages from a user-configured template.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public class CommitMessageTemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * Renders a commit message with configured template variables.
     *
     * @param result generated model result
     * @param template message template
     * @param variablesText key=value variables
     * @return rendered commit message
     */
    public String render(CommitMessageResult result, String template, String variablesText) {
        String effectiveTemplate = template == null || template.trim().isEmpty()
                ? AiCommitSettings.DEFAULT_MESSAGE_TEMPLATE
                : template;
        Map<String, String> values = parseVariables(variablesText);
        values.put("title", result.getTitle().isEmpty() ? "更新代码改动" : result.getTitle());
        values.put("items.bullets", formatBullets(result));
        values.put("items.numbered", formatNumbered(result));
        return replacePlaceholders(effectiveTemplate, values).trim();
    }

    static Map<String, String> parseVariables(String variablesText) {
        Map<String, String> variables = new LinkedHashMap<>();
        if (variablesText == null || variablesText.trim().isEmpty()) {
            return variables;
        }
        String[] lines = variablesText.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int separator = trimmed.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = trimmed.substring(0, separator).trim();
            String value = trimmed.substring(separator + 1).trim();
            if (!key.isEmpty()) {
                variables.put(key, value);
            }
        }
        return variables;
    }

    private static String replacePlaceholders(String template, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = values.containsKey(key) ? values.get(key) : "";
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private static String formatBullets(CommitMessageResult result) {
        StringBuilder builder = new StringBuilder();
        for (String item : result.getItems()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("- ").append(item);
        }
        return builder.toString();
    }

    private static String formatNumbered(CommitMessageResult result) {
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (String item : result.getItems()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(index++).append('.').append(item);
        }
        return builder.toString();
    }
}
