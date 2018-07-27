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
 * Данный класс позволяет представить соединение с модулем бортового компьютера в виде объекта.
 * Соединение - это виртуальный COM - порт, через который программма обменивается командами и данными с модулем.
 * Микроконтроллер передаёт данные через UART (последовательный порт TTL - уровня), а они, в свою очередь, принимаются
 * установленной на плате микросхемой - конвертором, которая подключается к компьютеру с помощью USB.
 * В конечном итоге, в системе данные можно принять из виртуального эмулированного COM - порта.
 * Данный класс использует библиотеку JSSC для работы с последовательным портом. Это библиотека с открытым исходным кодом,
 * позволяющая организовать многопоточную асинхронную работу с COM - портами.
 * Класс принимает данные с модуля и складывает их в стек полученных ответов, из которого они могут быть доступны в дальнейшем.
 * Содержит методы, позволяющие полноценно работать с модулем, а также реализует интерфейс Closeable, поэтому его можно
 * использовать в конструкциях try с ресурсами.
 */
public class BORT_connection implements SerialPortEventListener, Closeable {

    private final SerialPort serialPort;
    private final StringBuilder receivingBuf = new StringBuilder();
    private final DebugInformationListener debugInformationListener;
    @SuppressWarnings("CanBeFinal")
    private volatile ArrayList<BORT_response> responsesStack = new ArrayList<>();
    private boolean isConnected = false;

    /**
     * Конструктор. Открывает COM - порт на скорости 115200 бод, подключается и ждёт ответа от устройства.
     *
     * @param portName Название COM - порта, через который будет осуществлено подключение
     */
    public BORT_connection(String portName, DebugInformationListener debugInformationListener) {
        this.debugInformationListener = debugInformationListener;
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
     *
     * @param buf входной массив байтов, который необходимо конвертировать
     * @return Строка, состоящая из символов, которые представляют каждый отдельный элемент.
     */
    private static String convertByteArrayToANSIStr(byte[] buf) {
        try {
            return new String(buf, "cp1251");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "error";
    }

    /**
     * Это метод, который вызывается обработчиком соединения com - порта.
     */
    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR() && event.getEventValue() > 1) {
            try {
                String receivedStr = convertByteArrayToANSIStr(serialPort.readBytes(event.getEventValue()));
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
        BORT_response gettedResponse = new BORT_response(receivedStr.trim());
        switch (gettedResponse.getResponseType()) {
            case MODULE_STARTED:
                isConnected = true;
                send(CommandsConstList.CMD_CONNECTION_APPROVED);
                break;
            case DEBUG_INFORMATION:
                debugInformationListener.debugInformationGetted(gettedResponse);
                break;
        }
        responsesStack.add(gettedResponse);
    }

    public ArrayList<BORT_response> getResponsesStack() {
        return responsesStack;
    }

    public void clearResponsesStack() {
        responsesStack.clear();
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
        } catch (SerialPortException ignored) {
        }
    }
}
