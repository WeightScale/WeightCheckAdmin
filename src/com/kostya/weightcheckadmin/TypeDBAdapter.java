package com.kostya.weightcheckadmin;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
//import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
//import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 24.09.13
 * Time: 12:27
 * To change this template use File | Settings | File Templates.
 */
class TypeDBAdapter {
    private final Context context;
    public static final String TABLE_TYPE = "typeTable";

    public static final String KEY_ID = BaseColumns._ID;
    public static final String KEY_TYPE = "type";
    public static final String KEY_PRICE = "price";
    public static final String KEY_SYS = "system";

    public static final String TABLE_CREATE_TYPE = "create table "
            + TABLE_TYPE + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_TYPE + " text, "
            + KEY_PRICE + " integer, "
            + KEY_SYS + " integer );";

    static final String TABLE_TYPE_PATH = TABLE_TYPE;
    private static final Uri TABLE_TYPE_CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE_TYPE_PATH);

    public TypeDBAdapter(Context cnt) {
        context = cnt;
    }

    private int getKeyInt(int _rowIndex, String key) {
        Uri uri = ContentUris.withAppendedId(TABLE_TYPE_CONTENT_URI, _rowIndex);
        if (uri == null)
            return -1;
        Cursor result = context.getContentResolver().query(uri, new String[]{KEY_ID, key}, null, null, null);
        if (result == null)
            return -1;
        if (result.getCount() == 0 || !result.moveToFirst()) {
            result.close();
            return -1;
        }
        int in = result.getInt(result.getColumnIndex(key));
        result.close();
        return in;
    }

    int getPriceColumn(int _rowIndex) {
        return getKeyInt(_rowIndex, KEY_PRICE);
    }

    public Cursor getAllEntries() {
        return context.getContentResolver().query(TABLE_TYPE_CONTENT_URI, new String[]{KEY_ID, KEY_TYPE}, null, null, null);
    }

    public Cursor getNotSystemEntries() {
        return context.getContentResolver().query(TABLE_TYPE_CONTENT_URI, new String[]{KEY_ID, KEY_TYPE}, KEY_SYS + "= 0" + " OR " + KEY_SYS + " IS NULL ", null, null);
    }

    Uri insertEntryType(String name) {
        ContentValues newTaskValues = new ContentValues();
        newTaskValues.put(KEY_TYPE, name);
        newTaskValues.put(KEY_SYS, 0);
        return context.getContentResolver().insert(TABLE_TYPE_CONTENT_URI, newTaskValues);
    }

    public boolean removeEntry(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(TABLE_TYPE_CONTENT_URI, _rowIndex);
        return uri != null && context.getContentResolver().delete(uri, null, null) > 0;
    }

    public boolean updateEntry(int _rowIndex, String key, int in) {
        Uri uri = ContentUris.withAppendedId(TABLE_TYPE_CONTENT_URI, _rowIndex);
        if (uri == null)
            return false;
        ContentValues newValues = new ContentValues();
        newValues.put(key, in);
        return context.getContentResolver().update(uri, newValues, null, null) > 0;
    }
}
