package arionum.net.cubedpixels.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class ServiceStopListener extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
        //DONE
        Log.i(ServiceStopListener.class.getSimpleName(), "Arionum Service is trying to restart!");
		TransactionListenerService t = new TransactionListenerService(context);
		Intent serviceint = new Intent(context, t.getClass());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            context.startForegroundService(serviceint);
		} else {
			context.startService(serviceint);
		}
	}
}