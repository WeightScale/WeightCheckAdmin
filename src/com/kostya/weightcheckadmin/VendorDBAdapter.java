package com.kostya.weightcheckadmin;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
//import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 23.09.13
 * Time: 16:05
 * To change this template use File | Settings | File Templates.
 */
class VendorDBAdapter {
    private final Context context;
    public static final String TABLE_VENDOR = "vendorTable";

    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "name";

    public static final String TABLE_CREATE_VENDOR = "create table "
            + TABLE_VENDOR + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_NAME + " text );";

    static final String TABLE_VENDOR_PATH = TABLE_VENDOR;
    private static final Uri TABLE_VENDOR_CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + "/" + TABLE_VENDOR_PATH);

    public VendorDBAdapter(Context cnt) {
        context = cnt;
    }

    /*String getKeyName(long  _rowIndex){
        Uri uri = ContentUris.withAppendedId(TABLE_VENDOR_CONTENT_URI, _rowIndex);
        if(uri == null)
            throw new SQLiteException("Uri значение null");
        Cursor result = context.getContentResolver().query(uri, new String[]{KEY_ID, KEY_NAME}, KEY_ID + "=" + _rowIndex, null, null);
        if((result.getCount() == 0) || !result.moveToFirst()){
            throw new SQLiteException("Нет записи с номером строки" + _rowIndex);
        }
        return result.getString(result.getColumnIndex(KEY_NAME));
    }*/

    Uri insertEntryName(String name) {
        ContentValues newTaskValues = new ContentValues();
        newTaskValues.put(KEY_NAME, name);
        return context.getContentResolver().insert(TABLE_VENDOR_CONTENT_URI, newTaskValues);
    }

    public boolean removeEntry(long _rowIndex) {
        Uri uri = ContentUris.withAppendedId(TABLE_VENDOR_CONTENT_URI, _rowIndex);
        return uri != null && context.getContentResolver().delete(uri, null, null) > 0;
    }

    public Cursor getAllEntries() {
        return context.getContentResolver().query(TABLE_VENDOR_CONTENT_URI, new String[]{KEY_ID, KEY_NAME}, null, null, null);
    }

}
