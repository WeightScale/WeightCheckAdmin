package com.kostya.weightcheckadmin;

/*
 * Created by Kostya on 01.05.14.
 */
interface ScaleInterface {

    //static final int filter = 0;

    String CMD_VERSION = "VRS";            //получить версию весов
    String CMD_NAME = "SNA";            //установить имя весов
    String CMD_SENSOR = "DCH";            //получить показание датчика веса
    String CMD_SENSOR_OFFSET = "DCO";            //получить показание датчика веса
    String CMD_SET_OFFSET = "SCO";            //установить offset
    String CMD_CALL_BATTERY = "CBT";            //каллибровать процент батареи
    String CMD_CALL_TEMP = "CTM";            //каллибровать процент батареи
    String CMD_BATTERY = "GBT";            //получить передать каллибровку температуры
    String CMD_FILTER = "FAD";            //получить/установить АЦП-фильтр
    String CMD_TIMER = "TOF";            //получить/установить таймер выключения весов
    String CMD_SPEED = "BST";            //получить/установить скорость передачи данных
    String CMD_DATA = "DAT";            //считать/записать данные весов
    String CMD_DATA_TEMP = "DTM";            //считать/записать данные температуры
    String CMD_SPREADSHEET = "SGD";            //считать/записать имя таблици созданой в google disc
    String CMD_G_USER = "UGD";            //считать/записать account google disc
    String CMD_G_PASS = "PGD";            //считать/записать password google disc

    String CR_LF = "\r\n";

    boolean load() throws Exception;//Загрузить данные весов

    int getWeightScale();//Получить показания датчика веса

    int getSensorScale();//Получить показания датчика веса

    boolean setOffsetScale();//Установить ноль

    boolean writeDataScale();//Записать данные

    boolean isDataValid(String d);//Проверить и установить правельные данные

    int getLimitTenzo();

    int getMarginTenzo();

    int getSensorTenzo();

    boolean isLimit();

    boolean isMargin();

    boolean setScaleNull();
}
