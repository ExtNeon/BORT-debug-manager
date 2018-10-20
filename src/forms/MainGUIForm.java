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
    public final static byte CHOSEN_DIAGRAMM_TYPE_VOLTAGE = 0;
    public final static byte CHOSEN_DIAGRAMM_TYPE_RPM = 1;
    public final static byte CHOSEN_DIAGRAMM_TYPE_TEMPERATURE = 2;
    public final static byte CHOSEN_DIAGRAMM_TYPE_FUEL_LEVEL = 3;
    private final static int MAX_DEBUG_CONSOLE_TEXT_LENGTH = 30000;
    private final static byte CONST_TAB_PARAMSLIST_INDEX = 1;
    private final static byte CONST_TAB_DIAGRAMMS_INDEX = 4;

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
    private JPanel DiagramPanel;
    private JSlider graphSizeSlider;
    private JComboBox<String> selectGraphComboBox;
    private JButton clearDiagramButton;
    private JTextField selectedSettingValueTextField;
    private JList<String> settingsListBox;
    private JLabel selectedSettingCaption;
    private INISettingsSection paramsList = null;

    private boolean lastStatusWasErroneous = false;
    private long lastStatusUpdateTime = 0;

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
        clearDiagramButton.addActionListener(listener);
        parametersListBox.addListSelectionListener(this);
        tabbedPane1.addChangeListener(this);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        selectedParameterComboBox.addActionListener(this);

        selectGraphComboBox.addItem("Voltage"); //0
        selectGraphComboBox.addItem("RPM"); //1
        selectGraphComboBox.addItem("Temperature"); //2
        selectGraphComboBox.addItem("Fuel level"); //3


        DefaultListModel<String> newModel = new DefaultListModel<>();
        newModel.addElement("Таймаут ожидания ответа от модуля");
        newModel.addElement("Количество последовательных ошибок соединения для его переподключения");
        newModel.addElement("Время удержания статуса с ошибкой");
        newModel.addElement("Время удержания статуса с информацией");
        settingsListBox.setModel(newModel);
        selectedSettingCaption.setText("Выберите параметр для отображения информации о нём");
    }

    public void updateStatus(String newStatus) {
        statusLabel.setText(newStatus);
        statusLabel.setForeground(new Color(0, 0, 0));
        lastStatusWasErroneous = false;
        lastStatusUpdateTime = System.currentTimeMillis();
        if (newStatus.length() > 0) {
            logConsoleTextArea.append(newStatus + '\n');
            logConsoleTextArea.select(logConsoleTextArea.getText().length() - newStatus.length(), logConsoleTextArea.getText().length());
            logConsoleTextArea.setSelectedTextColor(new Color(0, 0, 0));
            logConsoleTextArea.select(logConsoleTextArea.getText().length(), logConsoleTextArea.getText().length());
        }
    }

    public void updateErrorStatus(String newStatus) {
        lastStatusWasErroneous = true;
        lastStatusUpdateTime = System.currentTimeMillis();
        statusLabel.setText(newStatus);
        statusLabel.setForeground(new Color(202, 0, 0));
        if (newStatus.length() > 0) {
            logConsoleTextArea.append(newStatus + '\n');
            logConsoleTextArea.select(logConsoleTextArea.getText().length() - newStatus.length(), logConsoleTextArea.getText().length());
            logConsoleTextArea.setSelectedTextColor(new Color(202, 0, 0));
            logConsoleTextArea.select(logConsoleTextArea.getText().length(), logConsoleTextArea.getText().length());
        }
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

    public JPanel getDiagramPanel() {
        return DiagramPanel;
    }

    public JSlider getGraphSizeSlider() {
        return graphSizeSlider;
    }

    public JComboBox<String> getSelectGraphComboBox() {
        return selectGraphComboBox;
    }

    public boolean isLastStatusWasErroneous() {
        return lastStatusWasErroneous;
    }

    public long getLastStatusUpdateTime() {
        return lastStatusUpdateTime;
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
        switch (tabbedPane1.getSelectedIndex()) {
            case CONST_TAB_PARAMSLIST_INDEX:
                listener.actionPerformed(new ActionEvent(tabbedPane1, 23, "update paramslist"));
                break;
            case CONST_TAB_DIAGRAMMS_INDEX:
                listener.actionPerformed(new ActionEvent(tabbedPane1, 24, "refresh diagram"));
                break;
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
