package com.ai.commithelper.deepseek;

import com.ai.commithelper.config.AiCommitSettings;
import com.ai.commithelper.prompt.CommitMessageParser;
import com.ai.commithelper.prompt.CommitMessageResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Minimal DeepSeek OpenAI-compatible chat completion client.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public class DeepSeekClient {

    private final CommitMessageParser parser = new CommitMessageParser();

    /**
     * Generates a commit message.
     *
     * @param settings plugin settings
     * @param apiKey API key
     * @param prompt user prompt
     * @return generated message
     * @throws IOException when the API call fails
     */
    public CommitMessageResult generate(AiCommitSettings settings, String apiKey, String prompt) throws IOException {
        return generate(settings.getBaseUrl(), settings.getModel(), apiKey,
                settings.getTimeoutSeconds(), prompt);
    }

    /**
     * Performs a lightweight connection test using explicit parameters (no global settings side-effects).
     */
    public void testConnection(String baseUrl, String model, String apiKey, int timeoutSeconds) throws IOException {
        generate(baseUrl, model, apiKey, timeoutSeconds,
                "请只输出 JSON：{\"title\":\"连接测试\",\"items\":[\"DeepSeek 配置可用\"]}");
    }

    private CommitMessageResult generate(String baseUrl, String model, String apiKey,
                                         int timeoutSeconds, String prompt) throws IOException {
        URL url = new URL(baseUrl + "/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(timeoutSeconds * 1000);
        connection.setReadTimeout(timeoutSeconds * 1000);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);

        byte[] body = buildRequestBody(model, prompt).getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(body.length);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body);
        }

        int status = connection.getResponseCode();
        String response = read(status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream());
        if (status < 200 || status >= 300) {
            throw new IOException("DeepSeek API request failed: HTTP " + status + " " + response);
        }
        return parser.parse(extractMessageContent(response));
    }

    private String buildRequestBody(String model, String prompt) {
        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        root.addProperty("stream", false);
        root.addProperty("temperature", 0.2);

        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", "你是一个只输出严格 JSON 的 Git commit message 生成助手。");
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prompt);
        messages.add(user);

        root.add("messages", messages);
        return root.toString();
    }

    private String extractMessageContent(String response) throws IOException {
        try {
            JsonObject object = new JsonParser().parse(response).getAsJsonObject();
            JsonArray choices = object.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                throw new IOException("DeepSeek response has no choices.");
            }
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message == null || !message.has("content")) {
                throw new IOException("DeepSeek response has no message content.");
            }
            return message.get("content").getAsString();
        } catch (RuntimeException exception) {
            throw new IOException("Failed to parse DeepSeek response: " + exception.getMessage(), exception);
        }
    }

    private String read(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }
}
