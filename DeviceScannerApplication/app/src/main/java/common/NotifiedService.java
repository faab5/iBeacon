package common;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class NotifiedService extends Service {

    static private final String TAG = "NotifiedService";

    /**
     * Action to stop the service
     */
    static public final String ACTION_STOP = "com.fjansen.notifiedservice.action_stop";
    private BroadcastReceiver stopReceiver;

    /**
     * Default constructor
     */
    public NotifiedService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Creates a service with a persistent notification in the notification tray
     * This method is final to ensure a persistent notification.
     * Override onCreateService() to implement initializations.
     * Override notificationBuilder() for a custom notification
     */
    @Override
    final public void onCreate() {
        super.onCreate();

        // Register a broadcast receiver to catch service stop request
        stopReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(NotifiedService.ACTION_STOP)) {
                    stopSelf();
                }
            }
        };
        registerReceiver(stopReceiver, new IntentFilter(NotifiedService.ACTION_STOP));

        // Set up a persistent notification
        PersistentNotificationBuilder notificationBuilder = notificationBuilder();
        if (notificationBuilder==null) {
            stopSelf();
        } else {
            makePersistentNotification(notificationBuilder);
        }
        onCreateService();
    }

    public void onCreateService() {
    }

    /**
     * Override this method for a custom persistent notification
     */
    public PersistentNotificationBuilder notificationBuilder()  {
        String mainTitle = getPackageName();
        String mainText  = "Running";
        String stopText  = "STOP";
        try {
            int icon_id = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).icon;
            return new PersistentNotificationBuilder(this, icon_id, mainTitle, mainText, icon_id, stopText);
        } catch(PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Make or update the persistent notification
     */
    final public void makePersistentNotification(PersistentNotificationBuilder notificationBuilder) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Cleans up the service and notification
     * This method is made final to ensure cleaning up of the notification
     * Override onDestroyService() to implement other clean up actions
     */
    @Override
    final public void onDestroy() {
        super.onDestroy();
        // Unregister the receiver
        unregisterReceiver(stopReceiver);
        // Cancel the notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_TAG, NOTIFICATION_ID);
        onDestroyService();
    }

    public void onDestroyService(){
    }


    /*
     ***** NOTIFICATION MANAGEMENT *****
     */
    static private final String NOTIFICATION_TAG = "com.fjansen.common.notifiedservice.notification";
    static private final int    NOTIFICATION_ID  = 1055;
    static private final int    PENDING_INTENT_ACTION_STOP_REQUEST_CODE = 1056;

    /**
     * Class to easily build a persistent notification
     * containing a pending intent to Stop the service when clicked
     */
    static public class PersistentNotificationBuilder extends NotificationCompat.Builder {

        /**
         * The notification MUST have an icon, title and text.
         * The Stop action in the notification MUST have an icon and text
         */
        public PersistentNotificationBuilder(Context context, int mainIconId, String mainTitle, String mainText, int stopIconId, String stopText) {
            super(context);
            super.setAutoCancel(false);
            super.setOngoing(true);
            super.setPriority(NotificationCompat.PRIORITY_HIGH);
            super.setCategory(NotificationCompat.CATEGORY_SERVICE);
            super.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            setContentTitle(mainTitle);
            setContentText(mainText);
            setSmallIcon(mainIconId);
            PendingIntent pendingStopIntent = PendingIntent.getBroadcast(context, PENDING_INTENT_ACTION_STOP_REQUEST_CODE,
                    new Intent(NotifiedService.ACTION_STOP), PendingIntent.FLAG_CANCEL_CURRENT);
            addAction(stopIconId, stopText, pendingStopIntent);
        }

        @Override
        final public PersistentNotificationBuilder setVisibility(int visibility) {
            return this;
        }
        @Override
        final public PersistentNotificationBuilder setAutoCancel(boolean autoCancel) {
            return this;
        }
        @Override
        final public PersistentNotificationBuilder setOngoing(boolean ongoing) {
            return this;
        }
        @Override
        final public PersistentNotificationBuilder setPriority(int pri) {
            return this;
        }
        @Override
        final public PersistentNotificationBuilder setCategory(String category) {
            return this;
        }
    }
}
