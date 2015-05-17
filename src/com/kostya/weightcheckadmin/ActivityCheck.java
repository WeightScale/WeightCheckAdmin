package com.kostya.weightcheckadmin;


import android.app.ProgressDialog;
import android.content.ContentQueryMap;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;
import com.konst.module.HandlerWeightUpdate;
import com.konst.module.ScaleModule;
import com.kostya.weightcheckadmin.provider.CheckDBAdapter;


import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ActivityCheck extends FragmentActivity implements View.OnClickListener/*, View.OnLongClickListener, InputFragment.onSomeEventListener*/ {

    protected interface OnCheckEventListener {
        void someEvent();
    }

    private class ZeroThread extends Thread {
        private final ProgressDialog dialog;

        ZeroThread(Context context) {
            // Создаём новый поток
            super(getString(R.string.Zeroing));
            dialog = new ProgressDialog(context);
            dialog.setCancelable(false);
            dialog.setIndeterminate(false);
            dialog.show();
            dialog.setContentView(R.layout.custom_progress_dialog);
            TextView tv1 = (TextView) dialog.findViewById(R.id.textView1);
            tv1.setText(R.string.Zeroing);
            //start(); // Запускаем поток
        }

        @Override
        public void run() {
            ScaleModule.setOffsetScale();
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    private class AutoWeightThread extends Thread {
        private boolean start;
        private boolean cancelled;

        final int ACTION_STOP_WEIGHTING = 1;
        final int ACTION_START_WEIGHTING = 2;
        final int ACTION_STORE_WEIGHTING = 3;
        final int ACTION_UPDATE_PROGRESS = 4;

        @Override
        public synchronized void start() {
            setPriority(Thread.MIN_PRIORITY);
            super.start();
            start = true;
        }

        @Override
        public void run() {
            try {Thread.sleep(50); } catch (InterruptedException ignored) { }
            while (!cancelled) {

                weightViewIsSwipe = false;
                numStable = 0;

                while (!cancelled && !isCapture() && !weightViewIsSwipe) {                                              //ждём начала нагружения
                    try {Thread.sleep(50); } catch (InterruptedException ignored) { }
                }
                handler.sendMessage(handler.obtainMessage(ACTION_START_WEIGHTING));
                isStable = false;
                while (!cancelled && !(isStable || weightViewIsSwipe)) {                                                //ждем стабилизации веса или нажатием выбора веса
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    if (!touchWeightView) {                                                                              //если не прикасаемся к индикатору тогда стабилизируем вес
                        isStable = processStable(getWeightToStepMeasuring(moduleWeight));
                        handler.sendMessage(handler.obtainMessage(ACTION_UPDATE_PROGRESS, numStable, 0));
                    }
                }
                numStable = COUNT_STABLE;
                if (cancelled) {
                    break;
                }
                if (isStable) {
                    handler.sendMessage(handler.obtainMessage(ACTION_STORE_WEIGHTING, moduleWeight, 0));                 //сохраняем стабильный вес
                }

                weightViewIsSwipe = false;

                while (!cancelled && getWeightToStepMeasuring(moduleWeight) >= Main.default_min_auto_capture) {
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}                                   // ждем разгрузки весов
                }
                vibrator.vibrate(100);
                handler.sendMessage(handler.obtainMessage(ACTION_UPDATE_PROGRESS, 0, 0));
                if (cancelled) {
                    if (isStable && weightType == WeightType.SECOND) {                                                  //Если тара зафоксирована и выход через кнопку назад
                        weightType = WeightType.NETTO;
                    }
                    break;
                }
                try { TimeUnit.SECONDS.sleep(2);  } catch (InterruptedException ignored) { }                            //задержка

                if (weightType == WeightType.SECOND) {
                    cancelled = true;
                }

                handler.sendMessage(handler.obtainMessage(ACTION_STOP_WEIGHTING));
            }
            start = false;
        }

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                switch (msg.what) {
                    case ACTION_STORE_WEIGHTING:
                        saveWeight(msg.arg1);
                        break;
                    case ACTION_STOP_WEIGHTING:
                        weightTypeUpdate();
                        buttonFinish.setEnabled(true);
                        buttonFinish.setAlpha(255);
                        ((OnCheckEventListener) mTabsAdapter.getCurrentFragment()).someEvent();
                        flagExit = true;
                        break;
                    case ACTION_START_WEIGHTING:
                        buttonFinish.setEnabled(false);
                        buttonFinish.setAlpha(100);
                        flagExit = false;
                        break;
                    case ACTION_UPDATE_PROGRESS:
                        weightTextView.setSecondaryProgress(msg.arg1);
                        break;
                    default:
                }
            }
        };

        private void cancel() {
            cancelled = true;
        }

        public boolean isStart() {
            return start;
        }
    }

    private final AutoWeightThread autoWeightThread = new AutoWeightThread();
    CheckDBAdapter checkTable;
    private Vibrator vibrator; //вибратор
    private ProgressBar progressBarWeight;
    private WeightTextView weightTextView;
    private TabHost mTabHost;
    private TabsAdapter mTabsAdapter;
    private ImageView buttonFinish;
    private SimpleGestureFilter detectorWeightView;
    private Drawable dProgressWeight, dWeightDanger;

    protected enum WeightType {
        FIRST,
        SECOND,
        NETTO
    }

    public WeightType weightType;

    public static final int COUNT_STABLE = 64;                                                                          //колличество раз стабильно был вес

    ContentValues values = new ContentValues();
    public int entryID;
    public int numStable;
    protected boolean isStable;
    int moduleWeight;
    int moduleSensorValue;
    protected int tempWeight;

    private boolean flagExit = true;
    private boolean touchWeightView;
    private boolean weightViewIsSwipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        checkTable = new CheckDBAdapter(this);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        entryID = Integer.valueOf(getIntent().getStringExtra("id"));
        try {
            values = checkTable.getValuesItem(entryID);
        }catch (Exception e){
            exit();
        }

        setTitle(getString(R.string.input_check) + " № " + entryID + ' ' + ": " + values.getAsString(CheckDBAdapter.KEY_VENDOR)); //установить заголовок
        setupTabHost(savedInstanceState);
        setupWeightView();

        progressBarWeight = (ProgressBar) findViewById(R.id.progressBarWeight);
        progressBarWeight.setMax(ScaleModule.getMarginTenzo());
        progressBarWeight.setSecondaryProgress(ScaleModule.getLimitTenzo());

        buttonFinish = (ImageView) findViewById(R.id.buttonFinish);
        buttonFinish.setOnClickListener(this);

        findViewById(R.id.imageViewPage).setOnClickListener(this);

        if (values.getAsInteger(CheckDBAdapter.KEY_WEIGHT_FIRST) > 0) {
            weightType = values.getAsInteger(CheckDBAdapter.KEY_WEIGHT_SECOND) == 0 ? WeightType.SECOND : WeightType.NETTO;
        } else {
            weightType = WeightType.FIRST;
        }

        if (values.getAsInteger(CheckDBAdapter.KEY_WEIGHT_FIRST) == 0 || values.getAsInteger(CheckDBAdapter.KEY_WEIGHT_SECOND) == 0) {
            ScaleModule.processUpdate(true, handlerWeight);
        }
    }

    private void setupTabHost(Bundle savedInstanceState) {
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
        mTabsAdapter.addTab(mTabHost.newTabSpec("input").setIndicator(createTabView(this, "приход")), InputFragment.class);
        mTabsAdapter.addTab(mTabHost.newTabSpec("output").setIndicator(createTabView(this, "расход")), OutputFragment.class);
        switch (values.getAsInteger(CheckDBAdapter.KEY_DIRECT)) {
            case CheckDBAdapter.DIRECT_DOWN:
                mTabHost.setCurrentTab(0);
                break;
            case CheckDBAdapter.DIRECT_UP:
                mTabHost.setCurrentTab(1);
                break;
            default:
        }

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", mTabHost.getCurrentTabTag()); //save the tab selected
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!autoWeightThread.isStart()) {
            autoWeightThread.start();
        }
    }

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
            default:
        }
    }

    protected void exit() {
        ScaleModule.processUpdate(false, null);
        autoWeightThread.cancel();
        while (autoWeightThread.isStart()) ;
        if (weightType == WeightType.NETTO) {
            values.put(CheckDBAdapter.KEY_IS_READY, 1);
            checkTable.setCheckReady(entryID);
            startActivity(new Intent(getBaseContext(), ActivityViewCheck.class).putExtra("id", entryID));
        }
        checkTable.updateEntry(entryID, values);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (flagExit) {
            super.onBackPressed();
            exit();
        }
    }

    private static View createTabView(final Context context, final CharSequence text) {
        View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
        TextView tv = (TextView) view.findViewById(R.id.tabsText);
        tv.setText(text);
        return view;
    }

    public boolean isCapture() {
        boolean capture = false;
        while (getWeightToStepMeasuring(moduleWeight) > Main.autoCapture) {
            if (capture) {
                return true;
            } else {
                try {
                    TimeUnit.SECONDS.sleep(Main.timeDelayDetectCapture);
                } catch (InterruptedException ignored) {
                }
                capture = true;
            }
        }
        return false;
    }

    public boolean processStable(int weight) {
        if (tempWeight - Main.stepMeasuring <= weight && tempWeight + Main.stepMeasuring >= weight) {
            if (++numStable >= COUNT_STABLE) {
                return true;
            }
        } else {
            numStable = 0;
        }
        tempWeight = weight;
        return false;
    }

    private int getWeightToStepMeasuring(int weight) {
        return weight / Main.stepMeasuring * Main.stepMeasuring;
    }

    private void setupWeightView() {

        weightTextView = new WeightTextView(this);
        weightTextView = (WeightTextView) findViewById(R.id.weightTextView);
        weightTextView.setMax(COUNT_STABLE);
        weightTextView.setSecondaryProgress(numStable = 0);
        dProgressWeight = getResources().getDrawable(R.drawable.progress_weight);
        dWeightDanger = getResources().getDrawable(R.drawable.progress_weight_danger);

        SimpleGestureFilter.SimpleGestureListener weightViewGestureListener = new SimpleGestureFilter.SimpleGestureListener() {
            @Override
            public void onSwipe(int direction) {

                switch (direction) {
                    case SimpleGestureFilter.SWIPE_RIGHT:
                    case SimpleGestureFilter.SWIPE_LEFT:
                        if (saveWeight(moduleWeight)) {
                            weightViewIsSwipe = true;
                            buttonFinish.setEnabled(true);
                            buttonFinish.setAlpha(255);
                            flagExit = true;
                        }
                        if (weightType == WeightType.SECOND) {
                            weightTypeUpdate();
                        }
                        break;
                    default:
                }
            }

            @Override
            public void onDoubleTap() {
                weightTextView.setSecondaryProgress(0);
                vibrator.vibrate(100);
                new ZeroThread(ActivityCheck.this).start();
            }
        };

        detectorWeightView = new SimpleGestureFilter(this, weightViewGestureListener);
        detectorWeightView.setSwipeMinVelocity(50);
        weightTextView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                detectorWeightView.setSwipeMaxDistance(v.getMeasuredWidth());
                detectorWeightView.setSwipeMinDistance(detectorWeightView.getSwipeMaxDistance() / 3);
                detectorWeightView.onTouchEvent(event);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        touchWeightView = true;
                        vibrator.vibrate(5);
                        int progress = (int) (event.getX() / (detectorWeightView.getSwipeMaxDistance() / weightTextView.getMax()));
                        weightTextView.setSecondaryProgress(progress);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        touchWeightView = false;
                        break;
                    default:
                }
                return false;
            }
        });
    }

    private boolean saveWeight(int weight/*, WeightType type*/) {
        boolean flag = false;
        switch (weightType) {
            case FIRST:
                if (weight > 0) {
                    values.put(CheckDBAdapter.KEY_WEIGHT_FIRST, weight);
                    vibrator.vibrate(100); //вибрация
                    flag = true;
                }
                break;
            case SECOND:
                values.put(CheckDBAdapter.KEY_WEIGHT_SECOND, weight);
                int total = sumNetto();
                values.put(CheckDBAdapter.KEY_PRICE_SUM, total);
                vibrator.vibrate(100); //вибрация
                flag = true;
                break;
            case NETTO:
                ScaleModule.processUpdate(false, handlerWeight);
                exit();
                break;
        }
        if (flag) {
            ((OnCheckEventListener) mTabsAdapter.getCurrentFragment()).someEvent();
            buttonFinish.setEnabled(true);
            buttonFinish.setAlpha(255);
            flagExit = true;
        }
        return flag;
    }

    public int sumNetto() {
        int netto = values.getAsInteger(CheckDBAdapter.KEY_WEIGHT_FIRST) - values.getAsInteger(CheckDBAdapter.KEY_WEIGHT_SECOND);
        values.put(CheckDBAdapter.KEY_WEIGHT_NETTO, netto);
        return netto;
    }

    public float sumTotal(int netto) {
        return (float) netto * values.getAsInteger(CheckDBAdapter.KEY_PRICE) / 1000;
    }

    private void weightTypeUpdate() {
        switch (weightType) {
            case FIRST:
                weightType = WeightType.SECOND;
                break;
            case SECOND:
                weightType = WeightType.NETTO;
                saveWeight(0);
                break;
            default:
                weightType = WeightType.FIRST;
        }
        buttonFinish.setEnabled(true);
        buttonFinish.setAlpha(255);
        flagExit = true;
    }

    private class TabsAdapter extends FragmentPagerAdapter implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {

        private final FragmentManager fragmentManager;
        private Fragment mCurrentFragment;
        private final Context mContext;
        private final TabHost mTabHost;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<>();

        private class TabInfo {
            private final String tag;
            private final Class<?> mClass;
            private final Bundle args;

            TabInfo(final String _tag, final Class<?> _class, final Bundle _args) {
                tag = _tag;
                mClass = _class;
                args = _args;
            }

            public Class<?> getMClass() {
                return mClass;
            }

            public Bundle getArgs() {
                return args;
            }
        }

        private class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(final Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(final String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabsAdapter(final FragmentActivity activity, final TabHost tabHost, final ViewPager pager) {
            super(activity.getSupportFragmentManager());

            fragmentManager = activity.getSupportFragmentManager();
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(final TabHost.TabSpec tabSpec, final Class<?> _class) {

            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();
            TabInfo info = new TabInfo(tag, _class, null);

            mTabs.add(info);
            mTabHost.addTab(tabSpec);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(final int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.getMClass().getName(), info.getArgs());
        }

        @Override
        public void onTabChanged(final String tabId) {
            int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
        }

        @Override
        public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(final int position) {

            if (position == 0) {
                values.put(CheckDBAdapter.KEY_DIRECT, CheckDBAdapter.DIRECT_DOWN);
            } else if (position == 1) {
                values.put(CheckDBAdapter.KEY_DIRECT, CheckDBAdapter.DIRECT_UP);
            }
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
        }

        @Override
        public void onPageScrollStateChanged(final int state) {}

        public Fragment getCurrentFragment() { return mCurrentFragment; }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (!object.equals(mCurrentFragment)) {
                mCurrentFragment = (Fragment) object;
            }
            super.setPrimaryItem(container, position, object);
        }

    }

    final HandlerWeightUpdate handlerWeight = new HandlerWeightUpdate() {

        @Override
        public int handlerWeight(final HandlerWeightUpdate.Result what, final int weight, final int sensor) {

            runOnUiThread(new Runnable() {
                Rect bounds;
                @Override
                public void run() {
                    switch (what) {
                        case RESULT_WEIGHT_NORMAL:
                            moduleWeight = weight;
                            moduleSensorValue = sensor;
                            progressBarWeight.setProgress(sensor);
                            bounds = progressBarWeight.getProgressDrawable().getBounds();
                            weightTextView.updateProgress(getWeightToStepMeasuring(weight), Color.BLACK, getResources().getDimension(R.dimen.text_big));
                            progressBarWeight.setProgressDrawable(dProgressWeight);
                            progressBarWeight.getProgressDrawable().setBounds(bounds);
                            break;
                        case RESULT_WEIGHT_LIMIT:
                            moduleWeight = weight;
                            moduleSensorValue = sensor;
                            progressBarWeight.setProgress(sensor);
                            bounds = progressBarWeight.getProgressDrawable().getBounds();
                            weightTextView.updateProgress(getWeightToStepMeasuring(weight), Color.RED, getResources().getDimension(R.dimen.text_big));
                            progressBarWeight.setProgressDrawable(dWeightDanger);
                            progressBarWeight.getProgressDrawable().setBounds(bounds);
                            break;
                        case RESULT_WEIGHT_MARGIN:
                            moduleWeight = weight;
                            moduleSensorValue = sensor;
                            progressBarWeight.setProgress(sensor);
                            weightTextView.updateProgress(getString(R.string.OVER_LOAD), Color.RED, getResources().getDimension(R.dimen.text_large_xx));
                            vibrator.vibrate(100);
                            break;
                        case RESULT_WEIGHT_ERROR:
                            weightTextView.updateProgress(getString(R.string.NO_CONNECT), Color.BLACK, getResources().getDimension(R.dimen.text_large_xx));
                            progressBarWeight.setProgress(0);
                            break;
                    }

                }
            });
            return 20; // Обновляем через милисикунды
        }
    };
}

