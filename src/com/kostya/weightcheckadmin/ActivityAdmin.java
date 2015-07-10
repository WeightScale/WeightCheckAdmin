package com.kostya.weightcheckadmin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import com.kostya.weightcheckadmin.bootloader.ActivityBootloader;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 16.10.13
 * Time: 9:38
 * To change this template use File | Settings | File Templates.
 */
public class ActivityAdmin extends Activity implements OnClickListener {
    AlertDialog.Builder dialog;
    final String text_message = "Выключите весы. Нажмине кнопку включения и не отпускайте пока индикатор не погаснет. После этого нажмите ОК";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViewById(R.id.buttonBootloader).setOnClickListener(this);
        findViewById(R.id.buttonScales).setOnClickListener(this);
        findViewById(R.id.buttonBack).setOnClickListener(this);

        dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Внимание !");
        dialog.setCancelable(false);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //startActivity(new Intent(getBaseContext(), ActivitySearch.class).setAction("bootloader"));
                        startActivity(new Intent().setClass(getApplicationContext(), ActivityBootloader.class));
                        finish();
                        break;
                    default:
                }
            }
        });
        dialog.setNegativeButton("Закрыть", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonBootloader:
                dialog.setMessage(text_message);
                dialog.show();
                break;
            case R.id.buttonScales:
                Preferences.admin = true;
                startActivity(new Intent(getBaseContext(), ActivityScales.class).setAction("bootloader"));
                finish();
                break;
            case R.id.buttonBack:
                onBackPressed();
                break;
        }
    }
}
