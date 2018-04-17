package arionum.net.cubedpixels.views;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import arionum.net.cubedpixels.MainActivity;
import arionum.net.cubedpixels.R;
import arionum.net.cubedpixels.utils.Base58;

public class QRview extends AppCompatActivity implements QRCodeReaderView.OnQRCodeReadListener {

	private TextView resultTextView;
	private QRCodeReaderView qrCodeReaderView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.qr_login);
		resultTextView = findViewById(R.id.qrtextview);
		qrCodeReaderView = findViewById(R.id.asdqrview);
		qrCodeReaderView.setOnQRCodeReadListener(this);
		qrCodeReaderView.setQRDecodingEnabled(true);
		qrCodeReaderView.setAutofocusInterval(1000L);
		qrCodeReaderView.setTorchEnabled(true);
		qrCodeReaderView.setBackCamera();

}

	@Override
	public void onQRCodeRead(String text, PointF[] points) {
		resultTextView.setText(text);
		if (text.contains("|")) {
			try {
				String[] splitt = text.split("\\|");
				String address = splitt[0];
				String publickey = splitt[1];
				String privatekey = splitt[2];
				String privatekey2 = new String(Base58.encode(splitt[2].getBytes()));
				saveString("address", address);
				saveString("publickey", publickey);
				saveString("privatekey", privatekey2);
				qrCodeReaderView.stopCamera();
				

				Intent i = new Intent(QRview.this, MainActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				QRview.this.startActivity(i);
			} catch (Exception e) {
				System.out.println("ERROR !!!!!");
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onBackPressed() {
		new MaterialDialog.Builder(this).title("Do you want to exit?").cancelable(true).positiveText("Yes")
				.negativeText("No").onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						Intent intent = new Intent(Intent.ACTION_MAIN);
						intent.addCategory(Intent.CATEGORY_HOME);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
						finish();
						System.exit(0);
					}
				}).show();
	}

	public void saveString(String key, String string) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(key, string);
		editor.commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		qrCodeReaderView.startCamera();
	}

	@Override
	protected void onPause() {
		super.onPause();
		qrCodeReaderView.stopCamera();
	}
}
