package com.kostya.weightcheckadmin;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.IBinder;
//import com.google.gdata.client.docs.DocsService;
//import com.google.gdata.data.docs.DocumentListEntry;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 14.10.13
 * Time: 11:58
 * To change this template use File | Settings | File Templates.
 */
public class ServiceSendDateServer extends Service {
    private final ThreadIsCheck threadIsCheck = new ThreadIsCheck();
    private final ThreadConnectInternet threadConnectInternet = new ThreadConnectInternet();
    private NotificationManager notificationManager;
    private static BroadcastReceiver broadcastReceiver;
    //private static boolean  instance = false;
    private static boolean flagNewData;
    private static final String INTERNET_CONNECT = "internet_connect";
    private static final String INTERNET_DISCONNECT = "internet_disconnect";
    private GoogleSpreadsheets googleSpreadsheets;
    private Internet internet;

    //==================================================================================================================
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //==================================================================================================================
    public class ThreadConnectInternet extends AsyncTask<Void, Long, Void> {
        private boolean closed = true;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            closed = false;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            closed = true;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (googleSpreadsheets == null) {
                try {
                    googleSpreadsheets = new GoogleSpreadsheets(Scales.username, Scales.password, Scales.spreadsheet);
                } catch (RuntimeException ignored) {
                    stopSelf();
                }
                sendBroadcast(new Intent(INTERNET_CONNECT));
                int count = 0, time_wait = 0;
                while (!isCancelled()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignored) {
                    }
                    if (!internet.checkInternetConnection()) {
                        if (time_wait++ > 150) {
                            sendBroadcast(new Intent(INTERNET_DISCONNECT));
                            stopSelf();
                        }
                        continue;
                    }
                    time_wait = 0;
                    if (count++ > 3)                                                         //Колличество больше прекращяем попытки передачи
                        stopSelf();
                    try {
                        googleSpreadsheets.login();
                        googleSpreadsheets.loadSheet(Scales.spreadsheet);
                        googleSpreadsheets.UpdateListWorksheets();
                        if (!flagNewData)
                            sendBroadcast(new Intent(INTERNET_DISCONNECT));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        if (e.getMessage().equals("Invalid credentials"))
                            stopSelf();
                        if (e.getMessage().contains("Нет Таблицы"))
                            stopSelf();
                        if (e.getMessage().contains("Error connecting with login URI"))
                            stopSelf();
                        else
                            continue;
                    }
                    break;
                }
            }
            if (!isCancelled())
                threadIsCheck.execute();
            closed = true;
            return null;
        }
    }

    //==================================================================================================================
    public class ThreadIsCheck extends AsyncTask<Void, Integer, Void> {
        private boolean closed = true;
        final Date dateExecute = new Date();

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
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            while (!isCancelled()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
                if (!flagNewData) {
                    if (dayDiff(new Date(), dateExecute) > 1)                                 //Сколько живет сервис в днях
                        break;//stopForeground(true);//stopSelf();
                    continue;
                }

                sendBroadcast(new Intent(INTERNET_CONNECT));
                int count = 0, time_wait = 0;                                                              //Колличество попыток передать данные
                while (!isCancelled()) {

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignored) {
                    }
                    if (!Internet.flagIsInternet) {//if(!internet.checkInternetConnection()){
                        if (time_wait++ > 150) {
                            break;
                        }
                        continue;
                    }
                    time_wait = 0;
                    if (count++ > 3)                                                         //Колличество больше прекращяем попытки передачи
                        break;
                    if (new CheckDBAdapter(ServiceSendDateServer.this).getCheckServerIsReady())
                        sendIsReadyCheck();
                    if (new CheckDBAdapter(ServiceSendDateServer.this).getCheckServerOldIsReady())
                        sendOldIsReadyCheck();
                    if (new PreferencesDBAdapter(ServiceSendDateServer.this).getPrefServerIsReady())
                        sendPrefToServer();
                    if (isNewDataToServer())
                        continue;
                    flagNewData = false;
                    break;
                }
                sendBroadcast(new Intent(INTERNET_DISCONNECT));
            }
            flagNewData = false;
            closed = true;
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            notificationManager.notify(0, generateNotification(R.drawable.ic_stat_cloud_comment, getString(R.string.Check_send), getString(R.string.Check_N) + ' ' + String.valueOf(values[0]) + ' ' + getString(R.string.sent_to_the_server)));
        }

        //==================================================================================================================
        private void sendPrefToServer() {
            Cursor cursor = new PreferencesDBAdapter(getApplicationContext()).getAllNoCheckServer();
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    do {
                        int id = cursor.getInt(cursor.getColumnIndex(PreferencesDBAdapter.KEY_ID));
                        if (googleSpreadsheets.sendFormGo(cursor, PreferencesDBAdapter.TABLE_PREFERENCES)) {
                            new PreferencesDBAdapter(getApplicationContext()).removeEntry(id);
                            threadIsCheck.onProgressUpdate(id);
                        }
                    } while (cursor.moveToNext());
                }
            }
            cursor.close();
        }

        //==================================================================================================================
        private void sendOldIsReadyCheck() {
            Cursor cursor = new CheckDBAdapter(getApplicationContext()).getNotReady();
            if (cursor.getCount() > 0) {
                Date d = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    do {
                        try {
                            d = dateFormat.parse(cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_DATE_CREATE)));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        String date = cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_DATE_CREATE));
                        try {
                            long day = dayDiff(new Date(), dateFormat.parse(date));
                            if (day >= 2) {

                                if (Internet.sendFormGo(dateFormat.format(d), new SimpleDateFormat("HH:mm:ss").format(d),
                                        String.valueOf(cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_NETTO))),
                                        cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_NUMBER_BT)),
                                        cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_TYPE)),
                                        String.valueOf(cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_IS_READY))))) {

                                    new CheckDBAdapter(getApplicationContext()).updateEntry(cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_ID)), CheckDBAdapter.KEY_CHECK_ON_SERVER, 1);
                                }
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } while (cursor.moveToNext());
                }
            }
            cursor.close();
        }
    }

    //==================================================================================================================
    @Override
    public void onCreate() {
        super.onCreate();
        internet = new Internet(this);
        new CheckDBAdapter(getApplicationContext())
                .deleteCheckIsServer();//Удаляем чеки отправленые на сервер черз n дня
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) { //контроль состояний сетей
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(INTERNET_CONNECT)) {
                        internet.connect();
                    } else if (action.equals(INTERNET_DISCONNECT)) {
                        internet.disconnect();
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(INTERNET_CONNECT);
        filter.addAction(INTERNET_DISCONNECT);
        registerReceiver(broadcastReceiver, filter);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //notificationManager.notify(0,generateNotification(R.drawable.accept_database,"Запуск сервиса", "Сервис запущен"));
        //generateNotification("Запуск сервиса", "Сервис запущен");
        flagNewData = isNewDataToServer();

        threadConnectInternet.execute();
    }

    //==================================================================================================================
    @Override
    public void onDestroy() {
        super.onDestroy();
        //notificationManager.notify(0,generateNotification(R.drawable.accept_database,"Остановка сервиса","Сервис остановлен"));
        threadConnectInternet.cancel(true);
        threadIsCheck.cancel(true);
        while (!threadConnectInternet.closed) ;
        while (!threadIsCheck.closed) ;
        internet.disconnect();
        unregisterReceiver(broadcastReceiver);
        //instance = false;
    }

    //==================================================================================================================
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);
        if (intent != null)
            if (intent.getAction() != null)
                flagNewData = true;
        startForeground(1, generateNotification(R.drawable.scale_launcher, "Весы", "Отправка данных"));
        //return START_REDELIVER_INTENT;
        //return  START_STICKY_COMPATIBILITY;
        //return  START_NOT_STICKY;
        return START_STICKY;
    }
    //==================================================================================================================
    /*public static boolean isInstanceCreated(){
        return instance;
    }*/

    //==================================================================================================================
    void sendIsReadyCheck() {
        Cursor cursor = new CheckDBAdapter(this).getAllNoCheckServerIsReady();
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_ID));

                    if (googleSpreadsheets.sendFormGo(cursor, CheckDBAdapter.TABLE_CHECKS)) {
                        new CheckDBAdapter(this).updateEntry(id, CheckDBAdapter.KEY_CHECK_ON_SERVER, 1);
                        threadIsCheck.onProgressUpdate(id);//publishProgress(id);
                    }
                } while (cursor.moveToNext());
            }
        }
        cursor.close();
    }

    //==================================================================================================================
    long dayDiff(Date d1, Date d2) {
        final long DAY_MILLIS = 1000 * 60 * 60 * 24;//86400000
        long day1 = d1.getTime() / DAY_MILLIS;
        long day2 = d2.getTime() / DAY_MILLIS;
        return day1 - day2;
    }

    //==================================================================================================================
    private Notification generateNotification(int icon, String title, String message) {

        //NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, title, System.currentTimeMillis());
        Intent notificationIntent = new Intent(getApplicationContext(), ActivityApp.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getBaseContext(), 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, "Весы", message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        //notificationManager.notify(0, notification);
        return notification;
    }

    //==================================================================================================================
    boolean isNewDataToServer() {

        return new CheckDBAdapter(this).getCheckServerIsReady()
                || new CheckDBAdapter(this).getCheckServerOldIsReady()
                || new PreferencesDBAdapter(this).getPrefServerIsReady();
    }
}
