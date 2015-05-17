//Активность настроек
package com.kostya.weightcheckadmin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class ActivityPreferences extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    static final String KEY_STEP = "step";
    static final String KEY_NAME = "name";
    static final String KEY_ADMIN = "admin";
    static final String KEY_ADDRESS = "address";
    static final String KEY_DEVICES = "devices";
    static final String KEY_NULL = "null";
    static final String KEY_AUTO_CAPTURE = "auto_capture";
    static final String KEY_DAY_CLOSED_CHECK = "day_closed_check";
    static final String KEY_DAY_CHECK_DELETE = "day_check_delete";
    //static final String KEY_FILTER              = "filter";
    static final String KEY_ABOUT = "about";
    static final String KEY_TIMER = "timer";
    static final String KEY_LAST = "last";
    static final String KEY_TIMER_NULL = "timer_null";
    static final String KEY_MAX_NULL = "max_null";
    static final String KEY_DATA = "data";

    static final String PREFERENCES = "preferences";

    private boolean flagChange = false;

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
            name.setSummary(Scales.getName());
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty()) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    if (Scales.command(ScaleInterface.CMD_NAME + o).equals(ScaleInterface.CMD_NAME)) {
                        preference.setSummary(o.toString());
                        Toast.makeText(getBaseContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }

        name = findPreference(KEY_ADDRESS);
        if (name != null) {
            name.setSummary(Scales.getAddress());
        }

        name = findPreference(KEY_TIMER);
        if (name != null) {
            name.setSummary(getString(R.string.sum_timer) + ' ' + String.valueOf(Scales.timer) + ' ' + getString(R.string.minute));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || o.toString().equals("0") || Integer.valueOf(o.toString()) > 60) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    if (Scales.command(ScaleInterface.CMD_TIMER + o).equals(ScaleInterface.CMD_TIMER)) {
                        Scales.timer = Integer.valueOf(o.toString());
                        preference.setSummary(getString(R.string.sum_timer) + ' ' + String.valueOf(Scales.timer) + ' ' + getString(R.string.minute));
                        Toast.makeText(getBaseContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
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
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (Scales.vClass.setScaleNull()) {
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
            name.setSummary(getString(R.string.sum_time_auto_zero) + ' ' + String.valueOf(Scales.timerNull) + ' ' + getString(R.string.second));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || o.toString().equals("0") || Integer.valueOf(o.toString()) > 120) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    Scales.timerNull = Integer.valueOf(o.toString());
                    preference.setSummary(getString(R.string.sum_time_auto_zero) + ' ' + String.valueOf(Scales.timerNull) + ' ' + getString(R.string.second));
                    Preferences.write(KEY_TIMER_NULL, Scales.timerNull);
                    Toast.makeText(getBaseContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        name = findPreference(KEY_MAX_NULL);
        if (name != null) {
            name.setSummary(getString(R.string.sum_max_null) + ' ' + String.valueOf(Scales.weightError) + ' ' + getString(R.string.scales_kg));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || o.toString().equals("0") || Integer.valueOf(o.toString()) > 50) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    Scales.weightError = Integer.valueOf(o.toString());
                    preference.setSummary(getString(R.string.sum_max_null) + ' ' + String.valueOf(Scales.weightError) + ' ' + getString(R.string.scales_kg));
                    Preferences.write(KEY_TIMER_NULL, Scales.weightError);
                    Toast.makeText(getBaseContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }


        name = findPreference(KEY_STEP);
        if (name != null) {
            name.setSummary(getString(R.string.measuring_step) + ' ' + String.valueOf(Scales.step) + ' ' + getString(R.string.scales_kg));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || o.toString().equals("0") || Integer.valueOf(o.toString()) > 20) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    Scales.step = Integer.valueOf(o.toString());
                    preference.setSummary(getString(R.string.measuring_step) + ' ' + String.valueOf(Scales.step) + ' ' + getString(R.string.scales_kg));
                    Preferences.write(KEY_STEP, o.toString());
                    Toast.makeText(getBaseContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        name = findPreference(KEY_AUTO_CAPTURE);
        if (name != null) {
            name.setSummary(getString(R.string.sum_auto_capture) + ' ' + String.valueOf(Scales.autoCapture) + ' ' + getString(R.string.scales_kg));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || o.toString().equals("0") || Integer.valueOf(o.toString()) > 50) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    Scales.autoCapture = Integer.valueOf(o.toString());
                    preference.setSummary(getString(R.string.sum_auto_capture) + ' ' + String.valueOf(Scales.autoCapture) + ' ' + getString(R.string.scales_kg));
                    Preferences.write(KEY_AUTO_CAPTURE, o.toString());
                    Toast.makeText(getBaseContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }


        name = findPreference(getString(R.string.KEY_DAY_CLOSED_CHECK));
        if (name != null) {
            name.setSummary(getString(R.string.sum_closed_checks) + ' ' + String.valueOf(CheckDBAdapter.day_closed) + ' ' + getString(R.string.day));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || o.toString().equals("0") || Integer.valueOf(o.toString()) > 5) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    CheckDBAdapter.day_closed = Integer.valueOf(o.toString());
                    preference.setSummary(getString(R.string.sum_closed_checks) + ' ' + String.valueOf(CheckDBAdapter.day_closed) + ' ' + getString(R.string.day));
                    Preferences.write(getString(R.string.KEY_DAY_CLOSED_CHECK), o.toString());
                    Toast.makeText(getBaseContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        name = findPreference(KEY_DAY_CHECK_DELETE);
        if (name != null) {
            name.setSummary(getString(R.string.sum_delete_check) + ' ' + String.valueOf(CheckDBAdapter.day) + ' ' + getString(R.string.day));
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.toString().isEmpty() || o.toString().equals("0") || Integer.valueOf(o.toString()) > 5) {
                        Toast.makeText(getBaseContext(), R.string.preferences_no, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    CheckDBAdapter.day = Integer.valueOf(o.toString());
                    preference.setSummary(getString(R.string.sum_delete_check) + ' ' + String.valueOf(CheckDBAdapter.day) + ' ' + getString(R.string.day));
                    Preferences.write(KEY_DAY_CHECK_DELETE, o.toString());
                    Toast.makeText(getBaseContext(), R.string.preferences_yes, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        name = findPreference(KEY_ADMIN);
        if (name != null) {
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent().setClass(getApplicationContext(), ActivityTuning.class));
                    return true;
                }
            });
        }


        name = findPreference(KEY_ABOUT);
        if (name != null) {
            name.setSummary(getString(R.string.version) + ActivitySearch.versionName + ' ' + Integer.toString(ActivitySearch.versionNumber));
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent().setClass(getApplicationContext(), ActivityAbout.class));
                    return false;
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (flagChange) {
            long entryID = Long.parseLong(new PreferencesDBAdapter(this).insertAllEntry().getLastPathSegment());
            new TaskDBAdapter(getApplicationContext()).insertNewTask(TaskDBAdapter.TYPE_PREF_DISK, entryID, 0, "preferences");
            //startService(new Intent(this, ServiceSendDateServer.class).setAction("new_preference"));
            startService(new Intent(this, ServiceGetDateServer.class).setAction("new_preference"));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        flagChange = true;
    }
}
