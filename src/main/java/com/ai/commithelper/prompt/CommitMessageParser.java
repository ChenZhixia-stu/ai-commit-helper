package com.ai.commithelper.prompt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses model responses into a stable commit message shape.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public class CommitMessageParser {

    private final Gson gson = new Gson();

    /**
     * Parses JSON returned by the model.
     *
     * @param content model content
     * @return parsed result
     */
    public CommitMessageResult parse(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new CommitMessageResult("更新代码改动", null);
        }
        String json = extractJson(content.trim());
        try {
            JsonObject object = new JsonParser().parse(json).getAsJsonObject();
            String title = object.has("title") && !object.get("title").isJsonNull()
                    ? object.get("title").getAsString()
                    : "更新代码改动";
            List<String> items = new ArrayList<>();
            if (object.has("items") && object.get("items").isJsonArray()) {
                object.getAsJsonArray("items").forEach(element -> {
                    if (!element.isJsonNull()) {
                        items.add(element.getAsString());
                    }
                });
            }
            return new CommitMessageResult(title, items);
        } catch (RuntimeException ignored) {
            return parsePlainText(content);
        }
    }

    private CommitMessageResult parsePlainText(String content) {
        String[] lines = content.replace("```json", "").replace("```", "").split("\\R");
        String title = "更新代码改动";
        List<String> items = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if ("更新代码改动".equals(title)) {
                title = trimmed.replaceFirst("^#+\\s*", "");
            } else {
                items.add(trimmed);
            }
        }
        return new CommitMessageResult(title, items);
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return gson.toJson(content);
    }
}
