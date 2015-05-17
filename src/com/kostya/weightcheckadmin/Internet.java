//Управляет соединениями (Bluetooth, Wi-Fi, мобильная сеть)
package com.kostya.weightcheckadmin;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.util.List;

class Internet {
    private final Context context;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    static final String go_form_http = "https://docs.google.com/forms/d/11C5mq1Z-Syuw7ScsMlWgSnr9yB4L_eP-NhxnDdohtrw/formResponse"; // Форма движения
    static final String go_date_param_http = "entry.1974893725";     // Дата создания
    static final String go_bt_param_http = "entry.1465497317";     // Номер весов
    static final String go_weight_param_http = "entry.683315711";      // Вес
    static final String go_type_param_http = "entry.138748566";      // Тип
    static final String go_is_ready_param_http = "entry.1691625234";     // Готов
    static final String go_time_param_http = "entry.1280991625";     //Время

    //private static final String action_form_http = "https://docs.google.com/forms/d/1szvEl_ro2Lyns8impGtj8xDjTKabUaWNT0oK0hz3RDs/formResponse"; // Форма Действие (В сети)
    //private static final String action_bt_param_http =            "entry.1776395592";     // Номер весов
    //private static final String action_type_param_http =          "entry.1822283685";     // Тип действия

    static final String pref_form_http = "https://docs.google.com/forms/d/1T2Q5pEhtkNc039QrD3CMJZ15d0v-BXmGC0uQw9LxBzg/formResponse"; // Форма настроек
    static final String pref_date_param_http = "entry.1036338564";     // Дата создания
    static final String pref_bt_param_http = "entry.1127481796";     // Номер весов
    static final String pref_coeff_a_param_http = "entry.167414049";      // Коэфициент А
    static final String pref_coeff_b_param_http = "entry.1149110557";     // Коэфициент Б
    static final String pref_max_weight_param_http = "entry.2120930895";     // Максимальный вес
    static final String pref_filter_adc_param_http = "entry.947786976";      // Фильтер АЦП
    static final String pref_step_scale_param_http = "entry.1522652368";     // Шаг измерения
    static final String pref_step_capture_param_http = "entry.1143754554";     // Шаг захвата
    static final String pref_time_off_param_http = "entry.1936919325";     // Время выключения
    static final String pref_bt_terminal_param_http = "entry.152097551";      // Номер БТ терминала

    public static boolean flagIsInternet;

    Internet(Context c) {
        context = c;
    }

    void connect() {
        //context=c;
        //final WifiManager wifi=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onDataConnectionStateChanged(int state) {
                switch (state) {
                    case TelephonyManager.DATA_DISCONNECTED:
                        if (telephonyManager != null)
                            turnOnDataConnection(true);
                        break;
                    default:
                        break;
                }
            }
        };


        /*broadcastReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) { //контроль состояний сетей
                String action=intent.getAction();
                if(action != null){
                    if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {//включение и выключение wifi
                        if(wifi.getWifiState()==WifiManager.WIFI_STATE_DISABLED) {
                            if(broadcastReceiver!=null)
                                wifi.setWifiEnabled(true);
                        }
                    }else if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                        boolean isInternetPresent = checkInternetConnection();
                        if(isInternetPresent){
                            //Toast.makeText(context, "Есть соединение с интернетом", Toast.LENGTH_SHORT).show();
                        }else {
                            //Toast.makeText(context, "Нет соединение с интернетом", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

            }
        };*/
        //IntentFilter filter=new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        //filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        //context.registerReceiver(broadcastReceiver, filter);
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled())
            turnOnWiFiConnection(true);
        turnOnDataConnection(true);
    }

    void disconnect() {
        /*if(broadcastReceiver!=null){
            context.unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }*/
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            telephonyManager = null;
        }

        turnOnDataConnection(false);
        turnOnWiFiConnection(false);
    }

    public boolean checkInternetConnection() {
        try {
            ConnectivityManager con_manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            return con_manager != null && con_manager.getActiveNetworkInfo() != null && con_manager.getActiveNetworkInfo().isAvailable() && con_manager.getActiveNetworkInfo().isConnected();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static boolean checkInternetConnection(Context cont) {
        try {
            ConnectivityManager con_manager = (ConnectivityManager) cont.getSystemService(Context.CONNECTIVITY_SERVICE);
            return con_manager != null && con_manager.getActiveNetworkInfo() != null && con_manager.getActiveNetworkInfo().isAvailable() && con_manager.getActiveNetworkInfo().isConnected();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public void turnOnWiFiConnection(boolean on) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifi.setWifiEnabled(on);
    }

    private boolean turnOnDataConnection(boolean on) {
        try {
            int bv = Build.VERSION.SDK_INT;
            if (bv == Build.VERSION_CODES.FROYO) {

                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

                Class<?> telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
                getITelephonyMethod.setAccessible(true);
                Object ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
                Class<?> ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());

                Method dataConnSwitchMethod;
                if (on)
                    dataConnSwitchMethod = ITelephonyClass.getDeclaredMethod("enableDataConnectivity");
                else
                    dataConnSwitchMethod = ITelephonyClass.getDeclaredMethod("disableDataConnectivity");

                dataConnSwitchMethod.setAccessible(true);
                dataConnSwitchMethod.invoke(ITelephonyStub);
            } else {
                //log.i("App running on Ginger bread+");
                final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                final Class<?> conmanClass = Class.forName(conman.getClass().getName());
                final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
                iConnectivityManagerField.setAccessible(true);
                final Object iConnectivityManager = iConnectivityManagerField.get(conman);
                final Class<?> iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
                final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                setMobileDataEnabledMethod.setAccessible(true);
                setMobileDataEnabledMethod.invoke(iConnectivityManager, on);
            }
            return true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception ignored) {
            Log.e("hhh", "error turning on/off data");
        }
        return false;
    }

    private static boolean send(URL url) {
        try {
            URLConnection urlConnection = url.openConnection();
            if (!(urlConnection instanceof HttpURLConnection)) {
                throw new IOException("URL is not an Http URL");
            }
            HttpURLConnection connection = (HttpURLConnection) urlConnection;
            //connection.setReadTimeout(3000);
            //connection.setConnectTimeout(3000);
            connection.connect();
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;

        } catch (MalformedURLException ignored) {
            return false;
        } catch (IOException ignored) {
            return false;
        }
    }

    public static boolean sendFormGo(String date, String time, String weight, String number_bt, String type, String is_ready) {
        URL url = null;
        try {
            url = new URL(go_form_http + '?'
                    + go_date_param_http + '=' + URLEncoder.encode(date, "UTF-8") +
                    '&' + go_bt_param_http + '=' + number_bt +
                    '&' + go_weight_param_http + '=' + weight +
                    '&' + go_type_param_http + '=' + URLEncoder.encode(type, "UTF-8") +
                    '&' + go_is_ready_param_http + '=' + is_ready +
                    '&' + go_time_param_http + '=' + time +
                    "&submit=Submit");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return send(url);
    }

    public static boolean submitData(String http_post, List<NameValuePair> results) {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(http_post);
        HttpParams httpParameters = new BasicHttpParams();
        int timeoutConnection = 15000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        int timeoutSocket = 30000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        post.setParams(httpParameters);
        try {
            post.setEntity(new UrlEncodedFormEntity(results, "UTF-8"));
        } catch (UnsupportedEncodingException ignored) {
            return false;
        }
        try {
            HttpResponse httpResponse = client.execute(post);
            return httpResponse.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK;
        } catch (ClientProtocolException ignored) {
            return false;
        } catch (IOException ignored) {
            return false;
        }
    }

}
