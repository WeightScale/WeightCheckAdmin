//Активность для стартовой настройки весов
package com.kostya.weightcheckadmin;

//import android.content.SharedPreferences;

import android.graphics.Point;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
//import android.preference.PreferenceManager;
import android.widget.Toast;

public class ActivityTuning extends PreferenceActivity {
    private final Point point1 = new Point(Integer.MIN_VALUE, 0);
    private final Point point2 = new Point(Integer.MIN_VALUE, 0);
    private boolean flag_restore = false;
    //boolean flag_change = false;

    final String KEY_POINT1 = "point1";
    final String KEY_POINT2 = "point2";
    final String KEY_WEIGHT_MAX = "weightMax";
    final String KEY_SPEED = "speed";
    final String KEY_FILTER = "filter";
    final String KEY_COEFFICIENT_A = "coefficientA";
    final String KEY_CALL_BATTERY = "call_battery";
    final String KEY_CALL_TEMP = "call_temp";
    final String KEY_SHEET = "sheet";
    final String KEY_NAME = "name";
    final String KEY_PASSWORD = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tuning);
        Preference name = findPreference(KEY_POINT1);
        if (name != null) {
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    String str = Scales.command(ScaleInterface.CMD_SENSOR);
                    if (str.equals("")) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    //Scales.sensor=Integer.valueOf(str);
                    point1.x = Scales.sensorTenzo = Integer.valueOf(str);
                    point1.y = 0;
                    Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    flag_restore = true;
                    return true;
                }
            });
        }
        if (name != null) {
            name = findPreference(KEY_POINT2);
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String str = Scales.command(ScaleInterface.CMD_SENSOR);
                    if (str.equals("")) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    point2.x = Scales.sensorTenzo = Integer.valueOf(str);
                    point2.y = Integer.valueOf(o.toString());
                    Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    flag_restore = true;
                    return true;
                }
            });
        }
        if (name != null) {
            name = findPreference(KEY_WEIGHT_MAX);
            name.setSummary("Предельный вес весов " + String.valueOf(Scales.weightMax) + "кг");
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().equals("") || Integer.valueOf(o.toString()) < 1000) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    Scales.weightMax = Integer.valueOf(o.toString());
                    Scales.weightMargin = (int) (Scales.weightMax * 1.2);
                    Scales.weightError = (int) (Scales.weightMax * 0.001);
                    preference.setSummary("Предельный вес весов " + String.valueOf(Scales.weightMax) + "кг");
                    Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    flag_restore = true;
                    return true;
                }
            });
        }
        if (name != null) {
            name = findPreference(KEY_SPEED);
            name.setSummary(String.valueOf(Scales.speed));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().equals("") || Integer.valueOf(o.toString()) > 5 || Integer.valueOf(o.toString()) < 1) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    if (Scales.command(ScaleInterface.CMD_SPEED + o.toString()).equals(ScaleInterface.CMD_SPEED)) {
                        Scales.speed = Integer.valueOf(o.toString());
                        preference.setSummary(o.toString());
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
        if (name != null) {
            name = findPreference(KEY_FILTER);
            name.setSummary("Фильтер АЦП " + String.valueOf(Scales.filter) + " чем больше число тем точнее, но медленей измерения.");
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().equals("") || Integer.valueOf(o.toString()) > 15) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (Scales.command(ScaleInterface.CMD_FILTER + o.toString()).equals(ScaleInterface.CMD_FILTER)) {
                        Scales.filter = Integer.valueOf(o.toString());
                        preference.setSummary("Фильтер АЦП " + String.valueOf(Scales.filter) + " чем больше число тем точнее, но медленей измерения.");
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
        if (name != null) {
            name = findPreference(KEY_COEFFICIENT_A);
            name.setSummary("Установлен коэффициент " + Float.toString(Scales.coefficientA));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().equals("") || !Scales.isFloat(o.toString())) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    Scales.coefficientA = Float.valueOf(o.toString());
                    preference.setSummary(o.toString());
                    Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    flag_restore = true;
                    return true;
                }
            });
        }
        if (name != null) {
            name = findPreference(KEY_CALL_BATTERY);
            name.setSummary("Установка процента заряда ");
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().equals("") || o.toString().equals("0") || Integer.valueOf(o.toString()) > 99) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (Scales.command(ScaleInterface.CMD_CALL_BATTERY + o.toString()).equals(ScaleInterface.CMD_CALL_BATTERY)) {
                        Scales.battery = Integer.valueOf(o.toString());
                        preference.setSummary(o.toString());
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
        if (name != null) {
            name = findPreference(KEY_CALL_TEMP);
            name.setSummary("Установка температуры ");
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().equals("") || o.toString().equals("0") || Integer.valueOf(o.toString()) > 99) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    /*String str = Scales.command(ScaleInterface.CMD_DATA_TEMP);
                    if(str.isEmpty()){
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    float temp = (float)((float)((Integer.valueOf(str) - 0x800000) / 7169) / 0.81)-273;*/
                    //temp -= 273;

                    //Scales.temp = Integer.valueOf(o.toString());
                    //Scales.temp = Integer.valueOf(o.toString());
                    //Scales.coefficient_temp = (float) Scales.temp / Float.valueOf(str);//Float.valueOf((float)Scales.temp / (float)Integer.valueOf(str));
                    Scales.getTemp();
                    if (Scales.command(ScaleInterface.CMD_CALL_TEMP + String.valueOf(Scales.coefficientTemp)).equals(ScaleInterface.CMD_CALL_TEMP)) {
                        //preference.setSummary(o.toString());
                        preference.setSummary(String.valueOf(Scales.temp) + "\u00b0" + "C");
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
        if (name != null) {
            name = findPreference(KEY_SHEET);
            name.setTitle("Таблица: " + "\"" + Scales.spreadsheet + "\"");
            name.setSummary("Имя таблици spreadsheet в Google drive ");
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().equals("")) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (Scales.command(ScaleInterface.CMD_SPREADSHEET + String.valueOf(o.toString())).equals(ScaleInterface.CMD_SPREADSHEET)) {
                        preference.setTitle("Таблица: " + "\"" + o.toString() + "\"");
                        Scales.spreadsheet = o.toString();
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    preference.setTitle("Таблица: ???");
                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();

                    return false;
                }
            });
        }
        if (name != null) {
            name = findPreference(KEY_NAME);
            name.setSummary("Account Google: " + Scales.username);
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().equals("")) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (Scales.command(ScaleInterface.CMD_G_USER + String.valueOf(o.toString())).equals(ScaleInterface.CMD_G_USER)) {
                        preference.setSummary("Account Google: " + o.toString());
                        Scales.username = o.toString();
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    preference.setSummary("Account Google: ???");
                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
        if (name != null) {
            name = findPreference(KEY_PASSWORD);
            name.setSummary("Password account Google - " + Scales.password);
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().equals("")) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    if (Scales.command(ScaleInterface.CMD_G_PASS + String.valueOf(o.toString())).equals(ScaleInterface.CMD_G_PASS)) {
                        preference.setSummary("Password account Google: " + o.toString());
                        Scales.password = o.toString();
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    preference.setSummary("Password account Google: ???");
                    Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();

                    return false;
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        boolean b;
        if (flag_restore) {
            if (point1.x != Integer.MIN_VALUE && point2.x != Integer.MIN_VALUE) {
                Scales.coefficientA = (float) (point1.y - point2.y) / (point1.x - point2.x);
                Scales.coefficientB = point1.y - Scales.coefficientA * point1.x;
            }
            Scales.limitTenzo = (int) ((float) Scales.weightMax / Scales.coefficientA);
            if (Scales.limitTenzo > 0xffffff) {
                Scales.limitTenzo = 0xffffff;
                Scales.weightMax = (int) ((float) 0xffffff * Scales.coefficientA);
            }
            b = Scales.vClass.writeDataScale();
            if (b)
                Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getApplicationContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
        }/*else
            b = Scales.vClass.writeDataScale();*/

    }
}
