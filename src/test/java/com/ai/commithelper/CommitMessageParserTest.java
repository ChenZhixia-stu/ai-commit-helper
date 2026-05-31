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
    public void shouldFallbackToPlainText() {
        CommitMessageResult result = new CommitMessageParser()
                .parse("优化提交生成\n- 读取勾选文件\n- 回填输入框");

        Assert.assertEquals("优化提交生成", result.getTitle());
        Assert.assertTrue(result.format().contains("- 读取勾选文件"));
    }
}
