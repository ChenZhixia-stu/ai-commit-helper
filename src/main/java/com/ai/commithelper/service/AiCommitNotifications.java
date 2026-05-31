package com.ai.commithelper.service;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

/**
 * Notification helper for the plugin.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public final class AiCommitNotifications {

    private static final String GROUP_ID = "AI Commit Helper";

    private AiCommitNotifications() {
    }

    /**
     * Shows an information balloon.
     *
     * @param project project
     * @param content message
     */
    public static void info(Project project, String content) {
        notify(project, content, NotificationType.INFORMATION);
    }

    /**
     * Shows a warning balloon.
     *
     * @param project project
     * @param content message
     */
    public static void warn(Project project, String content) {
        notify(project, content, NotificationType.WARNING);
    }

    /**
     * Shows an error balloon.
     *
     * @param project project
     * @param content message
     */
    public static void error(Project project, String content) {
        notify(project, content, NotificationType.ERROR);
    }

    private static void notify(Project project, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(content, type)
                .notify(project);
    }
}
