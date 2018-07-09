package arionum.net.cubedpixels.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String CUSTOM_INTENT = "net.cubedpixels.action.ALARM";

    public static void cancelAlarm(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        /* cancel any pending alarm */
        alarm.cancel(getPendingIntent(context));
    }

    public static void setAlarm(Context context, boolean force) {
        cancelAlarm(context);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // EVERY X MINUTES
        long delay = (1000 * 60 * 3);
        long when = System.currentTimeMillis();
        if (!force) {
            when += delay;
        } else when += 1000 * 1;
        System.out.println("ALARM ON AT " + when);
        /* fire the broadcast */
        alarm.set(AlarmManager.RTC_WAKEUP, when, getPendingIntent(context));
    }

    private static PendingIntent getPendingIntent(Context context) {
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        alarmIntent.setAction(CUSTOM_INTENT);

        return PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
            setAlarm(context, true);
        /* enqueue the job */
        System.out.println("RECEIVE!!!!!!!!!!!!!!!!");
        TransactionListenerService.enqueueWork(context, intent);
    }
}