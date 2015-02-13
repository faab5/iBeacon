package com.example.fjansen.devicescannerapplication.DeviceScannerApplication.DeviceScannerApplication;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.fjansen.devicescannerapplication.R;

public class ScanActivity extends ActionBarActivity {

    private static final String TAG = "ScanActivity";

    public BluetoothAdapter bluetoothAdapter;

    private ScanActivityListener scanActivityListener;

    public TextView text1;
    public Button   button1;
    public Button   button2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        button1 = (Button) findViewById(R.id.scan_activity_button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(new Intent(ScanActivity.this,ScanService.class));
            }
        });

        button2 = (Button) findViewById(R.id.scan_activity_button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            }
        });
        button2.setVisibility(Button.INVISIBLE);

        text1 = (TextView)findViewById(R.id.scan_activity_text1);

        // ***** Register for events *****
        scanActivityListener = new ScanActivityListener();
        // Bluetooth on or off
        registerReceiver(scanActivityListener, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        // Discovering bluetooth devices
        registerReceiver(scanActivityListener, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(scanActivityListener, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        // Bluetooth self discoverable
        registerReceiver(scanActivityListener, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        // Scan Service on or off
        registerReceiver(scanActivityListener, new IntentFilter(ScanService.ACTION_STATE_CHANGED));
        // New device
        registerReceiver(scanActivityListener, new IntentFilter(ScanService.ACTION_FOUND));
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
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
        // ***** Unregister broadcast receiver
        unregisterReceiver(scanActivityListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scan, menu);
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



    /**
     * Listens for changes in Bluetooth connection
     */
    public class ScanActivityListener extends BroadcastReceiver {

        static private final String TAG = "ScanActivityListener";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int state;
            Log.d(TAG, action);
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:

                            break;
                        case BluetoothAdapter.STATE_ON:
                            break;
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    break;
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    break;
                case ScanService.ACTION_STATE_CHANGED:
                    state = intent.getIntExtra(ScanService.EXTRA_STATE, -1);
                    switch (state) {
                        case ScanService.STATE_OFF:
                            text1.setText("The device scanner is disabled. Click the button to start scanning for devices.");
                            button1.setText("Start scanning");
                            button2.setVisibility(Button.INVISIBLE);
                            break;
                        case ScanService.STATE_ON:
                            if (bluetoothAdapter.isEnabled()) {
                                text1.setText("The device scanner is scanning for devices.");
                                button1.setText("Stop scanning");
                                button2.setVisibility(Button.INVISIBLE);
                            } else {
                                text1.setText("The device scanner is disabled. Click the button to start scanning for devices.");
                                button1.setText("Start scanning");
                                button2.setVisibility(Button.INVISIBLE);
                            }
                            break;
                    }
                    break;
                case ScanService.ACTION_FOUND:
                    Device device = (Device)intent.getParcelableExtra(ScanService.EXTRA_DEVICE);
                    if (device == null) break;
                    break;
            }
        }
    }
}
