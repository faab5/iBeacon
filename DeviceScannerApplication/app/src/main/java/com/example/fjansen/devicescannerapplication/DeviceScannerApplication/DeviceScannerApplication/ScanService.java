package com.example.fjansen.devicescannerapplication.DeviceScannerApplication.DeviceScannerApplication;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.example.fjansen.devicescannerapplication.R;

import java.sql.Timestamp;
import java.util.ArrayList;

import common.NotifiedService;

public class ScanService extends NotifiedService {

    static private final String TAG = "ScanService";

    // ***** Broadcast Messages
    static public final String ACTION_STATE_CHANGED = "com.example.fjansen.devicescannerapplication.scanservice.action_state_changed";
    static public final String EXTRA_STATE          = "com.example.fjansen.devicescannerapplication.scanservice.extra_state";
    static public final String ACTION_FOUND         = "com.example.fjansen.devicescannerapplication.scanservice.action_found";
    static public final String EXTRA_DEVICE         = "com.example.fjansen.devicescannerapplication.scanservice.extra_device";
    static public final String ACTION_LIST          = "com.example.fjansen.devicescannerapplication.scanservice.action_list";
    static public final String EXTRA_LIST           = "com.example.fjansen.devicescannerapplication.scanservice.extra_list";

    // ***** Scanner
    private Runnable scanner;
    private Thread   scannerThread = null;
    private BluetoothAdapter.LeScanCallback leScanCallback;
    private Control  controller;
    private Receiver receiver;

    static private ArrayList<Device> DEVICESLIST = new ArrayList<>();

    static public Bundle getDevicesList() {
        Bundle result = new Bundle();
        synchronized (DEVICESLIST) {
            result.putParcelableArrayList(EXTRA_LIST, DEVICESLIST);
        }
        return result;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        receiver.onReceive(this, new Intent(ACTION_START));
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    public class LocalBinder extends Binder {
        ScanService getService() {
            return ScanService.this;
        }
    }
    private IBinder binder = new LocalBinder();

    @Override
    public void onCreateService() {
        Log.d(TAG, "onCreateService()");

        controller = new Control();
        receiver = new Receiver(controller);
        registerReceiver(receiver, new IntentFilter(Receiver.ACTION_START));
        registerReceiver(receiver, new IntentFilter(Receiver.ACTION_STOP));
        registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED));


        // ***** Setup a callback when a Bluetooth Low Energy device is found
        leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice btdevice, int rssi, byte[] scanRecord) {
                Device device = new Device(btdevice, new Timestamp(System.currentTimeMillis()), rssi, scanRecord);
                synchronized (DEVICESLIST) {
                    int pos = DEVICESLIST.size();
                    for (int i = 0; i < DEVICESLIST.size(); ++i) {
                        if (DEVICESLIST.get(i).device.getAddress().trim().equalsIgnoreCase(btdevice.getAddress().trim())) {
                            pos = i;
                            break;
                        }
                    }
                    if (pos < DEVICESLIST.size()) {
                        DEVICESLIST.set(pos, device);
                    } else {
                        DEVICESLIST.add(device);
                    }
                }
                Intent intent = new Intent(ScanService.ACTION_FOUND);
                intent.putExtra(ScanService.EXTRA_DEVICE, device);
                ScanService.this.sendBroadcast(intent);
            }
        };

        // Setup the scanner to scan for Bluetooth Low Energy devices
        scanner = new Runnable() {
            @Override
            public void run() {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                while (true) {
                    try {
                        synchronized (DEVICESLIST) {
                            DEVICESLIST.clear();
                        }
                        bluetoothAdapter.startLeScan(leScanCallback);
                        Thread.sleep(SCANLENGTHSECONDS * 1000);
                        bluetoothAdapter.stopLeScan(leScanCallback);
                        Intent intent = new Intent(ScanService.ACTION_LIST);
                        synchronized (DEVICESLIST) {
                            intent.putParcelableArrayListExtra(ScanService.EXTRA_LIST, DEVICESLIST);
                        }
                        sendBroadcast(intent);
                        if (SCANWAITSECONDS > 0) Thread.sleep(SCANWAITSECONDS *1000);
                    } catch (InterruptedException e) {
                        synchronized (DEVICESLIST) {
                            DEVICESLIST.clear();
                        }
                        break;
                    }
                }
            }
        };


    }

    @Override
    public void onDestroyService() {
        Log.d(TAG, "onDestroyService()");
        if (scannerThread != null) {
            scannerThread.interrupt();
            scannerThread = null;
        }
        unregisterReceiver(receiver);
    }


    // ***** Notifications
    static private final int NOTIFICATION_CONTENT_INTENT_REQUEST_CODE           = 20105;
    static private final int NOTIFICATION_ACTION_TURN_ON_BLUETOOTH_REQUEST_CODE = 20106;
    static private final int NOTIFICATION_ACTION_TURN_ON_SCANNER_REQUEST_CODE   = 20107;

    @Override
    public PersistentNotificationBuilder notificationBuilder() {
        PersistentNotificationBuilder notificationBuilder = new PersistentNotificationBuilder(this, R.drawable.ic_scanservice, "Scanner", "Scanning for beacons", R.drawable.ic_scanservice_notification_stop, "Stop the scanner");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, NOTIFICATION_CONTENT_INTENT_REQUEST_CODE, new Intent(this, ScanActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        return notificationBuilder;
    }











    static public final String REQUEST_STATE = "REQUEST_STATUS";
    static public final int STATE_OFF        = 0;
    static public final int STATE_ON         = 1;

    static public final String BLUETOOTH_DEPENDENCE  = "BLUETOOTH_DEPENDENCE";
    static public final int RUN_IF_BLUETOOTH_ON_ONLY = 1;

    static public final String BLUETOOTH_CONNECTION_DEPENDENCE    = "BLUETOOTH_CONNECTION_DEPENDENCE";
    static public final int RUN_IF_BLUETOOTH_DISCONNECTED_ONLY    = 1;
    static public final int RUN_IF_BLUETOOTH_CONNECTION_UNKNOWN   = 2;
    static public final int RUN_REGARDLESS_OF_BLUETOOTH_CONNECTED = 3;

    static public String SCAN_LENGTH_SECONDS = "SCAN_LENGTH_SECONDS";
    static public String SCAN_WAIT_SECONDS   = "SCAN_WAIT_SECONDS";

    static public class Control {
        private Bundle config;
        public Control() {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            config = new Bundle();
            config.putInt(REQUEST_STATE, STATE_OFF);
            config.putInt(BluetoothAdapter.EXTRA_STATE, bluetoothAdapter.getState());
            config.putInt(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
            config.putInt(BLUETOOTH_DEPENDENCE, RUN_IF_BLUETOOTH_ON_ONLY);
            config.putInt(BLUETOOTH_CONNECTION_DEPENDENCE, RUN_IF_BLUETOOTH_DISCONNECTED_ONLY);
            config.putInt(SCAN_LENGTH_SECONDS, 10);
            config.putInt(SCAN_WAIT_SECONDS, 10);
        }
        final public Bundle get() {
            return new Bundle(config);
        }
        final public void set(Bundle new_config) {
            int request_state       = new_config.getInt(REQUEST_STATE);
            int bt_state            = new_config.getInt(BluetoothAdapter.EXTRA_STATE);
            int bt_connection_state = new_config.getInt(BluetoothAdapter.EXTRA_CONNECTION_STATE);

            config = new_config;
        }
    }


    static public final String ACTION_START = "ACTION_START";
    static public final String ACTION_STOP  = "ACTION_STOP";

    static public class Receiver extends BroadcastReceiver {
        private Control control;
        public Receiver(Control control) {
            this.control = control;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle config = control.get();
            String action = intent.getAction();
            switch (action) {
                case ACTION_START:
                    config.putInt(REQUEST_STATE, STATE_ON);
                    break;
                case ACTION_STOP:
                    config.putInt(REQUEST_STATE, STATE_OFF);
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    config.putInt(BluetoothAdapter.EXTRA_STATE, intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1));
                    break;
                case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                    config.putInt(BluetoothAdapter.EXTRA_CONNECTION_STATE, intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1));
            }
            control.set(config);
        }
    }
}