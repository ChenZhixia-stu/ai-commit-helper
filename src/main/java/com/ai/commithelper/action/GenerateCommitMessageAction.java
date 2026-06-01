package com.ai.commithelper.action;

import com.ai.commithelper.config.ApiKeyStore;
import com.ai.commithelper.service.AiCommitNotifications;
import com.ai.commithelper.service.CommitMessageGenerationService;
import com.ai.commithelper.ui.AiCommitSettingsConfigurable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.vcs.commit.CommitWorkflowUi;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Action that generates and fills the commit message.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public class GenerateCommitMessageAction extends AnAction implements DumbAware {

    private static final String DEFAULT_TEXT = "Generate AI Commit Message";
    private static final String GENERATING_TEXT = "生成中...";
    private static final Set<Project> GENERATING_PROJECTS = ConcurrentHashMap.newKeySet();

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        boolean generating = project != null && GENERATING_PROJECTS.contains(project);
        event.getPresentation().setVisible(project != null);
        event.getPresentation().setEnabled(project != null && !generating);
        event.getPresentation().setText(generating ? GENERATING_TEXT : DEFAULT_TEXT);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getRequiredData(CommonDataKeys.PROJECT);
        CommitMessageI commitMessage = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        List<Change> changes = getIncludedChanges(event);

        if (!ApiKeyStore.hasApiKey()) {
            AiCommitNotifications.warn(project, "Please configure the DeepSeek API key first.");
            ShowSettingsUtil.getInstance().showSettingsDialog(project, AiCommitSettingsConfigurable.class);
            return;
        }
        if (commitMessage == null) {
            AiCommitNotifications.warn(project, "Commit message editor is not available in the current context.");
            return;
        }
        if (changes.isEmpty()) {
            AiCommitNotifications.warn(project, "Please select files to commit before generating a message.");
            return;
        }
        if (!GENERATING_PROJECTS.add(project)) {
            return;
        }
        event.getPresentation().setEnabled(false);
        event.getPresentation().setText(GENERATING_TEXT);

        new Task.Backgroundable(project, "Generating AI Commit Message", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(GENERATING_TEXT);
                try {
                    String generated = new CommitMessageGenerationService().generate(changes);
                    indicator.setText("Filling commit message...");
                    ApplicationManager.getApplication().invokeLater(() -> {
                        commitMessage.setCommitMessage(generated);
                        AiCommitNotifications.info(project, "AI commit message generated.");
                    });
                } catch (IOException | RuntimeException exception) {
                    AiCommitNotifications.error(project, "Failed to generate commit message: " + exception.getMessage());
                } finally {
                    GENERATING_PROJECTS.remove(project);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        event.getPresentation().setEnabled(true);
                        event.getPresentation().setText(DEFAULT_TEXT);
                    });
                }
            }
        }.queue();
    }

    private List<Change> getIncludedChanges(AnActionEvent event) {
        CommitWorkflowUi workflowUi = event.getData(VcsDataKeys.COMMIT_WORKFLOW_UI);
        if (workflowUi != null) {
            List<Change> includedChanges = workflowUi.getIncludedChanges();
            if (includedChanges != null && !includedChanges.isEmpty()) {
                return new ArrayList<>(includedChanges);
            }
        }

        Change[] selected = event.getData(VcsDataKeys.SELECTED_CHANGES);
        if (selected != null && selected.length > 0) {
            return new ArrayList<>(Arrays.asList(selected));
        }

        Change[] all = event.getData(VcsDataKeys.CHANGES);
        if (all != null && all.length > 0) {
            return new ArrayList<>(Arrays.asList(all));
        }

        return new ArrayList<>();
    }
}
