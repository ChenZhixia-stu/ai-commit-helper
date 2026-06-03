package com.ai.commithelper;

import com.ai.commithelper.prompt.CommitMessageResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Tests commit message rendering.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public class CommitMessageResultTest {

    @Test
    public void shouldRenderTitleAndBullets() {
        CommitMessageResult result = new CommitMessageResult("优化私信功能",
                Arrays.asList("调整私信列表", "修复未读状态"));

        Assert.assertEquals("优化私信功能\n\n- 调整私信列表\n- 修复未读状态", result.format());
    }

    @Test
    public void shouldStripExistingBulletPrefixes() {
        CommitMessageResult result = new CommitMessageResult("优化模板",
                Arrays.asList("- 新增模板", "* 补充说明", "• 清理格式"));

        Assert.assertEquals("优化模板\n\n- 新增模板\n- 补充说明\n- 清理格式", result.format());
    }

    @Test
    public void shouldFallbackWhenTitleAndItemsAreMissing() {
        CommitMessageResult result = new CommitMessageResult(" ", Collections.emptyList());

        Assert.assertEquals("更新代码改动\n\n- 总结本次代码改动", result.format());
    }
}
