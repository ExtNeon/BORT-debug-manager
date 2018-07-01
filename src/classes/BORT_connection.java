package classes;

import exceptions.InterpretationException;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Данный класс представляет собой обработчик модуля инфракрасного приёмника для ИК - пультов.
 * Модуль инфракрасного приёмника представляет собой плату Arduino Nano c ИК - детектором и миниатюрным динамиком.
 * Микроконтроллер передаёт данные через UART (последовательный порт TTL - уровня), а они, в свою очередь, принимаются
 * установленной на плате микросхемой - конвертором, которая подключается к компьютеру с помощью USB.
 * В конечном итоге, в системе данные можно принять из виртуального эмулированного COM - порта.
 * Данный класс использует библиотеку JSSC для работы с последовательным портом. Это библиотека с открытым исходным кодом,
 * позволяющая организовать многопоточную асинхронную работу с COM - портами.
 * Класс принимает данные с модуля, позволяет подключить обработчики событий нажатия на кнопки, принимать одиночные нажатия,
 * чтобы организовать, например, ввод определённх ожидаемых клавиш.
 * Содержит методы, позволяющие полноценно работать с модулем, а также реализует интерфейс Closeable, поэтому его можно
 * использовать в конструкциях try с ресурсами.
 */
public class BORT_connection implements SerialPortEventListener, Closeable {

    // --Commented out by Inspection (01.07.2018 0:21):private boolean responsesStackUpdated = false;
    private final ArrayList<BORT_response> responsesStack = new ArrayList<>();
    private final SerialPort serialPort;
    private final StringBuilder receivingBuf = new StringBuilder();
    /*
    ********Стандартные мелодии********
     */
    private boolean isConnected = false;

    /**
     * Конструктор. Открывает COM - порт на скорости 115200 бод, подключается и ждёт ответа от устройства.
     *
     * @param portName                    Название COM - порта, через который будет осуществлено подключение
     */
    public BORT_connection(String portName) {
        serialPort = new SerialPort(portName);
        try {
            serialPort.openPort();
            serialPort.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
            serialPort.addEventListener(this);
        } catch (SerialPortException ignored) {

        }
    }

    /**
     * Метод конвертирует массив байтов в строку, представляя каждый байт, как символ.
     * @param buf входной массив байтов, который необходимо конвертировать
     * @return Строка, состоящая из символов, которые представляют каждый отдельный элемент.
     */
    private static String convertByteArrayToANSIStr(byte[] buf) {
        try {
            return new String(buf, "cp1251");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Это метод, который вызывается обработчиком соединения com - порта.
     */
    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR() && event.getEventValue() > 1) {
            try {
                String receivedStr = convertByteArrayToANSIStr(serialPort.readBytes(event.getEventValue()));
               /* if (receivedStr.contains("\n")) {
                    receivedStr = receivedStr.substring(0, receivedStr.indexOf('\n'));
                }*/
               for (int i = 0; i < receivedStr.length(); i++) {
                   if (receivedStr.charAt(i) == '\r') {
                       String gettedStr = receivingBuf.toString();
                       receivingBuf.setLength(0);
                       processReceivedStr(gettedStr);
                   } else {
                       if (receivingBuf.length() > 32768) {
                           receivingBuf.setLength(0);
                       }
                       if (receivedStr.charAt(i) != '\n')
                       receivingBuf.append(receivedStr.charAt(i));
                   }

               }
            } catch (SerialPortException ignored) {
            } catch (InterpretationException e) {
                System.err.println("Непредвиденная ошибка приёма данных: " + e);
            }
        }
    }

    private void processReceivedStr(String receivedStr) throws InterpretationException {
        receivedStr = receivedStr.trim();
        if (receivedStr.equals("!101:")) {
            isConnected = true;
        }
        responsesStack.add(new BORT_response(receivedStr));
        //responsesStackUpdated = true;
    }

    public ArrayList<BORT_response> getResponsesStack() {
        return responsesStack;
    }

// --Commented out by Inspection START (01.07.2018 0:18):
//    public boolean isResponsesStackUpdated(boolean resetFlag) {
//        boolean flag_copy = responsesStackUpdated;
//        if (resetFlag) {
//            responsesStackUpdated = false;
//        }
//        return flag_copy;
//    }
// --Commented out by Inspection STOP (01.07.2018 0:18)

    public void clearResponsesStack() {
        responsesStack.clear();
        //responsesStackUpdated = false;
    }

    /**
     * @return true, если модуль идентефицирован и подключён, false во всех остальных случаях.
     */
    public boolean isConnected() {
        return isConnected;
    }

    public void send(String str) {
        try {
            serialPort.writeString(str + '\n');
        } catch (SerialPortException e) {
            System.err.println("Ошибка отправки информации: " + e.getMessage());
        }
    }

    /**
     * @return название COM - порта, через который работает подключение.
     */
    public SerialPort getSerialPort() {
        return serialPort;
    }

    @Override
    public void close() {
        try {
            serialPort.closePort();
        } catch (SerialPortException ignored) {}
    }
}
