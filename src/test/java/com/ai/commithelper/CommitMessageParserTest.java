package com.ai.commithelper;

import com.ai.commithelper.prompt.CommitMessageParser;
import com.ai.commithelper.prompt.CommitMessageResult;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests model response parsing.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public class CommitMessageParserTest {

    @Test
    public void shouldParseJsonContent() {
        CommitMessageResult result = new CommitMessageParser()
                .parse("{\"title\":\"优化提交生成\",\"items\":[\"读取勾选文件\",\"回填输入框\"]}");

        Assert.assertEquals("优化提交生成", result.getTitle());
        Assert.assertEquals(2, result.getItems().size());
        Assert.assertEquals("读取勾选文件", result.getItems().get(0));
        Assert.assertEquals("回填输入框", result.getItems().get(1));
    }

    @Test
    public void shouldParseJsonWrappedInMarkdownFence() {
        CommitMessageResult result = new CommitMessageParser()
                .parse("```json\n{\"title\":\"优化模板\",\"items\":[\"新增模板配置\"]}\n```");

        Assert.assertEquals("优化模板", result.getTitle());
        Assert.assertEquals("新增模板配置", result.getItems().get(0));
    }

    @Test
    public void shouldFallbackWhenContentIsBlank() {
        CommitMessageResult result = new CommitMessageParser().parse("  ");

        Assert.assertEquals("更新代码改动", result.getTitle());
        Assert.assertEquals("总结本次代码改动", result.getItems().get(0));
    }

    @Test
    public void shouldFallbackToPlainText() {
        CommitMessageResult result = new CommitMessageParser()
                .parse("优化提交生成\n- 读取勾选文件\n- 回填输入框");

        Assert.assertEquals("优化提交生成", result.getTitle());
        Assert.assertTrue(result.format().contains("- 读取勾选文件"));
    }
}
