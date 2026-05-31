package com.ai.commithelper.prompt;

/**
 * Builds the LLM prompt used to generate commit messages.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public class CommitPromptBuilder {

    /**
     * Creates a strict JSON prompt.
     *
     * @param diffSummary selected changes summary
     * @param language output language
     * @return prompt
     */
    public String build(String diffSummary, String language) {
        String outputLanguage = language == null || language.trim().isEmpty() ? "中文" : language.trim();
        return "你是资深软件工程师，请根据下面的代码变更生成 Git commit message。\n"
                + "要求：\n"
                + "1. 只总结本次 diff 中体现的实际改动，不要编造。\n"
                + "2. 使用" + outputLanguage + "。\n"
                + "3. 标题不超过 50 个中文字符，简短准确。\n"
                + "4. items 输出 2 到 6 条，每条描述一个业务或技术改动点。\n"
                + "5. 只能输出 JSON，不要 Markdown，不要代码块。\n"
                + "JSON 格式：{\"title\":\"...\",\"items\":[\"...\",\"...\"]}\n\n"
                + "代码变更：\n"
                + diffSummary;
    }
}
