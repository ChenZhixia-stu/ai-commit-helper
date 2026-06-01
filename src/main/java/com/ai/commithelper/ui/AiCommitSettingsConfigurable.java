package com.ai.commithelper.ui;

import com.ai.commithelper.config.AiCommitSettings;
import com.ai.commithelper.config.ApiKeyStore;
import com.ai.commithelper.deepseek.DeepSeekClient;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
    private JComboBox<String> modelComboBox;
    private JPasswordField apiKeyField;
    private JSpinner timeoutSpinner;
    private JSpinner maxDiffCharsSpinner;
    private JComboBox<String> languageComboBox;
    private JComboBox<String> templatePresetComboBox;
    private JTextArea messageTemplateArea;
    private JTextArea templateVariablesArea;
    private JButton fetchModelsButton;
    private JButton testConnectionButton;

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
        panel = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        panel.add(form, BorderLayout.NORTH);

        baseUrlField = new JTextField();
        modelComboBox = new JComboBox<>();
        modelComboBox.setEditable(true);
        apiKeyField = new JPasswordField();
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 300, 1));
        maxDiffCharsSpinner = new JSpinner(new SpinnerNumberModel(100000, 1000, 500000, 1000));
        languageComboBox = new JComboBox<>(new String[]{"中文", "English"});
        templatePresetComboBox = new JComboBox<>(new String[]{
                AiCommitSettings.PRESET_DEFAULT,
                AiCommitSettings.PRESET_COMPANY_NUMBERED,
                AiCommitSettings.PRESET_CUSTOM
        });
        templatePresetComboBox.addActionListener(event -> applySelectedTemplatePreset());
        messageTemplateArea = new JTextArea(7, 20);
        templateVariablesArea = new JTextArea(5, 20);
        fetchModelsButton = new JButton("Fetch Models");
        fetchModelsButton.setFocusable(false);
        fetchModelsButton.addActionListener(event -> fetchModels());
        testConnectionButton = new JButton("Test Connection");
        testConnectionButton.setFocusable(false);
        testConnectionButton.addActionListener(event -> testConnection());

        int row = 0;
        addRow(form, row++, "Base URL:", baseUrlField);
        addRow(form, row++, "Model:", createModelSelector());
        addRow(form, row++, "API Key:", apiKeyField);
        addRow(form, row++, "Timeout Seconds:", timeoutSpinner);
        addRow(form, row++, "Max Diff Characters:", maxDiffCharsSpinner);
        addRow(form, row++, "Language:", languageComboBox);
        addRow(form, row++, "Message Template Preset:", templatePresetComboBox);
        addRow(form, row++, "Message Template:", new JScrollPane(messageTemplateArea));
        addRow(form, row++, "", createTemplateHelp());
        addRow(form, row++, "Template Variables:", new JScrollPane(templateVariablesArea));
        addRow(form, row++, "", testConnectionButton);

        JLabel hint = new JLabel("Only the selected commit changes are sent to the configured API URL.");
        GridBagConstraints hintConstraints = new GridBagConstraints();
        hintConstraints.gridx = 1;
        hintConstraints.gridy = row;
        hintConstraints.anchor = GridBagConstraints.WEST;
        hintConstraints.insets = new Insets(8, 0, 0, 0);
        form.add(hint, hintConstraints);

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        AiCommitSettings settings = AiCommitSettings.getInstance();
        String currentLanguage = (String) languageComboBox.getSelectedItem();
        return !baseUrlField.getText().trim().equals(settings.getBaseUrl())
                || !getModelText().equals(settings.getModel())
                || !new String(apiKeyField.getPassword()).trim().equals(ApiKeyStore.getApiKey())
                || !timeoutSpinner.getValue().equals(settings.getTimeoutSeconds())
                || !maxDiffCharsSpinner.getValue().equals(settings.getMaxDiffChars())
                || !currentLanguage.equals(settings.getLanguage())
                || !getTemplatePreset().equals(settings.getMessageTemplatePreset())
                || !messageTemplateArea.getText().equals(settings.getMessageTemplate())
                || !templateVariablesArea.getText().equals(settings.getTemplateVariables());
    }

    @Override
    public void apply() throws ConfigurationException {
        String baseUrl = baseUrlField.getText().trim();
        String model = getModelText();
        String language = (String) languageComboBox.getSelectedItem();
        String messageTemplate = messageTemplateArea.getText();
        if (baseUrl.isEmpty()) {
            throw new ConfigurationException("Base URL cannot be empty.");
        }
        if (model.isEmpty()) {
            throw new ConfigurationException("Model cannot be empty.");
        }
        if (messageTemplate.trim().isEmpty()) {
            throw new ConfigurationException("Message Template cannot be empty.");
        }
        AiCommitSettings settings = AiCommitSettings.getInstance();
        settings.setBaseUrl(baseUrl);
        settings.setModel(model);
        settings.setTimeoutSeconds((Integer) timeoutSpinner.getValue());
        settings.setMaxDiffChars((Integer) maxDiffCharsSpinner.getValue());
        settings.setLanguage(language);
        settings.setMessageTemplatePreset(getTemplatePreset());
        settings.setMessageTemplate(messageTemplate);
        settings.setTemplateVariables(templateVariablesArea.getText());
        ApiKeyStore.setApiKey(new String(apiKeyField.getPassword()));
    }

    @Override
    public void reset() {
        AiCommitSettings settings = AiCommitSettings.getInstance();
        baseUrlField.setText(settings.getBaseUrl());
        setModelOptions(settings.getModel(), null);
        apiKeyField.setText(ApiKeyStore.getApiKey());
        timeoutSpinner.setValue(settings.getTimeoutSeconds());
        maxDiffCharsSpinner.setValue(settings.getMaxDiffChars());
        languageComboBox.setSelectedItem(settings.getLanguage());
        templatePresetComboBox.setSelectedItem(settings.getMessageTemplatePreset());
        messageTemplateArea.setText(settings.getMessageTemplate());
        templateVariablesArea.setText(settings.getTemplateVariables());
    }

    @Override
    public void disposeUIResources() {
        disposed = true;
        if (apiKeyField != null) {
            Arrays.fill(apiKeyField.getPassword(), '\0');
        }
    }

    private JPanel createModelSelector() {
        JPanel modelPanel = new JPanel(new BorderLayout(6, 0));
        modelPanel.add(modelComboBox, BorderLayout.CENTER);
        modelPanel.add(fetchModelsButton, BorderLayout.EAST);
        return modelPanel;
    }

    private JComponent createTemplateHelp() {
        JTextArea help = new JTextArea(
                "模板配置说明：\n"
                        + "1. Message Template 是最终 commit message 的格式模板，可以直接自定义。\n"
                        + "2. 支持占位符：${title} 表示 AI 生成的概要；${items.bullets} 表示 - 列表；${items.numbered} 表示 1.xxx 编号列表。\n"
                        + "3. Template Variables 使用 key=value 配置固定字段，模板中可用 ${key} 引用；未配置的变量会输出为空。\n"
                        + "4. 公司格式示例：\n"
                        + "   [修改单编号]${changeId}\n"
                        + "   [缺陷编号]${bugId}\n"
                        + "   [修改说明]${description}\n"
                        + "   ${title}\n"
                        + "   ${items.numbered}\n"
                        + "5. Template Variables 示例：\n"
                        + "   changeId=T202604205176-1\n"
                        + "   bugId=\n"
                        + "   description=江苏信托iOS");
        help.setEditable(false);
        help.setOpaque(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setFocusable(false);
        return help;
    }

    private void applySelectedTemplatePreset() {
        if (messageTemplateArea == null) {
            return;
        }
        String preset = getTemplatePreset();
        if (AiCommitSettings.PRESET_DEFAULT.equals(preset)) {
            messageTemplateArea.setText(AiCommitSettings.DEFAULT_MESSAGE_TEMPLATE);
        } else if (AiCommitSettings.PRESET_COMPANY_NUMBERED.equals(preset)) {
            messageTemplateArea.setText(AiCommitSettings.COMPANY_NUMBERED_TEMPLATE);
        }
    }

    private void fetchModels() {
        String baseUrl = normalizeBaseUrl(baseUrlField.getText());
        String apiKey = new String(apiKeyField.getPassword()).trim();
        int timeoutSeconds = (Integer) timeoutSpinner.getValue();

        if (baseUrl.isEmpty()) {
            Messages.showWarningDialog(panel, "Base URL cannot be empty.", "AI Commit Helper");
            return;
        }
        if (apiKey.isEmpty()) {
            Messages.showWarningDialog(panel, "API Key cannot be empty.", "AI Commit Helper");
            return;
        }

        fetchModelsButton.setEnabled(false);
        fetchModelsButton.setText("Fetching...");
        focusModelEditor();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<String> models = new DeepSeekClient().listModels(baseUrl, apiKey, timeoutSeconds);
                showModelsResult(models, null);
            } catch (IOException | RuntimeException exception) {
                showModelsResult(null, exception);
            }
        });
    }

    private void testConnection() {
        String baseUrl = normalizeBaseUrl(baseUrlField.getText());
        String model = getModelText();
        String apiKey = new String(apiKeyField.getPassword()).trim();
        int timeoutSeconds = (Integer) timeoutSpinner.getValue();

        if (baseUrl.isEmpty()) {
            Messages.showWarningDialog(panel, "Base URL cannot be empty.", "AI Commit Helper");
            return;
        }
        if (model.isEmpty()) {
            Messages.showWarningDialog(panel, "Model cannot be empty.", "AI Commit Helper");
            return;
        }
        if (apiKey.isEmpty()) {
            Messages.showWarningDialog(panel, "API Key cannot be empty.", "AI Commit Helper");
            return;
        }

        testConnectionButton.setEnabled(false);
        testConnectionButton.setText("Testing...");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                new DeepSeekClient().testConnection(baseUrl, model, apiKey, timeoutSeconds);
                showTestResult(null);
            } catch (IOException | RuntimeException exception) {
                showTestResult(exception);
            }
        });
    }

    private void showModelsResult(List<String> models, Exception exception) {
        SwingUtilities.invokeLater(() -> {
            if (disposed || fetchModelsButton == null) {
                return;
            }
            fetchModelsButton.setEnabled(true);
            fetchModelsButton.setText("Fetch Models");
            if (exception != null) {
                Messages.showErrorDialog(panel,
                        "Failed to fetch models: " + exception.getMessage(),
                        "AI Commit Helper");
                return;
            }
            setModelOptions(getModelText(), models);
            if (models == null || models.isEmpty()) {
                Messages.showWarningDialog(panel, "No models were returned by the API.", "AI Commit Helper");
            } else {
                Messages.showInfoMessage(panel, "Fetched " + models.size() + " models.", "AI Commit Helper");
            }
        });
    }

    private void showTestResult(Exception exception) {
        SwingUtilities.invokeLater(() -> {
            if (disposed || testConnectionButton == null) {
                return;
            }
            testConnectionButton.setEnabled(true);
            testConnectionButton.setText("Test Connection");
            if (exception == null) {
                Messages.showInfoMessage(panel, "Connection test succeeded.", "AI Commit Helper");
            } else {
                Messages.showErrorDialog(panel,
                        "Connection test failed: " + exception.getMessage(),
                        "AI Commit Helper");
            }
        });
    }

    private String getModelText() {
        Object selected = modelComboBox.getEditor().getItem();
        return selected == null ? "" : selected.toString().trim();
    }

    private String getTemplatePreset() {
        Object selected = templatePresetComboBox.getSelectedItem();
        return selected == null ? AiCommitSettings.PRESET_DEFAULT : selected.toString();
    }

    private void setModelOptions(String selectedModel, List<String> models) {
        String selected = selectedModel == null ? "" : selectedModel.trim();
        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>();
        if (!selected.isEmpty()) {
            comboModel.addElement(selected);
        }
        if (models != null) {
            for (String model : models) {
                if (model != null && !model.trim().isEmpty() && comboModel.getIndexOf(model.trim()) < 0) {
                    comboModel.addElement(model.trim());
                }
            }
        }
        modelComboBox.setModel(comboModel);
        modelComboBox.setEditable(true);
        modelComboBox.setSelectedItem(selected);
    }

    private void focusModelEditor() {
        SwingUtilities.invokeLater(() -> {
            if (disposed || modelComboBox == null) {
                return;
            }
            JComponent editor = (JComponent) modelComboBox.getEditor().getEditorComponent();
            editor.requestFocusInWindow();
        });
    }

    private static String normalizeBaseUrl(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
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
