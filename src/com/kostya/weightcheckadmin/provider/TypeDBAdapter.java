package com.kostya.weightcheckadmin.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
//import android.content.res.Resources;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import com.kostya.weightcheckadmin.R;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 24.09.13
 * Time: 12:27
 * To change this template use File | Settings | File Templates.
 */
public class TypeDBAdapter {
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

    //static final String TABLE_TYPE_PATH = TABLE_TYPE;
    private static final Uri CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE_TYPE);

    public TypeDBAdapter(Context cnt) {
        context = cnt;
    }

    private int getKeyInt(int _rowIndex, String key) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            Cursor result = context.getContentResolver().query(uri, new String[]{KEY_ID, key}, null, null, null);
            int in = -1;
            if (result.getCount() > 0) {
                result.moveToFirst();
                in = result.getInt(result.getColumnIndex(key));
            }
            result.close();
            return in;
        }catch (Exception e){return -1;}
    }

    public int getPriceColumn(int _rowIndex) {
        return getKeyInt(_rowIndex, KEY_PRICE);
    }

    public Cursor getAllEntries() {
        return context.getContentResolver().query(CONTENT_URI, new String[]{KEY_ID, KEY_TYPE}, null, null, null);
    }

    public Cursor getNotSystemEntries() {
        return context.getContentResolver().query(CONTENT_URI, new String[]{KEY_ID, KEY_TYPE}, KEY_SYS + "= 0" + " OR " + KEY_SYS + " IS NULL ", null, null);
    }

    public Uri insertEntryType(String name) {
        ContentValues newTaskValues = new ContentValues();
        newTaskValues.put(KEY_TYPE, name);
        newTaskValues.put(KEY_SYS, 0);
        return context.getContentResolver().insert(CONTENT_URI, newTaskValues);
    }

    public boolean removeEntry(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        return uri != null && context.getContentResolver().delete(uri, null, null) > 0;
    }

    public boolean updateEntry(int _rowIndex, String key, int in) {
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, _rowIndex);
        try {
            ContentValues newValues = new ContentValues();
            newValues.put(key, in);
            return context.getContentResolver().update(uri, newValues, null, null) > 0;
        }catch (Exception e){return false;}

    }

    public void addSystemRow(SQLiteDatabase db) {
        ContentValues contentValues = new ContentValues();
        Resources res = context.getResources();
        contentValues.put(KEY_TYPE, "тест");
        contentValues.put(KEY_SYS, 1);
        db.insert(TABLE_TYPE, null, contentValues);
        String[] type_records = res.getStringArray(R.array.type_array);
        for (String type_record : type_records) {
            contentValues.put(KEY_TYPE, type_record);
            contentValues.put(KEY_SYS, 0);
            db.insert(TABLE_TYPE, null, contentValues);
        }
    }
}
