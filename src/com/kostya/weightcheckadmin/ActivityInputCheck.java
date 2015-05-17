package com.kostya.weightcheckadmin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import java.util.concurrent.TimeUnit;

public class ActivityInputCheck extends Activity {
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
            //progressBarBattery.updateProgress(Scales.battery);
            weightTextView.setSecondaryProgress(Scales.numStable);
        }
    }

    private class ThreadAutoWeight extends AsyncTask<Void, Integer, Boolean> { //автосуммирование и передача в интернет
        private boolean closed = true;
        private WeightType tempTypeState;
        private static final boolean pause = true;

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
                //while(!isCancelled() && ((Scales.vClass.getWeightScale() < Scales.autoCapture && !buttonIsClick) || !pause) ){try { Thread.sleep(10);} catch (InterruptedException ignored) {}}//ждём начала нагружения
                while (!isCancelled() && (Scales.vClass.getWeightScale() < Scales.autoCapture && !buttonIsClick || !pause)) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                }//ждём начала нагружения
                sendBroadcast(new Intent(ACTION_START_WEIGHTING));
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

                if (!isCancelled())
                    try {
                        TimeUnit.SECONDS.sleep(2);/*Thread.sleep(2000);*/
                    } catch (InterruptedException ignored) {
                    }
                else {
                    if (Scales.flagStable && weightType == WeightType.TARE)//Если тара зафоксирована и выход через кнопку назад
                        weightType = WeightType.NETTO;
                    break;
                }

                if (weightType == WeightType.TARE)
                    threadAutoWeight.cancel(false);

                if (tempTypeState == weightType)
                    sendBroadcast(new Intent(ACTION_STOP_WEIGHTING));
                else if (weightType == WeightType.NETTO) {
                    threadAutoWeight.cancel(false);
                    sendBroadcast(new Intent(ACTION_NETTO_WEIGHTING));
                }
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
    private ProgressBar progressBarWeight/*, progressBarStable*/;
    //private BatteryProgressBar progressBarBattery;
    private WeightTextView weightTextView;
    private TextView /*textViewWeight,*/ textViewGross, textViewNetto, textViewTare, textViewSum;
    private Button buttonGross, buttonTare;
    private Spinner spinnerType;
    private EditText editTextPrice;
    private ImageView buttonFinish;

    private boolean buttonIsClick;

    private static final String ACTION_STOP_WEIGHTING = "stop_weighting";
    private static final String ACTION_START_WEIGHTING = "start_weighting";
    private static final String ACTION_STORE_WEIGHTING = "store_weighting";
    private static final String ACTION_NETTO_WEIGHTING = "netto_weighting";
    private static final String EXTRA_WEIGHT_STABLE = "weightStable";

    /*private final int TARE_TYPE = 10;
    private final int GROSS_TYPE = 20;
    private final int NETTO_TYPE = 30;*/

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
    private int price;
    private float priceSum;
    private String type;
    //private int type_item_id;
    private int typeId;
    private int vendorId;
    //private int is_ready = 0;

    private boolean flagExit = true;
    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_check);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        entryID = Integer.valueOf(getIntent().getStringExtra("id"));
        Cursor cursor = new CheckDBAdapter(getApplicationContext()).getEntryItem(entryID);
        setTitle(getString(R.string.input_check) + " № " + entryID + ' ' + /*getString(R.string.Vendor)+*/": " + cursor.getString(cursor.getColumnIndex(CheckDBAdapter.KEY_VENDOR))); //установить заголовок

        typeId = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_TYPE_ID));
        weightGross = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_GROSS));
        weightTare = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_TARE));
        weightNetto = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_WEIGHT_NETTO));
        vendorId = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_VENDOR_ID));
        //is_ready = cursor.getInt(cursor.getColumnIndex(InputCheckDBAdapter.KEY_IS_READY));
        price = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_PRICE));
        priceSum = cursor.getInt(cursor.getColumnIndex(CheckDBAdapter.KEY_PRICE_SUM));
        cursor.close();

        //textViewWeight=(TextView)findViewById(R.id.textViewWeight);
        weightTextView = new WeightTextView(this);
        weightTextView = (WeightTextView) findViewById(R.id.weightTextView);
        progressBarWeight = (ProgressBar) findViewById(R.id.progressBarWeight);
        //progressBarBattery=(BatteryProgressBar)findViewById(R.id.progressBarBattery);
        //progressBarBattery=ActivityScales.progressBarBattery;
        //progressBarStable=(ProgressBar)findViewById(R.id.progressBarStable);

        textViewGross = (TextView) findViewById(R.id.textViewGross);
        textViewGross.setText(String.valueOf(weightGross) + ' ' + getString(R.string.scales_kg));

        textViewNetto = (TextView) findViewById(R.id.textViewNetto);
        textViewNetto.setText(String.valueOf(weightNetto) + ' ' + getString(R.string.scales_kg));

        textViewTare = (TextView) findViewById(R.id.textViewTare);
        textViewTare.setText(String.valueOf(weightTare) + ' ' + getString(R.string.scales_kg));

        Button buttonZero = (Button) findViewById(R.id.buttonZero);
        buttonZero.setOnLongClickListener(longClickListener);

        buttonGross = (Button) findViewById(R.id.buttonGross);
        buttonGross.setOnLongClickListener(longClickListener);

        buttonTare = (Button) findViewById(R.id.buttonTare);
        buttonTare.setOnLongClickListener(longClickListener);

        buttonFinish = (ImageView) findViewById(R.id.buttonFinish);
        buttonFinish.setOnClickListener(clickListener);

        ImageView imageViewPage = (ImageView) findViewById(R.id.imageViewPage);
        imageViewPage.setOnClickListener(clickListener);


        spinnerType = (Spinner) findViewById(R.id.spinnerType);
        loadTypeSpinnerData();
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //type_item_id = i;
                typeId = (int) l;
                type = ((TextView) view.findViewById(R.id.text1)).getText().toString();
                price = new TypeDBAdapter(getApplicationContext()).getPriceColumn((int) l);
                new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_TYPE_ID, typeId);
                new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_TYPE, type);
                editTextPrice.setText(String.valueOf(price));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }

        });

        for (int i = 0; i < spinnerType.getCount(); i++) {
            long object = spinnerType.getItemIdAtPosition(i);
            if ((int) object == typeId) {
                spinnerType.setSelection(i);
                break;
            }
        }

        if (weightGross > 0) {
            buttonGross.setEnabled(false);
            if (weightTare == 0) {
                weightType = WeightType.TARE;
            } else {
                buttonTare.setEnabled(false);
                weightType = WeightType.NETTO;
            }
        } else {
            weightType = WeightType.GROSS;
            buttonTare.setEnabled(false);
        }

        editTextPrice = (EditText) findViewById(R.id.editTextPrice);
        if (price != 0)
            editTextPrice.setText(String.valueOf(price));
        editTextPrice.clearFocus();

        textViewSum = (TextView) findViewById(R.id.textViewSum);
        textViewSum.setText(R.string.Total + " " + priceSum + " грв");

        editTextPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!editable.toString().isEmpty()) {
                    price = Integer.valueOf(editable.toString());
                    new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_PRICE, price);
                    new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_PRICE_SUM, sumTotal());
                    new TypeDBAdapter(getApplicationContext()).updateEntry(typeId, TypeDBAdapter.KEY_PRICE, price);
                    textViewSum.setText(priceSum + " грв");
                }//else
                //editTextPrice.setText(String.valueOf(0));
            }
        });

        //progressBarBattery.setMax(100);
        //weightTextView.setMax(Scales.vClass.getLimitTenzo());
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
                    buttonFinish.setAlpha(255);
                    flagExit = true;
                } else if (action.equals(ACTION_START_WEIGHTING)) {
                    buttonFinish.setEnabled(false);
                    buttonFinish.setAlpha(100);
                    flagExit = false;
                } else if (action.equals(ACTION_NETTO_WEIGHTING)) {
                    buttonFinish.setEnabled(true);
                    buttonFinish.setAlpha(255);
                    flagExit = true;
                }
            }
        };
        // создаем фильтр для BroadcastReceiver
        IntentFilter intentFilter = new IntentFilter(ACTION_START_WEIGHTING);
        intentFilter.addAction(ACTION_STOP_WEIGHTING);
        intentFilter.addAction(ACTION_STORE_WEIGHTING);
        intentFilter.addAction(ACTION_NETTO_WEIGHTING);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(broadcastReceiver, intentFilter);

        if (weightGross == 0 || weightTare == 0) {
            threadUpdate.executeStart();
        }

        threadAutoWeight.executeStart();

    }

    final View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            switch (v.getId()) {
                case R.id.buttonZero:
                    vibrator.vibrate(100);
                    new ProgressZeroTask(ActivityInputCheck.this).execute();
                    break;
                case R.id.buttonGross:
                    if (weightControl(Scales.weight, WeightType.GROSS)) {
                        buttonGross.setEnabled(false);
                        buttonIsClick = true;
                        buttonFinish.setEnabled(true);
                        buttonFinish.setAlpha(255);
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
                case R.id.imageViewPage:
                    vibrator.vibrate(100);
                    startActivity(new Intent(getBaseContext(), ActivityViewCheck.class).putExtra("id", entryID));
                    exit();
                    break;
            }

        }
    };

    private void exit() {
        threadUpdate.cancel(false);
        threadAutoWeight.cancel(false);
        while (!threadUpdate.closed) ;
        while (!threadAutoWeight.closed) ;
        if (weightType == WeightType.NETTO) {
            new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_IS_READY, 1);
            new TaskDBAdapter(getApplicationContext()).insertNewTask(TaskDBAdapter.TYPE_CHECK_DISK, entryID, vendorId, "input");
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
                        textViewGross.setText(String.valueOf(weightGross) + ' ' + getString(R.string.scales_kg));
                        new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_WEIGHT_GROSS, weightGross);
                        vibrator.vibrate(200); //вибрация
                        buttonFinish.setEnabled(true);
                        buttonFinish.setAlpha(255);
                        flagExit = true;
                        return true;
                    }
                    break;
                case TARE:
                    weightTare = weight;
                    textViewTare.setText(String.valueOf(weightTare) + ' ' + getString(R.string.scales_kg));
                    new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_WEIGHT_TARE, weightTare);
                    textViewNetto.setText(String.valueOf(sumNetto()) + ' ' + getString(R.string.scales_kg));
                    new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_PRICE_SUM, sumTotal());
                    textViewSum.setText(priceSum + " грв");
                    vibrator.vibrate(200); //вибрация
                    buttonFinish.setEnabled(true);
                    buttonFinish.setAlpha(255);
                    flagExit = true;
                    return true;
                case NETTO:
                    //is_ready = 1;
                    threadUpdate.cancel(false);
                    buttonFinish.setEnabled(true);
                    buttonFinish.setAlpha(255);
                    flagExit = true;
                    break;
                default:
                    return false;
            }
        return false;
    }

    private int sumNetto() {
        weightNetto = weightGross - weightTare;
        new CheckDBAdapter(getApplicationContext()).updateEntry(entryID, CheckDBAdapter.KEY_WEIGHT_NETTO, weightNetto);
        return weightNetto;
    }

    private float sumTotal() {
        priceSum = (float) weightNetto * price / 1000;
        return priceSum;
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
            default:
                weightType = WeightType.GROSS;
                break;
        }
        buttonFinish.setEnabled(true);
        buttonFinish.setAlpha(255);
        flagExit = true;
        return weightType;
    }

}