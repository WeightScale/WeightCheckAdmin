//Создаёт интерфейс управления весами
package com.kostya.weightcheckadmin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.*;
import android.widget.*;

import java.util.concurrent.TimeUnit;

public class ActivityScales extends Activity implements View.OnClickListener, View.OnLongClickListener {

    private class ThreadConnect extends AsyncTask<Void, Void, Short> { //поток подключения к весам4
        private boolean closed = true;
        AlertDialog.Builder dialog;
        String text_message = "Error";

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
            setTitle(getString(R.string.app_name) + " загрузка настроек..."); //установить заголовок
            dialog = new AlertDialog.Builder(ActivityScales.this);
            dialog.setTitle("Ошибка в настройках");
            dialog.setCancelable(false);
            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    switch (i) {
                        case DialogInterface.BUTTON_POSITIVE:
                            onBackPressed();
                            break;
                        default:
                    }
                }
            });
        }

        @Override
        protected Short doInBackground(Void... voids) {
            //boolean  b = false;
            try {
                if (Scales.vClass.load())
                    return 1;
            } catch (Exception e) {
                text_message = e.getMessage();
                return 0;
            }
            //closed = true;
            return -1;
        }

        @Override
        protected void onPostExecute(final Short result) {
            super.onPostExecute(result);
            closed = true;
            setProgressBarIndeterminateVisibility(false);
            switch (result) {
                case 1:
                    setTitle(getString(R.string.app_name) + " \"" + Scales.getName() + "\", v." + Scales.version); //установить заголовок
                    Preferences.write(ActivityPreferences.KEY_LAST, Scales.getAddress());
                    linearScreen.setVisibility(View.VISIBLE);   //показываем экрана если загрузились настройки весов
                    threadBattery.executeStart();
                    //startService(new Intent(ActivityScales.this, ServiceSendDateServer.class));// Запускаем сервис для передачи данных на google disk
                    startService(new Intent(ActivityScales.this, ServiceGetDateServer.class));// Запускаем сервис для передачи данных на google disk
                    break;
                case 0:
                case -1:
                    dialog.setMessage(text_message);
                    Toast.makeText(getBaseContext(), R.string.preferences_error, Toast.LENGTH_SHORT).show();
                    setTitle(getString(R.string.app_name) + ": " + getString(R.string.preferences_error));
                    dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            switch (i) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    if (Preferences.admin) {
                                        startActivity(new Intent().setClass(getApplicationContext(), ActivityTuning.class));
                                        Toast.makeText(getApplicationContext(), R.string.scales_tuning_no, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(), R.string.preferences_error, Toast.LENGTH_SHORT).show();
                                        onBackPressed();
                                    }
                                    break;
                                default:
                            }
                        }
                    });
                    dialog.show();
                    break;
                default:
            }
        }
    }

    private class ThreadBattery extends AsyncTask<Void, Integer, Void> { //поток получения батареи
        private boolean closed = true;
        private int autoNull; //счётчик автообнуления

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
        protected Void doInBackground(Void... voids) {
            while (!isCancelled()) {
                publishProgress(Scales.readBattery());
                try {
                    TimeUnit.SECONDS.sleep(Scales.DIVIDER_AUTO_NULL);/*Thread.sleep(2000);*/
                } catch (InterruptedException ignored) {
                    closed = true;
                }
                if (Scales.weight != Integer.MIN_VALUE && Math.abs(Scales.weight) < Scales.weightError) { //автоноль
                    autoNull++;
                    if (autoNull > Scales.timerNull / Scales.DIVIDER_AUTO_NULL) {
                        Scales.vClass.setOffsetScale();
                        autoNull = 0;
                    }
                } else
                    autoNull = 0;
            }
            closed = true;
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... result) {
            super.onProgressUpdate(result);

            progressBarBattery.updateProgress(result[0]);
            temperatureProgressBar.updateProgress(Scales.getTemp());
        }
    }

    private ThreadConnect threadConnect;//=new ThreadConnect();
    private final ThreadBattery threadBattery = new ThreadBattery();

    private BroadcastReceiver broadcastReceiver; //приёмник намерений
    private Vibrator vibrator; //вибратор
    private BatteryProgressBar progressBarBattery; //текст батареи
    private TemperatureProgressBar temperatureProgressBar;
    private ImageView imageViewRemote;
    private ImageButton imageButtonDown;
    private TextView textView3;
    private AlertDialog.Builder dialog;
    //private TextView textViewPercentage;
    private LinearLayout linearScreen;//лайаут для экрана показывать когда загрузились настройки
    //public static final String ID_OUTPUT = "отгрузка";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //TODO лог всех событий внизу экрана
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        //requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.scales);
        //getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.windows_title);

        /*icon  = (ImageView) findViewById(R.id.icon);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOptionsMenu();
            }
        });*/

        //setTitle(getString(R.string.app_name) + ": загрузка настроек");
        setProgressBarIndeterminateVisibility(true);

        linearScreen = (LinearLayout) findViewById(R.id.searchScreen);
        linearScreen.setVisibility(View.INVISIBLE);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = 1.0f;
        getWindow().setAttributes(layoutParams);

        dialog = new AlertDialog.Builder(this);// R.style.AlertDialogCustom
        dialog.setMessage("Вы хотите создать отгрузку?");
        dialog.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(getBaseContext(), ActivityContact.class).setAction("up"));
            }
        });
        dialog.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) { //обработчик Bluetooth
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) { //устройство отсоеденено
                        imageViewRemote.setAlpha(90);
                        textView3.setText(getString(R.string.OFF));
                        imageButtonDown.setEnabled(false);
                    } else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) { //найдено соеденено
                        imageViewRemote.setAlpha(255);
                        textView3.setText(getString(R.string.ON));
                        imageButtonDown.setEnabled(true);
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(broadcastReceiver, intentFilter);

        textView3 = (TextView) findViewById(R.id.textView3);
        imageViewRemote = (ImageView) findViewById(R.id.imageViewRemote);
        imageViewRemote.setImageDrawable(getResources().getDrawable(R.drawable.rss));

        imageButtonDown = (ImageButton) findViewById(R.id.imageButtonDown);
        imageButtonDown.setOnLongClickListener(this);
        findViewById(R.id.imageButtonUp).setOnLongClickListener(this);

        progressBarBattery = new BatteryProgressBar(this);
        progressBarBattery = (BatteryProgressBar) findViewById(R.id.progressBarBattery);

        temperatureProgressBar = (TemperatureProgressBar) findViewById(R.id.progressBarTemperature);

        findViewById(R.id.buttonMenu).setOnClickListener(this);
        findViewById(R.id.buttonBack).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonMenu:
                openOptionsMenu();
                break;
            case R.id.buttonBack:
                onBackPressed();
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.imageButtonDown:
                vibrator.vibrate(100);
                threadBattery.autoNull = 0;
                startActivity(new Intent(getBaseContext(), ActivityContact.class).setAction("down"));
                break;
            case R.id.imageButtonUp:
                vibrator.vibrate(100);
                threadBattery.autoNull = 0;
                dialog.show();
                break;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        listCheckSetup();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        threadBattery.cancel(false);
        threadConnect.cancel(true);
        while (!threadBattery.closed) ;
        while (!threadConnect.closed) ;
        Scales.disconnect();
    }

    @Override
    public void onDestroy() { //при разрушении активности
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_scales, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.preferences:
                startActivity(new Intent(this, ActivityPreferences.class));
                break;
            case R.id.exit:
                //onBackPressed();
                closeOptionsMenu();
                break;
            case R.id.type:
                startActivity(new Intent(getBaseContext(), ActivityType.class));
                break;
            case R.id.checks:
                startActivity(new Intent(getBaseContext(), ActivityListChecks.class));
                //startActivity(new Intent(getBaseContext(), ActivityPageChecks.class));
                break;
            case R.id.contact:
                startActivity(new Intent(getBaseContext(), ActivityContact.class).setAction("contact"));
                break;
        }
        return true;
    }

    private void listCheckSetup() {
        ListView listView = (ListView) findViewById(R.id.listViewWeights);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int direct = new CheckDBAdapter(getApplicationContext())
                        .getKeyInt(Integer.valueOf(((TextView) view.findViewById(R.id.check_id)).getText().toString()), CheckDBAdapter.KEY_DIRECT);
                switch (direct) {
                    case CheckDBAdapter.DIRECT_UP:
                        startActivity(new Intent().setClass(getApplicationContext(), ActivityOutputCheck.class).putExtra("id", String.valueOf(l)));
                        break;
                    case CheckDBAdapter.DIRECT_DOWN:
                        startActivity(new Intent().setClass(getApplicationContext(), ActivityInputCheck.class).putExtra("id", String.valueOf(l)));
                        break;
                }
            }
        });
        Cursor cursor = new CheckDBAdapter(getApplicationContext()).getAllNoReadyCheck();
        startManagingCursor(cursor);
        String[] columns = {CheckDBAdapter.KEY_ID,
                CheckDBAdapter.KEY_DATE_CREATE,
                CheckDBAdapter.KEY_TIME_CREATE,
                CheckDBAdapter.KEY_VENDOR,
                CheckDBAdapter.KEY_WEIGHT_GROSS,
                CheckDBAdapter.KEY_WEIGHT_TARE,
                CheckDBAdapter.KEY_WEIGHT_NETTO,
                CheckDBAdapter.KEY_PRICE_SUM, CheckDBAdapter.KEY_DIRECT};
        int[] to = {R.id.check_id, R.id.date, R.id.time, R.id.vendor, R.id.gross_row, R.id.tare_row, R.id.netto_row, R.id.sum_row, R.id.imageDirect};
        SimpleCursorAdapter namesAdapter = new SimpleCursorAdapter(this, R.layout.item_check, cursor, columns, to);
        listView.setAdapter(namesAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        threadConnect = new ThreadConnect();
        if (threadBattery.closed)
            threadConnect.executeStart();
    }


}