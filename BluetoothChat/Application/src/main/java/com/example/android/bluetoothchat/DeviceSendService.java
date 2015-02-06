package com.example.android.bluetoothchat;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.example.android.common.logger.Log;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;


public class DeviceSendService extends IntentService {

    /**
     * Tag for Log
     */
    private static final String TAG = "DeviceSendService";

    private static final String ACTION_SEND_BT_DEVICE = "action.SEND_BT_DEVICE";

    private int pendingIntentCounter = 0;
    private int notificationCounter  = 0;
    private String notificationTag   = TAG;

    public static void sendDevice(Context context, BTDevice btdevice) {
        Intent intent = new Intent(context, DeviceSendService.class);
        intent.setAction(ACTION_SEND_BT_DEVICE);
        intent.putExtra("btdevice", btdevice);
        context.startService(intent);
    }

    public DeviceSendService() {
        super("DeviceSendService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SEND_BT_DEVICE.equals(action)) {
                BTDevice btDevice = intent.getParcelableExtra("btdevice");
                handleDevice(btDevice);
            }
        }
    }

    /**
     * Handle action in the provided background thread with the provided
     * parameters.
     */
    private void handleDevice(BTDevice btDevice) {
        Log.d(TAG, btDevice.toString());
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://192.168.0.104:8080");
        httppost.setHeader("Content-type", "application/json");
        try {
            JSONObject json = new JSONObject();
            json.put("name", btDevice.name());
            json.put("address", btDevice.addressString());
            json.put("type", btDevice.typeString());
            json.put("uuid", btDevice.uuidString());
            json.put("timestamp", btDevice.timestampString());
            json.put("rssi", btDevice.RSSI);
            json.put("scanrecord", btDevice.scanRecordString());
            StringEntity params = new StringEntity(json.toString());
            httppost.setEntity(params);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.toString());
        }
        try {
            HttpResponse httpResponse = httpclient.execute(httppost);
            // The initial response line
            int statusCode      = httpResponse.getStatusLine().getStatusCode();
            String reasonPhrase = httpResponse.getStatusLine().getReasonPhrase();
            // The headers
            Header actionHeader = httpResponse.getFirstHeader("action");
            if (actionHeader != null) {
                Log.d(TAG, actionHeader.getName() + ":" + actionHeader.getValue());
                String responseContent = EntityUtils.toString(httpResponse.getEntity());

                Intent intent = new Intent(this, HttpResponseActivity.class);
                intent.putExtra("viewcontent", responseContent);

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addParentStack(HttpResponseActivity.class);
                stackBuilder.addNextIntent(intent);

                PendingIntent pendingIntent = stackBuilder.getPendingIntent(pendingIntentCounter, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setContentTitle("BT Chat result")
                                .setContentText("A bluetooth device was recognized");
                notificationBuilder.setContentIntent(pendingIntent);
                notificationBuilder.setAutoCancel(true);

                NotificationManager notificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(notificationTag+(notificationCounter), notificationCounter, notificationBuilder.build());

            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        return sb.toString();
    }
}
