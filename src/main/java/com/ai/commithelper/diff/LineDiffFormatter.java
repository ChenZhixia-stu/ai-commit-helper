package com.ai.commithelper.diff;

/**
 * Formats a compact line-level diff from two text snapshots.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
final class LineDiffFormatter {

    private static final int MAX_LINE_CHARS = 300;
    private static final int MAX_LCS_LINES = 1200;

    private LineDiffFormatter() {
    }

    static int appendModified(StringBuilder builder, String beforeContent,
                              String afterContent, int maxChangedLines) {
        String[] beforeLines = lines(beforeContent);
        String[] afterLines = lines(afterContent);
        if (beforeLines.length > MAX_LCS_LINES || afterLines.length > MAX_LCS_LINES) {
            return appendPositional(builder, beforeLines, afterLines, maxChangedLines);
        }
        return appendLcs(builder, beforeLines, afterLines, maxChangedLines);
    }

    static void appendLines(StringBuilder builder, String prefix, String content, int maxLines) {
        String[] contentLines = lines(content);
        int count = Math.min(contentLines.length, maxLines);
        for (int i = 0; i < count; i++) {
            builder.append(prefix).append(trimLine(contentLines[i])).append('\n');
        }
        if (contentLines.length > count) {
            builder.append("[File diff truncated]\n");
        }
    }

    private static int appendLcs(StringBuilder builder, String[] beforeLines,
                                 String[] afterLines, int maxChangedLines) {
        int[][] lengths = buildLcsLengths(beforeLines, afterLines);
        int beforeIndex = 0;
        int afterIndex = 0;
        int emitted = 0;

        while (beforeIndex < beforeLines.length && afterIndex < afterLines.length
                && emitted < maxChangedLines) {
            if (beforeLines[beforeIndex].equals(afterLines[afterIndex])) {
                beforeIndex++;
                afterIndex++;
            } else if (lengths[beforeIndex + 1][afterIndex] > lengths[beforeIndex][afterIndex + 1]) {
                builder.append("- ").append(trimLine(beforeLines[beforeIndex])).append('\n');
                beforeIndex++;
                emitted++;
            } else if (lengths[beforeIndex + 1][afterIndex] < lengths[beforeIndex][afterIndex + 1]) {
                builder.append("+ ").append(trimLine(afterLines[afterIndex])).append('\n');
                afterIndex++;
                emitted++;
            } else {
                builder.append("- ").append(trimLine(beforeLines[beforeIndex])).append('\n');
                beforeIndex++;
                emitted++;
                if (emitted < maxChangedLines) {
                    builder.append("+ ").append(trimLine(afterLines[afterIndex])).append('\n');
                    afterIndex++;
                    emitted++;
                }
            }
        }

        while (beforeIndex < beforeLines.length && emitted < maxChangedLines) {
            builder.append("- ").append(trimLine(beforeLines[beforeIndex])).append('\n');
            beforeIndex++;
            emitted++;
        }
        while (afterIndex < afterLines.length && emitted < maxChangedLines) {
            builder.append("+ ").append(trimLine(afterLines[afterIndex])).append('\n');
            afterIndex++;
            emitted++;
        }
        appendFooter(builder, emitted, maxChangedLines);
        return emitted;
    }

    private static int[][] buildLcsLengths(String[] beforeLines, String[] afterLines) {
        int[][] lengths = new int[beforeLines.length + 1][afterLines.length + 1];
        for (int i = beforeLines.length - 1; i >= 0; i--) {
            for (int j = afterLines.length - 1; j >= 0; j--) {
                if (beforeLines[i].equals(afterLines[j])) {
                    lengths[i][j] = lengths[i + 1][j + 1] + 1;
                } else {
                    lengths[i][j] = Math.max(lengths[i + 1][j], lengths[i][j + 1]);
                }
            }
        }
        return lengths;
    }

    private static int appendPositional(StringBuilder builder, String[] beforeLines,
                                        String[] afterLines, int maxChangedLines) {
        int max = Math.max(beforeLines.length, afterLines.length);
        int emitted = 0;

        builder.append("[Large line count; using compact positional summary]\n");
        for (int i = 0; i < max && emitted < maxChangedLines; i++) {
            String beforeLine = i < beforeLines.length ? beforeLines[i] : null;
            String afterLine = i < afterLines.length ? afterLines[i] : null;
            if (equals(beforeLine, afterLine)) {
                continue;
            }
            if (beforeLine != null) {
                builder.append("- ").append(trimLine(beforeLine)).append('\n');
                emitted++;
            }
            if (afterLine != null && emitted < maxChangedLines) {
                builder.append("+ ").append(trimLine(afterLine)).append('\n');
                emitted++;
            }
        }
        appendFooter(builder, emitted, maxChangedLines);
        return emitted;
    }

    private static void appendFooter(StringBuilder builder, int emitted, int maxChangedLines) {
        if (emitted == maxChangedLines) {
            builder.append("[File diff truncated]\n");
        }
        if (emitted == 0) {
            builder.append("[Metadata-only or whitespace-normalized change]\n");
        }
    }

    private static String[] lines(String content) {
        if (content == null || content.isEmpty()) {
            return new String[0];
        }
        return content.split("\\R", -1);
    }

    private static String trimLine(String line) {
        if (line == null) {
            return "";
        }
        if (line.length() <= MAX_LINE_CHARS) {
            return line;
        }
        return line.substring(0, MAX_LINE_CHARS) + "...";
    }

    private static boolean equals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
