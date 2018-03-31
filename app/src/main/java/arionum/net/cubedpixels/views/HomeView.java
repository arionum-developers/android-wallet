package arionum.net.cubedpixels.views;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.mikepenz.crossfader.Crossfader;
import com.mikepenz.crossfader.util.UIUtils;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.context.IconicsLayoutInflater2;
import com.mikepenz.ionicons_typeface_library.Ionicons;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.MiniDrawer;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;

import net.glxn.qrgen.android.QRCode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.Scanner;

import arionum.net.cubedpixels.MainActivity;
import arionum.net.cubedpixels.R;
import arionum.net.cubedpixels.api.ApiRequest;
import arionum.net.cubedpixels.style.Styler;
import arionum.net.cubedpixels.utils.Base58;
import arionum.net.cubedpixels.utils.CrossfadeWrapper;
import arionum.net.cubedpixels.utils.DoneTask;

public class HomeView extends AppCompatActivity {

	private HomeView instance;
	private static ArrayList<String> peers = new ArrayList<>();
	private static ArrayList<Page> pages = new ArrayList<>();
	private static String currentPeer = "";
	private static String public_key = "";
	private static String private_key = "";
	private static String address = "";

	private static final int PROFILE_SETTING = 1;

	private AccountHeader headerResult = null;
	private Drawer result = null;
	private MiniDrawer miniResult = null;
	private Crossfader crossFader;
	private boolean refreshing = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.instance = this;

		this.public_key = getString("publickey");
		if (!getString("privatekey").isEmpty())
			try {
				this.private_key = new String(Base58.decode(getString("privatekey")));
			} catch (Exception e) {
				new MaterialDialog.Builder(HomeView.this).title("D3C0D3 exception!")
						.content("Your private key couldn't be encrypted!").show();
			}
		this.address = getString("address");

		LayoutInflaterCompat.setFactory2(getLayoutInflater(), new IconicsLayoutInflater2(getDelegate()));
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);

		// SETUP
		this.currentPeer = peers.get(new Random().nextInt(peers.size()));
		TextView test = findViewById(R.id.connected);
		test.setText(currentPeer.replace("http://", ""));
		TextView address = findViewById(R.id.address);
		address.setText(this.address);
		setupQR();

		// QR
		float[] hsv = new float[3];
		int color = ContextCompat.getColor(this, R.color.colorBackground);
		Color.colorToHSV(color, hsv);
		hsv[2] *= 1.2f;
		color = Color.HSVToColor(hsv);
		Bitmap myBitmap = QRCode.from("sendaro" + "|" + this.address + "||").withSize(200, 200)
				.withColor(color, Color.parseColor("#00000000")).bitmap();
		myBitmap.setHasAlpha(true);
		ImageView myImage = (ImageView) findViewById(R.id.qrimage);
		myImage.setImageBitmap(myBitmap);
		myImage.setAlpha(150);

		// ICONS
		ImageView sync = findViewById(R.id.refreshIcon);
		IconicsDrawable syncd = new IconicsDrawable(HomeView.this).icon(GoogleMaterial.Icon.gmd_refresh_sync)
				.color(Color.WHITE).sizeDp(28);
		syncd.setAlpha(130);
		sync.setImageDrawable(syncd);
		sync.setClickable(true);
		sync.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!refreshing)
					refreshLastTransactions();
			}
		});

		// DESIGN
		createDrawer(savedInstanceState);
		setupPages();

		// STYLER
		Styler.initStyle(this, findViewById(R.id.HEIGHTESTVIEW));
		Styler.initStyle(this, findViewById(R.id.crossview));

		// GET BALANCE
		ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
			@Override
			public void onFeedback(JSONObject object) {
				System.out.println("GOT RESPONSE!");
				try {

					TextView test = findViewById(R.id.balancevalue);
					test.setText(object.get("data").toString() + " ARO");
				} catch (Exception e) {

				}
			}
		}, "getBalance", new ApiRequest.Argument("public_key", this.public_key),
				new ApiRequest.Argument("account", this.address));

		// GETTRANSACTIONS
		ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
			@Override
			public void onPreFetch(JSONObject object) {
				try {
					System.out.println("PREFETCH");
					sortArrayAndPutInList(object.getJSONArray("data"), (ListView) findViewById(R.id.transactionlist));

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onFeedback(JSONObject object) {
				System.out.println("GOT RESPONSE! TRANSACTIONS!");
				try {
					if (object.getJSONArray("data").length() > 0) {
						saveString("lastID", object.getJSONArray("data").getJSONObject(0).get("id").toString());
						sortArrayAndPutInList(object.getJSONArray("data"),
								(ListView) findViewById(R.id.transactionlist));
					}
					Handler h = new Handler(instance.getMainLooper());
					h.post(new Runnable() {
						@Override
						public void run() {
							findViewById(R.id.waitingtransbar).setVisibility(View.GONE);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				refreshing = false;
			}
		}, "getTransactions", new ApiRequest.Argument("public_key", this.public_key),
				new ApiRequest.Argument("account", this.address), new ApiRequest.Argument("limit", "10"));

	}

	public void refreshLastTransactions() {
		refreshing = true;
		findViewById(R.id.waitingtransbar).setVisibility(View.VISIBLE);
		ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
			@Override
			public void onFeedback(JSONObject object) {
				System.out.println("GOT RESPONSE!");
				try {

					TextView test = findViewById(R.id.balancevalue);
					test.setText(object.get("data").toString() + " ARO");
				} catch (Exception e) {

				}
			}
		}, "getBalance", new ApiRequest.Argument("public_key", this.public_key),
				new ApiRequest.Argument("account", this.address));
		ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
			@Override
			public void onPreFetch(JSONObject object) {
				try {
					sortArrayAndPutInList(object.getJSONArray("data"), (ListView) findViewById(R.id.transactionlist));

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onFeedback(JSONObject object) {
				try {
					sortArrayAndPutInList(object.getJSONArray("data"), (ListView) findViewById(R.id.transactionlist));
					Handler h = new Handler(instance.getMainLooper());
					h.post(new Runnable() {
						@Override
						public void run() {
							findViewById(R.id.waitingtransbar).setVisibility(View.GONE);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				refreshing = false;
			}
		}, "getTransactions", new ApiRequest.Argument("public_key", this.public_key),
				new ApiRequest.Argument("account", this.address), new ApiRequest.Argument("limit", "10"));
	}

	private static QRCodeReaderView.OnQRCodeReadListener upcminglstnr;
	private static QRCodeReaderView qrCodeReaderView;
	private static QRCodeReaderView savedState;

	public void setupPages() {
		// SETUP BALANCE SCREEN
		pages.add(new Page("BALANCE", (RelativeLayout) findViewById(R.id.balanceview)) {
			@Override
			public void onEnable() {

			}
		});

		// ->
		final TextView addressinfo = findViewById(R.id.address);
		addressinfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("Arionum-Address", addressinfo.getText().toString());
				clipboard.setPrimaryClip(clip);
				Toast.makeText(instance, "Address copied to Clipboard", Toast.LENGTH_SHORT).show();
			}
		});

		final ImageView qrimagerequest = findViewById(R.id.qrimage);
		qrimagerequest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				new MaterialDialog.Builder(HomeView.this).title("Request ARO")
						.content("Enter your requested amount of ARO")
						.inputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER)
						.input("Amount", "0.00", new MaterialDialog.InputCallback() {
							@Override
							public void onInput(MaterialDialog dialog, CharSequence input) {
								// Do something
							}
						}).positiveText("Request").negativeText("Cancel")
						.onPositive(new MaterialDialog.SingleButtonCallback() {
							@Override
							public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
								Double amount = Double.parseDouble(dialog.getInputEditText().getText().toString());
								ImageView qrimage = findViewById(R.id.qrreuqestimage);
								Bitmap myBitmap = QRCode
										.from("sendaro" + "|" + HomeView.this.address + "|" + amount + "|")
										.withSize(600, 600).withColor(Color.BLACK, Color.parseColor("#00000000"))
										.bitmap();
								qrimage.setImageBitmap(myBitmap);
								findViewById(R.id.qrrequestview).setVisibility(View.VISIBLE);
							}
						}).show();
			}
		});
		final ImageView qrrequestclose = findViewById(R.id.closeqrrequest);
		qrrequestclose.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				findViewById(R.id.qrrequestview).setVisibility(View.GONE);
			}
		});

		// SETUP SEND SCREEN
		pages.add(new Page("SEND", (RelativeLayout) findViewById(R.id.send)) {
			@Override
			public void onEnable() {

			}
		});
		final EditText amountedit = findViewById(R.id.amountto);
		final TextView fee = findViewById(R.id.fee);
		amountedit.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				try {
					String t = editable.toString();
					Double d = Double.parseDouble(t);
					Double a = d * 0.0025;
					if (a > 10)
						a = 10.0;
					fee.setText("Fee: " + a + " ARO");
				} catch (Exception e) {
					fee.setText("Fee: 0.000 ARO");
				}
			}
		});
		Button b = findViewById(R.id.sendbutton);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					final Double amount = Double.parseDouble(amountedit.getText().toString());
					final String address = ((EditText) findViewById(R.id.addressto)).getText().toString();
					final String message = ((EditText) findViewById(R.id.messageedit)).getText().toString();
					new MaterialDialog.Builder(HomeView.this).title("Transaction")
							.content("Are you sure you want to send " + amount + " ARO " + "\n to: " + address)
							.cancelable(false).positiveText("Yes").negativeText("No").autoDismiss(false)
							.onPositive(new MaterialDialog.SingleButtonCallback() {
								@Override
								public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
									dialog.dismiss();
									final MaterialDialog d = new MaterialDialog.Builder(HomeView.this).title("Sending")
											.progress(true, 100).progressIndeterminateStyle(true).cancelable(false)
											.show();
									ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
										@Override
										public void onFeedback(final JSONObject object) {
											d.dismiss();
											if (object == null || object.toString().contains("error")) {
												Handler h = new Handler(instance.getMainLooper());
												h.post(new Runnable() {
													@Override
													public void run() {
														try {
															MaterialDialog d = new MaterialDialog.Builder(HomeView.this)
																	.title("Error:").content("Message: " + "\n"
																			+ object.get("data") + " <-> ")
																	.cancelable(true).show();
														} catch (Exception e) {
															e.printStackTrace();
															MaterialDialog d = new MaterialDialog.Builder(HomeView.this)
																	.title("Error:")
																	.content("Message: " + "\n" + e.getMessage())
																	.cancelable(true).show();
														}
													}
												});

											} else {
												Handler h = new Handler(instance.getMainLooper());
												h.post(new Runnable() {
													@Override
													public void run() {
														try {
															MaterialDialog d = new MaterialDialog.Builder(HomeView.this)
																	.title("Transaction sent!")
																	.content("Your transaction ID:" + "\n"
																			+ object.get("data").toString())
																	.cancelable(true).show();
														} catch (final Exception e) {
															e.printStackTrace();
															Handler h = new Handler(instance.getMainLooper());
															h.post(new Runnable() {
																@Override
																public void run() {
																	MaterialDialog d = new MaterialDialog.Builder(
																			HomeView.this).title("Error:")
																					.content("Debug: " + e.getMessage())
																					.cancelable(true).show();
																}
															});
														}
													}
												});

											}

										}
									}, "send", new ApiRequest.Argument("val", amount),
											new ApiRequest.Argument("dst", address),
											new ApiRequest.Argument("public_key", HomeView.this.public_key),
											new ApiRequest.Argument("private_key", HomeView.this.private_key),
											new ApiRequest.Argument("date", System.currentTimeMillis() / 1000),
											new ApiRequest.Argument("message",
													"Send from Arionum Android Wallet | " + message));

								}
							}).show();
				} catch (Exception e) {

				}
			}
		});
		qrCodeReaderView = findViewById(R.id.receivescanner);
		savedState = new QRCodeReaderView(this);
		savedState.setId(R.id.receivescanner);
		savedState.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT));

		final QRCodeReaderView.OnQRCodeReadListener listener = createQRlistener();
		qrCodeReaderView.setQRDecodingEnabled(false);
		qrCodeReaderView.setOnQRCodeReadListener(listener);
		qrCodeReaderView.setAutofocusInterval(1000L);
		qrCodeReaderView.setBackCamera();

		// SETUP RECEIVE SCREEN
		pages.add(new Page("RECEIVE", (RelativeLayout) findViewById(R.id.receiveview)) {
			@Override
			public void onEnable() {
				// REPLACE AND COPY
				replaceView(qrCodeReaderView, savedState);
				savedState = new QRCodeReaderView(HomeView.this);
				savedState.setId(R.id.receivescanner);
				savedState.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
						RelativeLayout.LayoutParams.MATCH_PARENT));

				qrCodeReaderView = findViewById(R.id.receivescanner);
				qrCodeReaderView.setQRDecodingEnabled(true);
				qrCodeReaderView.startCamera();
				qrCodeReaderView.setQRDecodingEnabled(true);
				qrCodeReaderView.startCamera();
				qrCodeReaderView.bringToFront();
				qrCodeReaderView.setOnQRCodeReadListener(createQRlistener());
				qrCodeReaderView.setAutofocusInterval(1000L);
				qrCodeReaderView.setBackCamera();
				qrCodeReaderView.setQRDecodingEnabled(true);
			}

			@Override
			public void onDisable() {
				qrCodeReaderView.stopCamera();
				qrCodeReaderView.setQRDecodingEnabled(false);
			}
		});

		// SETUP HISTORY SCREEN
		pages.add(new Page("HISTORY", (RelativeLayout) findViewById(R.id.historyview)) {
			@Override
			public void onEnable() {
				try {
					String transactions = getString("transactions");
					final JSONObject p = new JSONObject(transactions);
					sortArrayAndPutInList(p.getJSONArray("data"),
							(ListView) findViewById(R.id.historylisttransactions));
				} catch (Exception e) {
				}

				checkIfLastTransactionIsSame(new LastTransactionTimer() {
					@Override
					public void onSame(String id) {
						try {
							String transactions = getString("transactions");
							final JSONObject p = new JSONObject(transactions);
							JSONArray a = p.getJSONArray("data");
							if (a.length() < 11) {
								downloadTransactions(new Call() {
									@Override
									public void onDone(final JSONObject o) {
										try {
											sortArrayAndPutInList(o.getJSONArray("data"),
													(ListView) findViewById(R.id.historylisttransactions));
											Handler h = new Handler(instance.getMainLooper());
											h.post(new Runnable() {
												@Override
												public void run() {
													findViewById(R.id.progressBar).setVisibility(View.GONE);
												}
											});
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								});
							} else {
								String currentID = getString("lastID");
								if (currentID != ((JSONObject) a.get(0)).get("id")) {
									downloadTransactions(new Call() {
										@Override
										public void onDone(final JSONObject o) {
											try {
												sortArrayAndPutInList(o.getJSONArray("data"),
														(ListView) findViewById(R.id.historylisttransactions));
												Handler h = new Handler(instance.getMainLooper());
												h.post(new Runnable() {
													@Override
													public void run() {
														findViewById(R.id.progressBar).setVisibility(View.GONE);
													}
												});
											} catch (Exception e) {
												e.printStackTrace();
											}
										}
									});
								} else {
									sortArrayAndPutInList(p.getJSONArray("data"),
											(ListView) findViewById(R.id.historylisttransactions));
									Handler h = new Handler(instance.getMainLooper());
									h.post(new Runnable() {
										@Override
										public void run() {
											findViewById(R.id.progressBar).setVisibility(View.GONE);
										}
									});
								}
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onDifferect(String id) {
						downloadTransactions(new Call() {
							@Override
							public void onDone(JSONObject o) {
								try {
									ListView l = (ListView) findViewById(R.id.historylisttransactions);
									sortArrayAndPutInList(o.getJSONArray("data"),
											(ListView) findViewById(R.id.historylisttransactions));
									Handler h = new Handler(instance.getMainLooper());
									h.post(new Runnable() {
										@Override
										public void run() {
											findViewById(R.id.progressBar).setVisibility(View.GONE);
										}
									});
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
					}
				});
			}
		});
	}

	public static ViewGroup getParent(View view) {
		return (ViewGroup) view.getParent();
	}

	public static void removeView(View view) {
		ViewGroup parent = getParent(view);
		if (parent != null) {
			parent.removeView(view);
		}
	}

	public static void replaceView(View currentView, View newView) {
		ViewGroup parent = getParent(currentView);
		if (parent == null) {
			return;
		}
		final int index = parent.indexOfChild(currentView);
		removeView(currentView);
		removeView(newView);
		parent.addView(newView, index);
	}

	public void sortArrayAndPutInList(JSONArray array, final ListView view) {
		try {
			int size = array.length();
			ArrayList<String> name = new ArrayList<String>();
			ArrayList<GoogleMaterial.Icon> icon = new ArrayList<GoogleMaterial.Icon>();
			for (int i = 0; i < size; i++) {
				JSONObject o = array.getJSONObject(i);
				name.add(o.get("id").toString() + "," + o.get("val").toString() + "," + o.get("src") + ","
						+ o.get("dst") + "," + o.get("date"));
				// type <-
				if (o.get("type").toString().equals("credit")) {
					icon.add(GoogleMaterial.Icon.gmd_long_arrow_down);
				} else {
					icon.add(GoogleMaterial.Icon.gmd_long_arrow_up);

				}
			}
			final CustomList adapter = new CustomList(HomeView.this, name, icon);
			Handler h = new Handler(instance.getMainLooper());
			h.post(new Runnable() {
				@Override
				public void run() {
					view.setAdapter(adapter);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void downloadTransactions(final Call call) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// GETTRANSACTIONS
				findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
				ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
					@Override
					public void onPreFetch(JSONObject object) {
						try {
							call.onDone(object);

						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onFeedback(JSONObject object) {
						try {
							saveString("lastID", object.getJSONArray("data").getJSONObject(0).get("id").toString());
							saveString("transactions", object.toString());
							call.onDone(object);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}, "getTransactions", new ApiRequest.Argument("public_key", HomeView.this.public_key),
						new ApiRequest.Argument("account", HomeView.this.address),
						new ApiRequest.Argument("limit", "1000"));
			}
		}).start();
	}

	public QRCodeReaderView.OnQRCodeReadListener createQRlistener() {
		return new QRCodeReaderView.OnQRCodeReadListener() {
			@Override
			public void onQRCodeRead(String text, PointF[] points) {
				System.out.println(text);
				try {
					String[] splitt = text.split("\\|");
					System.out.println(splitt.length);
					if (splitt.length == 4 || splitt.length == 2) {
						String arosend = splitt[0];
						if (arosend.equalsIgnoreCase("arosend")) {
							qrCodeReaderView.stopCamera();
							qrCodeReaderView.setQRDecodingEnabled(false);
							final String address = splitt[1];
							try {
								Double.parseDouble(splitt[2]);
							} catch (Exception e) {
								showPage("send");
								EditText et = findViewById(R.id.addressto);
								et.setText(address);
								new MaterialDialog.Builder(HomeView.this).title("Transaction Reader")
										.content("No amount was given so you got redirected to the Send-Page")
										.positiveText("Ok").show();
								return;
							}
							final Double val = Double.parseDouble(splitt[2]);
							String message = splitt[3];
							if (message.isEmpty())
								message = "No Message given";
							new MaterialDialog.Builder(HomeView.this).title("Scanned Transaction Request")
									.content("Do you want to accept the QR request?" + "\nAddress to: " + address
											+ "\nValue: " + val + "\nMessage: " + message)
									.positiveText("Yes").negativeText("No")
									.onNegative(new MaterialDialog.SingleButtonCallback() {
										@Override
										public void onClick(@NonNull MaterialDialog dialog,
												@NonNull DialogAction which) {
											// REPLACE AND COPY
											replaceView(qrCodeReaderView, savedState);
											savedState = new QRCodeReaderView(HomeView.this);
											savedState.setId(R.id.receivescanner);
											savedState.setLayoutParams(new RelativeLayout.LayoutParams(
													RelativeLayout.LayoutParams.MATCH_PARENT,
													RelativeLayout.LayoutParams.MATCH_PARENT));

											qrCodeReaderView = findViewById(R.id.receivescanner);
											qrCodeReaderView.setQRDecodingEnabled(true);
											qrCodeReaderView.startCamera();
											qrCodeReaderView.setQRDecodingEnabled(true);
											qrCodeReaderView.startCamera();
											qrCodeReaderView.bringToFront();
											qrCodeReaderView.setOnQRCodeReadListener(createQRlistener());
											qrCodeReaderView.setAutofocusInterval(1000L);
											qrCodeReaderView.setBackCamera();
											qrCodeReaderView.setQRDecodingEnabled(true);
										}
									}).cancelable(false).onPositive(new MaterialDialog.SingleButtonCallback() {
										@Override
										public void onClick(@NonNull MaterialDialog dialog,
												@NonNull DialogAction which) {
											ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
												@Override
												public void onFeedback(final JSONObject object) {
													if (object == null || object.toString().contains("error")) {
														Handler h = new Handler(instance.getMainLooper());
														h.post(new Runnable() {
															@Override
															public void run() {
																try {
																	MaterialDialog d = new MaterialDialog.Builder(
																			HomeView.this)
																					.title("Error:")
																					.content("Message: " + "\n"
																							+ object.get("data")
																							+ " <-> ")
																					.cancelable(true).show();
																	// REPLACE
																	// AND COPY
																	replaceView(qrCodeReaderView, savedState);
																	savedState = new QRCodeReaderView(HomeView.this);
																	savedState.setId(R.id.receivescanner);
																	savedState.setLayoutParams(
																			new RelativeLayout.LayoutParams(
																					RelativeLayout.LayoutParams.MATCH_PARENT,
																					RelativeLayout.LayoutParams.MATCH_PARENT));

																	qrCodeReaderView = findViewById(
																			R.id.receivescanner);
																	qrCodeReaderView.setQRDecodingEnabled(true);
																	qrCodeReaderView.startCamera();
																	qrCodeReaderView.setQRDecodingEnabled(true);
																	qrCodeReaderView.startCamera();
																	qrCodeReaderView.bringToFront();
																	qrCodeReaderView.setOnQRCodeReadListener(
																			createQRlistener());
																	qrCodeReaderView.setAutofocusInterval(1000L);
																	qrCodeReaderView.setBackCamera();
																	qrCodeReaderView.setQRDecodingEnabled(true);
																} catch (Exception e) {
																	e.printStackTrace();
																	MaterialDialog d = new MaterialDialog.Builder(
																			HomeView.this)
																					.title("Error:")
																					.content("Message: " + "\n"
																							+ e.getMessage())
																					.cancelable(true).show();
																	// REPLACE
																	// AND COPY
																	replaceView(qrCodeReaderView, savedState);
																	savedState = new QRCodeReaderView(HomeView.this);
																	savedState.setId(R.id.receivescanner);
																	savedState.setLayoutParams(
																			new RelativeLayout.LayoutParams(
																					RelativeLayout.LayoutParams.MATCH_PARENT,
																					RelativeLayout.LayoutParams.MATCH_PARENT));

																	qrCodeReaderView = findViewById(
																			R.id.receivescanner);
																	qrCodeReaderView.setQRDecodingEnabled(true);
																	qrCodeReaderView.startCamera();
																	qrCodeReaderView.setQRDecodingEnabled(true);
																	qrCodeReaderView.startCamera();
																	qrCodeReaderView.bringToFront();
																	qrCodeReaderView.setOnQRCodeReadListener(
																			createQRlistener());
																	qrCodeReaderView.setAutofocusInterval(1000L);
																	qrCodeReaderView.setBackCamera();
																	qrCodeReaderView.setQRDecodingEnabled(true);
																}
															}
														});

													} else {
														Handler h = new Handler(instance.getMainLooper());
														h.post(new Runnable() {
															@Override
															public void run() {
																try {
																	MaterialDialog d = new MaterialDialog.Builder(
																			HomeView.this)
																					.title("Transaction sent!")
																					.content("Your transaction ID:"
																							+ "\n" + object.get("data")
																									.toString())
																					.cancelable(true).show();
																	// REPLACE
																	// AND COPY
																	replaceView(qrCodeReaderView, savedState);
																	savedState = new QRCodeReaderView(HomeView.this);
																	savedState.setId(R.id.receivescanner);
																	savedState.setLayoutParams(
																			new RelativeLayout.LayoutParams(
																					RelativeLayout.LayoutParams.MATCH_PARENT,
																					RelativeLayout.LayoutParams.MATCH_PARENT));

																	qrCodeReaderView = findViewById(
																			R.id.receivescanner);
																	qrCodeReaderView.setQRDecodingEnabled(true);
																	qrCodeReaderView.startCamera();
																	qrCodeReaderView.setQRDecodingEnabled(true);
																	qrCodeReaderView.startCamera();
																	qrCodeReaderView.bringToFront();
																	qrCodeReaderView.setOnQRCodeReadListener(
																			createQRlistener());
																	qrCodeReaderView.setAutofocusInterval(1000L);
																	qrCodeReaderView.setBackCamera();
																	qrCodeReaderView.setQRDecodingEnabled(true);
																} catch (final Exception e) {
																	e.printStackTrace();
																	Handler h = new Handler(instance.getMainLooper());
																	h.post(new Runnable() {
																		@Override
																		public void run() {
																			MaterialDialog d = new MaterialDialog.Builder(
																					HomeView.this)
																							.title("Error:")
																							.content("Debug: "
																									+ e.getMessage())
																							.cancelable(true).show();
																			// REPLACE
																			// AND
																			// COPY
																			replaceView(qrCodeReaderView, savedState);
																			savedState = new QRCodeReaderView(
																					HomeView.this);
																			savedState.setId(R.id.receivescanner);
																			savedState.setLayoutParams(
																					new RelativeLayout.LayoutParams(
																							RelativeLayout.LayoutParams.MATCH_PARENT,
																							RelativeLayout.LayoutParams.MATCH_PARENT));

																			qrCodeReaderView = findViewById(
																					R.id.receivescanner);
																			qrCodeReaderView.setQRDecodingEnabled(true);
																			qrCodeReaderView.startCamera();
																			qrCodeReaderView.setQRDecodingEnabled(true);
																			qrCodeReaderView.startCamera();
																			qrCodeReaderView.bringToFront();
																			qrCodeReaderView.setOnQRCodeReadListener(
																					createQRlistener());
																			qrCodeReaderView
																					.setAutofocusInterval(1000L);
																			qrCodeReaderView.setBackCamera();
																			qrCodeReaderView.setQRDecodingEnabled(true);
																		}
																	});
																}
															}
														});

													}

												}
											}, "send", new ApiRequest.Argument("val", val),
													new ApiRequest.Argument("dst", address),
													new ApiRequest.Argument("public_key", HomeView.this.public_key),
													new ApiRequest.Argument("private_key", HomeView.this.private_key),
													new ApiRequest.Argument("date", System.currentTimeMillis() / 1000),
													new ApiRequest.Argument("message",
															"Send from Arionum Android Wallet"));
										}
									}).show();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		};
	}

	public void showPage(String name) {
		for (Page p : pages) {
			if (p.getName().equalsIgnoreCase(name)) {
				p.onEnable();
				p.getLayout().setVisibility(View.VISIBLE);
			} else {
				if (p.getLayout().getVisibility() == View.VISIBLE)
					p.onDisable();
				p.getLayout().setVisibility(View.GONE);
			}

		}
	}

	public static void setup(final DoneTask done) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					System.out.print(">>Running Peer download");
					URL url = new URL("http://api.arionum.com/peers.txt");
					Scanner s = new Scanner(url.openStream());
					while (s.hasNext())
						peers.add(s.next());
					currentPeer = peers.get(new Random().nextInt(peers.size()));
					done.onDone();
				} catch (Exception e) {
					done.onError();
				}
			}
		}).start();
	}

	public void createDrawer(Bundle savedinstance) {
		final IProfile profile = new ProfileDrawerItem().withName(address).withEmail(public_key)
				.withIcon(R.drawable.aro).withSelectedBackgroundAnimated(true);

		headerResult = new AccountHeaderBuilder().withActivity(this).withHeaderBackground(R.drawable.background)
				.withTranslucentStatusBar(false)
				.addProfiles(profile,
						new ProfileSettingDrawerItem().withName("Logout").withIcon(GoogleMaterial.Icon.gmd_settings)
								.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
									@Override
									public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
										new MaterialDialog.Builder(HomeView.this)
												.title("Are you sure you want to logout?").cancelable(false)
												.negativeText("No").positiveText("Yes")
												.onPositive(new MaterialDialog.SingleButtonCallback() {
													@Override
													public void onClick(@NonNull MaterialDialog dialog,
															@NonNull DialogAction which) {
														dialog.dismiss();
														saveString("address", "");
														saveString("privatekey", "");
														saveString("publickey", "");
														Intent i = new Intent(HomeView.this, MainActivity.class);
														HomeView.this.startActivity(i);
													}
												}).onNegative(new MaterialDialog.SingleButtonCallback() {
													@Override
													public void onClick(@NonNull MaterialDialog dialog,
															@NonNull DialogAction which) {
														dialog.dismiss();
													}
												}).show();
										return false;
									}
								}))
				.withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
					@Override
					public boolean onProfileChanged(View view, IProfile profile, boolean current) {
						if (profile instanceof IDrawerItem
								&& ((IDrawerItem) profile).getIdentifier() == PROFILE_SETTING) {
							IProfile newProfile = new ProfileDrawerItem().withNameShown(true).withName("Batman")
									.withEmail("batman@gmail.com").withIcon(getResources().getDrawable(R.drawable.aro));
							if (headerResult.getProfiles() != null) {
								headerResult.addProfile(newProfile, headerResult.getProfiles().size() - 2);
							} else {
								headerResult.addProfiles(newProfile);
							}
						}

						return false;
					}
				}).withSavedInstance(savedinstance).build();

		result = new DrawerBuilder().withActivity(this).withTranslucentStatusBar(true).withAccountHeader(headerResult)
				.addDrawerItems(
						new PrimaryDrawerItem().withName("Balance").withIcon(GoogleMaterial.Icon.gmd_money_box)
								.withIdentifier(1).withSetSelected(true),
						new PrimaryDrawerItem().withName("Send").withIcon(Ionicons.Icon.ion_paper_airplane)
								.withIdentifier(2),
						new PrimaryDrawerItem().withName("Receive").withIcon(Ionicons.Icon.ion_ios_barcode)
								.withIdentifier(3),
						new PrimaryDrawerItem().withName("History").withIcon(FontAwesome.Icon.faw_hourglass_start)
								.withIdentifier(4))
				/*
				 * new SectionDrawerItem().withName("Settings"), new
				 * SecondaryDrawerItem().withName("Settings").withIcon(
				 * GoogleMaterial.Icon.gmd_settings).withIdentifier(5))
				 */
				.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
					@Override
					public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
						if (drawerItem instanceof Nameable) {
							showPage(((Nameable) drawerItem).getName().getText(HomeView.this));
						}
						return false;
					}
				}).withGenerateMiniDrawer(true).withSavedInstance(savedinstance).buildView();

		miniResult = result.getMiniDrawer();

		int firstWidth = (int) UIUtils.convertDpToPixel(300, this);
		int secondWidth = (int) UIUtils.convertDpToPixel(72, this);

		crossFader = new Crossfader().withContent(findViewById(R.id.crossview))
				.withFirst(result.getSlider(), firstWidth).withSecond(miniResult.build(this), secondWidth)
				.withSavedInstance(savedinstance).build();

		miniResult.withCrossFader(new CrossfadeWrapper(crossFader));

		crossFader.getCrossFadeSlidingPaneLayout().setShadowResourceLeft(R.drawable.material_drawer_shadow_left);
	}

	private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
			if (drawerItem instanceof Nameable) {
				Log.i("material-drawer",
						"DrawerItem: " + ((Nameable) drawerItem).getName() + " - toggleChecked: " + isChecked);
			} else {
				Log.i("material-drawer", "toggleChecked: " + isChecked);
			}
		}
	};

	public static String getCurrentPeer() {
		return currentPeer;
	}

	public class CustomList extends ArrayAdapter<String> {

		private final Activity context;
		private final ArrayList<String> strings;
		private final ArrayList<GoogleMaterial.Icon> imageId;

		private IconicsDrawable drawablepositive;
		private IconicsDrawable drawablenegative;

		public CustomList(Activity context, ArrayList<String> strings, ArrayList<GoogleMaterial.Icon> imageId) {
			super(context, R.layout.list_single, strings);
			this.context = context;
			this.strings = strings;
			this.imageId = imageId;
			if (drawablepositive == null) {
				drawablepositive = new IconicsDrawable(HomeView.this).icon(GoogleMaterial.Icon.gmd_long_arrow_down)
						.color(getColor(R.color.colorGreen)).sizeDp(24);
			}
			if (drawablenegative == null) {
				drawablenegative = new IconicsDrawable(HomeView.this).icon(GoogleMaterial.Icon.gmd_long_arrow_up)
						.color(getColor(R.color.colorRed)).sizeDp(24);
			}
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			LayoutInflater inflater = context.getLayoutInflater();
			View rowView = inflater.inflate(R.layout.list_single, null, true);
			TextView txtTitle = (TextView) rowView.findViewById(R.id.txt);
			TextView value = (TextView) rowView.findViewById(R.id.value);
			TextView from = (TextView) rowView.findViewById(R.id.from);
			TextView to = (TextView) rowView.findViewById(R.id.to);
			TextView date = (TextView) rowView.findViewById(R.id.date);
			ImageView imageView = (ImageView) rowView.findViewById(R.id.img);
			txtTitle.setText("ID: " + strings.get(position).split(",")[0]);
			value.setText(strings.get(position).split(",")[1] + " ARO");

			if (imageId.get(position) == GoogleMaterial.Icon.gmd_long_arrow_down)
				value.setTextColor(getColor(R.color.colorGreen));
			else
				value.setTextColor(getColor(R.color.colorRed));

			from.setText("<- " + strings.get(position).split(",")[2]);
			to.setText("-> " + strings.get(position).split(",")[3]);
			long t = Long.parseLong(strings.get(position).split(",")[4]) * 1000;
			Calendar dated = Calendar.getInstance();
			SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
			dated.setTimeInMillis(t);
			String date1 = format1.format(dated.getTime());
			date.setText(date1);

			if (imageId.get(position) == GoogleMaterial.Icon.gmd_long_arrow_down) {

				imageView.setImageDrawable(drawablepositive);
			} else {

				imageView.setImageDrawable(drawablenegative);
			}
			return rowView;
		}
	}

	public void checkIfLastTransactionIsSame(final LastTransactionTimer timer) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// GETTRANSACTIONS
				ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
					@Override
					public void onFeedback(JSONObject object) {
						try {
							JSONArray array = object.getJSONArray("data");
							String id = ((JSONObject) array.get(0)).get("id").toString();
							if (getString("lastID").equalsIgnoreCase(id))
								timer.onSame(id);
							else
								timer.onDifferect(id);
						} catch (Exception e) {
						}
					}
				}, "getTransactions", new ApiRequest.Argument("public_key", HomeView.this.public_key),
						new ApiRequest.Argument("account", HomeView.this.address),
						new ApiRequest.Argument("limit", "1"));
			}
		}).start();
	}
	private int version = 0;
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

	public String getString(String key) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String value = sharedPref.getString(key, "");
		return value;
	}

	boolean upToDate = false;
	
	public void setupQR() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					URL u = new URL("http://cubedpixels.net/qr/generateQRcode.php?"+version);
					Scanner s = new Scanner(u.openConnection().getInputStream());
					while (s.hasNext()) {
						String d = s.next();
						if (d.contains("UPDATE")) {
							String packageURL = "";
							upToDate = true;
							packageURL = d.replace("UPDATE", "");
							try {
								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageURL)));
							} catch (android.content.ActivityNotFoundException anfe) {
								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageURL)));
							}
						}
					}
					s.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();

	}

	public static abstract class Page {
		private String name;
		private RelativeLayout layout;

		public Page(String name, RelativeLayout layout) {
			this.name = name;
			this.layout = layout;
		}

		public String getName() {
			return name;
		}

		public RelativeLayout getLayout() {
			return layout;
		}

		public abstract void onEnable();

		public void onDisable() {
		};
	}

	public abstract class LastTransactionTimer {
		public abstract void onSame(String id);

		public abstract void onDifferect(String id);
	}

	public abstract class Call {
		public abstract void onDone(JSONObject o);
	}
}
