package com.ai.commithelper.diff;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests compact line-level diff formatting.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public class LineDiffFormatterTest {

    @Test
    public void shouldOnlyEmitInsertedLine() {
        StringBuilder builder = new StringBuilder();

        LineDiffFormatter.appendModified(builder,
                "alpha\nbeta\ngamma",
                "alpha\nnew line\nbeta\ngamma",
                20);

        Assert.assertEquals("+ new line\n", builder.toString());
    }

    @Test
    public void shouldOnlyEmitDeletedLine() {
        StringBuilder builder = new StringBuilder();

        LineDiffFormatter.appendModified(builder,
                "alpha\nremoved\nbeta\ngamma",
                "alpha\nbeta\ngamma",
                20);

        Assert.assertEquals("- removed\n", builder.toString());
    }

    @Test
    public void shouldEmitReplacementAsDeleteAndAdd() {
        StringBuilder builder = new StringBuilder();

        LineDiffFormatter.appendModified(builder,
                "alpha\nold value\ngamma",
                "alpha\nnew value\ngamma",
                20);

        Assert.assertEquals("- old value\n+ new value\n", builder.toString());
    }

    @Test
    public void shouldTruncateChangedLines() {
        StringBuilder builder = new StringBuilder();

        LineDiffFormatter.appendModified(builder,
                "one\ntwo\nthree",
                "uno\ndos\ntres",
                2);

        Assert.assertEquals("- one\n+ uno\n[File diff truncated]\n", builder.toString());
    }
}
