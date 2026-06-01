package com.ai.commithelper.diff;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;

import java.util.List;

/**
 * Builds a compact text diff summary from IntelliJ VCS changes.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public class ChangeDiffCollector {

    private static final int MAX_FILE_CHARS = 20000;
    private static final int MAX_CHANGED_LINES_PER_FILE = 220;

    /**
     * Collects a bounded diff prompt payload.
     *
     * @param changes selected changes
     * @param maxChars max output characters
     * @return diff summary
     */
    public String collect(List<Change> changes, int maxChars) {
        StringBuilder builder = new StringBuilder(Math.min(maxChars, 4096));
        int included = 0;
        boolean truncated = false;

        for (Change change : changes) {
            String fileSummary = summarize(change);
            if (fileSummary.trim().isEmpty()) {
                continue;
            }
            if (builder.length() + fileSummary.length() > maxChars) {
                int remaining = maxChars - builder.length();
                if (remaining > 200) {
                    builder.append(fileSummary, 0, Math.min(remaining, fileSummary.length()));
                }
                truncated = true;
                break;
            }
            builder.append(fileSummary);
            included++;
        }

        if (truncated) {
            builder.append("\n[Diff truncated because it exceeded the configured limit]\n");
        }
        if (included == 0 && !changes.isEmpty()) {
            builder.append("[No text diff could be extracted. Changes may be binary or unavailable.]\n");
        }
        return builder.toString();
    }

    private String summarize(Change change) {
        ContentRevision before = change.getBeforeRevision();
        ContentRevision after = change.getAfterRevision();
        String path = pathOf(after != null ? after : before);
        String beforeContent = readContent(before);
        String afterContent = readContent(after);

        StringBuilder builder = new StringBuilder();
        builder.append("File: ").append(path).append('\n');
        builder.append("Change-Type: ").append(change.getType().name()).append('\n');

        if (beforeContent == null && afterContent == null) {
            builder.append("[Binary or unavailable content]\n\n");
            return builder.toString();
        }
        if (tooLarge(beforeContent) || tooLarge(afterContent)) {
            builder.append("[Large file content skipped]\n\n");
            return builder.toString();
        }

        if (beforeContent == null) {
            appendAdded(builder, afterContent);
        } else if (afterContent == null) {
            appendDeleted(builder, beforeContent);
        } else {
            appendModified(builder, beforeContent, afterContent);
        }
        builder.append('\n');
        return builder.toString();
    }

    private void appendAdded(StringBuilder builder, String content) {
        builder.append("@@ added file @@\n");
        LineDiffFormatter.appendLines(builder, "+ ", content, MAX_CHANGED_LINES_PER_FILE);
    }

    private void appendDeleted(StringBuilder builder, String content) {
        builder.append("@@ deleted file @@\n");
        LineDiffFormatter.appendLines(builder, "- ", content, MAX_CHANGED_LINES_PER_FILE);
    }

    private void appendModified(StringBuilder builder, String beforeContent, String afterContent) {
        builder.append("@@ changed lines @@\n");
        LineDiffFormatter.appendModified(builder, beforeContent, afterContent, MAX_CHANGED_LINES_PER_FILE);
    }

    private static boolean tooLarge(String content) {
        return content != null && content.length() > MAX_FILE_CHARS;
    }

    private static String readContent(ContentRevision revision) {
        if (revision == null) {
            return null;
        }
        try {
            return revision.getContent();
        } catch (VcsException | RuntimeException ignored) {
            return null;
        }
    }

    private static String pathOf(ContentRevision revision) {
        if (revision == null) {
            return "<unknown>";
        }
        FilePath file = revision.getFile();
        return file == null ? "<unknown>" : file.getPath();
    }
}
