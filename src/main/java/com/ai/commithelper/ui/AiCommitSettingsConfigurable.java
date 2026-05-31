package com.ai.commithelper.ui;

import com.ai.commithelper.config.AiCommitSettings;
import com.ai.commithelper.config.ApiKeyStore;
import com.ai.commithelper.deepseek.DeepSeekClient;
import com.ai.commithelper.service.AiCommitNotifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.Arrays;

/**
 * Settings page under Tools | AI Commit Helper.
 *
 * @author AI Commit Helper
 * @since 1.0.0
 */
public class AiCommitSettingsConfigurable implements SearchableConfigurable {

    private volatile boolean disposed;
    private JPanel panel;
    private JTextField baseUrlField;
    private JTextField modelField;
    private JPasswordField apiKeyField;
    private JSpinner timeoutSpinner;
    private JSpinner maxDiffCharsSpinner;
    private JComboBox<String> languageComboBox;
    private JButton testButton;

    @NotNull
    @Override
    public String getId() {
        return "com.ai.commithelper.settings";
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "AI Commit Helper";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        panel = new JPanel(new GridBagLayout());
        panel.setLayout(new GridBagLayout());

        baseUrlField = new JTextField();
        modelField = new JTextField();
        apiKeyField = new JPasswordField();
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 300, 1));
        maxDiffCharsSpinner = new JSpinner(new SpinnerNumberModel(100000, 1000, 500000, 1000));
        languageComboBox = new JComboBox<>(new String[]{"中文", "English"});

        int row = 0;
        addRow(panel, row++, "Base URL:", baseUrlField);
        addRow(panel, row++, "Model:", modelField);
        addRow(panel, row++, "API Key:", apiKeyField);
        addRow(panel, row++, "Timeout Seconds:", timeoutSpinner);
        addRow(panel, row++, "Max Diff Characters:", maxDiffCharsSpinner);
        addRow(panel, row++, "Language:", languageComboBox);

        testButton = new JButton("Test Connection");
        testButton.addActionListener(event -> testConnection());
        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 1;
        buttonConstraints.gridy = row++;
        buttonConstraints.anchor = GridBagConstraints.WEST;
        buttonConstraints.insets = new Insets(8, 0, 0, 0);
        panel.add(testButton, buttonConstraints);

        JLabel hint = new JLabel("Only the selected commit changes are sent to the configured API URL.");
        GridBagConstraints hintConstraints = new GridBagConstraints();
        hintConstraints.gridx = 1;
        hintConstraints.gridy = row;
        hintConstraints.anchor = GridBagConstraints.WEST;
        hintConstraints.insets = new Insets(8, 0, 0, 0);
        panel.add(hint, hintConstraints);

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        AiCommitSettings settings = AiCommitSettings.getInstance();
        String currentLanguage = (String) languageComboBox.getSelectedItem();
        return !baseUrlField.getText().trim().equals(settings.getBaseUrl())
                || !modelField.getText().trim().equals(settings.getModel())
                || !new String(apiKeyField.getPassword()).trim().equals(ApiKeyStore.getApiKey())
                || !timeoutSpinner.getValue().equals(settings.getTimeoutSeconds())
                || !maxDiffCharsSpinner.getValue().equals(settings.getMaxDiffChars())
                || !currentLanguage.equals(settings.getLanguage());
    }

    @Override
    public void apply() throws ConfigurationException {
        String baseUrl = baseUrlField.getText().trim();
        String model = modelField.getText().trim();
        String language = (String) languageComboBox.getSelectedItem();
        if (baseUrl.isEmpty()) {
            throw new ConfigurationException("Base URL cannot be empty.");
        }
        if (model.isEmpty()) {
            throw new ConfigurationException("Model cannot be empty.");
        }
        AiCommitSettings settings = AiCommitSettings.getInstance();
        settings.setBaseUrl(baseUrl);
        settings.setModel(model);
        settings.setTimeoutSeconds((Integer) timeoutSpinner.getValue());
        settings.setMaxDiffChars((Integer) maxDiffCharsSpinner.getValue());
        settings.setLanguage(language);
        ApiKeyStore.setApiKey(new String(apiKeyField.getPassword()));
    }

    @Override
    public void reset() {
        AiCommitSettings settings = AiCommitSettings.getInstance();
        baseUrlField.setText(settings.getBaseUrl());
        modelField.setText(settings.getModel());
        apiKeyField.setText(ApiKeyStore.getApiKey());
        timeoutSpinner.setValue(settings.getTimeoutSeconds());
        maxDiffCharsSpinner.setValue(settings.getMaxDiffChars());
        languageComboBox.setSelectedItem(settings.getLanguage());
    }

    @Override
    public void disposeUIResources() {
        disposed = true;
        if (apiKeyField != null) {
            Arrays.fill(apiKeyField.getPassword(), '\0');
        }
    }

    private void runWhenAlive(Runnable action) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!disposed) {
                action.run();
            }
        });
    }

    private void testConnection() {
        testButton.setEnabled(false);
        testButton.setText("Testing...");

        String baseUrl = baseUrlField.getText().trim();
        String model = modelField.getText().trim();
        String apiKey = new String(apiKeyField.getPassword()).trim();
        int timeout = (Integer) timeoutSpinner.getValue();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                new DeepSeekClient().testConnection(baseUrl, model, apiKey, timeout);
                runWhenAlive(() ->
                        AiCommitNotifications.info(null, "Connection succeeded."));
            } catch (IOException | RuntimeException exception) {
                runWhenAlive(() ->
                        AiCommitNotifications.error(null, "Connection failed: " + exception.getMessage()));
            } finally {
                runWhenAlive(() -> {
                    testButton.setEnabled(true);
                    testButton.setText("Test Connection");
                });
            }
        });
    }

    private void addRow(JPanel form, int row, String label, JComponent component) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(6, 0, 6, 12);
        form.add(new JLabel(label), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(6, 0, 6, 0);
        form.add(component, fieldConstraints);
    }
}
