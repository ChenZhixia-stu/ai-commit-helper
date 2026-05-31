package com.ai.commithelper;

import com.ai.commithelper.prompt.CommitMessageResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

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
}
