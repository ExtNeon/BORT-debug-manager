package forms.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Диалоговое окно с текстом и двумя кнопками.
 */
class AreYouSureDialog extends JFrame {

    private JPanel panel1;
    private JButton okButton;
    private JButton cancelButton;
    private JTextPane dialogText;

    public AreYouSureDialog() {
        this.getContentPane().add(panel1);
        this.pack();
        this.setSize(new Dimension(300, 150));
    }

    public void setActionListener(ActionListener listener) {
        okButton.addActionListener(listener);
        cancelButton.addActionListener(listener);
    }

    public JTextPane getDialogText() {
        return dialogText;
    }
}
