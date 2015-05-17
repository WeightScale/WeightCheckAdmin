package com.kostya.weightcheckadmin.bootloader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.*;
import android.view.View;
import android.widget.*;
import com.konst.bootloader.AVRProgrammer;
import com.konst.bootloader.HandlerBootloader;
import com.konst.module.ScaleModule;
import com.kostya.weightcheckadmin.*;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 25.12.13
 * Time: 21:49
 * To change this template use File | Settings | File Templates.
 */
public class ActivityBootloader extends Activity {
    private ImageView startBoot;
    private TextView textViewLog;
    private ProgressDialog progressDialog;
    private HandlerBootloader handler;

    private AVRProgrammer programmer;
    //private AVRDevice avr;
    boolean flag_programs_finish = true;

    private String deviceFileName = "atmega88"; // Specified device name.
    private String bootFileName = "weightscales"; // Specified device name.
    private final String dirDeviceFiles = "device";
    private final String dirBootFiles = "bootfiles";

    private int flashStartAddress; // Limit Flash operations, -1 if not.
    private int flashEndAddress = -1; // ...to this address, inclusive, -1 if not.

    private int eepromStartAddress; // Same as above for E2.
    private int eepromEndAddress = -1; // ...

    private class ThreadDoDeviceDependent extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                programmer.doDeviceDependent();
            } catch (Exception e) {
                handler.sendMessage(handler.obtainMessage(1, e.getMessage() + " \r\n"));
            }
            flag_programs_finish = true;
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bootloder);

        Spinner spinnerField = (Spinner) findViewById(R.id.spinnerField);
        Spinner spinnerDevice = (Spinner) findViewById(R.id.spinnerDevice);
        textViewLog = (TextView) findViewById(R.id.textLog);
        startBoot = (ImageView) findViewById(R.id.buttonBoot);
        //String[] stringsBoots = new String[0];
        try {
            //stringsBoots = getAssets().list(dirBootFiles);
            ArrayAdapter<String> adapterDevice = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getAssets().list(dirDeviceFiles));
            adapterDevice.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDevice.setAdapter(adapterDevice);
            spinnerDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    deviceFileName = adapterView.getItemAtPosition(i).toString();
                    try {
                        setupProgrammer();
                        startBoot.setEnabled(true);
                    } catch (Exception e) {
                        handler.sendMessage(handler.obtainMessage(1, e.getMessage() + " \r\n"));
                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {}
            });

            ArrayAdapter<String> adapterBoot = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getAssets().list(dirBootFiles));
            adapterBoot.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerField.setAdapter(adapterBoot);
            spinnerField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    bootFileName = adapterView.getItemAtPosition(i).toString();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {}
            });

        } catch (IOException e) {
            log(e.getMessage() + "\r\n");
        }

        handler = new HandlerBootloader() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        log(msg.obj.toString());// обновляем TextView
                        break;
                    case 2:
                        progressDialog = new ProgressDialog(ActivityBootloader.this);
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progressDialog.setMessage(msg.obj.toString());
                        progressDialog.setMax(msg.arg1);
                        progressDialog.setProgress(0);
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.show();
                        break;
                    case 3:
                        progressDialog.setProgress(msg.arg1);
                        break;
                    case 4:
                        progressDialog.dismiss();
                        break;
                }
            }
        };

        ImageView buttonBack = (ImageView) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exit();
            }
        });
        startBoot.setEnabled(false);
        startBoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!programmer.isProgrammerId()) {
                    scaleModule.dettach();
                    new ActivitySearch().log("Не программатор");
                    finish();
                    return;
                }

                try {
                    if (programmer.checkSignature(programmer.getAvrDevice().getSignature0(), programmer.getAvrDevice().getSignature1(), programmer.getAvrDevice().getSignature2())) {
                        log("Проверка сигнатуры устройства!\r\n");
                        flag_programs_finish = false;
                        new ThreadDoDeviceDependent().execute();
                        startBoot.setEnabled(false);
                        return;
                    }
                    log("Сигнатура не поддерживается!");
                } catch (Exception e) {
                    handler.sendMessage(handler.obtainMessage(1, e.getMessage() + " \r\n"));
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        exit();
    }

    void log(String string) { //для текста
        textViewLog.append(string);
    }

    void setupProgrammer() throws Exception {

        programmer = new AVRProgrammer(handler) {
            @Override
            public void sendByte(byte b) {

            }

            @Override
            public int getByte() {
                return 0;
            }
        };
        if (deviceFileName.length() == 0) {
            log("Device name not specified!\r\n");
            return;
        }
        InputStream inputDeviceFile = getAssets().open(dirDeviceFiles + '/' + deviceFileName);
        InputStream inputHexFile = getAssets().open(dirBootFiles + '/' + bootFileName);

        programmer.doJob(inputDeviceFile, inputHexFile);

        //avr = new AVRDevice(dirDeviceFiles + "/" + deviceFileName, getApplicationContext(), handler);
        //log("Parsing XML file for device parameters...\r\n");
        //avr.readParametersFromAVRStudio();
    }

    /*void doDeviceDependent(AVRProgrammer prog, AVRDevice avr, Handler _handler) throws Exception {
        HEXFile hex = null;
        HEXFile hex_v; // Used for verifying memory contents.
        int pos; // Used when comparing data.
        //long bits = 0; // Used for lock and fuse bits.

	    *//* Set programmer pagesize *//*
        prog.setPagesize(avr.getPageSize());

	    *//* Check if specified address limits are within device range *//*
        if (flashEndAddress != -1) {
            if (flashEndAddress >= avr.getFlashSize())
                throw new Exception("Specified Flash address range is outside device address space!");
        } else {
            flashStartAddress = 0;
            flashEndAddress = avr.getFlashSize() - 1;
        }

        if (eepromEndAddress != -1) {
            if (eepromEndAddress >= avr.getEEPROMSize())
                throw new Exception("Specified EEPROM address range is outside device address space!");
        } else {
            eepromStartAddress = 0;
            eepromEndAddress = avr.getEEPROMSize() - 1;
        }

	    *//* Erase chip before programming anything? *//*
        boolean chipErase = true;
        if (chipErase) {
            _handler.sendMessage(_handler.obtainMessage(1, "Erasing chip contents...\r\n"));
            if (!prog.chipErase())
                throw new Exception("Chip erase is not supported by this programmer!");
        }


	    *//* Prepare input hex file for flash *//*
        int memoryFillPattern = -1;
        boolean verifyFlash = true;
        boolean programFlash = true;
        if (programFlash || verifyFlash) {
            *//* Check that filename has been specified *//*
            if (bootFileName.length() == 0)
                throw new Exception("Cannot program or verify Flash without input file specified!");

		    *//* Prepare the file *//*
            hex = new HEXFile(avr.getFlashSize(), (byte) 0xff, getApplicationContext(), _handler);

		    *//* Fill if wanted *//*
            if (memoryFillPattern != -1)
                hex.clearAll((byte) memoryFillPattern);

		    *//* Read file *//*
            _handler.sendMessage(_handler.obtainMessage(1, "Reading HEX input file for flash operations...\r\n"));
            _handler.sendMessage(_handler.obtainMessage(2, flashEndAddress, 0, "Reading HEX..."));
            //hex.readFile(hex.getFieldFile(inputFileFlash));dirDeviceFiles+"/"+ deviceFileName
            hex.readFile(dirBootFiles + "/" + bootFileName);

		    *//* Check limits *//*
            if (hex.getRangeStart() > flashEndAddress || hex.getRangeEnd() < flashStartAddress)
                throw new Exception("HEX file defines data outside specified range!");

            if (memoryFillPattern == -1) {
                if (hex.getRangeStart() > flashStartAddress)
                    flashStartAddress = hex.getRangeStart();

                if (hex.getRangeEnd() < flashEndAddress)
                    flashEndAddress = hex.getRangeEnd();
            }

            hex.setUsedRange(flashStartAddress, (15 - (flashEndAddress % 16)) + flashEndAddress);
        }

	    *//* Program new Flash contents? *//*
        if (programFlash) {
		    *//* Program data *//*
            _handler.sendMessage(_handler.obtainMessage(1, "Programming Flash contents...\r\n"));
            _handler.sendMessage(_handler.obtainMessage(2, flashEndAddress, 0, "Programming Flash..."));
            if (!prog.writeFlash(hex)) {
                _handler.sendMessage(_handler.obtainMessage(4));
                throw new Exception("Flash programming is not supported by this programmer!");
            }
            _handler.sendMessage(_handler.obtainMessage(4));
        }


	    *//* Verify Flash contents? *//*
        if (verifyFlash) {
		    *//* Prepare HEX file for comparision *//*
            hex_v = new HEXFile(avr.getFlashSize(), (byte) 0xff, getApplicationContext(), _handler);

		    *//* Compare to Flash *//*
            _handler.sendMessage(_handler.obtainMessage(1, "Reading Flash contents...\r\n"));
            _handler.sendMessage(_handler.obtainMessage(2, flashEndAddress, 0, "Reading Flash..."));
            hex_v.setUsedRange(hex.getRangeStart(), hex.getRangeEnd());
            if (!prog.readFlash(hex_v)) {
                _handler.sendMessage(_handler.obtainMessage(4));
                throw new Exception("Flash readout is not supported by this programmer!");
            }
            _handler.sendMessage(_handler.obtainMessage(4));

		    *//* Compare data *//*
            _handler.sendMessage(_handler.obtainMessage(1, "Comparing Flash data...\r\n"));
            _handler.sendMessage(_handler.obtainMessage(2, flashEndAddress, 0, "Comparing Flash data..."));
            for (pos = hex.getRangeStart(); pos <= hex.getRangeEnd(); pos++) {
                handler.sendMessage(handler.obtainMessage(3, pos, 0));
                if (hex.getData(pos) != hex_v.getData(pos)) {
                    _handler.sendMessage(_handler.obtainMessage(1, "Unequal at address 0x" + hex.getData(pos) + "!\r\n"));
                    _handler.sendMessage(_handler.obtainMessage(4));
                    break;
                }
            }
            _handler.sendMessage(_handler.obtainMessage(4));
            if (pos > hex.getRangeEnd()) { // All equal?
                _handler.sendMessage(_handler.obtainMessage(1, "Equal!\r\n"));
            }
        }

        Scales.sendByte((byte) 'E');   //Exit bootloader
        _handler.sendMessage(_handler.obtainMessage(1, "Exit bootloader\r\n"));
        flag_programs_finish = true;

    }*/

    ScaleModule scaleModule = new ScaleModule() {
        @Override
        public void handleModuleConnect(Result result) {

        }
    };

    void exit() {
        if (flag_programs_finish) {
            scaleModule.dettach();
            finish();
        }
    }

}
