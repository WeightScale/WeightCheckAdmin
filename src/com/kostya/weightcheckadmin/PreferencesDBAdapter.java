package com.kostya.weightcheckadmin;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 11.11.13
 * Time: 14:15
 * To change this template use File | Settings | File Templates.
 */
class PreferencesDBAdapter {
    private final Context context;
    public static final String TABLE_PREFERENCES = "preferencesTable";

    public static final String KEY_ID = "_id";
    public static final String KEY_DATE_CREATE = "dateCreate";
    public static final String KEY_TIME_CREATE = "timeCreate";
    public static final String KEY_NUMBER_BT = "numberBt";
    public static final String KEY_COEFFICIENT_A = "coefficientA";
    public static final String KEY_COEFFICIENT_B = "coefficientB";
    public static final String KEY_MAX_WEIGHT = "maxWeight";
    public static final String KEY_FILTER_ADC = "filterADC";
    public static final String KEY_STEP_SCALE = "stepScale";
    public static final String KEY_STEP_CAPTURE = "stepCapture";
    public static final String KEY_TIME_OFF = "timeOff";
    public static final String KEY_NUMBER_BT_TERMINAL = "numberBtTerminal";
    private static final String KEY_CHECK_ON_SERVER = "checkOnServer";

    public static final String TABLE_CREATE_PREFERENCES = "create table "
            + TABLE_PREFERENCES + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_DATE_CREATE + " text,"
            + KEY_TIME_CREATE + " text,"
            + KEY_NUMBER_BT + " text,"
            + KEY_COEFFICIENT_A + " float,"
            + KEY_COEFFICIENT_B + " float,"
            + KEY_MAX_WEIGHT + " integer,"
            + KEY_FILTER_ADC + " integer,"
            + KEY_STEP_SCALE + " integer,"
            + KEY_STEP_CAPTURE + " integer,"
            + KEY_TIME_OFF + " integer,"
            + KEY_NUMBER_BT_TERMINAL + " text,"
            + KEY_CHECK_ON_SERVER + " integer );";


    static final String TABLE_PREFERENCES_PATH = TABLE_PREFERENCES;
    private static final Uri PREFERENCES_CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE_PREFERENCES_PATH);


    public PreferencesDBAdapter(Context cnt) {
        context = cnt;
    }

    public Uri insertAllEntry() {
        ContentValues newTaskValues = new ContentValues();
        Date date = new Date();
        newTaskValues.put(KEY_DATE_CREATE, new SimpleDateFormat("dd.MM.yyyy").format(date));
        newTaskValues.put(KEY_TIME_CREATE, new SimpleDateFormat("HH:mm:ss").format(date));
        newTaskValues.put(KEY_NUMBER_BT, Scales.getAddress());
        newTaskValues.put(KEY_COEFFICIENT_A, Scales.coefficientA);
        newTaskValues.put(KEY_COEFFICIENT_B, Scales.coefficientB);
        newTaskValues.put(KEY_MAX_WEIGHT, Scales.weightMax);
        newTaskValues.put(KEY_FILTER_ADC, Scales.filter);
        newTaskValues.put(KEY_STEP_SCALE, Scales.step);
        newTaskValues.put(KEY_STEP_CAPTURE, Scales.autoCapture);
        newTaskValues.put(KEY_TIME_OFF, Scales.timer);
        newTaskValues.put(KEY_NUMBER_BT_TERMINAL, BluetoothAdapter.getDefaultAdapter().getAddress());
        newTaskValues.put(KEY_CHECK_ON_SERVER, 0);
        return context.getContentResolver().insert(PREFERENCES_CONTENT_URI, newTaskValues);
    }

    boolean getPrefServerIsReady() {

        Cursor result = context.getContentResolver().query(PREFERENCES_CONTENT_URI, new String[]{KEY_ID, KEY_CHECK_ON_SERVER}, KEY_CHECK_ON_SERVER + "= 0", null, null);
        if (result == null)
            return false;
        if (result.getCount() == 0 || !result.moveToFirst()) {
            result.close();
            return false;
        }
        result.close();
        return true;
    }

    public Cursor getAllNoCheckServer() {
        return context.getContentResolver().query(PREFERENCES_CONTENT_URI, new String[]{KEY_ID,
                KEY_DATE_CREATE,
                KEY_TIME_CREATE,
                KEY_NUMBER_BT,
                KEY_COEFFICIENT_A,
                KEY_COEFFICIENT_B,
                KEY_MAX_WEIGHT,
                KEY_FILTER_ADC,
                KEY_STEP_SCALE,
                KEY_STEP_CAPTURE,
                KEY_TIME_OFF,
                KEY_NUMBER_BT_TERMINAL,
                KEY_CHECK_ON_SERVER}, KEY_CHECK_ON_SERVER + "= 0", null, null);
    }

    public boolean removeEntry(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(PREFERENCES_CONTENT_URI, _rowIndex);
        return uri != null && context.getContentResolver().delete(uri, null, null) > 0;
    }

    public Cursor getEntryItem(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(PREFERENCES_CONTENT_URI, _rowIndex);
        if (uri == null)
            return null;
        Cursor result = context.getContentResolver().query(uri, null, null, null, null);
        if (result == null)
            return null;
        if (result.getCount() == 0 || !result.moveToFirst()) {
            return null;
        }
        return result;
    }

}
