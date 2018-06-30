package forms;

import iniSettings.INISettingsRecord;
import iniSettings.INISettingsSection;
import iniSettings.exceptions.NotFoundException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Кирилл on 30.06.2018.
 */
public class MainGUIForm extends JFrame implements ChangeListener, ActionListener{
    private final ActionListener listener;
    private JLabel MainCaption;
    private JLabel statusLabel;
    private JButton RestartModuleButton;
    private JTabbedPane tabbedPane1;
    private JPanel MainPanel;
    private JButton importSettingsButton;
    private JButton exportSettingsButton;
    private JTextPane MainInfoTextPanel;
    private JTextPane parametersListPane;
    private JComboBox<String> selectedParameterComboBox;
    private JTextArea logConsoleTextArea;
    private JTextField selectedParamNewValueField;
    private JButton setNewParamValueButton;
    private JButton saveSettingsToMemoryButton;
    private JButton resetSettingButton;
    private JProgressBar progressBar;
    private INISettingsSection paramsList = null;

    public MainGUIForm(Dimension size, ActionListener listener) {
        this.getContentPane().add(MainPanel);
        this.pack();
        this.setSize(size);
        this.setVisible(true);
        this.listener = listener;
        RestartModuleButton.addActionListener(listener);
        setNewParamValueButton.addActionListener(listener);
        saveSettingsToMemoryButton.addActionListener(listener);
        resetSettingButton.addActionListener(listener);
        importSettingsButton.addActionListener(listener);
        exportSettingsButton.addActionListener(listener);
        tabbedPane1.addChangeListener(this);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        selectedParameterComboBox.addActionListener(this);
    }

    public void updateStatus(String newStatus) {
        statusLabel.setText(newStatus);
        statusLabel.setForeground(new Color(0,0,0));

        logConsoleTextArea.append(newStatus + '\n');
        logConsoleTextArea.select(logConsoleTextArea.getText().length() - newStatus.length(), logConsoleTextArea.getText().length());
        logConsoleTextArea.setSelectedTextColor(new Color(0,0,0));
        logConsoleTextArea.select(logConsoleTextArea.getText().length(),logConsoleTextArea.getText().length());
    }

    public void updateErrorStatus(String newStatus) {
        statusLabel.setText(newStatus);
        statusLabel.setForeground(new Color(202, 0,0));
        logConsoleTextArea.append(newStatus + '\n');
        logConsoleTextArea.select(logConsoleTextArea.getText().length() - newStatus.length(), logConsoleTextArea.getText().length());
        logConsoleTextArea.setSelectedTextColor(new Color(202, 0, 0));
        logConsoleTextArea.select(logConsoleTextArea.getText().length(),logConsoleTextArea.getText().length());
    }

    public void updateStatistics(String newStatistics) {
        MainInfoTextPanel.setText(newStatistics);
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public void setParamsList(INISettingsSection paramsList) {
        this.paramsList = paramsList;
        parametersListPane.setEnabled(true);
        selectedParameterComboBox.setEnabled(true);
        parametersListPane.setText(paramsList.toString());
        selectedParameterComboBox.removeAllItems();
        for (INISettingsRecord currentRecord : paramsList.getRecords()) {
            selectedParameterComboBox.addItem(currentRecord.getKey());
        }
    }

    public JComboBox<String> getSelectedParameterComboBox() {
        return selectedParameterComboBox;
    }

    public JTextPane getParametersListPane() {
        return parametersListPane;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here

    }

    public JTextField getSelectedParamNewValueField() {
        return selectedParamNewValueField;
    }

    /**
     * Invoked when the target of the listener has changed its state.
     *
     * @param e a ChangeEvent object
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        if (tabbedPane1.getSelectedIndex() == 1) {
            listener.actionPerformed(new ActionEvent(tabbedPane1, 23, "update paramslist"));
        }
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == selectedParameterComboBox) {
            try {
                selectedParamNewValueField.setText(paramsList.getFieldByKey((String)selectedParameterComboBox.getSelectedItem()).getValue());
            } catch (NotFoundException ignored) {
                //updateErrorStatus("Не удалось найти параметр с таким именем");
            }
        }
    }
}
