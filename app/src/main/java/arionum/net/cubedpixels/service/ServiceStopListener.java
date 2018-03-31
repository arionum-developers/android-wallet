package arionum.net.cubedpixels.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceStopListener extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(ServiceStopListener.class.getSimpleName(), "Trying to start Service!");
		TransactionListenerService t = new TransactionListenerService(context);
		Intent serviceint = new Intent(context, t.getClass());
		context.startService(serviceint);
	}
}