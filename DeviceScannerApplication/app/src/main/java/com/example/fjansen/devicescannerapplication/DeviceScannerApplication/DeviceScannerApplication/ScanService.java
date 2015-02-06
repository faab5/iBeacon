package com.example.fjansen.devicescannerapplication.DeviceScannerApplication.DeviceScannerApplication;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.fjansen.devicescannerapplication.R;

public class ScanService extends Service {

    static private final String TAG = "ScanService";

    static private final String SCANSERVICE_NOTIFICATION_TAG = "com.example.fjansen.devicescannerapplication.scanservice.notification";
    static private final int SCANSERVICE_NOTIFICATION_ID = 1055;

    static public final int PENDING_INTENT_ACTION_STOP_REQUEST_CODE = 1056;

    static public final String ACTION_STOP = "com.example.fjansen.devicescannerapplication.scanservice.action_stop";

    static private Status STATUS = Status.OFF;

    private BluetoothAdapter bluetoothAdapter;

    private ScanServiceListener scanServiceListener;

    private NotificationCompat.Builder notificationBuilder = null;

    private Scanner scanner;
    private Thread  scannerThread = null;


    /**
     * Constructor
     */
    public ScanService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();

        scanner             = new Scanner();
        bluetoothAdapter    = BluetoothAdapter.getDefaultAdapter();
        scanServiceListener = new ScanServiceListener();

        setupNotification();

        // ***** Register for events *****
        // Bluetooth on or off
        registerReceiver(scanServiceListener, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        // Discovering bluetooth devices
        registerReceiver(scanServiceListener, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(scanServiceListener, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        registerReceiver(scanServiceListener, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        // Bluetooth self discoverable
        registerReceiver(scanServiceListener, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        // Turn this Scan Service off
        //registerReceiver(scanServiceListener, new IntentFilter(ScanService.ACTION_STOP));
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

        // ***** Start Scanning *****
        if (bluetoothAdapter.isEnabled()) {
            startScanning();
        } else {
            stopScanning();
            // ToDo ask user to turn on Bluetooth
        }

        // ***** Send Notification *****
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(SCANSERVICE_NOTIFICATION_TAG, SCANSERVICE_NOTIFICATION_ID, notificationBuilder.build());

        // ToDo Send out broadcast

        // ***** Update status *****
        STATUS = Status.ON;

        return START_REDELIVER_INTENT;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        // ***** Stop scanning *****
        if (scannerThread != null) {
            scannerThread.interrupt();
            scannerThread = null;
        }

        // ***** Unregister broadcast receiver
        unregisterReceiver(scanServiceListener);

        // ***** Cancel notification *****
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(SCANSERVICE_NOTIFICATION_TAG, SCANSERVICE_NOTIFICATION_ID);

        // ToDo send out broadcast

        // ***** Update status *****
        STATUS = Status.OFF;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    private void startScanning() {
        Log.d(TAG, "startScanning()");
        if (scannerThread == null) {
            scannerThread = new Thread(scanner);
            scannerThread.start();
        }
    }
    private void stopScanning() {
        Log.d(TAG, "stopScanning()");
        if (scannerThread != null) {
            scannerThread.interrupt();
            scannerThread = null;
        }
    }

    static public ScanService.Status Status() {
        return STATUS;
    }
    static public boolean isRunning() {
        return STATUS != ScanService.Status.OFF;
    }

    private void setupNotification(){
        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_scanservice)
                .setTicker("Scanning for devices")
                .setContentTitle("Device Scanner")
                .setContentText("Scanning for devices")
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        Intent stopIntent = new Intent(ScanService.ACTION_STOP, null, this, ScanService.class);
        PendingIntent pendingStopIntent = PendingIntent.getBroadcast(this, ScanService.PENDING_INTENT_ACTION_STOP_REQUEST_CODE, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.addAction(R.drawable.ic_scanservice_notification_stop, "Stop scanner", pendingStopIntent);
    }

    /*
     * INTERNAL CLASSES
     */

    /**
     * Contains the status of the ScanService
     */
    static public enum Status {
        ON,
        OFF
    }




    /**
     * Listens for changes in Bluetooth connection
     */
    private class ScanServiceListener extends BroadcastReceiver {

        static private final String TAG = "ScanServiceListener";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            stopScanning();
                            // ToDo Update notification
                            break;
                        case BluetoothAdapter.STATE_ON:
                            startScanning();
                            // ToDo Update notification
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
                case ScanService.ACTION_STOP:
                    // ToDo stop the service
                    stopService(new Intent(ScanService.this, ScanService.class));
                    break;
            }
        }
    }




    /**
     * Scans for Bluetooth devices
     */
    public class Scanner implements Runnable {

        static private final String TAG = "Scanner";

        @Override
        public void run() {
            Log.d(TAG, "run()");
        }
    }
}