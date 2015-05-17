//Активность настроек
package com.kostya.weightcheckadmin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.konst.module.InterfaceScaleModule;
import com.konst.module.ScaleModule;
import com.konst.module.Versions;
import com.kostya.weightcheckadmin.bootloader.ActivityBootloader;
import com.kostya.weightcheckadmin.provider.CheckDBAdapter;
import com.kostya.weightcheckadmin.provider.PreferencesDBAdapter;
import com.kostya.weightcheckadmin.provider.TaskDBAdapter;

public class ActivityPreferences extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    static final String KEY_STEP = "step";
    private static final String KEY_NAME = "name";
    static final String KEY_ADMIN = "admin";
    static final String KEY_ADDRESS = "address";
    static final String KEY_DEVICES = "devices";
    private static final String KEY_NULL = "null";
    static final String KEY_AUTO_CAPTURE = "auto_capture";
    public static final String KEY_DAY_CLOSED_CHECK = "day_closed_check";
    public static final String KEY_DAY_CHECK_DELETE = "day_check_delete";
    private static final String KEY_FILTER = "filter";
    private static final String KEY_ABOUT = "about";
    private static final String KEY_TIMER = "timer";
    static final String KEY_LAST = "last";
    static final String KEY_TIMER_NULL = "timer_null";
    static final String KEY_MAX_NULL = "max_null";
    static final String KEY_UPDATE = "update";
    public static final String KEY_FLAG_UPDATE = "flag_update";
    public static final String KEY_TIME_DELAY_DETECT_CAPTURE = "key_time_delay_capture";
    //static final String KEY_DATA                = "data";
    private boolean flagChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        if (!Preferences.admin) {
            findPreference(KEY_NAME).setEnabled(false);
            findPreference(KEY_ADMIN).setEnabled(false);
        }

        Preference name = findPreference(KEY_NAME);
        if (name != null) {
            try {
                name.setSummary(ScaleModule.getName());
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty()) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                        if (ScaleModule.setModuleName(o.toString())) {
                        preference.setSummary(o.toString());
                            Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + o.toString(), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            } catch (Exception e) {
                name.setEnabled(false);
        }
        }

        name = findPreference(KEY_ADDRESS);
        if (name != null) {
            name.setSummary(ScaleModule.getAddress());
        }

        name = findPreference(KEY_TIMER);
        if (name != null) {
            name.setTitle(getString(R.string.Timer_off) + ' ' + Versions.timeOff + ' ' + getString(R.string.minute));
            name.setSummary(getString(R.string.sum_timer) + ' ' + getString(R.string.range) + Main.default_min_time_off + getString(R.string.to) + Main.default_max_time_off);
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString())
                            || Integer.valueOf(o.toString()) < Main.default_min_time_off
                            || Integer.valueOf(o.toString()) > Main.default_max_time_off) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    if (ScaleModule.setModuleTimeOff(Integer.valueOf(o.toString()))) {
                        Versions.timeOff = Integer.valueOf(o.toString());
                        preference.setTitle(getString(R.string.Timer_off) + ' ' + Versions.timeOff + ' ' + getString(R.string.minute));
                        Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + Versions.timeOff + ' ' + getString(R.string.minute), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }

        name = findPreference(KEY_NULL);
        if (name != null) {
            name.setSummary(getString(R.string.sum_zeroing));
            if (!ScaleModule.isAttach()) {
                name.setEnabled(false);
            }
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (ScaleModule.setScaleNull()) {
                        Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    Toast.makeText(getApplicationContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }

        name = findPreference(KEY_TIMER_NULL);
        if (name != null) {
            name.setTitle(getString(R.string.Time) + ' ' + ScaleModule.timerNull + ' ' + getString(R.string.second));
            name.setSummary(getString(R.string.sum_time_auto_zero) + ' ' + Main.default_max_time_auto_null + ' ' + getString(R.string.second));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > Main.default_max_time_auto_null) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    ScaleModule.timerNull = Integer.valueOf(o.toString());
                    preference.setTitle(getString(R.string.Time) + ' ' + ScaleModule.timerNull + ' ' + getString(R.string.second));
                    Preferences.write(KEY_TIMER_NULL, ScaleModule.timerNull);
                    Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + ScaleModule.timerNull + ' ' + getString(R.string.second), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        name = findPreference(KEY_MAX_NULL);
        if (name != null) {
            name.setTitle(getString(R.string.sum_weight) + ' ' + ScaleModule.weightError + ' ' + getString(R.string.scales_kg));
            name.setSummary(getString(R.string.sum_max_null) + ' ' + Main.default_limit_auto_null + ' ' + getString(R.string.scales_kg));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > Main.default_limit_auto_null) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    ScaleModule.weightError = Integer.valueOf(o.toString());
                    preference.setTitle(getString(R.string.sum_weight) + ' ' + ScaleModule.weightError + ' ' + getString(R.string.scales_kg));
                    Preferences.write(KEY_TIMER_NULL, ScaleModule.weightError);
                    Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + ScaleModule.weightError + ' ' + getString(R.string.scales_kg), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }


        name = findPreference(KEY_STEP);
        if (name != null) {
            name.setTitle(getString(R.string.measuring_step) + ' ' + Main.stepMeasuring + ' ' + getString(R.string.scales_kg));
            name.setSummary(getString(R.string.The_range_is_from_1_to) + Main.default_max_step_scale + ' ' + getString(R.string.scales_kg));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > Main.default_max_step_scale) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    Main.stepMeasuring = Integer.valueOf(o.toString());
                    preference.setTitle(getString(R.string.measuring_step) + ' ' + Main.stepMeasuring + ' ' + getString(R.string.scales_kg));
                    Preferences.write(KEY_STEP, Main.stepMeasuring);
                    Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + Main.stepMeasuring + ' ' + getString(R.string.scales_kg), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        name = findPreference(KEY_AUTO_CAPTURE);
        if (name != null) {
            name.setTitle(getString(R.string.auto_capture) + ' ' + Main.autoCapture + ' ' + getString(R.string.scales_kg));
            name.setSummary(getString(R.string.Range_between) + (Main.default_min_auto_capture + Main.default_delta_auto_capture) + ' ' + getString(R.string.scales_kg) +
                    getString(R.string.and) + Main.default_max_auto_capture + ' ' + getString(R.string.scales_kg));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || Integer.valueOf(o.toString()) < Main.default_min_auto_capture || Integer.valueOf(o.toString()) > Main.default_max_auto_capture) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    Main.autoCapture = Integer.valueOf(o.toString());
                    if (Main.autoCapture < Main.default_min_auto_capture + Main.default_delta_auto_capture) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    preference.setTitle(getString(R.string.auto_capture) + ' ' + Main.autoCapture + ' ' + getString(R.string.scales_kg));
                    Preferences.write(KEY_AUTO_CAPTURE, Main.autoCapture);
                    Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + Main.autoCapture + ' ' + getString(R.string.scales_kg), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }


        name = findPreference(KEY_DAY_CLOSED_CHECK);
        if (name != null) {
            name.setTitle(getString(R.string.closed_checks) + ' ' + CheckDBAdapter.day_closed + ' ' + getString(R.string.day));
            name.setSummary(getString(R.string.sum_closed_checks));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > Main.default_day_close_check) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    CheckDBAdapter.day_closed = Integer.valueOf(o.toString());
                    preference.setTitle(getString(R.string.closed_checks) + ' ' + CheckDBAdapter.day_closed + ' ' + getString(R.string.day));
                    Preferences.write(KEY_DAY_CLOSED_CHECK, CheckDBAdapter.day_closed);
                    Toast.makeText(getBaseContext(), getString(R.string.preferences_yes) + ' ' + CheckDBAdapter.day_closed + ' ' + getString(R.string.day), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        name = findPreference(KEY_DAY_CHECK_DELETE);
        if (name != null) {
            name.setTitle(getString(R.string.sum_delete_check) + ' ' + String.valueOf(CheckDBAdapter.day) + ' ' + getString(R.string.day));
            name.setSummary(getString(R.string.sum_removing_checks));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || "0".equals(o.toString()) || Integer.valueOf(o.toString()) > Main.default_day_delete_check) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    CheckDBAdapter.day = Integer.valueOf(o.toString());
                    preference.setTitle(getString(R.string.sum_delete_check) + ' ' + String.valueOf(CheckDBAdapter.day) + ' ' + getString(R.string.day));
                    Preferences.write(KEY_DAY_CHECK_DELETE, CheckDBAdapter.day);
                    Toast.makeText(getBaseContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }


        name = findPreference(KEY_FILTER);
        if (name != null) {
            name.setTitle(getString(R.string.filter_adc) + ' ' + String.valueOf(Versions.filterADC));
            name.setSummary(getString(R.string.sum_filter_adc) + ' ' + getString(R.string.The_range_is_from_0_to) + Main.default_adc_filter);
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || Integer.valueOf(o.toString()) > Main.default_adc_filter) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    if (ScaleModule.setModuleFilterADC(Integer.valueOf(o.toString()))) {
                        Versions.filterADC = Integer.valueOf(o.toString());
                        preference.setTitle(getString(R.string.filter_adc) + ' ' + String.valueOf(Versions.filterADC));
                        Toast.makeText(getBaseContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }


        name = findPreference(KEY_ABOUT);
        if (name != null) {
            name.setSummary(getString(R.string.version) + Main.versionName + ' ' + Integer.toString(Main.versionNumber));
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent().setClass(getApplicationContext(), ActivityAbout.class));
                    return false;
                }
            });
        }

        name = findPreference(KEY_UPDATE);
        if (name != null) {
            if (ScaleModule.Version != null) {
                if (ScaleModule.numVersion < Main.microSoftware) {
                    name.setSummary(getString(R.string.Is_new_version));
                    name.setEnabled(true);
                } else {
                    name.setSummary(getString(R.string.Scale_update));
                    name.setEnabled(false);
    }

                name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    //@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        //Scales.vScale.backupPreference();
                        String hardware = ScaleModule.getModuleHardware();
                        if (hardware.isEmpty()) {
                            hardware = "MBC04.36.2";
                        }
                        Intent intent = new Intent(ActivityPreferences.this, ActivityBootloader.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        else
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(KEY_ADDRESS, ScaleModule.getAddress());
                        intent.putExtra(InterfaceScaleModule.CMD_HARDWARE, hardware);
                        intent.putExtra(InterfaceScaleModule.CMD_VERSION, ScaleModule.numVersion);
                        startActivity(intent);
                        return false;
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (flagChange) {
            long entryID = Long.parseLong(new PreferencesDBAdapter(this).insertAllEntry().getLastPathSegment());
            new TaskDBAdapter(getApplicationContext()).insertNewTask(TaskCommand.TaskType.TYPE_PREF_SEND_SHEET_DISK, entryID, 0, "preferences");
            //startService(new Intent(this, ServiceGetDateServer.class).setAction("new_preference"));
            //startService(new Intent(this, ServiceSentSheetServer.class).setAction("new_preference"));//todo временно отключен
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        flagChange = true;
    }
}
