package com.example.fjansen.devicescannerapplication.DeviceScannerApplication.DeviceScannerApplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.example.fjansen.devicescannerapplication.R;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";

    //private Switch switch1;
    private Button button1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //switch1 = (Switch) findViewById(R.id.switch1);

        // ***** Set Button to turn the ScanService on and off *****
        button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "click "+ScanService.isRunning());
                if (ScanService.isRunning()) {
                    stopService(new Intent(MainActivity.this, ScanService.class));
                } else {
                    startService(new Intent(MainActivity.this, ScanService.class));
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void set() {
        if (ScanService.isRunning()) {
            button1.setText(R.string.scanservice_action_stop);
        } else {
            button1.setText(R.string.scanservice_action_start);
        }
    }
}
