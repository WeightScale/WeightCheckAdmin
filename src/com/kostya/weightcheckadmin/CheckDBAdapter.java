package com.kostya.weightcheckadmin;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

class CheckDBAdapter {

    private final Context context;
    //private SQLiteDatabase db;

    public static int day;
    public static int day_closed;

    public static final String TABLE_CHECKS = "inputCheckTable";

    public static final String KEY_ID = "_id";
    public static final String KEY_DATE_CREATE = "dateCreate";
    public static final String KEY_TIME_CREATE = "timeCreate";
    public static final String KEY_NUMBER_BT = "numberBt";
    public static final String KEY_WEIGHT_GROSS = "weightGross";
    public static final String KEY_WEIGHT_TARE = "weightTare";
    public static final String KEY_WEIGHT_NETTO = "weightNetto";
    public static final String KEY_VENDOR = "vendor";
    public static final String KEY_VENDOR_ID = "vendorId";
    public static final String KEY_TYPE = "type";
    public static final String KEY_TYPE_ID = "typeId";
    public static final String KEY_PRICE = "price";
    public static final String KEY_PRICE_SUM = "priceSum";
    public static final String KEY_CHECK_ON_SERVER = "checkOnServer";
    public static final String KEY_IS_READY = "checkIsReady";
    public static final String KEY_VISIBILITY = "visibility";
    public static final String KEY_DIRECT = "direct";

    public static final int INVISIBLE = 0;
    public static final int VISIBLE = 1;


    public static final int DIRECT_DOWN = R.drawable.ic_action_down;
    public static final int DIRECT_UP = R.drawable.ic_action_up;

    private static final String[] All_COLUMN_CHECKS_TABLE = {
            KEY_ID,
            KEY_DATE_CREATE,
            KEY_TIME_CREATE,
            KEY_NUMBER_BT,
            KEY_WEIGHT_GROSS,
            KEY_WEIGHT_TARE,
            KEY_WEIGHT_NETTO,
            KEY_VENDOR,
            KEY_VENDOR_ID,
            KEY_TYPE,
            KEY_TYPE_ID,
            KEY_PRICE,
            KEY_PRICE_SUM,
            KEY_CHECK_ON_SERVER,
            KEY_IS_READY,
            KEY_VISIBILITY,
            KEY_DIRECT};

    public static final String TABLE_CREATE_CHECKS = "create table "
            + TABLE_CHECKS + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_DATE_CREATE + " text,"
            + KEY_TIME_CREATE + " text,"
            + KEY_NUMBER_BT + " text,"
            + KEY_WEIGHT_GROSS + " integer,"
            + KEY_WEIGHT_TARE + " integer,"
            + KEY_WEIGHT_NETTO + " integer,"
            + KEY_VENDOR + " text,"
            + KEY_VENDOR_ID + " integer,"
            + KEY_TYPE + " text,"
            + KEY_TYPE_ID + " integer,"
            + KEY_PRICE + " integer,"
            + KEY_PRICE_SUM + " real,"
            + KEY_CHECK_ON_SERVER + " integer,"
            + KEY_IS_READY + " integer,"
            + KEY_VISIBILITY + " integer,"
            + KEY_DIRECT + " integer );";

    static final String TABLE_CHECKS_PATH = TABLE_CHECKS;
    private static final Uri CHECKS_CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE_CHECKS_PATH);

    public CheckDBAdapter(Context cnt) {
        context = cnt;
    }

    public CheckDBAdapter(Context cnt, int d) {
        context = cnt;
        day = d;
    }

    /*public Uri insertOutNewEntry(String vendor){
        ContentValues newTaskValues = new ContentValues();
        Date date=new Date();
        newTaskValues.put(KEY_DATE_CREATE, new SimpleDateFormat("dd.MM.yyyy").format(date));
        newTaskValues.put(KEY_TIME_CREATE, new SimpleDateFormat("HH:mm:ss").format(date));
        newTaskValues.put(KEY_NUMBER_BT,vendor+Scales.getAddress());
        newTaskValues.put(KEY_VENDOR,vendor);
        newTaskValues.put(KEY_CHECK_ON_SERVER,false);
        newTaskValues.put(KEY_IS_READY,false);
        newTaskValues.put(KEY_VISIBILITY, VISIBLE);
        return context.getContentResolver().insert(CHECKS_CONTENT_URI,newTaskValues);
    }*/

    public Uri insertNewEntry(String vendor, int vendorId, int direct) {
        ContentValues newTaskValues = new ContentValues();
        Date date = new Date();
        newTaskValues.put(KEY_DATE_CREATE, new SimpleDateFormat("dd.MM.yyyy").format(date));
        newTaskValues.put(KEY_TIME_CREATE, new SimpleDateFormat("HH:mm:ss").format(date));
        newTaskValues.put(KEY_NUMBER_BT, Scales.getAddress());
        newTaskValues.put(KEY_VENDOR, vendor);
        newTaskValues.put(KEY_VENDOR_ID, vendorId);
        newTaskValues.put(KEY_CHECK_ON_SERVER, false);
        newTaskValues.put(KEY_IS_READY, false);
        newTaskValues.put(KEY_WEIGHT_GROSS, 0);
        newTaskValues.put(KEY_WEIGHT_NETTO, 0);
        newTaskValues.put(KEY_WEIGHT_TARE, 0);
        newTaskValues.put(KEY_PRICE_SUM, 0);
        newTaskValues.put(KEY_TYPE_ID, 1);
        newTaskValues.put(KEY_VISIBILITY, VISIBLE);
        newTaskValues.put(KEY_DIRECT, direct);
        return context.getContentResolver().insert(CHECKS_CONTENT_URI, newTaskValues);
    }

    boolean removeEntry(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(CHECKS_CONTENT_URI, _rowIndex);
        return uri != null && context.getContentResolver().delete(uri, null, null) > 0;
    }

    public boolean deleteCheckIsServer(/*long  dayAfter*/) {
        Cursor result = context.getContentResolver().query(CHECKS_CONTENT_URI, new String[]{KEY_ID, KEY_DATE_CREATE},
                KEY_CHECK_ON_SERVER + "= 1" + " and " + KEY_VISIBILITY + "= " + INVISIBLE, null, null);
        if (result == null)
            return false;
        if (result.getCount() > 0) {
            //SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yy");
            result.moveToFirst();
            if (!result.isAfterLast()) {
                do {
                    int id = result.getInt(result.getColumnIndex(KEY_ID));
                    //String date = result.getString(result.getColumnIndex(KEY_DATE_CREATE));
                    //long day = 0;
                    //try {day = dayDiff(new Date(), new SimpleDateFormat("dd.MM.yy").parse(date));} catch (ParseException e) {e.printStackTrace();}
                    //if(day >= dayAfter)
                    removeEntry(id);
                } while (result.moveToNext());
                result.close();
            }
        } else
            return false;
        return true;
    }

    public boolean invisibleCheckIsReady(long dayAfter) {
        Cursor result = context.getContentResolver().query(CHECKS_CONTENT_URI, new String[]{KEY_ID, KEY_DATE_CREATE},
                KEY_IS_READY + "= 1 and " + KEY_VISIBILITY + "= " + VISIBLE, null, null);
        if (result == null)
            return false;
        if (result.getCount() > 0) {
            //SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yy");
            result.moveToFirst();
            if (!result.isAfterLast()) {
                do {
                    int id = result.getInt(result.getColumnIndex(KEY_ID));
                    String date = result.getString(result.getColumnIndex(KEY_DATE_CREATE));
                    long day = 0;
                    try {
                        day = dayDiff(new Date(), new SimpleDateFormat("dd.MM.yy").parse(date));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    if (day >= dayAfter)
                        updateEntry(id, KEY_VISIBILITY, INVISIBLE);
                } while (result.moveToNext());
                result.close();
            }
        } else
            return false;
        return true;
    }

    long dayDiff(Date d1, Date d2) {
        final long DAY_MILLIS = 1000 * 60 * 60 * 24;
        long day1 = d1.getTime() / DAY_MILLIS;
        long day2 = d2.getTime() / DAY_MILLIS;
        return day1 - day2;
    }

    private String getKeyString(int _rowIndex, String key) {
        Uri uri = ContentUris.withAppendedId(CHECKS_CONTENT_URI, _rowIndex);
        if (uri == null)
            return "";
        Cursor result = context.getContentResolver().query(uri, new String[]{KEY_ID, key}, null, null, null);
        if (result == null)
            return "";
        if (result.getCount() == 0 || !result.moveToFirst()) {
            result.close();
            return "";
        }
        String str = result.getString(result.getColumnIndex(key));
        result.close();
        return str;
    }

    public int getKeyInt(int _rowIndex, String key) {
        Uri uri = ContentUris.withAppendedId(CHECKS_CONTENT_URI, _rowIndex);
        if (uri == null)
            return 0;
        Cursor result = context.getContentResolver().query(uri, new String[]{KEY_ID, key}, null, null, null);
        if (result == null)
            return 0;
        if (result.getCount() == 0 || !result.moveToFirst()) {
            result.close();
            return 0;
        }
        int i = result.getInt(result.getColumnIndex(key));
        result.close();
        return i;
    }

    public Cursor getAllEntries(int view) {
        return context.getContentResolver().query(CHECKS_CONTENT_URI, All_COLUMN_CHECKS_TABLE, KEY_IS_READY + "= 1" + " and " + KEY_VISIBILITY + "= " + view, null, null);
    }

    public Cursor getAllNoReadyCheck() {
        return context.getContentResolver().query(CHECKS_CONTENT_URI, All_COLUMN_CHECKS_TABLE, KEY_CHECK_ON_SERVER + "= 0" + " and " + KEY_IS_READY + "= 0", null, null);
    }

    boolean getCheckServerOldIsReady() {
        Cursor result = context.getContentResolver().query(CHECKS_CONTENT_URI, null, KEY_CHECK_ON_SERVER + "= 0" + " and " + KEY_IS_READY + "= 0", null, null);
        if (result == null)
            return false;
        if (result.getCount() == 0 || !result.moveToFirst()) {
            result.close();
            return false;
        }
        result.close();
        return true;
    }

    boolean getCheckServerIsReady() {
        Cursor result = context.getContentResolver().query(CHECKS_CONTENT_URI, null, KEY_CHECK_ON_SERVER + "= 0" + " and " + KEY_IS_READY + "= 1", null, null);
        if (result == null)
            return false;
        if (result.getCount() == 0 || !result.moveToFirst()) {
            result.close();
            return false;
        }
        result.close();
        return true;
    }

    public Cursor getAllNoCheckServerIsReady() {
        return context.getContentResolver().query(CHECKS_CONTENT_URI, All_COLUMN_CHECKS_TABLE, KEY_CHECK_ON_SERVER + "= 0" + " and " + KEY_IS_READY + "= 1", null, null);
    }

    public Cursor getNotReady() {
        return context.getContentResolver().query(CHECKS_CONTENT_URI, All_COLUMN_CHECKS_TABLE, KEY_IS_READY + "= 0", null, null);
    }

    String getKeyNumberBt(int _rowIndex) {
        return getKeyString(_rowIndex, KEY_NUMBER_BT);
    }

    public Cursor getEntryItem(int _rowIndex) throws SQLiteException {
        Uri uri = ContentUris.withAppendedId(CHECKS_CONTENT_URI, _rowIndex);
        if (uri == null)
            return null;
        Cursor result = context.getContentResolver().query(uri, All_COLUMN_CHECKS_TABLE, null, null, null);
        if (result == null)
            return null;
        if (result.getCount() == 0 || !result.moveToFirst()) {
            return null;
        }
        return result;
    }

    public boolean updateEntry(int _rowIndex, String key, int in) {
        //boolean b;
        Uri uri = ContentUris.withAppendedId(CHECKS_CONTENT_URI, _rowIndex);
        if (uri == null)
            return false;
        ContentValues newValues = new ContentValues();
        newValues.put(key, in);
        return context.getContentResolver().update(uri, newValues, null, null) > 0;
    }

    public boolean updateEntry(int _rowIndex, String key, float fl) {
        Uri uri = ContentUris.withAppendedId(CHECKS_CONTENT_URI, _rowIndex);
        if (uri == null)
            return false;
        ContentValues newValues = new ContentValues();
        newValues.put(key, fl);
        return context.getContentResolver().update(uri, newValues, null, null) > 0;
    }

    public boolean updateEntry(int _rowIndex, String key, String st) {
        Uri uri = ContentUris.withAppendedId(CHECKS_CONTENT_URI, _rowIndex);
        if (uri == null)
            return false;
        ContentValues newValues = new ContentValues();
        newValues.put(key, st);
        return context.getContentResolver().update(uri, newValues, null, null) > 0;
    }

}
