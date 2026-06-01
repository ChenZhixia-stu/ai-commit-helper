package com.ai.commithelper;

import com.ai.commithelper.config.AiCommitSettings;
import com.ai.commithelper.prompt.CommitMessageResult;
import com.ai.commithelper.prompt.CommitMessageTemplateRenderer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Tests commit message template rendering.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public class CommitMessageTemplateRendererTest {

    private final CommitMessageTemplateRenderer renderer = new CommitMessageTemplateRenderer();

    @Test
    public void shouldRenderDefaultTemplateLikePreviousFormat() {
        CommitMessageResult result = new CommitMessageResult("优化私信功能",
                Arrays.asList("调整私信列表", "修复未读状态"));

        String rendered = renderer.render(result, AiCommitSettings.DEFAULT_MESSAGE_TEMPLATE, "");

        Assert.assertEquals("优化私信功能\n\n- 调整私信列表\n- 修复未读状态", rendered);
    }

    @Test
    public void shouldRenderCompanyNumberedTemplate() {
        CommitMessageResult result = new CommitMessageResult("message概要",
                Arrays.asList("升级Xcode26以及iOS26适配", "补充构建配置"));
        String variables = "changeId=T202604205176-1\n"
                + "bugId=\n"
                + "description=江苏信托iOS";

        String rendered = renderer.render(result, AiCommitSettings.COMPANY_NUMBERED_TEMPLATE, variables);

        Assert.assertEquals("[修改单编号]T202604205176-1\n"
                + "[缺陷编号]\n"
                + "[修改说明]江苏信托iOS\n"
                + "message概要\n"
                + "1.升级Xcode26以及iOS26适配\n"
                + "2.补充构建配置", rendered);
    }

    @Test
    public void shouldRenderCustomVariables() {
        CommitMessageResult result = new CommitMessageResult("优化登录",
                Arrays.asList("调整验证码校验"));
        String template = "[需求编号]${requirementId}\n"
                + "[缺陷编号]${bugId}\n"
                + "[影响范围]${scope}\n\n"
                + "${title}\n\n"
                + "${items.numbered}";
        String variables = "requirementId=REQ-2026-001\n"
                + "bugId=BUG-8848\n"
                + "scope=移动端登录";

        String rendered = renderer.render(result, template, variables);

        Assert.assertEquals("[需求编号]REQ-2026-001\n"
                + "[缺陷编号]BUG-8848\n"
                + "[影响范围]移动端登录\n\n"
                + "优化登录\n\n"
                + "1.调整验证码校验", rendered);
    }

    @Test
    public void shouldRenderMissingVariablesAsEmptyString() {
        CommitMessageResult result = new CommitMessageResult("优化登录",
                Arrays.asList("调整验证码校验"));

        String rendered = renderer.render(result, "[缺陷编号]${bugId}\n${unknown}\n${items.bullets}", "");

        Assert.assertEquals("[缺陷编号]\n\n- 调整验证码校验", rendered);
    }
}
