package com.example.android.bluetoothchat;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.sql.Timestamp;

/**
 * Wrapper around BluetoothDevice with extra info
 * BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
 * int  rssi              = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
 */
public class BTDevice implements Parcelable {

    public BluetoothDevice device;
    public Integer RSSI = null;
    public byte[] scanRecord = null;
    public Timestamp timestamp = null;

    public BTDevice(BluetoothDevice device) {
        this.device = device;
    }
    public BTDevice(BluetoothDevice device, Timestamp ts) {
        this.device = device;
        this.timestamp = ts;
    }
    public BTDevice(BluetoothDevice device, Timestamp ts, int rssi) {
        this.device = device;
        this.timestamp = ts;
        this.RSSI = rssi;
    }
    public BTDevice(BluetoothDevice device, Timestamp ts, int rssi, byte[] scanRecord) {
        this.device = device;
        this.timestamp = ts;
        this.RSSI = rssi;
        this.scanRecord = scanRecord;
    }

    public BTDevice(Parcel in) {
        this.device = in.readParcelable(null);
        this.timestamp = new Timestamp(in.readLong());
        this.RSSI = in.readInt();
        int scanRecordLength = in.readInt();
        this.scanRecord = new byte[scanRecordLength];
        in.readByteArray(this.scanRecord);
    }

    @Override
    public String toString() {
        if (scanRecord==null || scanRecord.length==0) {
            return name() + ' ' + addressString() + ' ' + timestampString();
        } else {
            return name() + ' ' + addressString() + ' ' + timestampString() + ' ' + scanRecordString();
        }
    }
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(device, flags);
        dest.writeLong(timestamp == null ? 0 : timestamp.getTime());
        dest.writeInt(RSSI);
        dest.writeInt(scanRecord == null? 0 : scanRecord.length);
        dest.writeByteArray(scanRecord == null? new byte[0]: scanRecord);
    }

    public static final Parcelable.Creator<BTDevice> CREATOR = new Parcelable.Creator<BTDevice>() {
        public BTDevice createFromParcel(Parcel in) {
            return new BTDevice(in);
        }
        public BTDevice[] newArray(int size) {
            return new BTDevice[size];
        }
    };

    public Bundle bundle() {
        Bundle b = new Bundle();
        b.putParcelable("btdevice", device);
        b.putLong("timestamp", timestamp==null?null:timestamp.getTime());
        b.putInt("rssi", RSSI);
        b.putByteArray("scanrecord", scanRecord);
        return b;
    }


    /**
     * Convenience method
     */
    public String name() {
        return device.getName();
    }

    /**
     * Convenience method
     */
    public String addressString() {
        return device.getAddress().toString();
    }

    /**
     * Convenience method
     */
    public String typeString() {
        switch (device.getType()) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                return "BR/EDR";
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                return "BR/EDR/LE";
            case BluetoothDevice.DEVICE_TYPE_LE:
                return "LE";
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                return "unkonwn";
            default:
                return "API unknown";
        }
    }

    /**
     * Convenience method
     */
    public String uuidString() {
        if (device.getUuids() != null) {
            return device.getUuids().toString();
        } else {
            return "";
        }
    }

    /**
     * Convenience method
     */
    public String timestampString() {
        if (timestamp != null) {
            return timestamp.toString();
        } else {
            return "";
        }
    }

    /**
     * Convenience method
     */
    public String scanRecordString() {
        if (scanRecord != null && scanRecord.length>0) {
            StringBuilder sb = new StringBuilder();
            for (byte b : scanRecord) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } else {
            return "";
        }
    }
}