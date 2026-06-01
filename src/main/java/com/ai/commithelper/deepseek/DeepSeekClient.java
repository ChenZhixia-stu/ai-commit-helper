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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     * Generates a commit message with an explicit request timeout.
     */
    public CommitMessageResult generate(AiCommitSettings settings, String apiKey,
                                        String prompt, int timeoutSeconds) throws IOException {
        return generate(settings.getBaseUrl(), settings.getModel(), apiKey, timeoutSeconds, prompt);
    }

    /**
     * Performs a lightweight connection test using explicit parameters (no global settings side-effects).
     */
    public void testConnection(String baseUrl, String model, String apiKey, int timeoutSeconds) throws IOException {
        streamCheck(baseUrl, model, apiKey, timeoutSeconds);
    }

    /**
     * Lists models from an OpenAI-compatible /models endpoint.
     */
    public List<String> listModels(String baseUrl, String apiKey, int timeoutSeconds) throws IOException {
        URL url = new URL(baseUrl + "/models");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(timeoutSeconds * 1000);
        connection.setReadTimeout(timeoutSeconds * 1000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);

        int status = connection.getResponseCode();
        String response = read(status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream());
        if (status < 200 || status >= 300) {
            throw new IOException("DeepSeek models request failed: HTTP " + status + " " + response);
        }
        return parseModels(response);
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

    private void streamCheck(String baseUrl, String model, String apiKey, int timeoutSeconds) throws IOException {
        URL url = new URL(baseUrl + "/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(timeoutSeconds * 1000);
        connection.setReadTimeout(timeoutSeconds * 1000);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);

        byte[] body = buildStreamCheckBody(model).getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(body.length);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body);
        }

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IOException("DeepSeek stream check failed: HTTP " + status + " " + read(connection.getErrorStream()));
        }
        readFirstStreamChunk(connection.getInputStream());
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

    private String buildStreamCheckBody(String model) {
        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        root.addProperty("stream", true);
        root.addProperty("temperature", 0);
        root.addProperty("max_tokens", 8);

        JsonArray messages = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", "Reply with OK.");
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

    static List<String> parseModels(String response) throws IOException {
        try {
            JsonObject object = new JsonParser().parse(response).getAsJsonObject();
            JsonArray data = object.getAsJsonArray("data");
            if (data == null) {
                return Collections.emptyList();
            }

            List<String> models = new ArrayList<>();
            data.forEach(element -> {
                if (!element.isJsonObject()) {
                    return;
                }
                JsonObject model = element.getAsJsonObject();
                if (model.has("id") && !model.get("id").isJsonNull()) {
                    String id = model.get("id").getAsString().trim();
                    if (!id.isEmpty() && !models.contains(id)) {
                        models.add(id);
                    }
                }
            });
            Collections.sort(models);
            return models;
        } catch (RuntimeException exception) {
            throw new IOException("Failed to parse DeepSeek models response: " + exception.getMessage(), exception);
        }
    }

    private void readFirstStreamChunk(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("DeepSeek stream check returned an empty response.");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String data = extractSseData(line);
                if (data == null || data.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    throw new IOException("DeepSeek stream ended before any model chunk was received.");
                }
                if (isValidStreamChunk(data)) {
                    return;
                }
            }
        }
        throw new IOException("DeepSeek stream check ended without a valid model chunk.");
    }

    static String extractSseData(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("data:")) {
            return null;
        }
        return trimmed.substring("data:".length()).trim();
    }

    static boolean isValidStreamChunk(String data) throws IOException {
        try {
            JsonObject object = new JsonParser().parse(data).getAsJsonObject();
            if (object.has("error") && object.get("error").isJsonObject()) {
                JsonObject error = object.getAsJsonObject("error");
                String message = error.has("message") && !error.get("message").isJsonNull()
                        ? error.get("message").getAsString()
                        : error.toString();
                throw new IOException("DeepSeek stream returned an error: " + message);
            }
            JsonArray choices = object.getAsJsonArray("choices");
            return choices != null && choices.size() > 0;
        } catch (IOException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IOException("Failed to parse DeepSeek stream chunk: " + exception.getMessage(), exception);
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
