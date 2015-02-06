package com.example.android.bluetoothchat;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.example.android.common.logger.Log;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceScanService extends Service {

    /**
     * Tag for Log
     */
    private static final String TAG = "DeviceScanService";

    private IBinder mBinder = new LocalBinder();
    private boolean mAllowRebind = true;

    /**
     * BluetoothAdapter to interact with bluetooth
     */
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    /**
     * Device Scanner, and thread
     */
    private DeviceScanner mDeviceScanner;
    private Thread mDeviceScannerThread = null;

    /**
     * Broadcast receiver for devices found
     */
    private final DeviceScanReceiver mDeviceScanReceiver = new DeviceScanReceiver();

    public DeviceScanService() {
    }

    /**
     * Newly discovered devices
     */
    private ArrayList<BTDevice> devicesList =  new ArrayList<>();

    public Bundle getDevices(){
        Bundle result = new Bundle();
        synchronized (devicesList) {
            result.putParcelableArrayList("btdevices", devicesList);
        }
        return result;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        mDeviceScanner = new DeviceScanner(20 * 1000);
        mDeviceScannerThread = new Thread(mDeviceScanner);
        mDeviceScannerThread.start();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mDeviceScanReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        this.registerReceiver(mDeviceScanReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mDeviceScanReceiver, filter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        // All clients have unbound with unbindService()
        Log.d(TAG, "onUnbind");
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
        Log.d(TAG, "onRebind");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        this.unregisterReceiver(mDeviceScanReceiver);

        if (mDeviceScannerThread != null) {
            mDeviceScannerThread.interrupt();
            mDeviceScannerThread = null;
        }
    }

    public class LocalBinder extends Binder {
        DeviceScanService getService() {
            return DeviceScanService.this;
        }
    }

    /**
     * Regular expression pattern to search for MAC addresses with in strings
     */
    static public final Pattern mMacPattern = Pattern.compile("[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}");

    /**
     * Find the item that holds the specified mac address
     */
    static public int findDevice(String address, ArrayList<String> adapter) {
        for (int i = 0; i < adapter.size(); ++i) {
            Matcher macPatternMatcher = mMacPattern.matcher(adapter.get(i));
            if (macPatternMatcher.find() && macPatternMatcher.group().equalsIgnoreCase(address)) {
                return i;
            }
        }
        return adapter.size();
    }







    /**
     * Runnable DeviceScanner
     */
    public class DeviceScanner implements Runnable {

        /**
         * Tag for Log
         */
        private static final String TAG = "DeviceScanner";

        /**
         * Delay
         */
        private long delayms;

        /**
         * The bluetooth adapter
         */
        private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        /**
         * Constructor
         * @param delayms
         */
        public DeviceScanner(long delayms) {
            this.delayms = delayms;
        }

        /**
         * Starts executing the active part of the class' code. This method is
         * called when a thread is started that has been created with a class which
         * implements {@code Runnable}.
         */
        @Override
        public void run() {
            Log.d(TAG, "started");
            while(true) {
                if (Thread.interrupted()) {
                    Log.d(TAG, "interrupted");
                    // Make sure we're not doing discovery anymore
                    mBluetoothAdapter.cancelDiscovery();
                    mBluetoothAdapter.stopLeScan(BtLeScanCallback);
                    break;
                }
                if (!mBluetoothAdapter.isDiscovering()) {
                    Log.d(TAG, "startDiscovery");
                    mBluetoothAdapter.startDiscovery();
                }
                try {
                    Log.d(TAG, "sleep "+delayms/1000+" seconds");
                    Thread.sleep(delayms);
                } catch (InterruptedException e) {
                    Log.d(TAG, "interrupted");
                    // Make sure we're not doing discovery anymore
                    mBluetoothAdapter.cancelDiscovery();
                    mBluetoothAdapter.stopLeScan(BtLeScanCallback);
                    break;
                }
                try {
                    if (mBluetoothAdapter.startLeScan(BtLeScanCallback)) {
                        Log.d(TAG, "LE scan " + 12 + " seconds");
                        Thread.sleep(12000);
                    }
                    mBluetoothAdapter.stopLeScan(BtLeScanCallback);
                    Thread.sleep(12000);
                } catch (InterruptedException e) {
                    Log.d(TAG, "interrupted");
                    // Make sure we're not doing discovery anymore
                    mBluetoothAdapter.cancelDiscovery();
                    mBluetoothAdapter.stopLeScan(BtLeScanCallback);
                    break;
                }
            }
            Log.d(TAG, "stopped");
        }
    }


    /**
     * BroadcastReceiver to process scanned devices
     */
    class DeviceScanReceiver extends BroadcastReceiver {

        /**
         * Tag for Log
         */
        private static final String TAG = "DeviceScanReceiver";

        public DeviceScanReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            //ToDo: You could parse on here the complete context and intent to a (intent)service
            // to handle the bluetooth scanning, since found devices get passed on to a
            // DeviceSendService anyway.
            // At any rate you cannot start threads here cuz the process gets killed on return
            // from this method, that's why the extra DeviceSendService.
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                Timestamp ts = new Timestamp(System.currentTimeMillis());
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                Log.d(TAG, "ACTION_FOUND "+new BTDevice(device, ts, rssi).toString());

                synchronized (devicesList) {
                    int pos = devicesList.size();
                    for (int i=0; i<devicesList.size(); ++i) {
                        if (devicesList.get(i).device.getAddress().trim().equalsIgnoreCase(device.getAddress().trim())) {
                            pos = i;
                            break;
                        }
                    }
                    if (pos < devicesList.size()) {
                        devicesList.set(pos, new BTDevice(device, ts, rssi));
                    } else {
                        devicesList.add(new BTDevice(device, ts, rssi));
                        //DeviceSendService.sendDevice(context, new BTDevice(device, ts, rssi));
                    }
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                synchronized (devicesList) {
                    devicesList.clear();
                }
                Log.d(TAG, "Started scanning");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "Stopped scanning");
            }
        }
    }


    /**
     * Callback for LE devices found
     */
    BluetoothAdapter.LeScanCallback BtLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Timestamp ts = new Timestamp(System.currentTimeMillis());

            Log.d(TAG, "BTLE callback "+new BTDevice(device, ts, rssi, scanRecord).toString());

            synchronized (devicesList) {
                int pos = devicesList.size();
                for (int i=0; i<devicesList.size(); ++i) {
                    if (devicesList.get(i).device.getAddress().trim().equalsIgnoreCase(device.getAddress().trim())) {
                        pos = i;
                        break;
                    }
                }
                if (pos < devicesList.size()) {
                    devicesList.set(pos, new BTDevice(device, ts, rssi, scanRecord));
                } else {
                    devicesList.add(new BTDevice(device, ts, rssi));
                }
                DeviceSendService.sendDevice(DeviceScanService.this, new BTDevice(device, ts, rssi, scanRecord));
            }
        }
    };

}