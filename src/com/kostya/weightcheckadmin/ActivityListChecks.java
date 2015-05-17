package com.kostya.weightcheckadmin;


import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 20.10.13
 * Time: 8:37
 * To change this template use File | Settings | File Templates.
 */
public class ActivityListChecks extends ListActivity {

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_checks);

        setTitle(getString(R.string.app_name) + ' ' + "получаем чеки..."); //установить заголовок*/

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        listView = getListView();
        listView.setOnItemClickListener(onItemClickListener);

        ImageView buttonBack = (ImageView) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(clickListener);

        listSetup();
        //new ThreadViewChecks().execute();
    }

    /*@Override
    public void onBackPressed() {
        super.onBackPressed();
    }*/

    final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //startActivity(new Intent().setClass(getApplicationContext(),ActivityViewCheck.class).putExtra("id",id));
            startActivity(new Intent().setClass(getApplicationContext(), ActivityPageChecks.class).putExtra("position", position));
        }
    };

    final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.buttonBack:
                    onBackPressed();
                    break;
            }
        }
    };

    private void listSetup() {

        Cursor cursor = new CheckDBAdapter(this).getAllEntries(CheckDBAdapter.VISIBLE);
        String[] columns = {
                CheckDBAdapter.KEY_ID,
                CheckDBAdapter.KEY_DATE_CREATE,
                CheckDBAdapter.KEY_TIME_CREATE,
                CheckDBAdapter.KEY_VENDOR,
                CheckDBAdapter.KEY_WEIGHT_GROSS,
                CheckDBAdapter.KEY_WEIGHT_TARE,
                CheckDBAdapter.KEY_WEIGHT_NETTO,
                CheckDBAdapter.KEY_PRICE_SUM, CheckDBAdapter.KEY_DIRECT};

        int[] to = {
                R.id.check_id,
                R.id.date,
                R.id.time,
                R.id.vendor,
                R.id.gross_row,
                R.id.tare_row,
                R.id.netto_row,
                R.id.sum_row, R.id.imageDirect};
        SimpleCursorAdapter namesAdapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.item_check, cursor, columns, to);
        setListAdapter(namesAdapter);
        setTitle(getString(R.string.Checks_closed) + " кол-во " + listView.getCount()); //установить заголовок

    }


}
