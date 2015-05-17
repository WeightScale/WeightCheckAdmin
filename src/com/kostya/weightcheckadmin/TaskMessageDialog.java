package com.kostya.weightcheckadmin;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.*;

/*
 * Created by Kostya on 09.01.2015.
 */
public class TaskMessageDialog {

    final Context mContext;
    final int mContactId;
    final int mCheckId;
    final Dialog dialog;
    final ContentResolver contentResolver;

    public TaskMessageDialog(Context context, int contactId, int checkId) {
        mContext = context;
        mContactId = contactId;
        mCheckId = checkId;
        dialog = new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        contentResolver = mContext.getContentResolver();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void openListEmailDialog() {

        final Cursor emails = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + mContactId, null, null);
        if (emails.moveToFirst()) {
            String[] columns = {ContactsContract.CommonDataKinds.Email.DATA, ContactsContract.CommonDataKinds.Email.ADDRESS};

            int[] to = {R.id.title, R.id.icon_list};
            SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(mContext, R.layout.item_list_dialog, emails, columns, to);
            cursorAdapter.setViewBinder(new MyViewBinder(mContext.getResources().getDrawable(R.drawable.mail1)));
            //LayoutInflater layoutInflater = mContext.getLayoutInflater();
            LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View convertView = layoutInflater.inflate(R.layout.dialog_list, null);
            ListView listView = (ListView) convertView.findViewById(R.id.component_list);
            TextView dialogTitle = (TextView) convertView.findViewById(R.id.dialog_title);
            dialogTitle.setText("Отправить MAIL");
            listView.setAdapter(cursorAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Uri uri = ContentUris.withAppendedId(ContactsContract.CommonDataKinds.Email.CONTENT_URI, id);
                    Cursor result = contentResolver.query(uri, new String[]{ContactsContract.CommonDataKinds.Email.DATA}, null, null, null);
                    if (result.moveToFirst()) {
                        String str = result.getString(result.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                        new TaskDBAdapter(mContext).insertNewTask(TaskDBAdapter.TYPE_CHECK_MAIL_CONTACT, mCheckId, mContactId, str);
                    }
                    dialog.dismiss();
                }
            });
            dialog.setContentView(convertView);
            dialog.setCancelable(false);
            Button positiveButton = (Button) dialog.findViewById(R.id.positive_button);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    dialog.dismiss();
                }
            });
            Button buttonAll = (Button) dialog.findViewById(R.id.buttonAll);
            buttonAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    emails.moveToFirst();
                    if (!emails.isAfterLast()) {
                        do {
                            String str = emails.getString(emails.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                            new TaskDBAdapter(mContext).insertNewTask(TaskDBAdapter.TYPE_CHECK_MAIL_CONTACT, mCheckId, mContactId, str);
                        } while (emails.moveToNext());
                    }
                    dialog.dismiss();
                }
            });
            Button buttonNew = (Button) dialog.findViewById(R.id.buttonNew);
            buttonNew.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    createEmailDialog();
                }
            });
            dialog.show();
        } else {
            createEmailDialog();
        }
    }

    private void createEmailDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setMessage("Введите адрес?");
        final EditText input = new EditText(mContext);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        alertDialog.setView(input);
        alertDialog.setIcon(mContext.getResources().getDrawable(R.drawable.mail1));
        alertDialog.setPositiveButton("Отправить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!input.getText().toString().isEmpty()) {
                    new TaskDBAdapter(mContext).insertNewTask(TaskDBAdapter.TYPE_CHECK_MAIL_CONTACT, mCheckId, mContactId, input.getText().toString());
                    dialog.dismiss();
                }
            }
        });
        alertDialog.setNegativeButton("Закрыть", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        alertDialog.setNeutralButton("Сохранить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!input.getText().toString().isEmpty()) {
                    if (addEmailToContact(input.getText().toString()) != null)
                        openListEmailDialog();
                    else
                        Toast.makeText(mContext, "Email не сохранен нет контакта", Toast.LENGTH_LONG).show();
                }
            }
        });
        alertDialog.show();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private Uri addEmailToContact(String mail) {
        Cursor cursor = contentResolver.query(ContactsContract.RawContacts.CONTENT_URI, null, ContactsContract.RawContacts.CONTACT_ID + " = " + mContactId, null, null);
        if (cursor.moveToFirst()) {
            int rawContactId = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
            ContentValues values = new ContentValues();
            values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
            values.put(ContactsContract.CommonDataKinds.Email.ADDRESS, mail);
            values.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);
            return contentResolver.insert(ContactsContract.Data.CONTENT_URI, values);
        }
        return null;
    }

    public void openListPhoneDialog() {
        final Cursor phones = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + mContactId, null, null);
        if (phones.moveToFirst()) {
            String[] columns = {ContactsContract.CommonDataKinds.Phone.DATA, ContactsContract.CommonDataKinds.Phone.NUMBER};
            int[] to = {R.id.title, R.id.icon_list};
            SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(mContext, R.layout.item_list_dialog, phones, columns, to);
            cursorAdapter.setViewBinder(new MyViewBinder(mContext.getResources().getDrawable(R.drawable.messages)));
            //LayoutInflater layoutInflater = mContext.getLayoutInflater();
            LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View convertView = layoutInflater.inflate(R.layout.dialog_list, null);
            ListView listView = (ListView) convertView.findViewById(R.id.component_list);
            TextView dialogTitle = (TextView) convertView.findViewById(R.id.dialog_title);
            dialogTitle.setText("Отправить SMS");
            listView.setAdapter(cursorAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Uri uri = ContentUris.withAppendedId(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, id);
                    Cursor result = contentResolver.query(uri, new String[]{ContactsContract.CommonDataKinds.Phone.DATA}, null, null, null);
                    if (result.moveToFirst()) {
                        String str = result.getString(result.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA));
                        new TaskDBAdapter(mContext).insertNewTask(TaskDBAdapter.TYPE_CHECK_SMS_CONTACT, mCheckId, mContactId, str);
                    }
                    dialog.dismiss();
                }
            });
            dialog.setContentView(convertView);
            dialog.setCancelable(false);
            Button positiveButton = (Button) dialog.findViewById(R.id.positive_button);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    dialog.dismiss();
                }
            });
            Button buttonAll = (Button) dialog.findViewById(R.id.buttonAll);
            buttonAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    phones.moveToFirst();
                    if (!phones.isAfterLast()) {
                        do {
                            String str = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA));
                            new TaskDBAdapter(mContext).insertNewTask(TaskDBAdapter.TYPE_CHECK_SMS_CONTACT, mCheckId, mContactId, str);
                        } while (phones.moveToNext());
                    }
                    dialog.dismiss();
                }
            });
            Button buttonNew = (Button) dialog.findViewById(R.id.buttonNew);
            buttonNew.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    createPhoneDialog();
                }
            });
            dialog.show();
        } else {
            createPhoneDialog();
        }
    }

    private void createPhoneDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setMessage("Введите телефон?");
        final EditText input = new EditText(mContext);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        alertDialog.setView(input);
        alertDialog.setIcon(mContext.getResources().getDrawable(R.drawable.messages));
        alertDialog.setPositiveButton("Отправить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!input.getText().toString().isEmpty()) {
                    new TaskDBAdapter(mContext).insertNewTask(TaskDBAdapter.TYPE_CHECK_SMS_CONTACT, mCheckId, mContactId, input.getText().toString());
                    dialog.dismiss();
                }
            }
        });
        alertDialog.setNegativeButton("Закрыть", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        alertDialog.setNeutralButton("Сохранить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!input.getText().toString().isEmpty()) {
                    if (addPhoneToContact(input.getText().toString()) != null)
                        openListPhoneDialog();
                    else
                        Toast.makeText(mContext, "Phone не сохранен нет контакта", Toast.LENGTH_LONG).show();
                }
            }
        });
        alertDialog.show();
    }

    private Uri addPhoneToContact(String phone) {
        Cursor cursor = contentResolver.query(ContactsContract.RawContacts.CONTENT_URI, null, ContactsContract.RawContacts.CONTACT_ID + " = " + mContactId, null, null);
        if (cursor.moveToFirst()) {
            int rawContactId = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
            ContentValues values = new ContentValues();
            values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
            values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone);
            values.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK);
            return contentResolver.insert(ContactsContract.Data.CONTENT_URI, values);
        }
        return null;
    }

    class MyViewBinder implements SimpleCursorAdapter.ViewBinder {
        final Drawable image;

        public MyViewBinder(Drawable i) {
            image = i;
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            switch (view.getId()) {
                // LinearLayout
                case R.id.icon_list:
                    if (view instanceof ImageView)
                        ((ImageView) view).setImageDrawable(image);
                    break;
                case R.id.title:
                    if (view instanceof TextView)
                        ((TextView) view).setText(cursor.getString(columnIndex));
                    break;
                default:
                    return false;
            }
            return true;
        }
    }
}
