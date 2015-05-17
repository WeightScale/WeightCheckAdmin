package com.kostya.weightcheckadmin;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

//import android.content.res.Resources;
//import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 24.09.13
 * Time: 12:27
 * To change this template use File | Settings | File Templates.
 */
class TaskDBAdapter {
    private final Context context;
    public static final String TABLE_TASK = "taskTable";

    public static final String KEY_ID = "_id";
    public static final String KEY_MIME_TYPE = "mime_type";
    public static final String KEY_DOC = "id_doc";
    public static final String KEY_ID_CONTACT = "id_contact";
    public static final String KEY_DATA1 = "data1";

    public static final int TYPE_CHECK_MAIL_CONTACT = 1;    //для електронной почты
    public static final int TYPE_CHECK_DISK = 2;            //для google disk
    public static final int TYPE_PREF_DISK = 3;             //для google disk
    public static final int TYPE_CHECK_SMS_CONTACT = 4;     //для смс отправки

    public static final String TABLE_CREATE_TASK = "create table "
            + TABLE_TASK + " ("
            + KEY_ID + " integer primary key autoincrement, "
            + KEY_MIME_TYPE + " integer,"
            + KEY_DOC + " integer,"
            + KEY_ID_CONTACT + " integer,"
            + KEY_DATA1 + " text );";

    static final String TABLE_TASK_PATH = TABLE_TASK;
    public static final Uri TASK_CONTENT_URI = Uri.parse("content://" + WeightCheckBaseProvider.AUTHORITY + '/' + TABLE_TASK_PATH);

    public TaskDBAdapter(Context cnt) {
        context = cnt;
    }

    public Uri insertNewTask(int mimeType, long documentId, long vendorId, String data1) {
        ContentValues newTaskValues = new ContentValues();
        newTaskValues.put(KEY_MIME_TYPE, mimeType);
        newTaskValues.put(KEY_DOC, documentId);
        newTaskValues.put(KEY_ID_CONTACT, vendorId);
        newTaskValues.put(KEY_DATA1, data1);
        return context.getContentResolver().insert(TASK_CONTENT_URI, newTaskValues);
    }

    boolean isTaskReady() {
        Cursor result = context.getContentResolver().query(TASK_CONTENT_URI, null, null, null, null);
        if (result == null)
            return false;
        if (result.getCount() == 0 || !result.moveToFirst()) {
            result.close();
            return false;
        }
        result.close();
        return true;
    }

    public Cursor getAllEntries() {
        return context.getContentResolver().query(TASK_CONTENT_URI, null, null, null, null);
    }

    boolean removeEntry(int _rowIndex) {
        Uri uri = ContentUris.withAppendedId(TASK_CONTENT_URI, _rowIndex);
        return uri != null && context.getContentResolver().delete(uri, null, null) > 0;
    }

}
