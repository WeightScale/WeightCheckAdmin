package com.kostya.weightcheckadmin;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

/*
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 02.10.13
 * Time: 16:57
 * To change this template use File | Settings | File Templates.
 */
public class ActivityType extends ListActivity {
    //private ListView listView;
    private EditText input;
    private AlertDialog.Builder dialog;
    private TypeDBAdapter typeDBAdapter;

    //private long entryID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.type);
        setTitle("Тип");

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);
        //dbType=new TypeDBAdapter(this,ActivitySearch.db);
        typeDBAdapter = new TypeDBAdapter(this);
        //dbType.open();
        //InputCheckDBAdapter dbInput = new InputCheckDBAdapter(this, ActivitySearch.db);
        //dbInput.open();

        dialog = new AlertDialog.Builder(this);
        //dialog.setTitle("Новый тип товара");
        dialog.setMessage("Введите Тип");
        //dialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        input = new EditText(this);
        dialog.setView(input);
        dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                //String value = String.valueOf(input.getText());
                if (input.getText() != null) {
                    typeDBAdapter.insertEntryType(input.getText().toString());
                    updateList();
                }
            }
        });

        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        ImageView buttonBack = (ImageView) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(clickListener);
        ImageView buttonNew = (ImageView) findViewById(R.id.buttonNew);
        buttonNew.setOnClickListener(clickListener);

        updateList();
    }

    final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.buttonBack:
                    onBackPressed();
                    break;
                case R.id.buttonNew:
                    input = new EditText(getBaseContext());
                    dialog.setView(input);
                    dialog.show();
                    break;
            }
        }
    };

    void updateList() {
        Cursor cursor = typeDBAdapter.getNotSystemEntries();
        if (cursor.getCount() > 0) {
            String[] from = {TypeDBAdapter.KEY_TYPE};
            int[] to = {R.id.topText};
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.list_item_type, cursor, from, to);
            setListAdapter(adapter);
        }
    }

    public void removeAtomPayOnClickHandler(View view) {

        ListView list = getListView();
        int position = list.getPositionForView(view);

        Cursor cursor = ((CursorAdapter) list.getAdapter()).getCursor();
        if (cursor == null)
            return;
        cursor.moveToPosition(position);
        int id = cursor.getInt(cursor.getColumnIndex(TypeDBAdapter.KEY_ID));

        typeDBAdapter.removeEntry(id);
        updateList();
    }
}
