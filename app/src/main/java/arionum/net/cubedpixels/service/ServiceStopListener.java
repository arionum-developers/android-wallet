package arionum.net.cubedpixels.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ServiceStopListener extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
        //DONE
        System.out.println("Arionum Service is trying to restart!");
		TransactionListenerService t = new TransactionListenerService(context);
		Intent serviceint = new Intent(context, t.getClass());
        serviceint.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            System.out.println("STARTING SERVICE FOR O");
            try {
                context.startForegroundService(serviceint);
            } catch (Exception e) {
                e.printStackTrace();
            }
		} else {
            System.out.println("STARTING SERVICE UNDER O");
			context.startService(serviceint);
		}
	}
}