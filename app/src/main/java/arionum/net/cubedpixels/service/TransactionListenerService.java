package arionum.net.cubedpixels.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import arionum.net.cubedpixels.R;
import arionum.net.cubedpixels.api.ApiRequest;
import arionum.net.cubedpixels.utils.DoneTask;
import arionum.net.cubedpixels.views.HomeView;

public class TransactionListenerService extends Service {



	long oldTime = 0;
	private Timer timer;
	private TimerTask timerTask;
    public TransactionListenerService(Context applicationContext) {
		super();
    }

	public TransactionListenerService() {
	}


    public static final String COUNTDOWN_BR = "your_package_name.countdown_br";
    String TAG = "BroadcastService";

    public static void sendImplicitBroadcast(Context ctxt, Intent i) {
        PackageManager pm = ctxt.getPackageManager();
        List<ResolveInfo> matches = pm.queryBroadcastReceivers(i, 0);

        for (ResolveInfo resolveInfo : matches) {
            Intent explicit = new Intent(i);
            ComponentName cn =
                    new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName,
                            resolveInfo.activityInfo.name);

            explicit.setComponent(cn);
            ctxt.sendBroadcast(explicit);
        }
    }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		startTimer();


        return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
        saveCurrentTimer(getCurrentTimer() + 5);
		Log.i("EXIT", "Arionum Service is shutting down! Trying to restart...");
        Intent broadcastIntent = new Intent("cubedpixels.arionum.restart.service");
        sendImplicitBroadcast(this, broadcastIntent);
		stoptimertask();
	}

	public void startTimer() {
		timer = new Timer();
		initializeTimerTask();
		timer.schedule(timerTask, 1000, 1000);
	}

    public void saveCurrentTimer(int time) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("currentTimer", time);
        editor.commit();
    }

    public int getCurrentTimer() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int value = sharedPref.getInt("currentTimer", 0);
        return value;
    }

	public void initializeTimerTask() {
		timerTask = new TimerTask() {
			public void run() {
				//USING INT COUNTER AS EFFICIENT NETWORK AND KEEP ALIVE SERVICE
				//CHECK IF NETWORK IS AVAILABLE
				if (!isNetworkAvailable()) {
					try {
                        saveCurrentTimer(getCurrentTimer() + 7);
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return;
				}
				//CURRENT PEER CHECK
				if (HomeView.getCurrentPeer() == "")
				{
					HomeView.setup(new DoneTask() {
						@Override
						public void onDone() {
							System.out.println("NEW PEER SET!");
						}
					});
					return;
				}
                saveCurrentTimer(getCurrentTimer() + 1);
                System.out.println("Arionum Counter: " + ((4 * 60) - getCurrentTimer()));
                //CHECK LAST TRANSACTION ID -> IF ADDRESS IS SET
				if (getString("address") != "")
                    if (getCurrentTimer() > 3 * 60) {
                        System.out.println("Checking for Transaction");
                        saveCurrentTimer(0);
						ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
							@Override
							public void onFeedback(JSONObject object) {
								try {
									JSONArray array = object.getJSONArray("data");
									String id = ((JSONObject) array.get(0)).get("id").toString();
									if (!getString("lastID").equalsIgnoreCase(id)) {
										saveString("lastID", id);
										JSONObject o = ((JSONObject) array.get(0));
										if (o.get("type").toString().equalsIgnoreCase("credit")) {
											NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
													TransactionListenerService.this, "ARONOTIFICATIONS")
															.setSmallIcon(R.drawable.aro)
                                                    .setChannelId("notify_001")
                                                    .setContentTitle("Arionum Wallet | New Transaction!")
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
                                                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                NotificationChannel channel = new NotificationChannel("notify_001",
                                                        "Channel human readable title",
                                                        NotificationManager.IMPORTANCE_DEFAULT);
                                                mNotificationManager.createNotificationChannel(channel);
                                            }
                                            mNotificationManager.notify(1347, mBuilder.build());
                                        } else {
											NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
													TransactionListenerService.this, "ARONOTIFICATIONS")
															.setSmallIcon(R.drawable.aro)
                                                    .setChannelId("notify_001")
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
                                                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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
							}
						}, "getTransactions", new ApiRequest.Argument("public_key", getString("publickey")),
								new ApiRequest.Argument("account", getString("address")),
								new ApiRequest.Argument("limit", "1"));
					}
			}
		};
	}

	public void saveString(String key, String string) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(key, string);
		editor.commit();
	}

	public String getString(String key) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String value = sharedPref.getString(key, "");
		return value;
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}


	public void stoptimertask() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}