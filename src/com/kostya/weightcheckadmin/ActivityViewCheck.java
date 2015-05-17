package com.kostya.weightcheckadmin;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.*;

/*
 * Created by Kostya on 27.12.2014.
 */
public class ActivityViewCheck extends Activity {

    private int entryID;
    ImageView imageViewBack, imageViewMail, imageViewMessage;
    LinearLayout layoutImageView;
    final String[] mColumns = {
            CheckDBAdapter.KEY_ID,
            CheckDBAdapter.KEY_DATE_CREATE,
            CheckDBAdapter.KEY_TIME_CREATE,
            CheckDBAdapter.KEY_VENDOR,
            CheckDBAdapter.KEY_WEIGHT_GROSS,
            CheckDBAdapter.KEY_WEIGHT_TARE,
            CheckDBAdapter.KEY_WEIGHT_NETTO,
            CheckDBAdapter.KEY_TYPE,
            CheckDBAdapter.KEY_PRICE,
            CheckDBAdapter.KEY_PRICE_SUM};
    final int[] mTo = {
            R.id.check_id,
            R.id.date,
            R.id.time,
            R.id.vendor,
            R.id.gross_row,
            R.id.tare_row,
            R.id.netto_row,
            R.id.type_row,
            R.id.price_row,
            R.id.sum_row};
    int[] mFrom;
    //String[] mails = null;
    //InternetAddress[] address = null;
    //StringBuilder stringBuilderBody;
    //StringBuilder builderMail = new StringBuilder();
    //Session session = null;

    Dialog dialog;
    int contactId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        entryID = getIntent().getIntExtra("id", 1);
        setTitle(getString(R.string.Check_N) + ' ' + entryID); //установить заголовок

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        Cursor cursor = new CheckDBAdapter(getApplicationContext()).getEntryItem(entryID);
        if (cursor.getCount() == 0 || !cursor.moveToFirst()) {
            return;
        }
        findColumns(mColumns, cursor);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.page_checks, null);

        imageViewBack = (ImageView) view.findViewById(R.id.imageViewBack);
        imageViewMail = (ImageView) view.findViewById(R.id.imageViewMail);
        imageViewMessage = (ImageView) view.findViewById(R.id.imageViewMessage);
        imageViewBack.setOnClickListener(onClickListener);
        imageViewMail.setOnClickListener(onClickListener);
        imageViewMessage.setOnClickListener(onClickListener);

        layoutImageView = (LinearLayout) view.findViewById(R.id.layoutImageView);
        layoutImageView.setVisibility(View.VISIBLE);

        bindView(view, cursor);
        setContentView(view);

        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ContentQueryMap mQueryMap = new ContentQueryMap(cursor, BaseColumns._ID, true, null);
        Map<String, ContentValues> map = mQueryMap.getRows();
        ContentValues values = map.get(String.valueOf(entryID));

        contactId = values.getAsInteger(CheckDBAdapter.KEY_VENDOR_ID);

        /*stringBuilderBody = new StringBuilder("ВЕСОВОЙ ЧЕК № "+entryID + "\n" + "\n");
        stringBuilderBody.append("Дата: "+ values.getAsString(CheckDBAdapter.KEY_DATE_CREATE)+"  "
                +values.getAsString(CheckDBAdapter.KEY_TIME_CREATE)+"\n");
        stringBuilderBody.append("Контакт:  "+ values.getAsString(CheckDBAdapter.KEY_VENDOR)+"\n");
        stringBuilderBody.append("Брутто:   "+ values.getAsString(CheckDBAdapter.KEY_WEIGHT_GROSS)+"\n");
        stringBuilderBody.append("Тара:     "+ values.getAsString(CheckDBAdapter.KEY_WEIGHT_TARE)+"\n");
        stringBuilderBody.append("Нетто:    "+ values.getAsString(CheckDBAdapter.KEY_WEIGHT_NETTO)+"\n");
        stringBuilderBody.append("Товар:    "+ values.getAsString(CheckDBAdapter.KEY_TYPE)+"\n");
        stringBuilderBody.append("Цена:     "+ values.getAsString(CheckDBAdapter.KEY_PRICE)+"\n");
        stringBuilderBody.append("Сумма:    "+ values.getAsString(CheckDBAdapter.KEY_PRICE_SUM)+"\n");*/

    }

    final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.imageViewBack:
                    onBackPressed();
                    break;
                case R.id.imageViewMail:
                    new TaskMessageDialog(ActivityViewCheck.this, contactId, entryID).openListEmailDialog();
                    break;
                case R.id.imageViewMessage:
                    new TaskMessageDialog(ActivityViewCheck.this, contactId, entryID).openListPhoneDialog();
                    break;
            }
        }
    };

    //Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact);
    //Cursor cursor = getContentResolver().query(contactUri,null,null,null,null);
    //ContentQueryMap mPhoneMap = new ContentQueryMap(cursor, BaseColumns._ID, true, null);
    //Map<String,ContentValues> mapPhones = mPhoneMap.getRows();

        /*Intent intent = new Intent(Intent.ACTION_EDIT);
        //intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, "qqq@www")
                .putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);
        intent.setDataAndType(contactUri,ContactsContract.Contacts.CONTENT_ITEM_TYPE);
        intent.putExtra("finishActivityOnSaveCompleted", true);
        //Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
        startActivity(intent);*/
    //ContentValues newTaskValues = new ContentValues();
    //newTaskValues.put(ContactsContract.CommonDataKinds.Email.CONTACT_ID, contact);
    //newTaskValues.put(ContactsContract.CommonDataKinds.Email.ADDRESS, "ddd@aa");
    //getContentResolver().insert(ContactsContract.CommonDataKinds.Email.CONTENT_URI,newTaskValues);
       /* ContentValues p=new ContentValues();
        p.put(ContactsContract.RawContacts.ACCOUNT_TYPE, "com.google");
        p.put(ContactsContract.RawContacts.ACCOUNT_NAME, "email");
        p.put(ContactsContract.RawContacts.CONTACT_ID, contact);
        Uri rowcontect= getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, p);
        long rawcontectid=ContentUris.parseId(rowcontect);

        ContentValues value = new ContentValues();
        value.put(ContactsContract.Data.RAW_CONTACT_ID,rawcontectid);
        value.put(ContactsContract.Data.CONTACT_ID,contact);
        value.put(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        //value.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, "kunja gajjar");
        getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, value);

        ContentValues ppv=new ContentValues();
        ppv.put(android.provider.ContactsContract.Data.RAW_CONTACT_ID, rawcontectid);
        ppv.put(android.provider.ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        ppv.put(ContactsContract.CommonDataKinds.Phone.NUMBER, "975657789");
        ppv.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        this.getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, ppv);*/

        /*ContentValues p=new ContentValues();
        p.put(ContactsContract.RawContacts.ACCOUNT_TYPE, "com.google");
        p.put(ContactsContract.RawContacts.ACCOUNT_NAME, "email");
        Uri rowcontect= getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, p);
        long rawcontectid=ContentUris.parseId(rowcontect);



        ContentValues value = new ContentValues();
        value.put(ContactsContract.Contacts.Data.RAW_CONTACT_ID,rawContactId);
        value.put(android.provider.ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        value.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, "kunja gajjar");
        getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, value);

        //adding the contents to the data
        ContentValues ppv=new ContentValues();
        ppv.put(ContactsContract.Data.RAW_CONTACT_ID, rawcontectid);
        ppv.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        ppv.put(ContactsContract.CommonDataKinds.Phone.NUMBER, "975657789");
        ppv.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        this.getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, ppv);

        ContentValues epv=new ContentValues();
        epv.put(ContactsContract.Data.RAW_CONTACT_ID, rawcontectid);
        epv.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        epv.put(ContactsContract.CommonDataKinds.Email.ADDRESS, "sss@com");
        epv.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);
        this.getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, epv);*/


    public void bindView(View view, Cursor cursor) {

        final int count = mTo.length;
        final int[] from = mFrom;
        final int[] to = mTo;

        for (int i = 0; i < count; i++) {
            final View v = view.findViewById(to[i]);
            if (v != null) {
                String text = cursor.getString(from[i]);
                if (text == null) {
                    text = "";
                }

                if (v instanceof TextView) {
                    setViewText((TextView) v, text);
                } else if (v instanceof ImageView) {
                    setViewImage((ImageView) v, text);
                } else {
                    throw new IllegalStateException(v.getClass().getName() + " is not a " +
                            " view that can be bounds by this CustomAdapter");
                }
            }
        }
    }

    public void setViewText(TextView v, String text) {
        v.setText(text);
    }

    public void setViewImage(ImageView v, String value) {
        try {
            v.setImageResource(Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            v.setImageURI(Uri.parse(value));
        }
    }

    private void findColumns(String[] from, Cursor cursor) {
        if (cursor != null) {
            int count = from.length;
            if (mFrom == null || mFrom.length != count) {
                mFrom = new int[count];
            }
            for (int i = 0; i < count; i++) {
                mFrom[i] = cursor.getColumnIndexOrThrow(from[i]);
            }
        } else {
            mFrom = null;
        }
    }

    class SendMailTask extends AsyncTask<Void, String, Boolean> {
        final Context context;
        final Session mSession;
        Message mMessage;
        ProgressDialog progressDialog;
        final String mEmail;
        final String mSubject;
        final String mBody;

        SendMailTask(Context cxt, String email, String subject, String messageBody) throws UnsupportedEncodingException, MessagingException {
            context = cxt;
            mEmail = email;
            mSubject = subject;
            mBody = messageBody;
            mSession = createSessionObject();
            mMessage = createMessage(mSession);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(ActivityViewCheck.this, "", "Sending Mail...", true);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Transport.send(mMessage);
            } catch (MessagingException ignored) {
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aVoid) {

            progressDialog.dismiss();
            if (aVoid)
                Toast.makeText(ActivityViewCheck.this, "Message send", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(ActivityViewCheck.this, "Message not send", Toast.LENGTH_LONG).show();
        }

        /*private void sendMail(String email, String subject, String messageBody) {
            Session session = createSessionObject();

            try {
                Message message = createMessage(session);
                new SendMailTask().execute(message);
            } catch (AddressException e) {
                e.printStackTrace();
            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }*/

        private Session createSessionObject() {
            Properties properties = new Properties();
            properties.setProperty("mail.smtp.host", "smtp.gmail.com");
            properties.setProperty("mail.smtp.socketFactory.port", "465");
            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.port", "465");
            properties.put("mail.smtp.timeout", 3000);
            properties.put("mail.smtp.connectiontimeout", 3000);

            return Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("kreogen.lg@gmail.com", "htcehc25");
                }
            });
        }

        private Message createMessage(Session session) throws MessagingException, UnsupportedEncodingException {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("scale", getString(R.string.app_name) + " \"" + Scales.getName()));
            //message.addRecipient(Message.RecipientType.TO, new InternetAddress(email, email));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mEmail));
            //message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(builderMail.toString(),false));
            message.setSubject(mSubject);
            message.setText(mBody);
            return message;
        }
    }

    /*Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + contactId, null, null);
                ContentQueryMap mPhoneMap = new ContentQueryMap(phones, BaseColumns._ID, true, null);
                Map<String,ContentValues> mapPhones = mPhoneMap.getRows();
                if(!mapPhones.isEmpty()){
                    int i = 0;
                    for (Map.Entry<String,ContentValues>entry : mapPhones.entrySet()){
                        new TaskDBAdapter(ActivityViewCheck.this).insertNewTask(TaskDBAdapter.TYPE_CHECK_SMS_CONTACT,entryID,contactId,
                                entry.getValue().getAsString(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    }
                }*/
}
