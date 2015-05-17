package com.kostya.weightcheckadmin;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import com.kostya.weightcheckadmin.provider.CheckDBAdapter;

import java.util.*;

/*
 * Created by Kostya on 27.12.2014.
 */
public class ActivityViewCheck extends Activity implements View.OnClickListener {

    private int entryID;
    //private LinearLayout layoutImageView;
    private final String[] mColumns = {
            CheckDBAdapter.KEY_ID,
            CheckDBAdapter.KEY_DATE_CREATE,
            CheckDBAdapter.KEY_TIME_CREATE,
            CheckDBAdapter.KEY_VENDOR,
            CheckDBAdapter.KEY_WEIGHT_FIRST,
            CheckDBAdapter.KEY_WEIGHT_SECOND,
            CheckDBAdapter.KEY_WEIGHT_NETTO,
            CheckDBAdapter.KEY_TYPE,
            CheckDBAdapter.KEY_PRICE,
            CheckDBAdapter.KEY_PRICE_SUM,
            CheckDBAdapter.KEY_NUMBER_BT,
            CheckDBAdapter.KEY_DIRECT, CheckDBAdapter.KEY_DIRECT, CheckDBAdapter.KEY_DIRECT};
    private final int[] mTo = {
            R.id.check_id,
            R.id.date,
            R.id.time,
            R.id.vendor,
            R.id.gross_row,
            R.id.tare_row,
            R.id.netto_row,
            R.id.type_row,
            R.id.price_row,
            R.id.sum_row,
            R.id.textNumScale,
            R.id.imageDirect, R.id.gross, R.id.tare};
    private int[] mFrom;

    private int contactId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        entryID = getIntent().getIntExtra("id", 1);
        setTitle(getString(R.string.Check_N) + ' ' + entryID); //установить заголовок

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        Cursor cursor = new CheckDBAdapter(getApplicationContext()).getEntryItem(entryID);
        if (cursor == null) {
            return;
        }
        if (cursor.getCount() == 0 || !cursor.moveToFirst()) {
            return;
        }
        findColumns(mColumns, cursor);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.page_checks, null);
        ((TextView) view.findViewById(R.id.textNumTerminal)).setText(BluetoothAdapter.getDefaultAdapter().getAddress());
        view.findViewById(R.id.imageViewBack).setOnClickListener(this);
        view.findViewById(R.id.imageViewMail).setOnClickListener(this);
        view.findViewById(R.id.imageViewMessage).setOnClickListener(this);
        //layoutImageView = (LinearLayout)view.findViewById(R.id.layoutImageView);
        //layoutImageView.setVisibility(View.VISIBLE);

        bindView(view, cursor);
        setContentView(view);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ContentQueryMap mQueryMap = new ContentQueryMap(cursor, BaseColumns._ID, true, null);
        Map<String, ContentValues> map = mQueryMap.getRows();
        ContentValues values = map.get(String.valueOf(entryID));

        contactId = values.getAsInteger(CheckDBAdapter.KEY_VENDOR_ID);
    }

    /*@Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
    }*/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageViewBack:
                onBackPressed();
                break;
            case R.id.imageViewMail:
                new TaskMessageDialog(this, contactId, entryID).openListEmailDialog();
                break;
            case R.id.imageViewMessage:
                new TaskMessageDialog(this, contactId, entryID).openListPhoneDialog();
                break;
        }
    }

    public void bindView(View view, Cursor cursor) {

        final int count = mTo.length;
        final int[] from = mFrom;
        final int[] to = mTo;
        int direct = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_DIRECT));

        for (int i = 0; i < count; i++) {
            final View v = view.findViewById(to[i]);
            if (v != null) {

                switch (v.getId()) {
                    case R.id.gross:
                        if (direct == CheckDBAdapter.DIRECT_UP) {
                            setViewText((TextView) v, getString(R.string.Tape));
                        } else {
                            setViewText((TextView) v, getString(R.string.Gross));
                        }
                        break;
                    case R.id.tare:
                        if (direct == CheckDBAdapter.DIRECT_DOWN) {
                            setViewText((TextView) v, getString(R.string.Tape));
                        } else {
                            setViewText((TextView) v, getString(R.string.Gross));
                        }
                        break;
                    default:
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
    }

    public void setViewText(TextView v, CharSequence text) {
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

    /*private class SendMailThread extends Thread{
        protected final Context context;
        protected final Session mSession;
        protected Message mMessage;
        protected ProgressDialog progressDialog;
        protected final String mEmail;
        protected final String mSubject;
        protected final String mBody;

        SendMailThread(Context cxt, String email, String subject, String messageBody) throws UnsupportedEncodingException, MessagingException {
            context = cxt;
            mEmail = email;
            mSubject = subject;
            mBody = messageBody;
            mSession = createSessionObject();
            mMessage = createMessage(mSession);
            progressDialog = ProgressDialog.show(ActivityViewCheck.this,"","Sending Mail...",true);
        }

        @Override
        public void run() {
            boolean aVoid = true;
            try {
                Transport.send(mMessage);
            }catch (MessagingException ignored){
                aVoid = false;
            }

            progressDialog.dismiss();
            if(aVoid)
                Toast.makeText(context, "Message send", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(context, "Message not send", Toast.LENGTH_LONG).show();
        }

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
                    return new PasswordAuthentication("kreogen.lg@gmail.com","htcehc25");
                }
            });
        }

        private Message createMessage(Session session) throws MessagingException, UnsupportedEncodingException {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("scale",getString(R.string.app_name) + " \"" + Scales.getName()));
            //message.addRecipient(Message.RecipientType.TO, new InternetAddress(email, email));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mEmail));
            //message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(builderMail.toString(),false));
            message.setSubject(mSubject);
            message.setText(mBody);
            return message;
        }
    }*/
}
