package arionum.net.cubedpixels;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v13.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Random;

import arionum.net.cubedpixels.service.TransactionListenerService;
import arionum.net.cubedpixels.utils.DoneTask;
import arionum.net.cubedpixels.views.HomeView;
import arionum.net.cubedpixels.views.QRview;

public class MainActivity extends AppCompatActivity {

	//MAIN AND LAUNCH ACTIVITY

	public static boolean running = true;
	private static MainActivity instance;
	private Drawable drawable;

	private String[] someStrings = { "Searching Node...", "Searching the Internet...", "HODLING...", "Waiting music...",
			"Wallet things...", "Loading into the matrix...", "Transactions are being cleaned...",
			"Cat powered Servers...", "Downloading Arionum...", "Downloading the Peer-List...", "History check...",
			"Contacting Node...", "Helping miners...", "Drawing cats...", "Crashing Bitcoin...",
			"Adding flying Cats...", "Doing Maths...", "Counting Stars...", "Counting AROs...", "Searching Server...",
			"Waking Arionum...", "Loading stuff...", "Doing stuff...", "Walking in the Internet...",
			"Destroying Meteors...", "Scuffing cats...", "Petting cats...", "Thinking about cats...",
			"Connecting to Arionum...", "Making cats smarter...", "Building Arionum...", "Thanking BitcoinJ for EC...",
			"Waking up the Devs...", "Playing Kurby music...", "Hey wake up!", "Doing really hard math..."};

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		instance = this;
		ImageView animationview = findViewById(R.id.imageView4);
		drawable = animationview.getDrawable();

		TransactionListenerService mSensorService = new TransactionListenerService(this);
		Intent serviceint = new Intent(this, mSensorService.getClass());
		if (!isServiceRunning(mSensorService.getClass())) {
			startService(serviceint);
		}

		if (drawable instanceof Animatable) {
			((Animatable) drawable).start();
			new Thread(new Runnable() {
				@Override
				public void run() {
					isStoragePermissionGranted();
					while (running) {
						setLoadingText(someStrings[new Random().nextInt(someStrings.length)]);
						while (((Animatable) drawable).isRunning()) {
						}
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						Handler h = new Handler(instance.getMainLooper());
						h.post(new Runnable() {
							@Override
							public void run() {
								((Animatable) drawable).start();
							}
						});
					}
				}
			}).start();
		}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			Log.v("ICM", "Permission: " + permissions[0] + "was " + grantResults[0]);
			start();
		}
		if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
			Log.v("ICM", "Permission: " + permissions[0] + "was " + grantResults[0]);
			new MaterialDialog.Builder(MainActivity.this).title("Error").content("You need to grant Permissions!")
					.positiveText("Back").onPositive(new MaterialDialog.SingleButtonCallback() {
						@Override
						public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
							finish();
						}
					}).show();
		}
	}

	public boolean isStoragePermissionGranted() {
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

				Log.v("IMC", "Permission is granted");
				start();
				return true;
			} else {

				Log.v("IMC", "Permission is revoked");
				ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, 1);
				return false;
			}
		} else {
			start();
			Log.v("IMC", "Permission is granted");
			return true;
		}
	}

	public void setLoadingText(final String text) {
		Handler h = new Handler(MainActivity.this.getMainLooper());
		h.post(new Runnable() {
			@Override
			public void run() {
				TextView tv = findViewById(R.id.loadingtext);
				tv.setText(text);
			}
		});
	}

	public void start() {
		setLoadingText(someStrings[new Random().nextInt(someStrings.length)]);
		HomeView.setup(new DoneTask() {
			@Override
			public void onError() {
				setLoadingText("Arionum Server is not reachable! Retrying...");
				try {
					Thread.sleep(1200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				start();
			}

			@Override
			public void onDone() {
				setLoadingText(someStrings[new Random().nextInt(someStrings.length)]);
				try {
					Thread.sleep(1500);
					setLoadingText(someStrings[new Random().nextInt(someStrings.length)]);
					Thread.sleep(new Random().nextInt(4000));
					setLoadingText(someStrings[new Random().nextInt(someStrings.length)]);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (getString("address") != "") {
					Intent i = new Intent(instance, HomeView.class);
					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					instance.startActivity(i);
					running = false;
				} else {
					Intent i = new Intent(instance, QRview.class);
					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					instance.startActivity(i);
					running = false;
				}
			}
		});
	}

	public String getString(String key) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String value = sharedPref.getString(key, "");
		return value;
	}

	private boolean isServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				Log.i("isMyServiceRunning?", true + "");
				return true;
			}
		}
		Log.i("isMyServiceRunning?", false + "");
		return false;
	}
}
