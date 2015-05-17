//Ищет весы
package com.kostya.weightcheckadmin;

import android.annotation.TargetApi;
import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.*;
import android.widget.*;
import com.kostya.bootloader.ActivityBootloader;

import java.util.*;

public class ActivitySearch extends Activity implements View.OnClickListener {

    private class ThreadBluetooth extends AsyncTask<Void, Void, Void> { //поток ожидания Bluetooth
        private boolean closed = true;

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
            while (!isCancelled() && !bluetooth.isEnabled()) ;
            closed = true;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //button.setEnabled(true);
            //button.setText(R.string.discovery_start);
            linearScreen.setVisibility(View.VISIBLE);
            if (foundDevice.isEmpty()) {
                bluetooth.startDiscovery();
            } else if (foundDevice.size() == 1)
                new ThreadConnect().executeStart((BluetoothDevice) foundDevice.toArray()[0]);
            else if (Preferences.read(ActivityPreferences.KEY_LAST, "").isEmpty())
                log(R.string.error_choice);
            else
                new ThreadConnect().executeStart(bluetooth.getRemoteDevice(Preferences.read(ActivityPreferences.KEY_LAST, "")));
        }
    }

    //==================================================================================================================
    private class ThreadConnect extends AsyncTask<BluetoothDevice, String, Short> { //поток подключения к весам
        // объявляем диалог
        private ProgressDialog dialog;
        private boolean closed = true;
        String deviceName = "";

        public void executeStart(BluetoothDevice... params) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                executePostHoneycomb(params);
            else
                super.execute(params);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private void executePostHoneycomb(BluetoothDevice... params) {
            super.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(ActivitySearch.this);
            //dialog = new ProgressDialog(ActivitySearch.this);
            dialog.setCancelable(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setIcon(R.drawable.scan_network);
            dialog.setTitle(R.string.connect);
            closed = false;
        }

        @Override
        protected Short doInBackground(BluetoothDevice... bluetoothDevice) {
            deviceName = bluetoothDevice[0].getName();
            publishProgress(deviceName);
            if (Scales.connect(bluetoothDevice[0]))
                if (bootloader)
                    return 1;
                else {
                    if (Scales.isScales())
                        return 1;
                    else
                        return -1;
                }
            return 0;
        }

        @Override
        protected void onProgressUpdate(String... name) {
            super.onProgressUpdate(name);
            dialog.setMessage(getString(R.string.connect_to) + '\n' + name[0]);
            dialog.show();
            log(R.string.connect_to, name[0]);
        }

        @Override
        protected void onPostExecute(Short result) {
            switch (result) {
                case 0:
                    Scales.disconnect();
                    log(getString(R.string.connect_no) + ' ' + deviceName);
                    break;
                case -1:
                    Scales.disconnect();
                    log(R.string.version_no);
                    break;
                case 1:
                    if (bootloader) {
                        log("Connect programmerID ");
                        startActivity(new Intent().setClass(getApplicationContext(), ActivityBootloader.class));
                    } else
                        startActivity(new Intent().setClass(getApplicationContext(), ActivityScales.class));
            }
            dialog.dismiss();
            listView.setEnabled(true);
            listView.setOnItemClickListener(onItemClickListener);
            closed = true;
        }
    }

    //==================================================================================================================
    private final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            listView.setEnabled(false);
            bluetooth.cancelDiscovery();
            new ThreadConnect().executeStart((BluetoothDevice) foundDevice.toArray()[i]);
        }
    };
    //==================================================================================================================
    private final ThreadBluetooth threadBluetooth = new ThreadBluetooth();

    private Vibrator vibrator; //вибратор

    private BroadcastReceiver broadcastReceiver; //приёмник намерений
    private BluetoothAdapter bluetooth; //блютуз адаптер
    private ArrayList<BluetoothDevice> foundDevice; //чужие устройства
    private ArrayAdapter<BluetoothDevice> bluetoothAdapter; //адаптер имён
    private IntentFilter intentFilter; //фильтр намерений
    private ListView listView; //список весов
    //private ProgressBar progressBar; //прогресс
    private TextView textViewLog; //лог событий
    private LinearLayout linearScreen;//лайаут для экрана показывать когда загрузились настройки

    public static int versionNumber;
    public static String versionName;
    //static boolean flag_connect = false;
    static boolean bootloader = false;

    private boolean doubleBackToExitPressedOnce;

    //==================================================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*Configuration  config = new Configuration(getResources().getConfiguration());
        config.locale = Locale.ENGLISH ;
        getResources().updateConfiguration(config,getResources().getDisplayMetrics());*/
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        ActivityApp.telephoneNumber = telephonyManager.getLine1Number();
        ActivityApp.simNumber = telephonyManager.getSimSerialNumber();
        ActivityApp.networkOperatorName = telephonyManager.getNetworkOperatorName();
        ActivityApp.networkCountry = telephonyManager.getNetworkCountryIso();
        int state = telephonyManager.getSimState();
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        switch (state) {
            case TelephonyManager.SIM_STATE_READY:
                if (bluetooth == null) {
                    Toast.makeText(getBaseContext(), R.string.bluetooth_no, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    setupScale();
                }
                break;
            default:
                Toast.makeText(getBaseContext(), R.string.telephony_sim_no, Toast.LENGTH_LONG).show();
                finish();
        }
    }

    //==================================================================================================================
    private void exit() {
        threadBluetooth.cancel(true);
        while (!threadBluetooth.closed) ;
        if (bluetooth.isDiscovering())
            bluetooth.cancelDiscovery();
        unregisterReceiver(broadcastReceiver);
        Scales.disconnect();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            Set<String> def = new HashSet<String>();
            def = Preferences.read(ActivityPreferences.KEY_DEVICES, def);
            if (!def.isEmpty())
                def.clear();

            for (BluetoothDevice aFoundDevice : foundDevice) {
                def.add(aFoundDevice.getAddress());
            }
            Preferences.write(ActivityPreferences.KEY_DEVICES, def);
        } else {
            for (int i = 0; Preferences.contains(ActivityPreferences.KEY_ADDRESS + i); i++) //стереть прошлый список
                Preferences.remove(ActivityPreferences.KEY_ADDRESS + i);
            for (int i = 0; i < foundDevice.size(); i++) //сохранить новый список
                Preferences.write(ActivityPreferences.KEY_ADDRESS + i, ((BluetoothDevice) foundDevice.toArray()[i]).getAddress());
        }
        bluetooth.disable();
        while (bluetooth.isEnabled()) ;
        sendBroadcast(new Intent(ServiceGetDateServer.CLOSED_SCALE));
        finish();
    }

    //==================================================================================================================
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            exit();
            return;
        }
        bluetooth.cancelDiscovery();
        doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.press_again_to_exit /*Please click BACK again to exit*/, Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;

            }
        }, 2000);
    }

    //==================================================================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_search, menu);
        return true;
    }

    //==================================================================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.checks_all:
                startActivity(new Intent(this, ActivityListChecks.class));
                break;
            case R.id.search:
                registerReceiver(broadcastReceiver, new IntentFilter());
                unregisterReceiver(broadcastReceiver);
                registerReceiver(broadcastReceiver, intentFilter);
                bluetooth.startDiscovery();
                break;
            case R.id.exit:
                exit();
                break;
        }
        return true;
    }

    //==================================================================================================================
    void log(int resource) { //для ресурсов
        textViewLog.setText(getString(resource) + '\n' + textViewLog.getText());
    }

    //==================================================================================================================
    public void log(String string) { //для текста
        textViewLog.setText(string + '\n' + textViewLog.getText());
    }

    //==================================================================================================================
    void log(int resource, boolean toast) { //для текста
        textViewLog.setText(getString(resource) + '\n' + textViewLog.getText());
        if (toast)
            Toast.makeText(getBaseContext(), resource, Toast.LENGTH_SHORT).show();
    }

    //==================================================================================================================
    void log(int resource, String str) { //для ресурсов с текстовым дополнением
        textViewLog.setText(getString(resource) + ' ' + str + '\n' + textViewLog.getText());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void setupScale() {
        /*Window window = getWindow();
        window.requestFeature(Window.FEATURE_CUSTOM_TITLE);*/
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.search);

        setProgressBarIndeterminateVisibility(true);

        /*window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_progress_bar);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.status_progress);
        progressBar.setVisibility(View.VISIBLE);*/

        linearScreen = (LinearLayout) findViewById(R.id.searchScreen);
        linearScreen.setVisibility(View.INVISIBLE);

        bootloader = getIntent().getBooleanExtra("bootloader", false);
        textViewLog = (TextView) findViewById(R.id.textLog);

        Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME, 1);       //Включаем автообновления дата время
        Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME_ZONE, 1);  //Включаем автообновления дата время

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo != null) {
            if (networkInfo.isAvailable()) //Если используется
                new Internet(this).turnOnWiFiConnection(false); // для телефонов у которых один модуль wifi и bluetooth
        }

        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) { //обработчик Bluetooth
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                        if (bluetooth.getState() == BluetoothAdapter.STATE_OFF) {
                            log(R.string.bluetooth_off);
                            bluetooth.enable();
                        } else if (bluetooth.getState() == BluetoothAdapter.STATE_TURNING_ON) {
                            log(R.string.bluetooth_turning_on, true);
                        } else if (bluetooth.getState() == BluetoothAdapter.STATE_ON) {
                            log(R.string.bluetooth_on, true);
                        }
                    } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {//поиск начался
                        log(R.string.discovery_started);
                        foundDevice.clear();
                        bluetoothAdapter.notifyDataSetChanged();
                        setTitle(getString(R.string.discovery_started)); //установить заголовок
                        setProgressBarIndeterminateVisibility(true);
                    } else if (action.equals(BluetoothDevice.ACTION_FOUND)) { //найдено устройство
                        BluetoothDevice bd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        foundDevice.add(bd);
                        bluetoothAdapter.notifyDataSetChanged();
                        String name = null;
                        if (bd != null) {
                            name = bd.getName();
                        }
                        if (name != null)
                            log(R.string.device_found, name);
                    } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) { //устройство отсоеденено
                        vibrator.vibrate(200);
                        log(R.string.bluetooth_disconnected);
                    } else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) { //найдено соеденено
                        vibrator.vibrate(200);
                        log(R.string.bluetooth_connected);
                    } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) { //поиск завершён
                        setTitle(getString(R.string.app_name) + " \"" + versionName + "\", v." + versionNumber); //установить заголовок
                        setProgressBarIndeterminateVisibility(false);
                    }
                }
            }
        };

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(broadcastReceiver, intentFilter);

        if (bluetooth != null)
            if (!bluetooth.isEnabled()) {
                log(R.string.bluetooth_off, true);
                bluetooth.enable();
            } else
                log(R.string.bluetooth_on, true);
        //}

        PackageInfo packageInfo = null;
        try {
            PackageManager packageManager = getPackageManager();
            if (packageManager != null)
                packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            log(e.getMessage());
        }

        if (packageInfo != null)
            versionNumber = packageInfo.versionCode;
        if (packageInfo != null)
            versionName = packageInfo.versionName;

        setTitle(getString(R.string.app_name) + " \"" + versionName + "\", v." + versionNumber); //установить заголовок
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        Preferences.load(getSharedPreferences(Preferences.PREFERENCES, Context.MODE_PRIVATE)); //загрузить настройки

        foundDevice = new ArrayList<BluetoothDevice>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            Set<String> def = new HashSet<String>();
            def = Preferences.read(ActivityPreferences.KEY_DEVICES, def);
            String define_device = Preferences.read(ActivityPreferences.KEY_LAST, "");
            if (!define_device.isEmpty())
                foundDevice.add(bluetooth.getRemoteDevice(define_device));
            if (!def.isEmpty()) {
                for (String str : def) {
                    if (!str.equals(define_device))
                        foundDevice.add(bluetooth.getRemoteDevice(str));
                }
            }
        } else {
            for (int i = 0; Preferences.contains(ActivityPreferences.KEY_ADDRESS + i); i++) //заполнение списка
                foundDevice.add(bluetooth.getRemoteDevice(Preferences.read(ActivityPreferences.KEY_ADDRESS + i, "")));
        }
        bluetoothAdapter = new BluetoothListAdapter(this, foundDevice);
        //bluetoothAdapter.notifyDataSetChanged(); TODO

        findViewById(R.id.buttonMenu).setOnClickListener(this);
        findViewById(R.id.buttonSearchBluetooth).setOnClickListener(this);
        findViewById(R.id.buttonBack).setOnClickListener(this);

        listView = (ListView) findViewById(R.id.listViewDevices);  //список весов
        listView.setAdapter(bluetoothAdapter);
        listView.setOnItemClickListener(onItemClickListener);

        threadBluetooth.executeStart();
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
            case R.id.buttonSearchBluetooth:
                registerReceiver(broadcastReceiver, new IntentFilter());
                unregisterReceiver(broadcastReceiver);
                registerReceiver(broadcastReceiver, intentFilter);
                bluetooth.startDiscovery();
                break;
        }
    }
}