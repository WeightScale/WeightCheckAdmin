package com.kostya.weightcheckadmin;

import android.annotation.TargetApi;
import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

/**
 * Created with IntelliJ IDEA.
 * User: VictorJava
 * Date: 25.09.13
 * Time: 16:59
 * To change this template use File | Settings | File Templates.
 */
public class ActivityContact extends ListActivity {


    private CheckDBAdapter checkDBAdapter;
    private Vibrator vibrator; //вибратор
    ListView listView;
    EditText textSearch;
    LinearLayout layoutSearch;
    SimpleCursorAdapter adapter;
    static final int PICK_CONTACT = 2;

    private String contactID;     // contacts unique ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //requestWindowFeature(Window.FEATURE_ACTION_BAR);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);


        //ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.contact);
        setTitle("Контакты");
        //getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_search);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        checkDBAdapter = new CheckDBAdapter(this);

        listView = getListView();
        listView.setLongClickable(true);
        listView.setClickable(true);
        listView.setItemsCanFocus(false);
        listView.setOnItemLongClickListener(onItemLongClickListener);
        listView.setOnItemClickListener(onItemClickListener);

        textSearch = (EditText) findViewById(R.id.textSearch);
        textSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        /*textSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });*/

        layoutSearch = (LinearLayout) findViewById(R.id.layoutSearch);
        layoutSearch.setVisibility(View.GONE);

        ImageView buttonBack = (ImageView) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(clickListener);

        ImageView buttonSearch = (ImageView) findViewById(R.id.buttonSearch);
        buttonSearch.setOnClickListener(clickListener);

        ImageView buttonNew = (ImageView) findViewById(R.id.buttonNew);
        buttonNew.setOnClickListener(clickListener);

        ImageView closedSearch = (ImageView) findViewById(R.id.closedSearch);
        closedSearch.setOnClickListener(clickListener);

        updateList(getContact());
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return new FilterCursorWrapper(getContact(), constraint.toString(), ContactsContract.Contacts.DISPLAY_NAME);
            }
        });
    }

    final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            switch (v.getId()) {
                case R.id.buttonBack:
                    onBackPressed();
                    break;
                case R.id.buttonSearch:
                    layoutSearch.setVisibility(View.VISIBLE);
                    textSearch.requestFocus();
                    imm.showSoftInput(textSearch, InputMethodManager.SHOW_IMPLICIT);
                    break;
                case R.id.buttonNew:
                    Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
                    startActivity(intent);
                    break;
                case R.id.closedSearch:
                    imm.hideSoftInputFromWindow(textSearch.getWindowToken(), 0);
                    adapter.getFilter().filter("");
                    textSearch.setText("");
                    layoutSearch.setVisibility(View.GONE);
                    break;
            }
        }
    };

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {

            case PICK_CONTACT:
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    //Cursor contact =  managedQuery(contactData, null, null, null, null);
                    Cursor contact = getContentResolver().query(contactData, null, null, null, null);
                    //ContentQueryMap mQueryMap = new ContentQueryMap(contact, BaseColumns._ID, true, null);
                    //Map<String,ContentValues> map = mQueryMap.getRows();

                    //ContentValues values = map.get(String.valueOf(69));
                    //String contactName =" ";
                    /*if (contact.moveToFirst()) {

                        // DISPLAY_NAME = The display name for the contact.
                        // HAS_PHONE_NUMBER =   An indicator of whether this contact has at least one phone number.

                        //contactName = contact.getString(contact.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    }*/

                    contact.close();
                    /*if (c.moveToFirst()) {
                        String name = c.getString(c.getColumnIndexOrThrow(Contacts.People.NAME));
                        // TODO Whatever you want to do with the selected contact name.
                    }*/
                    retrieveContactNumber(contactData);
                }
                break;
        }
    }

    private String retrieveContactName(Uri uriContact) {

        String contactName = null;
        Cursor cursor = getContentResolver().query(uriContact, null, null, null, null);
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        }
        cursor.close();
        return contactName;
    }

    private String retrieveContactNumber(Uri uriContact) {

        // getting contacts ID
        Cursor cursorID = getContentResolver().query(uriContact, new String[]{BaseColumns._ID}, null, null, null);
        if (cursorID.moveToFirst()) {
            contactID = cursorID.getString(cursorID.getColumnIndex(BaseColumns._ID));
        }
        cursorID.close();
        // Using the contact ID now we will get contact phone number
        Cursor cursorPhone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " +
                        ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                new String[]{contactID}, null);

        //if (cursorPhone.moveToFirst()) {
        String contactNumber = null;
        while (cursorPhone.moveToNext())
            contactNumber = cursorPhone.getString(cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        //}
        cursorPhone.close();
        return contactNumber;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void updateList(Cursor cursor) {
        if (cursor.getCount() > 0) {
            String[] from = {ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.PHOTO_URI};
            int[] to = {R.id.contactName, R.id.contactPhoto};
            adapter = new SimpleCursorAdapter(this, R.layout.item_contact, cursor, from, to);

            setListAdapter(adapter);
        }
    }

    Cursor getContact() {
        return getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
    }

    final AdapterView.OnItemLongClickListener onItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            vibrator.vibrate(100L);
            CharSequence[] colors = {"ИЗМЕНИТЬ", "УДАЛИТЬ", "ВЫБРАТЬ", "НАЗАД"};
            TextView textTop = (TextView) view.findViewById(R.id.contactName);
            int contactID = (int) id;
            String name = " ";

            AlertDialog.Builder builder = new AlertDialog.Builder(ActivityContact.this);
            if (textTop.getText() != null)
                name = textTop.getText().toString();
            builder.setTitle("Контакт: " + name);
            builder.setCancelable(false);
            final int finalContactID = contactID;
            final String finalName = name;
            builder.setItems(colors, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, finalContactID);
                    Intent intent;
                    switch (which) {
                        case 0:
                            //Intent intent = new Intent(Intent.ACTION_EDIT, contactUri);
                            //Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT, contactUri);
                            intent = new Intent(Intent.ACTION_VIEW, contactUri);
                            //Intent intent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, contactUri);
                            startActivity(intent);
                            break;
                        case 1:
                            AlertDialog.Builder deleteDialog = new AlertDialog.Builder(ActivityContact.this);
                            deleteDialog.setTitle("Удаление: " + finalName);
                            deleteDialog.setMessage("Вы хотите удалить?");
                            deleteDialog.setCancelable(false);
                            deleteDialog.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    getContentResolver().delete(contactUri, null, null);
                                    adapter.changeCursor(getContact());
                                }
                            });
                            deleteDialog.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            deleteDialog.show();
                            break;
                        case 2:
                            insertNewCheck(finalContactID);
                            break;
                        case 3:
                            dialog.cancel();
                            break;
                        default:
                    }
                    // the user clicked on colors[which]
                }
            });
            builder.show();
            return false;
        }
    };

    final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            insertNewCheck((int) id);
        }
    };

    private void insertNewCheck(int id) {
        vibrator.vibrate(100);
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        String action = getIntent().getAction();
        if (action != null) {
            if (action.equals("down")) {
                String name = retrieveContactName(contactUri);
                String entryID = checkDBAdapter.insertNewEntry(name, id, CheckDBAdapter.DIRECT_DOWN).getLastPathSegment();
                startActivity(new Intent().setClass(getApplicationContext(), ActivityInputCheck.class).putExtra("id", entryID));
                finish();
            } else if (action.equals("up")) {
                String name = retrieveContactName(contactUri);
                String entryID = checkDBAdapter.insertNewEntry(name, id, CheckDBAdapter.DIRECT_UP).getLastPathSegment();
                startActivity(new Intent().setClass(getApplicationContext(), ActivityOutputCheck.class).putExtra("id", entryID));
                finish();
            } else if (action.equals("contact")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
                startActivity(intent);
            }
        }
    }

    /*public Cursor fetchCountriesByName(String inputText) throws SQLException {
        Cursor mCursor = null;
        if (inputText == null  ||  inputText.length () == 0)  {
            mCursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null,null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
        }else {
            mCursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, ContactsContract.Contacts.DISPLAY_NAME + " LIKE '%" + inputText + "%'",null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");*//* _utf8 COLLATE utf8_general_ci COLLATE NOCASE*//*
        }
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }*/

    private class FilterCursorWrapper extends CursorWrapper {

        private final String filter;
        private final int column;
        private final int[] index;
        private int count;
        private int pos;

        FilterCursorWrapper(Cursor cursor, String filter, String column) {
            super(cursor);
            this.filter = filter.toLowerCase();
            this.column = cursor.getColumnIndex(column);
            if (!this.filter.isEmpty()) {
                count = super.getCount();
                index = new int[count];
                for (int i = 0; i < count; i++) {
                    super.moveToPosition(i);
                    String[] split = getString(this.column).toLowerCase().split(" ");
                    for (String str : split) {
                        if (str.trim().startsWith(this.filter))
                            index[pos++] = i;
                    }
                }
                count = pos;
                pos = 0;
                super.moveToFirst();
            } else {
                count = super.getCount();
                index = new int[count];
                for (int i = 0; i < count; i++) {
                    index[i] = i;
                }
            }
        }

        @Override
        public boolean move(int offset) {
            return moveToPosition(pos + offset);
        }

        @Override
        public boolean moveToNext() {
            return moveToPosition(pos + 1);
        }

        @Override
        public boolean moveToPrevious() {
            return moveToPosition(pos - 1);
        }

        @Override
        public boolean moveToFirst() {
            return moveToPosition(0);
        }

        @Override
        public boolean moveToLast() {
            return moveToPosition(count - 1);
        }

        @Override
        public boolean moveToPosition(int position) {
            return !(position >= count || position < 0) && super.moveToPosition(index[position]);
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public int getPosition() {
            return pos;
        }

    }
}

