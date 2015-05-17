package com.kostya.weightcheckadmin;

import java.util.Iterator;

/*
 * Created by Kostya on 30.11.2014.
 */
public class Version4 extends Scales implements ScaleInterface {

    private static final String CMD_GET_OFFSET = "GCO";

    private static final String CMD_DATA_CFA = "cfa";               //коэфициэнт А
    private static final String CMD_DATA_CFB = "cfb";               //коэфициэнт Б
    private static final String CMD_DATA_WGM = "wgm";               //вес максимальный
    private static final String CMD_DATA_LMT = "lmt";               //лимит тензодатчика

    @Override
    public boolean load() throws Exception { //загрузить данные
        //======================================================================
        String str = command(CMD_FILTER); //временная строка
        if (str.isEmpty() || !isInteger(str))
            throw new Exception("Фильтер АЦП не установлен в настройках");
        filter = Integer.valueOf(str);
        if (filter < 0 || filter > 15) {
            command(CMD_FILTER + 8);
            filter = 8;
        }
        //======================================================================
        str = command(CMD_TIMER);
        if (!isInteger(str))
            throw new Exception("Таймер выключения не установлен в настройках");
        timer = Integer.valueOf(str);
        if (timer < 10 || timer > 60) {
            command(CMD_TIMER + 10);
            timer = 10;
        }
        //======================================================================
        str = command(CMD_SPEED);
        if (!isInteger(str))
            throw new Exception("Непраильное значение скорости подключения");
        //======================================================================
        str = command(CMD_GET_OFFSET);
        if (!isInteger(str))
            throw new Exception("Сделать обнуление в настройках");
        offset = Integer.parseInt(str);
        //======================================================================
        str = command(CMD_CALL_TEMP);
        if (!isFloat(str))
            throw new Exception("Неправильная константа каллибровки температуры");
        coefficientTemp = Float.valueOf(str);
        //======================================================================
        str = command(CMD_SPREADSHEET);
        if (str.isEmpty())
            throw new Exception("Неправильное имя таблици google диск");
        spreadsheet = str;
        //======================================================================
        str = command(CMD_G_USER);
        if (str.isEmpty())
            throw new Exception("Неправильное имя пользователя google диск");
        username = str;
        //======================================================================
        str = command(CMD_G_PASS);
        if (str.isEmpty())
            throw new Exception("Неправильный пароль пользователя google диск");
        password = str;
        //======================================================================
        str = Preferences.read(ActivityPreferences.KEY_STEP, "5");
        if (!isInteger(str))
            throw new Exception("Установите шаг измерения в настройках");
        step = Integer.valueOf(str);
        //======================================================================
        str = Preferences.read(ActivityPreferences.KEY_AUTO_CAPTURE, "20");
        if (!isInteger(str))
            throw new Exception("Установите автозахват в настройках");
        autoCapture = Integer.valueOf(str);
        //======================================================================
        timerNull = Preferences.read(ActivityPreferences.KEY_TIMER_NULL, 60);
        //======================================================================
        str = Preferences.read(ActivityPreferences.KEY_DAY_CHECK_DELETE, "5");
        if (!isInteger(str))
            throw new Exception("Установите через сколько удалять чеки в настройках");
        CheckDBAdapter.day = Integer.valueOf(str);
        //======================================================================
        str = Preferences.read(ActivityPreferences.KEY_DAY_CLOSED_CHECK, "5");
        if (!isInteger(str))
            throw new Exception("Установите через сколько закрывать не закрытые чеки");
        CheckDBAdapter.day_closed = Integer.valueOf(str);
        //======================================================================
        if (!isDataValid(command(CMD_DATA)))
            throw new Exception("Неправельные данные калибровки");
        weightError = Preferences.read(ActivityPreferences.KEY_MAX_NULL, 50);
        weightMargin = (int) (weightMax * 1.2);
        marginTenzo = (int) ((weightMax / coefficientA) * 1.2);
        return true;
    }

    @Override
    public synchronized int getWeightScale() {
        String str = command(CMD_SENSOR_OFFSET);
        if (str.isEmpty())
            sensorTenzoOffset = weight = Integer.MIN_VALUE;
        else {
            sensorTenzoOffset = Integer.valueOf(str);
            weight = (int) (coefficientA * sensorTenzoOffset);
            weight = weight / step * step;
        }
        return weight;
    }

    @Override
    public synchronized int getSensorScale() {
        String sensor = command(CMD_SENSOR);
        if (sensor.isEmpty())
            sensorTenzo = Integer.MIN_VALUE;
        else
            sensorTenzo = Integer.valueOf(sensor);
        return sensorTenzo;
    }

    synchronized int getOffsetScale() {
        String data = command(CMD_GET_OFFSET);
        if (data.isEmpty())
            offset = -1;
        else
            offset = Integer.valueOf(data);
        return offset;
    }

    @Override
    public synchronized boolean setOffsetScale() { //обнуление
        if (command(CMD_SET_OFFSET).equals(CMD_SET_OFFSET))
            if (getOffsetScale() != -1)
                return true;
        return false;
    }

    @Override
    public boolean writeDataScale() {
        return command(CMD_DATA +
                CMD_DATA_CFA + '=' + coefficientA + ' ' +
                CMD_DATA_WGM + '=' + weightMax + ' ' +
                CMD_DATA_LMT + '=' + limitTenzo).equals(CMD_DATA);
    }

    @Override
    public synchronized boolean isDataValid(String d) {
        String[] parts = d.split(" ", 0);
        SimpleCommandLineParser data = new SimpleCommandLineParser(parts, "=");
        Iterator<String> iteratorData = data.getKeyIterator();
        if (iteratorData == null)
            return false;
        while (iteratorData.hasNext()) {
            String cmd = iteratorData.next();
            if (cmd.equals(CMD_DATA_CFA)) {
                String value = data.getValue(CMD_DATA_CFA); //получаем коэфициент
                if (!isFloat(value))
                    return false;
                coefficientA = Float.valueOf(value);
            } else if (cmd.equals(CMD_DATA_CFB)) {
                String value = data.getValue(CMD_DATA_CFB); //получить offset
                if (!isFloat(value))
                    return false;
                coefficientB = Float.valueOf(value);
            } else if (cmd.equals(CMD_DATA_WGM)) {
                String value = data.getValue(CMD_DATA_WGM); //получаем макимальнай вес
                if (!isInteger(value))
                    return false;
                weightMax = Integer.parseInt(value);
                if (weightMax <= 0) {
                    weightMax = 1000;
                }
            } else if (cmd.equals(CMD_DATA_LMT)) {
                String value = data.getValue(CMD_DATA_LMT); //получаем макимальнай показание перегруза
                if (!isInteger(value))
                    return false;
                limitTenzo = Integer.parseInt(value);
            }
        }
        return true;
    }

    @Override
    public int getLimitTenzo() {
        return limitTenzo;
    }

    @Override
    public int getMarginTenzo() {
        return marginTenzo;
    }

    @Override
    public int getSensorTenzo() {
        return sensorTenzoOffset + offset;
    }

    @Override
    public boolean isLimit() {
        return Math.abs(getSensorTenzo()) > limitTenzo;
    }

    @Override
    public boolean isMargin() {
        return Math.abs(getSensorTenzo()) > marginTenzo;
    }

    @Override
    public boolean setScaleNull() {
        return setOffsetScale();
    }
}
