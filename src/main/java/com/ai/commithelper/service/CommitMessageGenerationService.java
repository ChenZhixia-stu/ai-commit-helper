package com.ai.commithelper.service;

import com.ai.commithelper.config.AiCommitSettings;
import com.ai.commithelper.config.ApiKeyStore;
import com.ai.commithelper.deepseek.DeepSeekClient;
import com.ai.commithelper.diff.ChangeDiffCollector;
import com.ai.commithelper.prompt.CommitMessageResult;
import com.ai.commithelper.prompt.CommitPromptBuilder;
import com.intellij.openapi.vcs.changes.Change;

import java.io.IOException;
import java.util.List;

/**
 * Orchestrates diff collection, prompting, and DeepSeek generation.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public class CommitMessageGenerationService {

    private static final int GENERATION_TIMEOUT_SECONDS = 10;

    private final ChangeDiffCollector diffCollector = new ChangeDiffCollector();
    private final CommitPromptBuilder promptBuilder = new CommitPromptBuilder();
    private final DeepSeekClient deepSeekClient = new DeepSeekClient();

    /**
     * Generates the final commit message text.
     *
     * @param changes selected changes
     * @return formatted commit message
     * @throws IOException when model generation fails
     */
    public String generate(List<Change> changes) throws IOException {
        AiCommitSettings settings = AiCommitSettings.getInstance();
        String diffSummary = diffCollector.collect(changes, settings.getMaxDiffChars());
        String prompt = promptBuilder.build(diffSummary, settings.getLanguage());
        CommitMessageResult result = deepSeekClient.generate(settings, ApiKeyStore.getApiKey(),
                prompt, GENERATION_TIMEOUT_SECONDS);
        return result.format();
    }
}
