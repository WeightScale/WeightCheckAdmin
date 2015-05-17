package com.kostya.weightcheckadmin;

import android.content.*;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 08.12.13
 * Time: 13:11
 * To change this template use File | Settings | File Templates.
 */
public class WeightCheckBaseProvider extends ContentProvider {

    private static final String DATABASE_NAME = "weightCheck.db";
    private static final int DATABASE_VERSION = 8;
    static final String AUTHORITY = "com.kostya.weightcheckadmin.weightCheck";

    private static final int ALL_ROWS = 1;
    private static final int SINGLE_ROWS = 2;

    private static final int INPUT_CHECK_LIST = 1;
    private static final int INPUT_CHECK_ID = 2;
    private static final int VENDOR_LIST = 3;
    private static final int VENDOR_ID = 4;
    private static final int PREFERENCES_LIST = 5;
    private static final int PREFERENCES_ID = 6;
    private static final int TYPE_LIST = 7;
    private static final int TYPE_ID = 8;
    private static final int TASK_LIST = 9;
    private static final int TASK_ID = 10;

    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, CheckDBAdapter.TABLE_CHECKS_PATH, INPUT_CHECK_LIST);
        uriMatcher.addURI(AUTHORITY, CheckDBAdapter.TABLE_CHECKS_PATH + "/#", INPUT_CHECK_ID);
        //uriMatcher.addURI(AUTHORITY, VendorDBAdapter.TABLE_VENDOR_PATH, VENDOR_LIST);
        //uriMatcher.addURI(AUTHORITY, VendorDBAdapter.TABLE_VENDOR_PATH + "/#", VENDOR_ID);
        uriMatcher.addURI(AUTHORITY, PreferencesDBAdapter.TABLE_PREFERENCES_PATH, PREFERENCES_LIST);
        uriMatcher.addURI(AUTHORITY, PreferencesDBAdapter.TABLE_PREFERENCES_PATH + "/#", PREFERENCES_ID);
        uriMatcher.addURI(AUTHORITY, TypeDBAdapter.TABLE_TYPE_PATH, TYPE_LIST);
        uriMatcher.addURI(AUTHORITY, TypeDBAdapter.TABLE_TYPE_PATH + "/#", TYPE_ID);
        uriMatcher.addURI(AUTHORITY, TaskDBAdapter.TABLE_TASK_PATH, TASK_LIST);
        uriMatcher.addURI(AUTHORITY, TaskDBAdapter.TABLE_TASK_PATH + "/#", TASK_ID);
    }

    private SQLiteDatabase db;

    /*public void vacuum(){
        db.execSQL("VACUUM");
    }*/

    private String getTable(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case INPUT_CHECK_LIST:
            case INPUT_CHECK_ID:
                return CheckDBAdapter.TABLE_CHECKS_PATH; // return
            //case VENDOR_LIST: case VENDOR_ID:
            //return VendorDBAdapter.TABLE_VENDOR_PATH; // return
            case PREFERENCES_LIST:
            case PREFERENCES_ID:
                return PreferencesDBAdapter.TABLE_PREFERENCES_PATH; // return
            case TYPE_LIST:
            case TYPE_ID:
                return TypeDBAdapter.TABLE_TYPE_PATH; // return
            case TASK_LIST:
            case TASK_ID:
                return TaskDBAdapter.TABLE_TASK_PATH; // return
            /** PROVIDE A DEFAULT CASE HERE **/
            default:
                // If the URI doesn't match any of the known patterns, throw an exception.
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        DBHelper dbHelper = new DBHelper(getContext());
        //db = dbHelper.getWritableDatabase();
        db = dbHelper.getReadableDatabase();
        if (db != null)
            db.setLockingEnabled(false);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        switch (uriMatcher.match(uri)) {
            case INPUT_CHECK_LIST: // общий Uri
                queryBuilder.setTables(CheckDBAdapter.TABLE_CHECKS);
                break;
            case INPUT_CHECK_ID: // Uri с ID
                queryBuilder.setTables(CheckDBAdapter.TABLE_CHECKS);
                queryBuilder.appendWhere(CheckDBAdapter.KEY_ID + '=' + uri.getLastPathSegment());
                break;
            //case VENDOR_LIST: // общий Uri
            //queryBuilder.setTables(VendorDBAdapter.TABLE_VENDOR);
            //break;
            //case VENDOR_ID: // Uri с ID
            //queryBuilder.setTables(VendorDBAdapter.TABLE_VENDOR);
            //queryBuilder.appendWhere(VendorDBAdapter.KEY_ID + '=' + uri.getLastPathSegment());
            //break;
            case PREFERENCES_LIST: // общий Uri
                queryBuilder.setTables(PreferencesDBAdapter.TABLE_PREFERENCES);
                break;
            case PREFERENCES_ID: // Uri с ID
                queryBuilder.setTables(PreferencesDBAdapter.TABLE_PREFERENCES);
                queryBuilder.appendWhere(PreferencesDBAdapter.KEY_ID + '=' + uri.getLastPathSegment());
                break;
            case TYPE_LIST: // общий Uri
                queryBuilder.setTables(TypeDBAdapter.TABLE_TYPE);
                break;
            case TYPE_ID: // Uri с ID
                queryBuilder.setTables(TypeDBAdapter.TABLE_TYPE);
                queryBuilder.appendWhere(TypeDBAdapter.KEY_ID + '=' + uri.getLastPathSegment());
                break;
            case TASK_LIST: // общий Uri
                queryBuilder.setTables(TaskDBAdapter.TABLE_TASK);
                break;
            case TASK_ID: // Uri с ID
                queryBuilder.setTables(TaskDBAdapter.TABLE_TASK);
                queryBuilder.appendWhere(TaskDBAdapter.KEY_ID + '=' + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sort);
        if (cursor == null)
            return null;
        Context context = getContext();
        if (context != null) {
            ContentResolver contentResolver = context.getContentResolver();
            if (contentResolver != null)
                cursor.setNotificationUri(contentResolver, uri);
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ALL_ROWS:
                return "vnd.android.cursor.dir/vnd.";
            case SINGLE_ROWS:
                return "vnd.android.cursor.item/vnd.";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {

        long rowID = db.insert(getTable(uri), null, contentValues);
        if (rowID > 0L) {
            Uri resultUri = ContentUris.withAppendedId(uri, rowID);
            // уведомляем ContentResolver, что данные по адресу resultUri изменились
            //if(uri.equals(TaskDBAdapter.TASK_CONTENT_URI)) // ставим флаг что новые данные
            //ServiceGetDateServer.flagNewTask = true;
            Context context = getContext();
            if (context != null) {
                context.getContentResolver().notifyChange(resultUri, null);
                return resultUri;
            }
        }
        throw new SQLiteException("Ошибка добавления записи " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArg) {
        int delCount;
        String id;
        switch (uriMatcher.match(uri)) {
            case INPUT_CHECK_LIST: // общий Uri
                delCount = db.delete(CheckDBAdapter.TABLE_CHECKS, where, whereArg);
                break;
            case INPUT_CHECK_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = CheckDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + CheckDBAdapter.KEY_ID + " = " + id;
                delCount = db.delete(CheckDBAdapter.TABLE_CHECKS, where, whereArg);
                break;
            //case VENDOR_LIST: // общий Uri
            //delCount = db.delete(VendorDBAdapter.TABLE_VENDOR,where,whereArg);
            //break;
            /*case VENDOR_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = VendorDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + VendorDBAdapter.KEY_ID + " = " + id;
                delCount = db.delete(VendorDBAdapter.TABLE_VENDOR,where,whereArg);
            break;*/
            case PREFERENCES_LIST: // общий Uri
                delCount = db.delete(PreferencesDBAdapter.TABLE_PREFERENCES, where, whereArg);
                break;
            case PREFERENCES_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = PreferencesDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + PreferencesDBAdapter.KEY_ID + " = " + id;
                delCount = db.delete(PreferencesDBAdapter.TABLE_PREFERENCES, where, whereArg);
                break;
            case TYPE_LIST:
                delCount = db.delete(TypeDBAdapter.TABLE_TYPE, where, whereArg);
                break;
            case TYPE_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = TypeDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + TypeDBAdapter.KEY_ID + " = " + id;
                delCount = db.delete(TypeDBAdapter.TABLE_TYPE, where, whereArg);
                break;
            case TASK_LIST:
                delCount = db.delete(TaskDBAdapter.TABLE_TASK, where, whereArg);
                break;
            case TASK_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = TaskDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + TaskDBAdapter.KEY_ID + " = " + id;
                delCount = db.delete(TaskDBAdapter.TABLE_TASK, where, whereArg);
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        db.execSQL("VACUUM");
        if (delCount > 0) {
            if (getContext() != null)
                getContext().getContentResolver().notifyChange(uri, null);
        }

        return delCount;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String where, String[] whereArg) {
        int updateCount;
        String id;
        switch (uriMatcher.match(uri)) {
            case INPUT_CHECK_LIST: // общий Uri
                updateCount = db.update(CheckDBAdapter.TABLE_CHECKS, contentValues, where, whereArg);
                break;
            case INPUT_CHECK_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = CheckDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + CheckDBAdapter.KEY_ID + " = " + id;
                updateCount = db.update(CheckDBAdapter.TABLE_CHECKS, contentValues, where, whereArg);
                break;
            /*case VENDOR_LIST: // общий Uri
                updateCount = db.update(VendorDBAdapter.TABLE_VENDOR,contentValues, where, whereArg);
                break;
            case VENDOR_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = VendorDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + VendorDBAdapter.KEY_ID + " = " + id;
                updateCount = db.update(VendorDBAdapter.TABLE_VENDOR,contentValues, where, whereArg);
                break;*/
            case PREFERENCES_LIST: // общий Uri
                updateCount = db.update(PreferencesDBAdapter.TABLE_PREFERENCES, contentValues, where, whereArg);
                break;
            case PREFERENCES_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = PreferencesDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + PreferencesDBAdapter.KEY_ID + " = " + id;
                updateCount = db.update(PreferencesDBAdapter.TABLE_PREFERENCES, contentValues, where, whereArg);
                break;
            case TYPE_LIST: // общий Uri
                updateCount = db.update(TypeDBAdapter.TABLE_TYPE, contentValues, where, whereArg);
                break;
            case TYPE_ID: // Uri с ID
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = TypeDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + TypeDBAdapter.KEY_ID + " = " + id;
                updateCount = db.update(TypeDBAdapter.TABLE_TYPE, contentValues, where, whereArg);
                break;
            case TASK_LIST: // общий Uri
                updateCount = db.update(TaskDBAdapter.TABLE_TASK, contentValues, where, whereArg);
                break;
            case TASK_ID: // Uri с ID
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where))
                    where = TaskDBAdapter.KEY_ID + " = " + id;
                else
                    where = where + " AND " + TaskDBAdapter.KEY_ID + " = " + id;
                updateCount = db.update(TaskDBAdapter.TABLE_TASK, contentValues, where, whereArg);
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        if (updateCount > 0) {
            if (getContext() != null)
                getContext().getContentResolver().notifyChange(uri, null);
        }

        return updateCount;
    }

    private class DBHelper extends SQLiteOpenHelper {

        DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CheckDBAdapter.TABLE_CREATE_CHECKS);
            db.execSQL(TypeDBAdapter.TABLE_CREATE_TYPE);
            //db.execSQL(VendorDBAdapter.TABLE_CREATE_VENDOR);
            db.execSQL(PreferencesDBAdapter.TABLE_CREATE_PREFERENCES);
            db.execSQL(TaskDBAdapter.TABLE_CREATE_TASK);

            //Add default record to my table
            ContentValues contentValues = new ContentValues();
            if (getContext() != null) {
                Resources res = getContext().getResources();
                String[] type_records = res.getStringArray(R.array.type_array);
                for (String type_record : type_records) {
                    contentValues.put(TypeDBAdapter.KEY_TYPE, type_record);
                    contentValues.put(TypeDBAdapter.KEY_SYS, 1);
                    db.insert(TypeDBAdapter.TABLE_TYPE, null, contentValues);
                }
            }

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            /*if (!db.isReadOnly()) {
            // Enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys=ON;");
            }*/
            //db.execSQL("alter table inputCheckTable rename to inputCheckTable");
            db.execSQL("DROP TABLE IF EXISTS " + CheckDBAdapter.TABLE_CHECKS);
            db.execSQL("DROP TABLE IF EXISTS " + TypeDBAdapter.TABLE_TYPE);
            //db.execSQL("DROP TABLE IF EXISTS " + VendorDBAdapter.TABLE_VENDOR);
            db.execSQL("DROP TABLE IF EXISTS " + PreferencesDBAdapter.TABLE_PREFERENCES);
            db.execSQL("DROP TABLE IF EXISTS " + TaskDBAdapter.TABLE_TASK);
            onCreate(db);
        }

        /*public SQLiteDatabase open() throws SQLiteException {
            try {
                db = this.getWritableDatabase();
            }catch (SQLiteException ex){
                db = this.getReadableDatabase();
            }
            return db;
        }*/

    }

}
