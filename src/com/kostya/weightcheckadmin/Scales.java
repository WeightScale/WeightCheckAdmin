//Класс весов
package com.kostya.weightcheckadmin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import com.kostya.bootloader.AVRProgrammer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

public class Scales {
    private static BluetoothDevice device;                  //чужое устройство
    private static final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static BluetoothSocket socket;                  //соединение
    private static OutputStream outputStream;             //поток отправки
    private static OutputStreamWriter outputStreamWriter;
    private static InputStream is;                          //поток получения

    static ScaleInterface vClass;                    //Интерфейс для разных версий весов

    static String version;                                  // версия весов
    //static String data;                                     // данные весов
    static float coefficientA;                              // калибровочный коэффициент a
    static float coefficientTemp;                          // калибровочный коэффициент температуры
    static float coefficientB;                              // калибровочный коэффициент b
    public static int sensorTenzo;                                // показание датчика веса
    static int sensorTenzoOffset;                         // показание датчика веса минус offset
    static int offset;                                      // offset
    //static int sensor_temp;                                 // показание датчика температуры
    protected static int battery;                                     // процент батареи (0-100%)
    protected static int temp;                                        // темрература
    protected static int filter;                                      // АЦП-фильтр (0-15)
    protected static int timer;                                       // таймер выключения весов
    protected static int speed;                                       // скорость передачи данных
    protected static int step;                                        // шаг измерения (округление)
    protected static int autoCapture;                                 // шаг захвата (округление)
    protected static int weight;                                      // реальный вес
    protected static int weightMax;                                   // максимальный вес
    protected static int limitTenzo;                                  // максимальное показание датчика
    protected static int marginTenzo;                                 // предельное показани датчика
    protected static int weightMargin;                                // предельный вес
    protected static int weightError;                                 // ошибка авто нуля
    protected static int timerNull;                                   // время срабатывания авто нуля

    static final int COUNT_STABLE = 16;                            //колличество раз стабильно был вес
    static final int DIVIDER_AUTO_NULL = 3;                         //делитель для авто ноль

    private static int tempWeight;
    static int numStable;
    static boolean flagStable;
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    static String spreadsheet = "Весовой Чек";
    static String username = "kreogen.lg@gmail.com";
    static String password = "htcehc25";

    static boolean stable(int weight) {
        if (tempWeight - step <= weight && tempWeight + step >= weight) {
            if (++numStable >= COUNT_STABLE)
                return true;
        } else
            numStable = 0;
        tempWeight = weight;
        return false;
    }

    static synchronized boolean connect(BluetoothDevice bd) { //соединиться с весами
        disconnect();
        device = bd;
        BluetoothSocket bluetoothSocket = null;
        // Get a BluetoothSocket for a connection with the given BluetoothDevice
        try {
            bluetoothSocket = bd.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException ignored) {
            //
        }
        socket = bluetoothSocket;
        //if(BluetoothAdapter.getDefaultAdapter().isDiscovering())
        if (!bluetoothAdapter.isEnabled())
            return false;
        bluetoothAdapter.cancelDiscovery();


        try {
            socket.connect();
            is = socket.getInputStream();
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            disconnect();
            return false;
        }
        return true;
    }

    public static void disconnect() { //рассоединиться
        try {
            if (socket != null)
                socket.close();
            if (is != null)
                is.close();
            if (outputStreamWriter != null)
                outputStreamWriter.close();
            if (outputStream != null)
                outputStream.close();
        } catch (IOException ioe) {
            socket = null;
            //return;
        }
        is = null;
        outputStreamWriter = null;
        outputStream = null;
        socket = null;
    }

    static synchronized String command(String cmd) { //послать команду и получить ответ
        if (outputStreamWriter != null)
            try {
                int t = is.available();
                if (t > 0)
                    is.read(new byte[t]);
                outputStreamWriter.write(cmd);
                outputStreamWriter.write(ScaleInterface.CR_LF);
                outputStreamWriter.flush();
                String cmd_rtn = ""; //возвращённая команда
                for (int i = 0; i < 450 && cmd_rtn.length() < 129; i++) {
                    Thread.sleep(1);
                    if (is.available() > 0) {
                        i = 0;
                        char ch = (char) is.read(); //временный символ (байт)
                        if (ch == 0xffff) {
                            connect(device);
                            break;
                        }
                        if (ch == '\r')
                            continue;
                        if (ch == '\n')
                            if (cmd_rtn.startsWith(cmd.substring(0, 3)))
                                if (cmd_rtn.replace(cmd.substring(0, 3), "").isEmpty())
                                    return cmd.substring(0, 3);
                                else
                                    return cmd_rtn.replace(cmd.substring(0, 3), "");
                            else
                                return "";
                        cmd_rtn += ch;
                    }
                }
            } catch (IOException ioe) {
                connect(device);
            } catch (InterruptedException iex) {
            }
        else
            connect(device);
        return "";
    }

    static boolean isScales() { //Является ли весами и какой версии
        String vrs = command(ScaleInterface.CMD_VERSION);
        if (vrs.startsWith(ActivitySearch.versionName)) {
            version = vrs.replace(ActivitySearch.versionName, "");
            if (!isInteger(version))
                return false;
            if (Integer.valueOf(version) <= ActivitySearch.versionNumber) {
                switch (Integer.valueOf(version)) {
                    case 1:
                    case 2:
                        vClass = new Version1();
                        break;
                    case 3:
                        vClass = new Version2();
                        break;
                    case 4:
                        vClass = new Version4();
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    static String getName() {
        return device.getName();
    }

    static String getAddress() {
        return device.getAddress();
    }

    static int readBattery() {
        String str = command(ScaleInterface.CMD_BATTERY);
        if (str.isEmpty())
            battery = 0;
        else
            battery = Integer.valueOf(str);
        return battery;
    }

    public static int getTemp() { //Получить температуру
        String str = command(ScaleInterface.CMD_DATA_TEMP);
        if (str.isEmpty())
            return -273;
        return (int) ((float) ((Integer.valueOf(str) - 0x800000) / 7169) / 0.81) - 273;
    }

    static boolean isInteger(String str) {
        try {
            Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static boolean isFloat(String str) {
        try {
            Float.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    //==============================================================================
    public static synchronized boolean sendByte(byte ch) {
        if (outputStream != null)
            try {
                int t = is.available();
                if (t > 0)
                    is.read(new byte[t]);
                outputStream.write(ch);
                outputStream.flush(); //что этот метод делает?
                return true;
            } catch (IOException ioe) {
                connect(device);
            }
        else
            connect(device);
        return false;
    }

    public static synchronized int getByte() {
        if (is != null)
            try {
                for (int i = 0; i < 2000; i++) {
                    if (is.available() > 0) {
                        return is.read(); //временный символ (байт)
                    }
                    Thread.sleep(1);
                }
                return 0;
            } catch (IOException ioe) {
                connect(device);
            } catch (InterruptedException iex) {
                connect(device);
            }
        else
            connect(device);
        return 0;
    }

    public static boolean isProgrammerId() { //Является ли весами
        //String str = AVRProgrammer.readProgrammerID();
        return AVRProgrammer.readProgrammerID().equals("AVRBOOT");
    }
}
