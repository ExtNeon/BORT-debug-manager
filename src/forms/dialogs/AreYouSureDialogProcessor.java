package forms.dialogs;

import com.sun.jnlp.ApiDialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Диалоговое окно с текстом и двумя кнопками (OK, CANCEL). Является, по логике действия, модальным окном.
 */

public class AreYouSureDialogProcessor implements ActionListener {

    private final AreYouSureDialog dialog = new AreYouSureDialog();
    private ApiDialog.DialogResult result = null;

    /**
     * Создаёт окно с текстом dialogText и двумя кнопками - OK и CANCEL. При этом, ждёт ответа от пользователя.
     * Возвращает ту кнопку, которую он нажал в виде объекта DialogResult.
     *
     * @param dialogText Текст, который будет выведен в диалоге.
     * @return ответ пользователя в виде объекта DialogResult.
     */
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
