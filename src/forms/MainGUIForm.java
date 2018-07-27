package forms;

import iniSettings.INISettingsRecord;
import iniSettings.INISettingsSection;
import iniSettings.exceptions.NotFoundException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Основное окно программы.
 */
public class MainGUIForm extends JFrame implements ChangeListener, ActionListener, ListSelectionListener {
    private final static int MAX_DEBUG_CONSOLE_TEXT_LENGTH = 30000;
    private final ActionListener listener;
    private JLabel MainCaption;
    private JLabel statusLabel;
    private JButton RestartModuleButton;
    private JTabbedPane tabbedPane1;
    private JPanel MainPanel;
    private JButton importSettingsButton;
    private JButton exportSettingsButton;
    private JTextPane MainInfoTextPanel;
    private JComboBox<String> selectedParameterComboBox;
    private JTextArea logConsoleTextArea;
    private JTextField selectedParamNewValueField;
    private JButton setNewParamValueButton;
    private JButton saveSettingsToMemoryButton;
    private JButton resetSettingButton;
    private JProgressBar progressBar;
    private JCheckBox holdConnectionCheckBox;
    private JTextArea debugConsoleTextArea;
    private JLabel debugStatusLabel;
    private JButton refreshSettingsButton;
    private JList<String> parametersListBox;
    private INISettingsSection paramsList = null;

    private boolean lastStatusErrorneus = false;

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
        holdConnectionCheckBox.addActionListener(listener);
        refreshSettingsButton.addActionListener(listener);
        parametersListBox.addListSelectionListener(this);
        tabbedPane1.addChangeListener(this);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        selectedParameterComboBox.addActionListener(this);
    }

    public void updateStatus(String newStatus) {
        statusLabel.setText(newStatus);
        statusLabel.setForeground(new Color(0, 0, 0));
        lastStatusErrorneus = false;
        logConsoleTextArea.append(newStatus + '\n');
        logConsoleTextArea.select(logConsoleTextArea.getText().length() - newStatus.length(), logConsoleTextArea.getText().length());
        logConsoleTextArea.setSelectedTextColor(new Color(0, 0, 0));
        logConsoleTextArea.select(logConsoleTextArea.getText().length(), logConsoleTextArea.getText().length());
    }

    public void updateErrorStatus(String newStatus) {
        lastStatusErrorneus = true;
        statusLabel.setText(newStatus);
        statusLabel.setForeground(new Color(202, 0, 0));
        logConsoleTextArea.append(newStatus + '\n');
        logConsoleTextArea.select(logConsoleTextArea.getText().length() - newStatus.length(), logConsoleTextArea.getText().length());
        logConsoleTextArea.setSelectedTextColor(new Color(202, 0, 0));
        logConsoleTextArea.select(logConsoleTextArea.getText().length(), logConsoleTextArea.getText().length());
    }

    public JCheckBox getHoldConnectionCheckBox() {
        return holdConnectionCheckBox;
    }

    public void updateStatistics(String newStatistics) {
        MainInfoTextPanel.setText(newStatistics);
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public boolean isLastStatusErrorneus() {
        return lastStatusErrorneus;
    }

    public void updateDebugStatusBar(String info) {
        if (debugConsoleTextArea.getText().length() > MAX_DEBUG_CONSOLE_TEXT_LENGTH) {
            debugConsoleTextArea.setText("CLEARED");
        }
        debugConsoleTextArea.append(info + "\n");
        debugStatusLabel.setText(info);
    }

    public void setParamsList(INISettingsSection paramsList) {
        this.paramsList = paramsList;
        parametersListBox.setEnabled(true);
        selectedParameterComboBox.setEnabled(true);
        DefaultListModel<String> newModel = new DefaultListModel<>();
        selectedParameterComboBox.removeAllItems();
        int counter = 1;
        for (INISettingsRecord currentRecord : paramsList.getRecords()) {
            newModel.addElement(counter++ + ". " + currentRecord.getKey() + " = " + currentRecord.getValue());
            selectedParameterComboBox.addItem(currentRecord.getKey());
        }
        parametersListBox.setModel(newModel);
    }

    public JComboBox<String> getSelectedParameterComboBox() {
        return selectedParameterComboBox;
    }

    public JList<String> getParametersListBox() {
        return parametersListBox;
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
                selectedParamNewValueField.setText(paramsList.getFieldByKey((String) selectedParameterComboBox.getSelectedItem()).getValue());
            } catch (NotFoundException ignored) {
                //updateErrorStatus("Не удалось найти параметр с таким именем");
            }
        }
    }

    /**
     * Called whenever the value of the selection changes.
     *
     * @param e the event that characterizes the change.
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        selectedParameterComboBox.setSelectedIndex(parametersListBox.getSelectedIndex());
    }
}
