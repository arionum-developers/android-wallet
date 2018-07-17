package arionum.net.cubedpixels.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import arionum.net.cubedpixels.R;
import arionum.net.cubedpixels.api.ApiRequest;

public class TransactionListenerService extends JobIntentService {

    private static final int JOB_ID = 1337;

    public static void enqueueWork(Context ctx, Intent intent) {
        enqueueWork(ctx, TransactionListenerService.class, JOB_ID, intent);
    }

    public void checkTransactions(final Context ctx) {
        System.out.println("Checking for Transaction");
        if (!isNetworkAvailable(ctx))
            return;
        ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
                                       @Override
                                       public void onFeedback(JSONObject object) {
                                           try {
                                               JSONArray array = object.getJSONArray("data");
                                               String id = ((JSONObject) array.get(0)).get("id").toString();
                                               if (!getString(ctx, "lastID").equalsIgnoreCase(id)) {
                                                   saveString(ctx, "lastID", id);
                                                   JSONObject o = ((JSONObject) array.get(0));
                                                   if (o.get("type").toString().equalsIgnoreCase("credit")) {
                                                       NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                                                               ctx, "ARONOTIFICATIONS")
                                                               .setSmallIcon(R.drawable.aro)
                                                               .setChannelId("notify_001")
                                                               .setContentTitle("Arionum Wallet | New Transaction!")

                                                               .setColor(ContextCompat.getColor(ctx, R.color.colorPrimary))
                                                               .setDefaults(Notification.FLAG_ONGOING_EVENT)
                                                               .setColorized(true)

                                                               .setContentText("You got a new Transaction of: "
                                                                       + ((JSONObject) array.get(0)).get("val").toString()
                                                                       + "ARO" + " from " + ""
                                                                       + ((JSONObject) array.get(0))
                                                                       .get("src"))
                                                               .setStyle(
                                                                       new NotificationCompat.InboxStyle()
                                                                               .addLine("A new Transaction of: "
                                                                                       + ((JSONObject) array.get(0))
                                                                                       .get("val").toString()
                                                                                       + "ARO")
                                                                               .addLine("from "
                                                                                       + ((JSONObject) array.get(0))
                                                                                       .get("src")))
                                                               .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                                       NotificationManager mNotificationManager =
                                                               (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                                                       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                           NotificationChannel channel = new NotificationChannel("notify_001",
                                                                   "Channel human readable title",
                                                                   NotificationManager.IMPORTANCE_DEFAULT);
                                                           mNotificationManager.createNotificationChannel(channel);
                                                       }
                                                       mNotificationManager.notify(1347, mBuilder.build());
                                                   } else {
                                                       NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                                                               ctx, "ARONOTIFICATIONS")
                                                               .setSmallIcon(R.drawable.aro)
                                                               .setChannelId("notify_001")

                                                               .setColor(ContextCompat.getColor(ctx, R.color.colorPrimary))
                                                               .setDefaults(Notification.FLAG_ONGOING_EVENT)
                                                               .setColorized(true)

                                                               .setContentTitle("Arionum Wallet | Aro Sent!")
                                                               .setContentText("Your : "
                                                                       + ((JSONObject) array.get(0)).get("val").toString()
                                                                       + "ARO has been send" + " to " + ""
                                                                       + ((JSONObject) array.get(0)).get("src"))
                                                               .setStyle(
                                                                       new NotificationCompat.InboxStyle()
                                                                               .addLine("Your "
                                                                                       + ((JSONObject) array.get(0))
                                                                                       .get("val").toString()
                                                                                       + "ARO has been send")
                                                                               .addLine("to " + ((JSONObject) array.get(0))
                                                                                       .get("src")))
                                                               .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                                       NotificationManager mNotificationManager =
                                                               (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                                                       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                           NotificationChannel channel = new NotificationChannel("notify_001",
                                                                   "Channel human readable title",
                                                                   NotificationManager.IMPORTANCE_DEFAULT);
                                                           mNotificationManager.createNotificationChannel(channel);
                                                       }
                                                       mNotificationManager.notify(1347, mBuilder.build());
                                                   }
                                                   System.out.println("1 new notifications");
                                               } else {
                                                   System.out.println("SAME ID: " + id);
                                               }
                                           } catch (Exception e) {
                                               e.printStackTrace();
                                           }


                                           System.out.println("FINISHED! ");
                                           stopSelf();

                                       }
                                   }, "getTransactions", new ApiRequest.Argument("public_key", getString(ctx, "publickey")),
                new ApiRequest.Argument("account", getString(ctx, "address")),
                new ApiRequest.Argument("limit", "1"));
    }

    public static void saveString(Context ctx, String key, String string) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(key, string);
		editor.commit();
	}

    public static String getString(Context ctx, String key) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
		String value = sharedPref.getString(key, "");
		return value;
	}

    private static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        AlarmReceiver.setAlarm(this, false);
        checkTransactions(this);
        stopSelf();
	}
}