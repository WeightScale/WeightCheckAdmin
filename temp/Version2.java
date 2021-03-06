package com.kostya.weightcheckadmin;

import com.kostya.weightcheckadmin.provider.CheckDBAdapter;

/*
 * Created by Kostya on 30.11.2014.
 */
public class Version2 extends Scales implements ScaleInterface {

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
            return false;
        //======================================================================
        str = command(CMD_CALL_TEMP);
        if (!isFloat(str))
            return false;
        coefficientTemp = Float.valueOf(str);
        //======================================================================
        str = command(CMD_SPREADSHEET);
        if (str.isEmpty())
            return false;
        spreadsheet = str;
        //======================================================================
        str = command(CMD_G_USER);
        if (str.isEmpty())
            return false;
        username = str;
        //======================================================================
        str = command(CMD_G_PASS);
        if (str.isEmpty())
            return false;
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
            return false;
        weightError = Preferences.read(ActivityPreferences.KEY_MAX_NULL, 50);
        weightMargin = (int) (weightMax * 1.2);
        marginTenzo = (int) ((weightMax / coefficientA) * 1.2);
        limitTenzo = (int) (weightMax / coefficientA);
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

    @Override
    public synchronized boolean setOffsetScale() { //обнуление
        return command(CMD_SET_OFFSET).equals(CMD_SET_OFFSET);
    }

    @Override
    public boolean writeDataScale() {
        return command(CMD_DATA + 'S' + coefficientA + ' ' + weightMax).equals(CMD_DATA);
    }

    @Override
    public synchronized boolean isDataValid(String d) {
        StringBuilder dataBuffer = new StringBuilder(d);
        if (dataBuffer.toString().isEmpty())
            return false;
        dataBuffer.deleteCharAt(0);
        if (dataBuffer.indexOf(" ") == -1)
            return false;
        String str = dataBuffer.substring(0, dataBuffer.indexOf(" "));
        if (!isFloat(str))
            return false;
        coefficientA = Float.valueOf(str);
        dataBuffer.delete(0, dataBuffer.indexOf(" ") + 1);
        if (!isInteger(dataBuffer.toString()))
            return false;
        weightMax = Integer.valueOf(dataBuffer.toString());
        if (weightMax <= 0) {
            weightMax = 1000;
            //writeData();
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
        return sensorTenzoOffset;
    }

    @Override
    public boolean isLimit() {
        return Math.abs(sensorTenzoOffset) > limitTenzo;
    }

    @Override
    public boolean isMargin() {
        return Math.abs(sensorTenzoOffset) > marginTenzo;
    }

    @Override
    public boolean setScaleNull() {
        return setOffsetScale();
    }
}
