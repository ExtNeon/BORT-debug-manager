package forms.dialogs;

import com.sun.jnlp.ApiDialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Кирилл on 30.06.2018.
 */

public class AreYouSureDialogProcessor implements ActionListener {

    private final AreYouSureDialog dialog = new AreYouSureDialog();
    private ApiDialog.DialogResult result = null;

    public ApiDialog.DialogResult showDialog(String dialogText) {
        dialog.setVisible(true);
        dialog.getDialogText().setText(dialogText);
        dialog.setActionListener(this);
        while (result == null && dialog.isShowing()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        dialog.dispose();
        return result;
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().toLowerCase().equals("ok")) {
            result = ApiDialog.DialogResult.OK;
        } else {
            result = ApiDialog.DialogResult.CANCEL;
        }

    }
}
