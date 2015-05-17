package com.kostya.weightcheckadmin;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

/*
 * Created by Kostya on 26.04.14.
 */
public class ActivityAbout extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        setTitle(getString(R.string.About));

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        TextView textSoftVersion = (TextView) findViewById(R.id.textSoftVersion);
        textSoftVersion.setText(ActivitySearch.versionName + ' ' + String.valueOf(ActivitySearch.versionNumber));

        TextView textSettings = (TextView) findViewById(R.id.textSettings);
        textSettings.append("Версия прошивки весов: v." + Scales.version + '\n');
        textSettings.append("Имя модуля bluetooth: " + Scales.getName() + '\n');
        textSettings.append("Адресс модуля bluetooth: " + Scales.getAddress() + '\n');
        textSettings.append("\n");
        textSettings.append("Оператор связи: " + ActivityApp.networkOperatorName + '\n');
        textSettings.append("Номер телефона: " + ActivityApp.telephoneNumber + '\n');
        textSettings.append("\n");
        textSettings.append("Батарея: " + Scales.battery + " %" + '\n');
        textSettings.append("Температура: " + Scales.getTemp() + '°' + 'C' + '\n');
        textSettings.append("Коэфициэнт: " + Scales.coefficientA + '\n');
        textSettings.append("НПВ: " + Scales.weightMax + ' ' + getString(R.string.scales_kg) + '\n');
        textSettings.append("\n");
        textSettings.append("Таблица Google Disk: " + Scales.spreadsheet + '\n');
        textSettings.append("Пользователь Google Disk: " + Scales.username + '\n');
        textSettings.append("\n");
        textSettings.append("Таймер выключения: " + Scales.timer + ' ' + getString(R.string.minute) + '\n');
        textSettings.append("Шаг измерения веса: " + Scales.step + ' ' + getString(R.string.scales_kg) + '\n');
        textSettings.append("Захват взвешивания: " + Scales.autoCapture + ' ' + getString(R.string.scales_kg) + '\n');
        textSettings.append("\n");

        TextView textAuthority = (TextView) findViewById(R.id.textAuthority);
        textAuthority.append("Авторские права принадлежат 2012-2014 Konst" + '\n');
        textAuthority.append("Все права защищены" + '\n');
    }
}
