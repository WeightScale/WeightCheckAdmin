package com.kostya.weightcheckadmin.bootloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.*;
import android.view.View;
import android.widget.*;
import com.konst.bootloader.AVRProgrammer;
import com.konst.bootloader.HandlerBootloader;
import com.konst.module.BootModule;
import com.konst.module.Module;
import com.konst.module.OnEventConnectResult;
import com.konst.module.ScaleModule;
import com.kostya.weightcheckadmin.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Загрузчик программы.
 * Date: 25.12.13
 * Time: 21:49
 *
 * @author Kostya
 */
public class ActivityBootloader extends Activity {
    /**
     * Кнопка начать загрузку.
     */
    private ImageView startBoot;
    /**
     * Текс событий при загрузке.
     */
    private ProgressDialog progressDialog;
    private TextView textViewLog;
    BootModule bootModule;
    /**
     * Флаг финиша программирования
     */
    boolean flag_programs_finish = true;

    /**
     * Имя файла микросхемы
     */
    private String deviceFileName = "";
    /**
     * Имя файла прошивки
     */
    private String bootFileName = "";
    /**
     * имя папки хранения файлов микросхем
     */
    private final String dirDeviceFiles = "device";
    /**
     * Имя папки хранения файлов прошивок
     */
    private final String dirBootFiles = "bootfiles";

    /**
     * Запрос для соединения с бутлогером.
     */
    static final int REQUEST_CONNECT_BOOT = 1;

    /**
     * Поток сделать прошивку модуля
     */
    private class ThreadDoDeviceDependent extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                programmer.doDeviceDependent();
            } catch (Exception e) {
                handlerProgrammed.obtainMessage(1, e.getMessage() + " \r\n").sendToTarget();
            }
            flag_programs_finish = true;
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bootloder);

        try {
            bootModule = new BootModule("bootloader", onEventConnectResult);
            log(getString(R.string.bluetooth_off));
        } catch (Exception e) {
            log(e.getMessage());
            finish();
        }

        /** Спинер фаилов микросхем */
        Spinner spinnerField = (Spinner) findViewById(R.id.spinnerField);
        /** Спинер фаилов прошивок */
        Spinner spinnerDevice = (Spinner) findViewById(R.id.spinnerDevice);
        textViewLog = (TextView) findViewById(R.id.textLog);
        startBoot = (ImageView) findViewById(R.id.buttonBoot);
        progressDialog = new ProgressDialog(this);
        //String[] stringsBoots = new String[0];
        try {
            /** Адаптер для спинера микросхем */
            ArrayAdapter<String> adapterDevice = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getAssets().list(dirDeviceFiles));
            adapterDevice.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDevice.setAdapter(adapterDevice);
            spinnerDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    deviceFileName = adapterView.getItemAtPosition(i).toString();
                    handlerProgrammed.obtainMessage(1, deviceFileName + " \r\n").sendToTarget();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            /** Адаптер для спинера прошивок */
            ArrayAdapter<String> adapterBoot = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getAssets().list(dirBootFiles));
            adapterBoot.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerField.setAdapter(adapterBoot);
            spinnerField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    bootFileName = adapterView.getItemAtPosition(i).toString();
                    handlerProgrammed.obtainMessage(1, bootFileName + " \r\n").sendToTarget();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

        } catch (IOException e) {
            log(e.getMessage() + "\r\n");
        }
        /** Кнопка назад */
        ImageView buttonBack = (ImageView) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exit();
            }
        });
        /** Кнопка старт неактивна */
        startBoot.setEnabled(false);
        startBoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /** Запускаем начать загрузку прошивки
                 *  true - Запущено без ошибки */
                if (startProgramed()) {
                    flag_programs_finish = true;
                }

            }
        });
        /** Запускаем активность для выбора и соединения с модулем для загрузки прошивки */
        startActivityForResult(new Intent(getBaseContext(), ActivitySearch.class).setAction("bootloader"), REQUEST_CONNECT_BOOT);
    }

    @Override
    public void onBackPressed() {
        exit();
    }

    /**
     * Вывод сообщений.
     *
     * @param string Текст сообщения.
     */
    void log(String string) { //для текста
        textViewLog.setText(string + '\n' + textViewLog.getText());
    }

    /**
     * Экземпляр бутлодера модуля.
     */
    OnEventConnectResult onEventConnectResult = new OnEventConnectResult() {
        protected AlertDialog.Builder dialog;

        @Override
        public void handleResultConnect(final Module.ResultConnect result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (result) {
                        case STATUS_LOAD_OK:
                            startBoot.setEnabled(true);
                            startBoot.setAlpha(255);
                            break;
                        default:
                    }
                }
            });
        }

        @Override
        public void handleConnectError(Module.ResultError error, String s) {
            switch (error) {
                case CONNECT_ERROR:
                    //Intent intent = new Intent(getBaseContext(), ActivityConnect.class);
                    //intent.putExtra("address", addressDevice);
                    //startActivityForResult(intent, REQUEST_CONNECT_BOOT);
                    break;
            }
        }

    };

    /**
     * Обработчик сообщений бутлодера.
     */
    final HandlerBootloader handlerProgrammed = new HandlerBootloader() {

        @Override
        public void handleMessage(Message msg) {
            switch (HandlerBootloader.Result.values()[msg.what]) {
                case MSG_LOG:
                    log(msg.obj.toString());// обновляем TextView
                    break;
                case MSG_SHOW_DIALOG:
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setMessage(msg.obj.toString());
                    progressDialog.setMax(msg.arg1);
                    progressDialog.setProgress(0);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();
                    break;
                case MSG_UPDATE_DIALOG:
                    progressDialog.setProgress(msg.arg1);
                    break;
                case MSG_CLOSE_DIALOG:
                    progressDialog.dismiss();
                    break;
                default:
            }
        }

        @Override
        public String toString() {
            return super.toString();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case RESULT_OK:
                startBoot.setEnabled(true);
                startBoot.setAlpha(255);
                break;
        }
    }

    /**
     * Начать программирование.
     *
     * @return true - Программирование начато без ошибок.
     */
    boolean startProgramed() {
        /** Проверяем какой программатор */
        if (!programmer.isProgrammerId()) {
            log(getString(R.string.Not_programmer));
            return false;
        }
        flag_programs_finish = false;
        log(getString(R.string.Programmer_defined));
        try {

            if (deviceFileName.isEmpty()) {
                throw new Exception("Device name not specified!");
            }

            log("Device " + deviceFileName);

            if (bootFileName.isEmpty()) {
                throw new Exception("Boot фаил отсутствует для этого устройства!\r\n");
            }

            InputStream inputDeviceFile = getAssets().open(dirDeviceFiles + '/' + deviceFileName);
            InputStream inputHexFile = getAssets().open(dirBootFiles + '/' + bootFileName);


            startBoot.setEnabled(false);
            startBoot.setAlpha(128);
            programmer.doJob(inputDeviceFile, inputHexFile);
            new ThreadDoDeviceDependent().execute();
        } catch (IOException e) {
            handlerProgrammed.obtainMessage(HandlerBootloader.Result.MSG_LOG.ordinal(), e.getMessage()).sendToTarget();
            return false;
        } catch (Exception e) {
            handlerProgrammed.obtainMessage(HandlerBootloader.Result.MSG_LOG.ordinal(), e.getMessage()).sendToTarget();
            return false;
        }
        return true;
    }

    /**
     * Экземпляр программатора
     */
    private AVRProgrammer programmer = new AVRProgrammer(handlerProgrammed) {
        /** Послать байт
         * @param b Байт
         */
        @Override
        public void sendByte(byte b) {
            bootModule.sendByte(b);
        }

        /** Принять байт.
         * @return Байт.
         */
        @Override
        public int getByte() {
            return bootModule.getByte();
        }
    };

    /**
     * Выход из программы.
     */
    void exit() {
        if (flag_programs_finish) {
            bootModule.dettach();
            bootModule.getAdapter().disable();
            finish();
        }
    }

}
