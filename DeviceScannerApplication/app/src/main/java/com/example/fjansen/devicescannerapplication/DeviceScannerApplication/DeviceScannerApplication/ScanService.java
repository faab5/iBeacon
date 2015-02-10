package com.example.fjansen.devicescannerapplication.DeviceScannerApplication.DeviceScannerApplication;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.example.fjansen.devicescannerapplication.R;

import java.sql.Timestamp;
import java.util.ArrayList;

import common.NotifiedService;

public class ScanService extends NotifiedService {

    static private final String TAG = "ScanService";

    static public final String ACTION_STATE_CHANGED = "com.example.fjansen.devicescannerapplication.scanservice.action_state_changed";
    static public final String EXTRA_STATE          = "com.example.fjansen.devicescannerapplication.scanservice.extra_state";
    static public final String ACTION_FOUND         = "com.example.fjansen.devicescannerapplication.scanservice.action_found";
    static public final String EXTRA_DEVICE         = "com.example.fjansen.devicescannerapplication.scanservice.extra_device";
    static public final String ACTION_LIST          = "com.example.fjansen.devicescannerapplication.scanservice.action_list";
    static public final String EXTRA_LIST           = "com.example.fjansen.devicescannerapplication.scanservice.extra_list";

    static public final int STATE_ON  = 1;
    static public final int STATE_OFF = 0;

    static public int STATE = STATE_OFF;

    private BluetoothAdapter bluetoothAdapter;

    private Listener scanServiceListener;

    private Scanner scanner;
    private Thread  scannerThread = null;

    static public int scanLengthSeconds = 10;
    static public int scanIdleSeconds   = 10;
    static private ArrayList<Device> deviceList = new ArrayList<>();
    static public Bundle getDeviceList() {
        Bundle result = new Bundle();
        synchronized (deviceList) {
            result.putParcelableArrayList(EXTRA_LIST, deviceList);
        }
        return result;
    }

    @Override
    public PersistentNotificationBuilder getNotification() {
        return (PersistentNotificationBuilder)
                (new PersistentNotificationBuilder(this, R.drawable.ic_scanservice,
                        "Device Scanner", "Scanning for devices", R.drawable.ic_scanservice_notification_stop, "Stop Scanner"))
                        .setTicker("Scanning for devices").setContentIntent(PendingIntent.getActivity(this, 1,
                        new Intent(this, ScanActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
    }
    private void notifyFullService() {
        PersistentNotificationBuilder notificationBuilder = getNotification();
        doNotification(notificationBuilder);
    }
    private void notifyTurnOnBluetooth() {
        PersistentNotificationBuilder notificationBuilder = getNotification();
        notificationBuilder.setTicker("Bluetooth disabled");
        notificationBuilder.setContentText("Turn on bluetooth to start scanning");
        PendingIntent pendingBtIntent = PendingIntent.getActivity(ScanService.this, 0, new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.addAction(R.drawable.ic_bluetooth, "Start Bluetooth", pendingBtIntent);
        doNotification(notificationBuilder);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        scanner             = new Scanner();
        bluetoothAdapter    = BluetoothAdapter.getDefaultAdapter();
        scanServiceListener = new Listener();
        registerReceiver(scanServiceListener, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(scanServiceListener, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(scanServiceListener, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        registerReceiver(scanServiceListener, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(scanServiceListener, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
    }

    /**
     * @param intent  The intent
     * @param flags   TART_FLAG_REDELIVERY, or START_FLAG_RETRY
     * @param startId A unique integer representing this specific request to start.
     *                Use with stopSelfResult(int).
     * @return        START_NOT_STICKY, START_STICKY, or START_REDELIVER_INTENT
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        // ***** Start scanner *****
        PersistentNotificationBuilder notificationBuilder = getNotification();
        if (bluetoothAdapter.isEnabled()) {
            if (scannerThread == null) {
                scannerThread = new Thread(scanner);
                scannerThread.start();
            } else if (!scannerThread.isAlive()) {
                scannerThread.interrupt();
                scannerThread = new Thread(scanner);
                scannerThread.start();
            }
            notifyFullService();
        } else {
            if (scannerThread != null) {
                scannerThread.interrupt();
                scannerThread = null;
            }
            notifyTurnOnBluetooth();
        }

        // ***** Update status *****
        if (STATE != STATE_ON) {
            intent = new Intent();
            intent.setAction(ACTION_STATE_CHANGED);
            intent.putExtra(EXTRA_STATE, STATE_ON);
            sendBroadcast(intent);
        }
        STATE = STATE_ON;
        return START_REDELIVER_INTENT;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        // ***** Stop scanning *****
        if (scannerThread != null) {
            scannerThread.interrupt();
            scannerThread = null;
        }
        // ***** Unregister broadcast receiver
        unregisterReceiver(scanServiceListener);
        // ***** Update status *****
        Intent intent = new Intent();
        intent.setAction(ACTION_STATE_CHANGED);
        intent.putExtra(EXTRA_STATE, STATE_OFF);
        sendBroadcast(intent);
        STATE = STATE_OFF;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }


    /**
     * Listens for changes in Bluetooth connection
     */
    public class Listener extends BroadcastReceiver {
        static private final String TAG = "ScanService.Listener";
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, action);
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            notifyTurnOnBluetooth();
                            break;
                        case BluetoothAdapter.STATE_ON:
                            if (scannerThread == null) {
                                scannerThread = new Thread(scanner);
                                scannerThread.start();
                            } else if (!scannerThread.isAlive()) {
                                scannerThread.interrupt();
                                scannerThread = new Thread(scanner);
                                scannerThread.start();
                            }
                            notifyFullService();
                            break;
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    // ToDo Update notification
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    // ToDo Update notification
                    break;
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    // ToDo Update notification
                    break;
            }
        }
    }

    /**
     * Scans for Bluetooth devices
     */
    public class Scanner implements Runnable {
        static private final String TAG = "ScanService.Scanner";
        @Override
        public void run() {
            Log.d(TAG, "run()");
            while (true) {
                try {
                    synchronized (deviceList) {
                        deviceList.clear();
                    }
                    bluetoothAdapter.startLeScan(startLeScanCallback);
                    Thread.sleep(scanLengthSeconds * 1000);
                    bluetoothAdapter.stopLeScan(startLeScanCallback);
                    Intent intent = new Intent(ScanService.ACTION_LIST);
                    synchronized (deviceList) {
                        intent.putParcelableArrayListExtra(ScanService.EXTRA_LIST, deviceList);
                    }
                    sendBroadcast(intent);
                    if (scanIdleSeconds > 0) Thread.sleep(scanIdleSeconds*1000);
                } catch (InterruptedException e) {
                    Log.d(TAG, "interrupted");
                    synchronized (deviceList) {
                        deviceList.clear();
                    }
                    break;
                }
            }
        }
    }


    /**
     * Callback for LE devices found
     */
    BluetoothAdapter.LeScanCallback startLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        static private final String TAG = "startLeScanCallback";
        @Override
        public void onLeScan(BluetoothDevice btdevice, int rssi, byte[] scanRecord) {
            Device device = new Device(btdevice, new Timestamp(System.currentTimeMillis()), rssi, scanRecord);
            Log.d(TAG, device.toString());
            synchronized (deviceList) {
                int pos = deviceList.size();
                for (int i = 0; i < deviceList.size(); ++i) {
                    if (deviceList.get(i).device.getAddress().trim().equalsIgnoreCase(btdevice.getAddress().trim())) {
                        pos = i;
                        break;
                    }
                }
                if (pos < deviceList.size()) {
                    deviceList.set(pos, device);
                } else {
                    deviceList.add(device);
                }
            }
            // Broadcast the device
            Intent intent = new Intent(ScanService.ACTION_FOUND);
            intent.putExtra(ScanService.EXTRA_DEVICE, device);
            ScanService.this.sendBroadcast(intent);
        }
    };
}