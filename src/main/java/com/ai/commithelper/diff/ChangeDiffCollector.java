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
        appendLines(builder, "+ ", content);
    }

    private void appendDeleted(StringBuilder builder, String content) {
        builder.append("@@ deleted file @@\n");
        appendLines(builder, "- ", content);
    }

    private void appendModified(StringBuilder builder, String beforeContent, String afterContent) {
        String[] beforeLines = lines(beforeContent);
        String[] afterLines = lines(afterContent);
        int max = Math.max(beforeLines.length, afterLines.length);
        int emitted = 0;

        builder.append("@@ changed lines @@\n");
        for (int i = 0; i < max && emitted < MAX_CHANGED_LINES_PER_FILE; i++) {
            String beforeLine = i < beforeLines.length ? beforeLines[i] : null;
            String afterLine = i < afterLines.length ? afterLines[i] : null;
            if (equals(beforeLine, afterLine)) {
                continue;
            }
            if (beforeLine != null) {
                builder.append("- ").append(trimLine(beforeLine)).append('\n');
                emitted++;
            }
            if (afterLine != null && emitted < MAX_CHANGED_LINES_PER_FILE) {
                builder.append("+ ").append(trimLine(afterLine)).append('\n');
                emitted++;
            }
        }
        if (emitted == MAX_CHANGED_LINES_PER_FILE) {
            builder.append("[File diff truncated]\n");
        }
        if (emitted == 0) {
            builder.append("[Metadata-only or whitespace-normalized change]\n");
        }
    }

    private void appendLines(StringBuilder builder, String prefix, String content) {
        String[] lines = lines(content);
        int count = Math.min(lines.length, MAX_CHANGED_LINES_PER_FILE);
        for (int i = 0; i < count; i++) {
            builder.append(prefix).append(trimLine(lines[i])).append('\n');
        }
        if (lines.length > count) {
            builder.append("[File diff truncated]\n");
        }
    }

    private static String[] lines(String content) {
        if (content == null || content.isEmpty()) {
            return new String[0];
        }
        return content.split("\\R", -1);
    }

    private static boolean tooLarge(String content) {
        return content != null && content.length() > MAX_FILE_CHARS;
    }

    private static String trimLine(String line) {
        if (line == null) {
            return "";
        }
        if (line.length() <= 300) {
            return line;
        }
        return line.substring(0, 300) + "...";
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

    private static boolean equals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
