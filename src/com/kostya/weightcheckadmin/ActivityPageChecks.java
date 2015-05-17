package com.kostya.weightcheckadmin;


import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;

import java.util.Map;

public class ActivityPageChecks extends Activity {

    //Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pos = getIntent().getIntExtra("position", 0);
        //long checkId = getIntent().getIntExtra("id", 1);
        setTitle(getString(R.string.app_name) + ' ' + "Чек"); //установить заголовок*/

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        String[] columns = {
                CheckDBAdapter.KEY_ID,
                CheckDBAdapter.KEY_DATE_CREATE,
                CheckDBAdapter.KEY_TIME_CREATE,
                CheckDBAdapter.KEY_VENDOR,
                CheckDBAdapter.KEY_WEIGHT_GROSS,
                CheckDBAdapter.KEY_WEIGHT_TARE,
                CheckDBAdapter.KEY_WEIGHT_NETTO,
                CheckDBAdapter.KEY_TYPE,
                CheckDBAdapter.KEY_PRICE,
                CheckDBAdapter.KEY_PRICE_SUM};

        int[] to = {
                R.id.check_id,
                R.id.date,
                R.id.time,
                R.id.vendor,
                R.id.gross_row,
                R.id.tare_row,
                R.id.netto_row,
                R.id.type_row,
                R.id.price_row,
                R.id.sum_row};
        Cursor cursor = new CheckDBAdapter(getApplicationContext()).getAllEntries(CheckDBAdapter.VISIBLE);
        //ContentQueryMap mQueryMap = new ContentQueryMap(cursor, BaseColumns._ID, true, null);
        //Map<String,ContentValues> map = mQueryMap.getRows();
        MyAdapter myAdapter = new MyAdapter(getApplicationContext(), R.layout.page_checks, cursor, columns, to);
        ViewPager pager = new ViewPager(this);
        pager.setAdapter(myAdapter);
        pager.setCurrentItem(pos);
        setContentView(pager);


    }

    private class MyAdapter extends PagerAdapter {
        final Context context;
        View mCurrentView;
        private final Cursor mCursor;
        final int count;
        final int layout;
        final String[] mColumns;
        final int[] mTo;
        protected int[] mFrom;
        private SimpleCursorAdapter.ViewBinder mViewBinder;

        public MyAdapter(Context context, int layout, Cursor cursor, String[] columns, int... to) {
            this.context = context;
            this.layout = layout;
            mColumns = columns;
            mTo = to;
            mCursor = cursor;
            count = cursor.getCount();
            findColumns(mColumns);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            LayoutInflater inflater = (LayoutInflater) container.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            position %= mCursor.getCount();
            mCursor.moveToPosition(position);
            View view = inflater.inflate(layout, null);
            LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.layoutImageView);
            linearLayout.setVisibility(View.VISIBLE);
            ImageView imageViewBack = (ImageView) view.findViewById(R.id.imageViewBack);
            imageViewBack.setOnClickListener(onClickListener);
            ImageView imageViewMail = (ImageView) view.findViewById(R.id.imageViewMail);
            imageViewMail.setOnClickListener(onClickListener);
            ImageView imageViewMessage = (ImageView) view.findViewById(R.id.imageViewMessage);
            imageViewMessage.setOnClickListener(onClickListener);
            bindView(view, mCursor);
            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            mCurrentView = (View) object;
        }

        private void findColumns(String... from) {
            int count = from.length;
            if (mFrom == null || mFrom.length != count) {
                mFrom = new int[count];
            }
            for (int i = 0; i < count; i++) {
                mFrom[i] = mCursor.getColumnIndexOrThrow(from[i]);
            }
        }

        public void setViewText(TextView v, String text) {
            v.setText(text);
        }

        public void setViewImage(ImageView v, String value) {
            try {
                v.setImageResource(Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
                v.setImageURI(Uri.parse(value));
            }
        }

        public void setViewBinder(SimpleCursorAdapter.ViewBinder viewBinder) {
            mViewBinder = viewBinder;
        }

        public void bindView(View view, Cursor cursor) {
            final SimpleCursorAdapter.ViewBinder binder = mViewBinder;

            final int count = mTo.length;
            final int[] from = mFrom;
            final int[] to = mTo;

            for (int i = 0; i < count; i++) {
                final View v = view.findViewById(to[i]);
                if (v != null) {
                    boolean bound = false;
                    if (binder != null) {
                        bound = binder.setViewValue(v, cursor, from[i]);
                    }

                    if (!bound) {
                        String text = cursor.getString(from[i]);
                        if (text == null) {
                            text = "";
                        }

                        if (v instanceof TextView) {
                            setViewText((TextView) v, text);
                        } else if (v instanceof ImageView) {
                            setViewImage((ImageView) v, text);
                        } else {
                            throw new IllegalStateException(v.getClass().getName() + " is not a " +
                                    " view that can be bounds by this SimpleCursorAdapter");
                        }
                    }
                }
            }
        }

        final View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentView == null)
                    return;
                Cursor cursor = mCursor;
                ContentQueryMap mQueryMap = new ContentQueryMap(cursor, BaseColumns._ID, true, null);
                Map<String, ContentValues> map = mQueryMap.getRows();
                TextView textView = (TextView) mCurrentView.findViewById(R.id.check_id);
                String checkId = textView.getText().toString();
                ContentValues values = map.get(checkId);
                String contactId = values.getAsString(CheckDBAdapter.KEY_VENDOR_ID);
                switch (v.getId()) {
                    case R.id.imageViewBack:
                        onBackPressed();
                        break;
                    case R.id.imageViewMail:
                        if (contactId != null)
                            new TaskMessageDialog(ActivityPageChecks.this, Integer.valueOf(contactId), Integer.valueOf(checkId)).openListEmailDialog();
                        break;
                    case R.id.imageViewMessage:
                        if (contactId != null)
                            new TaskMessageDialog(ActivityPageChecks.this, Integer.valueOf(contactId), Integer.valueOf(checkId)).openListPhoneDialog();
                        break;
                }
            }
        };
    }
}
