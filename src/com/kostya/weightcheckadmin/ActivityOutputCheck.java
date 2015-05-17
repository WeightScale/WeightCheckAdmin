package com.kostya.weightcheckadmin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

import java.util.concurrent.TimeUnit;

public class ActivityOutputCheck extends Activity {
    private class ProgressZeroTask extends AsyncTask<Void, Void, Boolean> {
        private final ProgressDialog dialog;
        private final Context context;

        public ProgressZeroTask(Context activity) {
            context = activity;
            dialog = new ProgressDialog(context);
        }

        @Override
        protected void onPreExecute() {
            dialog.setCancelable(false);
            dialog.setIndeterminate(false);
            dialog.show();
            dialog.setContentView(R.layout.custom_progress_dialog);
            TextView tv1 = (TextView) dialog.findViewById(R.id.textView1);
            tv1.setText("Обнуление...");
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (dialog.isShowing())
                dialog.dismiss();
        }

        @Override
        protected Boolean doInBackground(Void... args) {
            return Scales.vClass.setOffsetScale();
        }
    }

    private class ThreadUpdate extends AsyncTask<Void, Void, Void> {
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
            while (!isCancelled()) {
                publishProgress();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                    closed = true;
                }
            }
            closed = true;
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            if (Scales.weight == Integer.MIN_VALUE) {
                weightTextView.updateProgress(getString(R.string.NO_CONNECT));
                progressBarWeight.setProgress(0);
            } else {
                progressBarWeight.setProgress(Scales.vClass.getSensorTenzo());
                Rect bounds = progressBarWeight.getProgressDrawable().getBounds();
                weightTextView.updateProgress(Scales.weight);
                if (!Scales.vClass.isLimit()) {
                    progressBarWeight.setProgressDrawable(getResources().getDrawable(R.drawable.progress_weight));
                    progressBarWeight.getProgressDrawable().setBounds(bounds);
                } else if (!Scales.vClass.isMargin()) {
                    progressBarWeight.setProgressDrawable(getResources().getDrawable(R.drawable.progress_weight_danger));
                    progressBarWeight.getProgressDrawable().setBounds(bounds);
                } else {
                    weightTextView.updateProgress(getString(R.string.OVER_LOAD));
                    vibrator.vibrate(200);
                }
            }
            //batteryProgressBar.updateProgress(Scales.battery);
            weightTextView.setSecondaryProgress(Scales.numStable);
        }
    }

    private class ThreadAutoWeight extends AsyncTask<Void, Integer, Boolean> { //автосуммирование и передача в интернет
        private boolean closed = true;
        private WeightType tempTypeState;

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
        protected Boolean doInBackground(Void... voids) {

            while (!isCancelled()) {

                tempTypeState = weightType;
                buttonIsClick = false;
                Scales.numStable = 0;
                while (!isCancelled() && Scales.vClass.getWeightScale() < Scales.autoCapture && !buttonIsClick) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                }//ждём начала нагружения
                sendBroadcast(new Intent(ACTION_START_WEIGHTING));
                //Scales.flag_stable = Scales.stable(Scales.readWeight());
                Scales.flagStable = false;
                //Scales.num_stable = 0;
                while (!isCancelled() && !(Scales.flagStable || buttonIsClick)) {//ждем стабилизации веса или нажатием выбора веса
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                    Scales.flagStable = Scales.stable(Scales.vClass.getWeightScale());
                }
                if (isCancelled())
                    break;
                if (!buttonIsClick)
                    if (tempTypeState == weightType)
                        sendBroadcast(new Intent(ACTION_STORE_WEIGHTING).putExtra(EXTRA_WEIGHT_STABLE, Scales.weight));
                buttonIsClick = false;

                while (!isCancelled() && Scales.vClass.getWeightScale() >= Scales.autoCapture) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                }// ждем конца нагружения
                if (!isCancelled()) {
                    try {
                        TimeUnit.SECONDS.sleep(2);/*Thread.sleep(2000);*/
                    } catch (InterruptedException ignored) {
                    }
                } else
                    break;
                if (tempTypeState == weightType)
                    sendBroadcast(new Intent(ACTION_STOP_WEIGHTING));
                else if (weightType == WeightType.NETTO)
                    sendBroadcast(new Intent(ACTION_STOP_WEIGHTING));

            }
            closed = true;
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);
            //closed=true;
        }

        @Override
        protected void onProgressUpdate(Integer... result) {
            super.onProgressUpdate(result);
        }
    }

    private final ThreadUpdate threadUpdate = new ThreadUpdate();
    private final ThreadAutoWeight threadAutoWeight = new ThreadAutoWeight();

    private Vibrator vibrator; //вибратор

    private WeightTextView weightTextView;
    private ProgressBar progressBarWeight;
    private TextView textViewGross;
    private EditText textViewNetto;
    private TextView textViewTare;
    private Button buttonGross, buttonTare;
    private ImageView buttonFinish;
    private Spinner spinnerType;

    private boolean buttonIsClick;

    private static final String ACTION_STOP_WEIGHTING = "stop_weighting";
    private static final String ACTION_START_WEIGHTING = "start_weighting";
    private static final String ACTION_STORE_WEIGHTING = "store_weighting";
    private static final String EXTRA_WEIGHT_STABLE = "weightStable";

    protected enum WeightType {
        TARE,
        GROSS,
        NETTO
    }

    private WeightType weightType;

    private int entryID;
    private int weightGross;
    private int weightTare;
    private int weightNetto;
    private String type;
    private int typeId;
    //private int is_ready = 0;

    private boolean flagExit = true;
    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.output_check);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        entryID = Integer.valueOf(getIntent().getStringExtra("id"));
        Cursor cursor = new CheckDBAdapter(getApplicationContext()).getEntryItem(entryID);
        //setTitle(getString(R.string.output_check) + " № " + entryID ); //установить заголовок
        setTitle(getString(R.string.output_check) + " № " + entryID + " : " + cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_VENDOR))); //установить заголовок

        typeId = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_TYPE_ID));
        weightGross = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_GROSS));
        weightTare = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_TARE));
        weightNetto = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_NETTO));
        //is_ready = cursor.getInt(cursor.getColumnIndex(InputCheckDBAdapter.KEY_IS_READY));
        cursor.close();

        //threadUpdate=new ThreadUpdate();
        //threadAutoWeight=new ThreadAutoWeight();

        if (weightGross == 0 || weightTare == 0) {
            threadUpdate.executeStart();
        }

        weightTextView = (WeightTextView) findViewById(R.id.weightTextView);
        progressBarWeight = (ProgressBar) findViewById(R.id.progressBarWeight);

        textViewGross = (TextView) findViewById(R.id.textViewGross);
        textViewGross.setText(weightGross + "кг");

        textViewNetto = (EditText) findViewById(R.id.textViewNetto);
        textViewNetto.setText(String.valueOf(weightNetto));

        textViewTare = (TextView) findViewById(R.id.textViewTare);
        textViewTare.setText(weightTare + "кг");

        Button buttonZero = (Button) findViewById(R.id.buttonZero);
        buttonZero.setOnLongClickListener(longClickListener);

        buttonGross = (Button) findViewById(R.id.buttonGross);
        buttonGross.setOnLongClickListener(longClickListener);

        buttonTare = (Button) findViewById(R.id.buttonTare);
        buttonTare.setOnLongClickListener(longClickListener);

        buttonFinish = (ImageView) findViewById(R.id.buttonFinish);
        buttonFinish.setOnClickListener(clickListener);

        ImageView buttonPage = (ImageView) findViewById(R.id.buttonPage);
        buttonPage.setOnClickListener(clickListener);

        if (weightGross > 0) {
            buttonGross.setEnabled(false);
            if (weightTare == 0) {
                weightType = WeightType.TARE;
            } else {
                buttonTare.setEnabled(false);
                weightType = WeightType.NETTO;
                //is_ready = 1;
            }
        } else {
            weightType = WeightType.GROSS;
            buttonTare.setEnabled(false);
        }

        threadAutoWeight.executeStart();

        spinnerType = (Spinner) findViewById(R.id.spinnerType);
        loadTypeSpinnerData();
        spinnerType.setSelection(typeId);
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                typeId = i;
                type = ((TextView) view.findViewById(R.id.text1)).getText().toString();
                new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_TYPE_ID, typeId);
                new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_TYPE, type);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }

        });

        textViewNetto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!editable.toString().isEmpty()) {
                    weightNetto = Integer.valueOf(editable.toString());
                    if (weightNetto > 0) {
                        new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_WEIGHT_NETTO, weightNetto * -1);
                        weightType = WeightType.NETTO;
                        weightTypeControl();
                    } else {
                        weightType = WeightType.GROSS;
                    }


                }//else
                //editTextPrice.setText(String.valueOf(0));
            }
        });

        weightTextView.setMax(Scales.COUNT_STABLE);
        weightTextView.setSecondaryProgress(Scales.numStable = 0);

        progressBarWeight.setMax(Scales.vClass.getMarginTenzo());
        progressBarWeight.setSecondaryProgress(Scales.vClass.getLimitTenzo());

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                assert action != null;
                if (action.equals(ACTION_STORE_WEIGHTING)) {
                    weightControl(intent.getIntExtra(EXTRA_WEIGHT_STABLE, 0), weightType);
                } else if (action.equals(ACTION_STOP_WEIGHTING)) {
                    threadAutoWeight.tempTypeState = weightTypeControl();
                    buttonFinish.setEnabled(true);
                    flagExit = true;
                } else if (action.equals(ACTION_START_WEIGHTING)) {
                    buttonFinish.setEnabled(false);
                    flagExit = false;
                }
            }
        };
        // создаем фильтр для BroadcastReceiver
        IntentFilter intentFilter = new IntentFilter(ACTION_START_WEIGHTING);
        intentFilter.addAction(ACTION_STOP_WEIGHTING);
        intentFilter.addAction(ACTION_STORE_WEIGHTING);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void exit() {
        threadUpdate.cancel(false);
        threadAutoWeight.cancel(false);
        while (!threadUpdate.closed) ;
        while (!threadAutoWeight.closed) ;
        if (weightType == WeightType.NETTO) {
            //String str = new CheckDBAdapter(getApplicationContext()).getKeyNumberBt(entryID);
            //new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_NUMBER_BT, str.replace(ActivityScales.ID_OUTPUT,""));
            new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_IS_READY, 1);
            new TaskDBAdapter(getApplicationContext()).insertNewTask(TaskDBAdapter.TYPE_CHECK_DISK, entryID, 0, "output");
            startService(new Intent(this, ServiceGetDateServer.class).setAction("new_check"));
            //startService(new Intent(this, ServiceSendDateServer.class).setAction("new_check"));
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        if (flagExit) {
            super.onBackPressed();
            exit();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    final View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            switch (v.getId()) {
                case R.id.buttonZero:
                    vibrator.vibrate(100);
                    new ProgressZeroTask(ActivityOutputCheck.this).execute();
                    break;
                case R.id.buttonGross:
                    if (weightControl(Scales.weight, WeightType.GROSS)) {
                        buttonGross.setEnabled(false);
                        buttonIsClick = true;
                        buttonFinish.setEnabled(true);
                        flagExit = true;
                    }
                    break;
                case R.id.buttonTare:
                    if (weightControl(Scales.weight, WeightType.TARE))
                        buttonIsClick = true;
                    weightTypeControl();
                    break;
            }
            return false;
        }
    };

    final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.buttonFinish:
                    exit();
                    break;
                case R.id.buttonPage:
                    vibrator.vibrate(100);
                    startActivity(new Intent(getBaseContext(), ActivityViewCheck.class).putExtra("id", entryID));
                    exit();
                    break;
            }
        }
    };

    private void loadTypeSpinnerData() {
        Cursor cursor = new TypeDBAdapter(getApplicationContext()).getAllEntries();
        if (cursor.getCount() > 0) {
            String[] columns = {TypeDBAdapter.KEY_TYPE};
            int[] to = {R.id.text1};
            SimpleCursorAdapter typeAdapter = new SimpleCursorAdapter(this, R.layout.type_spinner, cursor, columns, to);
            typeAdapter.setDropDownViewResource(R.layout.type_spinner_dropdown_item);
            spinnerType.setAdapter(typeAdapter);
        }
    }

    private boolean weightControl(int weight, WeightType button) {
        if (weightType == button)
            switch (weightType) {
                case GROSS:
                    if (weight > 0) {
                        weightGross = weight;
                        textViewGross.setText(weightGross + "кг");
                        new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_WEIGHT_GROSS, weightGross);
                        vibrator.vibrate(200); //вибрация
                        buttonFinish.setEnabled(true);
                        flagExit = true;
                        return true;
                    }
                    break;
                case TARE:
                    weightTare = weight;
                    textViewTare.setText(weightTare + "кг");
                    new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_WEIGHT_TARE, weightTare);
                    textViewNetto.setText(String.valueOf(sumNetto()));
                    vibrator.vibrate(200); //вибрация
                    buttonFinish.setEnabled(true);
                    flagExit = true;
                    return true;
                case NETTO:
                    //is_ready = 1;
                    threadUpdate.cancel(false);
                    buttonFinish.setEnabled(true);
                    flagExit = true;
                    break;
                default:
                    return false;
            }
        return false;
    }

    private int sumNetto() {
        weightNetto = weightTare - weightGross;
        if (weightNetto > 0) {
            new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_WEIGHT_NETTO, weightNetto * -1);
            return weightNetto;
        }
        return 0;
    }

    private WeightType weightTypeControl() {
        switch (weightType) {
            case GROSS:
                weightType = WeightType.TARE;
                buttonGross.setEnabled(false);
                buttonTare.setEnabled(true);
                break;
            case TARE:
                weightType = WeightType.NETTO;
                buttonTare.setEnabled(false);
                weightControl(0, weightType);
                break;
            case NETTO:

                break;
            default:
                weightType = WeightType.GROSS;
                break;
        }
        buttonFinish.setEnabled(true);
        flagExit = true;
        return weightType;
    }

}