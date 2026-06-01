package com.ai.commithelper.deepseek;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Tests DeepSeek API response parsing.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public class DeepSeekClientTest {

    @Test
    public void shouldParseModelIds() throws IOException {
        String response = "{"
                + "\"object\":\"list\","
                + "\"data\":["
                + "{\"id\":\"deepseek-reasoner\"},"
                + "{\"id\":\"deepseek-chat\"},"
                + "{\"id\":\"deepseek-chat\"},"
                + "{\"object\":\"model\"}"
                + "]"
                + "}";

        List<String> models = DeepSeekClient.parseModels(response);

        Assert.assertEquals(Arrays.asList("deepseek-chat", "deepseek-reasoner"), models);
    }

    @Test
    public void shouldExtractSseDataLine() {
        Assert.assertEquals("{\"choices\":[]}",
                DeepSeekClient.extractSseData("data: {\"choices\":[]}"));
        Assert.assertNull(DeepSeekClient.extractSseData("event: message"));
    }

    @Test
    public void shouldAcceptValidStreamChunk() throws IOException {
        String chunk = "{\"choices\":[{\"delta\":{\"content\":\"OK\"},\"index\":0}]}";

        Assert.assertTrue(DeepSeekClient.isValidStreamChunk(chunk));
    }

    @Test
    public void shouldRejectStreamErrorChunk() {
        String chunk = "{\"error\":{\"message\":\"model not found\"}}";

        try {
            DeepSeekClient.isValidStreamChunk(chunk);
            Assert.fail("Expected IOException");
        } catch (IOException exception) {
            Assert.assertTrue(exception.getMessage().contains("model not found"));
        }
    }
}
