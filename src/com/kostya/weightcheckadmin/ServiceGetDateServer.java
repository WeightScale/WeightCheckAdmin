package com.kostya.weightcheckadmin;

import android.annotation.TargetApi;
import android.app.*;
import android.app.Service;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import com.gc.android.market.api.MarketSession;
import com.gc.android.market.api.model.Market;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import android.provider.Settings.Secure;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 14.10.13
 * Time: 11:58
 * To change this template use File | Settings | File Templates.
 */
public class ServiceGetDateServer extends Service {
    private final ThreadIsCheck threadIsCheck = new ThreadIsCheck();
    private static BroadcastReceiver broadcastReceiver;
    private NotificationManager notificationManager;
    //private static boolean instance = false;
    //public static boolean flagNewTask;
    String androidId;
    private static boolean flagNewVersion = false;
    private static final String INTERNET_CONNECT = "internet_connect";
    private static final String INTERNET_DISCONNECT = "internet_disconnect";
    public static final String CLOSED_SCALE = "closed_scale";
    private Internet internet;
    private static final int NUM_TIME_WAIT = 150;

    //==================================================================================================================
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //==================================================================================================================
    public class ThreadIsCheck extends AsyncTask<Void, Integer, Void> {
        final Date dateExecute = new Date();
        private boolean closed = true;
        int time_wait;

        public void executeStart(Void... params) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                executePostHoneycomb(params);
            else
                super.execute(params);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private void executePostHoneycomb(Void... params) {
            super.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            closed = false;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stopSelf();
            closed = true;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            while (!isCancelled()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
                if (!isTaskReady()) {
                    if (dayDiff(new Date(), dateExecute) > 1)                                 //Сколько живет сервис в днях
                        break;//stopForeground(true);//stopSelf();
                    continue;
                }

                /*sendBroadcast(new Intent(INTERNET_CONNECT));
                time_wait = 0;*/
                int count = 0;                                             //Колличество попыток передать данные
                while (!isCancelled()) {

                    /*try {Thread.sleep(200);} catch(InterruptedException ignored) {}
                    if(!Internet.flagIsInternet){
                        if(time_wait++ > NUM_TIME_WAIT ){
                            break;
                        }
                        continue;
                    }
                    time_wait = 0;*/
                    if (count++ > 3)                                                         //Колличество больше прекращяем попытки передачи
                        break;
                    if (!getConnection(1000, 10))
                        continue;

                    processingTasks();                          //выполняем задачи

                    if (!flagNewVersion)
                        flagNewVersion = isNewVersion();//todo эта стока для проверки новой версии программы user должен dev market

                    oldCheckSetReady();                          //не закрытые чеки закрыть по условию даты

                    new CheckDBAdapter(ServiceGetDateServer.this)
                            .invisibleCheckIsReady(Integer.valueOf(getSharedPreferences(Preferences.PREFERENCES, Context.MODE_PRIVATE)
                                    .getString(ActivityPreferences.KEY_DAY_CHECK_DELETE, "5")));  //Скрываем чеки закрытые через n дней

                    new CheckDBAdapter(ServiceGetDateServer.this).deleteCheckIsServer();  //Удаляем чеки отправленые на сервер через n дней

                    if (isTaskReady())
                        continue;

                    //flagNewTask = false;
                    break;
                }
                sendBroadcast(new Intent(INTERNET_DISCONNECT));
            }
            //flagNewTask = false;
            closed = true;
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            notificationManager.notify(0, generateNotification(R.drawable.ic_stat_cloud_comment, "Чек отправлен", "Чека № " + values[0] + " отправлен на сервер"));
        }

        //==============================================================================================================
        private boolean getConnection(int timeout, int countConnect) {
            sendBroadcast(new Intent(INTERNET_CONNECT));
            int count = 0;
            while (!isCancelled()) {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException ignored) {
                }
                if (!Internet.flagIsInternet) {
                    if (count++ > countConnect) {
                        break;
                    }
                    continue;
                }
                return true;
            }
            return false;
        }

        //==============================================================================================================
        private void processingTasks() {
            Cursor cursor = new TaskDBAdapter(getApplicationContext()).getAllEntries();
            if (cursor.moveToFirst()) {
                if (!cursor.isAfterLast()) {
                    do {
                        int mimeType = cursor.getInt(cursor.getColumnIndex(TaskDBAdapter.KEY_MIME_TYPE));
                        int taskId = cursor.getInt(cursor.getColumnIndex(TaskDBAdapter.KEY_ID));
                        int checkId = cursor.getInt(cursor.getColumnIndex(TaskDBAdapter.KEY_DOC));
                        switch (mimeType) {
                            case TaskDBAdapter.TYPE_CHECK_DISK:
                                if (sendCheckToDisk(checkId)) {
                                    new TaskDBAdapter(getApplicationContext()).removeEntry(taskId);
                                }
                                break;
                            case TaskDBAdapter.TYPE_PREF_DISK:
                                if (sendPrefToDisk(checkId)) {
                                    new TaskDBAdapter(getApplicationContext()).removeEntry(taskId);
                                }
                                break;
                            case TaskDBAdapter.TYPE_CHECK_MAIL_CONTACT:
                            case TaskDBAdapter.TYPE_CHECK_SMS_CONTACT:
                                String address = cursor.getString(cursor.getColumnIndex(TaskDBAdapter.KEY_DATA1));
                                StringBuilder body = new StringBuilder("ВЕСОВОЙ ЧЕК № " + checkId + '\n' + '\n');
                                Cursor check = new CheckDBAdapter(getApplicationContext()).getEntryItem(checkId);
                                if (check == null)
                                    body.append("Нет данных чек №").append(checkId).append(" удален");
                                else {
                                    if (check.moveToFirst()) {
                                        body.append("Дата:_").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_DATE_CREATE))).append("__").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_TIME_CREATE))).append('\n');
                                        body.append("Контакт:__").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_VENDOR))).append('\n');
                                        body.append("Брутто:___").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_GROSS))).append('\n');
                                        body.append("Тара:_____").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_TARE))).append('\n');
                                        body.append("Нетто:____").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_NETTO))).append('\n');
                                        body.append("Товар:____").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_TYPE))).append('\n');
                                        body.append("Цена:_____").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_PRICE))).append('\n');
                                        body.append("Сумма:____").append(check.getString(check.getColumnIndex(CheckDBAdapter.KEY_PRICE_SUM))).append('\n');
                                    } else
                                        body.append("Нет данных чек №").append(checkId).append(" удален");
                                }
                                switch (mimeType) {
                                    case TaskDBAdapter.TYPE_CHECK_MAIL_CONTACT:
                                        MailSend mail = new MailSend(getApplicationContext(), address, "Чек №" + checkId, body.toString());
                                        if (mail.sendMail()) {
                                            new TaskDBAdapter(getApplicationContext()).removeEntry(taskId);
                                        }
                                        break;
                                    case TaskDBAdapter.TYPE_CHECK_SMS_CONTACT:
                                        if (sendSMS(address, body.toString()))
                                            new TaskDBAdapter(getApplicationContext()).removeEntry(taskId);
                                        break;
                                }
                                break;
                        }
                    } while (cursor.moveToNext());
                }
            }
        }

        //==============================================================================================================
        private void oldCheckSetReady() {
            Cursor cursor = new CheckDBAdapter(getApplicationContext()).getNotReady();
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    do {
                        String date = cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_DATE_CREATE));
                        try {
                            long day = dayDiff(new Date(), new SimpleDateFormat("dd.MM.yy").parse(date));
                            if (day >= Integer.valueOf(getSharedPreferences(Preferences.PREFERENCES, Context.MODE_PRIVATE).getString(getString(R.string.KEY_DAY_CLOSED_CHECK), "5"))) {
                                int id = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_ID));

                                new CheckDBAdapter(ServiceGetDateServer.this).updateEntry(id, CheckDBAdapter.KEY_IS_READY, 1);
                                threadIsCheck.onProgressUpdate(id);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } while (cursor.moveToNext());
                }
            }
            cursor.close();
        }

        //==============================================================================================================
        private boolean sendCheckToDisk(int doc) {
            Cursor cursor = new CheckDBAdapter(getApplicationContext()).getEntryItem(doc);
            if (cursor == null)
                return true;
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_ID));

                List<NameValuePair> results = new ArrayList<NameValuePair>();
                results.add(new BasicNameValuePair(Internet.go_date_param_http, cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_DATE_CREATE))));
                results.add(new BasicNameValuePair(Internet.go_bt_param_http, cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_NUMBER_BT))));
                results.add(new BasicNameValuePair(Internet.go_weight_param_http, String.valueOf(cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_NETTO)))));
                results.add(new BasicNameValuePair(Internet.go_type_param_http, cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_TYPE))));
                results.add(new BasicNameValuePair(Internet.go_is_ready_param_http, String.valueOf(cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_IS_READY)))));
                results.add(new BasicNameValuePair(Internet.go_time_param_http, cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_TIME_CREATE))));

                if (Internet.submitData(Internet.go_form_http, results)) {
                    new CheckDBAdapter(ServiceGetDateServer.this).updateEntry(id, CheckDBAdapter.KEY_CHECK_ON_SERVER, 1);
                    threadIsCheck.onProgressUpdate(id);
                    return true;
                }
            }
            return false;
        }

        //==============================================================================================================
        private boolean sendPrefToDisk(int doc) {
            Cursor cursor = new PreferencesDBAdapter(getApplicationContext()).getEntryItem(doc);
            if (cursor == null)
                return true;
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(PreferencesDBAdapter.KEY_ID));

                List<NameValuePair> results = new ArrayList<NameValuePair>();
                results.add(new BasicNameValuePair(Internet.pref_date_param_http, cursor.getString(cursor.getColumnIndex(PreferencesDBAdapter.KEY_DATE_CREATE))));
                results.add(new BasicNameValuePair(Internet.pref_bt_param_http, String.valueOf(cursor.getString(cursor.getColumnIndex(PreferencesDBAdapter.KEY_NUMBER_BT)))));
                results.add(new BasicNameValuePair(Internet.pref_coeff_a_param_http, cursor.getString(cursor.getColumnIndex(PreferencesDBAdapter.KEY_COEFFICIENT_A))));
                results.add(new BasicNameValuePair(Internet.pref_coeff_b_param_http, cursor.getString(cursor.getColumnIndex(PreferencesDBAdapter.KEY_COEFFICIENT_B))));
                results.add(new BasicNameValuePair(Internet.pref_max_weight_param_http, cursor.getString(cursor.getColumnIndex(PreferencesDBAdapter.KEY_MAX_WEIGHT))));
                results.add(new BasicNameValuePair(Internet.pref_filter_adc_param_http, cursor.getString(cursor.getColumnIndex(PreferencesDBAdapter.KEY_FILTER_ADC))));
                results.add(new BasicNameValuePair(Internet.pref_step_scale_param_http, cursor.getString(cursor.getColumnIndex(PreferencesDBAdapter.KEY_STEP_SCALE))));
                results.add(new BasicNameValuePair(Internet.pref_step_capture_param_http, cursor.getString(cursor.getColumnIndex(PreferencesDBAdapter.KEY_STEP_CAPTURE))));
                results.add(new BasicNameValuePair(Internet.pref_time_off_param_http, cursor.getString(cursor.getColumnIndex(PreferencesDBAdapter.KEY_TIME_OFF))));
                results.add(new BasicNameValuePair(Internet.pref_bt_terminal_param_http, cursor.getString(cursor.getColumnIndex(PreferencesDBAdapter.KEY_NUMBER_BT_TERMINAL))));

                if (Internet.submitData(Internet.pref_form_http, results)) {
                    new PreferencesDBAdapter(getApplicationContext()).removeEntry(id);
                    threadIsCheck.onProgressUpdate(id);
                    return true;
                }
            }
            return false;
        }

        //==============================================================================================================
        private boolean sendSMS(String phoneNumber, String message) {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(message);
            try {
                sms.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            } catch (RuntimeException ignored) {
                return false;
            }
            return true;
        }
    }

    //==================================================================================================================
    @Override
    public void onCreate() {
        super.onCreate();
        //instance = true;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        internet = new Internet(this);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) { //контроль состояний сетей
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(INTERNET_CONNECT)) {
                        internet.connect();
                    } else if (action.equals(INTERNET_DISCONNECT)) {
                        if (!flagNewVersion)
                            internet.disconnect();
                    } else if (action.equals(CLOSED_SCALE)) {
                        threadIsCheck.time_wait = NUM_TIME_WAIT;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(INTERNET_CONNECT);
        filter.addAction(INTERNET_DISCONNECT);
        filter.addAction(CLOSED_SCALE);
        registerReceiver(broadcastReceiver, filter);

        //flag_new_data = isNewDataToServer();
        androidId = getAndroidId(getApplicationContext());
        threadIsCheck.executeStart();
    }

    //==================================================================================================================
    @Override
    public void onDestroy() {
        super.onDestroy();
        threadIsCheck.cancel(true);
        while (!threadIsCheck.closed) ;
        internet.disconnect();
        unregisterReceiver(broadcastReceiver);
        //db.close();
        //instance = false;

    }

    //==================================================================================================================
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null)
            //if(intent.getAction()!=null)
            //flagNewTask = isTaskReady();
            startForeground(1, generateNotification(R.drawable.ic_stat_database, "Запускае сервис отправки данных", "Отправляем данные на сервер"));
        return START_STICKY;
    }


    /*//==================================================================================================================
    boolean isNoReadyCheck(){
        Cursor cursor = new CheckDBAdapter(getApplicationContext()).getNotReady();
        if(cursor.getCount()>0){
            cursor.moveToFirst();
            if (!cursor.isAfterLast()){
                do {
                    String date = cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_DATE_CREATE));
                    try {
                        long day = dayDiff(new Date(), new SimpleDateFormat("dd.MM.yy").parse(date));
                        if(day >= Integer.valueOf(getSharedPreferences(Preferences.PREFERENCES,Context.MODE_PRIVATE).getString(getString(R.string.KEY_DAY_CLOSED_CHECK), "5"))){
                            return true;
                        }
                    } catch (ParseException ignored) {
                        return true;
                    }
                }while (cursor.moveToNext());
            }
        }
        cursor.close();
        return false;
    }*/

    //==================================================================================================================
    long dayDiff(Date d1, Date d2) {
        final long DAY_MILLIS = 1000 * 60 * 60 * 24;
        long day1 = d1.getTime() / DAY_MILLIS;
        long day2 = d2.getTime() / DAY_MILLIS;
        return day1 - day2;
    }

    //==================================================================================================================
    private Notification generateNotification(int icon, String title, String message) {
        Notification notification = new Notification(icon, title, System.currentTimeMillis());
        Intent notificationIntent = new Intent(getApplicationContext(), ActivityListChecks.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getBaseContext(), 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getString(R.string.app_name), message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        //notificationManager.notify(0, notification);
        return notification;
    }

    //==================================================================================================================
    /*boolean isNewDataToServer(){
        return new CheckDBAdapter(ServiceGetDateServer.this).getCheckServerIsReady() || isNoReadyCheck() || new PreferencesDBAdapter(ServiceGetDateServer.this).getPrefServerIsReady();
    }*/
    //==================================================================================================================
    boolean isTaskReady() {
        return new TaskDBAdapter(this).isTaskReady();
    }

    //==================================================================================================================
    String getAndroidId(Context ctx) {
        String[] params = {Secure.ANDROID_ID};
        Cursor c = ctx.getContentResolver().query(Uri.parse("content://com.google.android.gsf.gservices"), null, null, params, null);
        if (c == null)
            return null;
        if (!c.moveToFirst() || c.getColumnCount() < 2)
            return null;
        try {
            return Long.toHexString(Long.parseLong(c.getString(1)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    //==================================================================================================================
    boolean isNewVersion() {
        try {
            MarketSession session = new MarketSession();
            session.login(Scales.username, Scales.password);    // пароль для маркет develop market
            session.getContext().setAndroidId(androidId);       //"37489D258B2193D5"
            String query = "pname:" + getPackageName();
            Market.AppsRequest appsRequest = Market.AppsRequest.newBuilder()
                    .setQuery(query)
                    .setStartIndex(0).setEntriesCount(1)
                    .setWithExtendedInfo(false)
                    .build();

            session.append(appsRequest, new MarketSession.Callback<Market.AppsResponse>() {
                @Override
                public void onResult(Market.ResponseContext context, Market.AppsResponse response) {
                    String v = response.getApp(0).getVersion();
                    // Your code here
                    // response.getApp(0).getCreator() ...
                    // see AppsResponse class definition for more info
                }
            });
            session.flush();
            Market.Request.RequestGroup requestGroup = Market.Request.RequestGroup.newBuilder().setAppsRequest(appsRequest).build();
            Market.Response.ResponseGroup responseGroup = session.execute(requestGroup);
            Market.AppsResponse response = responseGroup.getAppsResponse();
            int newVersion = response.getApp(0).getVersionCode();
            Application application = getApplication();
            if (application == null)
                return false;
            PackageManager packageManager = getApplication().getPackageManager();
            if (packageManager == null)
                return false;
            int curVersion = packageManager.getPackageInfo(getPackageName(), 0).versionCode;
            return (curVersion < newVersion);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //==================================================================================================================
    class MailSend {
        final Context mContext;
        final String mEmail;
        final String mSubject;
        final String mBody;
        //StringBuilder stringBuilderBody;

        MailSend(Context cxt, String email, String subject, String messageBody) {
            mContext = cxt;
            mEmail = email;
            mSubject = subject;
            mBody = messageBody;
        }

        private boolean sendMail() {
            Session session = createSessionObject();

            try {
                Message message = createMessage(mSubject, mBody, session);
                Transport.send(message);
                return true;
                //new SendMailTask().execute(message);
            } catch (AddressException ignored) {
                return true;
            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return false;
        }

        private Session createSessionObject() {
            Properties properties = new Properties();
            properties.setProperty("mail.smtp.host", "smtp.gmail.com");
            properties.setProperty("mail.smtp.socketFactory.port", "465");
            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.port", "465");
            properties.put("mail.smtp.timeout", 10000);
            properties.put("mail.smtp.connectiontimeout", 10000);

            return Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(Scales.username, Scales.password);
                }
            });
        }

        private Message createMessage(String subject, String messageBody, Session session) throws MessagingException, UnsupportedEncodingException {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("scale", getString(R.string.app_name) + " \"" + Scales.getName()));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(mEmail, mEmail));
            //message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mEmail));
            //message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(builderMail.toString(),false));
            message.setSubject(subject);
            message.setText(messageBody);
            return message;
        }

    }

}
