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
    public void shouldParseJsonResponse() {
        CommitMessageResult result = new CommitMessageParser()
                .parse("{\"title\":\"优化私信功能\",\"items\":[\"调整列表展示\",\"修复未读状态\"]}");

        Assert.assertEquals("优化私信功能", result.getTitle());
        Assert.assertEquals(2, result.getItems().size());
        Assert.assertTrue(result.format().contains("- 调整列表展示"));
    }

    @Test
    public void shouldFallbackToPlainText() {
        CommitMessageResult result = new CommitMessageParser()
                .parse("优化提交生成\n- 读取勾选文件\n- 回填输入框");

        Assert.assertEquals("优化提交生成", result.getTitle());
        Assert.assertTrue(result.format().contains("- 读取勾选文件"));
    }
}
