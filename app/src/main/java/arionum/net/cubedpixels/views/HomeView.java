package arionum.net.cubedpixels.views;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
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
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

import net.glxn.qrgen.android.QRCode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import arionum.net.cubedpixels.MainActivity;
import arionum.net.cubedpixels.R;
import arionum.net.cubedpixels.api.ApiRequest;
import arionum.net.cubedpixels.miner.ArionumMiner;
import arionum.net.cubedpixels.utils.Base58;
import arionum.net.cubedpixels.utils.CrossfadeWrapper;
import arionum.net.cubedpixels.utils.DoneTask;
import arionum.net.cubedpixels.utils.Easing;
import es.dmoral.toasty.Toasty;
import mehdi.sakout.fancybuttons.FancyButton;

import static android.view.View.GONE;

public class HomeView extends AppCompatActivity implements ComponentCallbacks2 {

    // TODO -> INSTANCE FOR EXTERNAL CLASSES
    public static HomeView instance;

    // TODO -> PEERS AND RELATED
    private static String currentPeer1 = "";
    private static String arionum_peer = "https://wallet.arionum.com";

    // TODO -> SAVED VARS
    private static String alias = "";
    private static String address = "";
    private static String public_key = "";
    private static String private_key = "";

    // TODO -> TRANSACTION RELATED VARS
    private static String signature = "";
    private static String unixTime = "";
    private static String val = "";
    private static String fee = "";
    private static String message = "";

    // TODO -> LISTS
    private static ArrayList<Page> pages = new ArrayList<>();
    private static ArrayList<String> peers = new ArrayList<>();
    private static ArrayList<Double> hashTime = new ArrayList<>();

    // TODO -> MINING RELATED VARS
    private long bestRECORDEDdelay = Long.MAX_VALUE;

    // TODO -> UI RELATED VARS
    private Crossfader crossFader;
    private Page currentPage;
    private AccountHeader headerResult = null;
    private MiniDrawer miniResult = null;
    private Drawer result = null;

    // TODO -> UTIL VARS AND NOT CATEGORIZED
    private double temp = 0;
    private boolean refreshing = true;

    // TODO -> QR READER RELATED VARS
    private static final int PROFILE_SETTING = 1;
    private static QRCodeReaderView qrCodeReaderView;



    //////////////////////////////////////////
    // TODO -> INIT AND SETUP METHODS
    //////////////////////////////////////////


    // TODO -> THIS METHOD IS GETTING EXECUTED FROM THE MAINACTITY
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
                    currentPeer1 = peers.get(new Random().nextInt(peers.size()));
                    done.onDone();
                } catch (Exception e) {
                    done.onError();
                }
            }
        }).start();
    }

    // TODO -> ONCREATE GETS CALLED WHEN THE UI IS GETTING LOADED !!!!!!!!!
    // WARING !!!!!!!!!!! DONT USE HEAVY METHODS FOR FAST LOADING TIMES
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //TODO -> THEME PROCESSOR
        setTheme(((boolean) SettingsView.getSettingFromName("blacktheme").getValue()) ? R.style.DarkAppTheme : R.style.AppTheme);

        // TODO -> SET INSTANCE
        instance = this;
        checkPasswordEntry();
        registerValues();

        // TODO -> UI RELATED STUFF
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new IconicsLayoutInflater2(getDelegate()));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        initUI();
        initDesign(savedInstanceState);
    }

    public void registerValues() {
        public_key = getString("publickey");
        if (!getString("privatekey").isEmpty())
            try {
                String tempsave = getString("privatekey");
                try {
                    if (new String(Base58.decode(tempsave)).contains(" ")) throw new Exception();
                } catch (Exception e) {
                    saveString("privatekey", Base58.encode(tempsave.getBytes()));
                    new MaterialDialog.Builder(HomeView.this).title("Repaired!")
                            .content("Your private key seemed broken! So we repaired it.").show();
                }
                private_key = new String(Base58.decode(getString("privatekey")));
            } catch (Exception e) {
                new MaterialDialog.Builder(HomeView.this).title("D3C0D3 exception!")
                        .content("Your private key couldn't get decrypted!").show();
            }
        address = getString("address");
        // TODO -> ALIAS SEARCH AND SET
        setupAlias();
    }

    public void initUI() {
        if (peers.size() > 0)
            currentPeer1 = peers.get(new Random().nextInt(peers.size()));
        else
            currentPeer1 = "http://peer1.arionum.com";
        TextView test = findViewById(R.id.connected);
        test.setText(currentPeer1.replace("http://", ""));
        TextView address = findViewById(R.id.address);
        address.setText(HomeView.address);
        setupThankyouList();
        initQR();
    }

    public void setupThankyouList() {
        ArrayList<String> thanks = new ArrayList<>();
        thanks.add("AroDev for developing ARIONUM");
        thanks.add("Mercury80 for developing ARIONUM");
        thanks.add("ProgrammerDan for his awesome Miner");
        thanks.add("Mikepenz for the awesome libs!");
        thanks.add("Stormie Selling Fcapes -Mental Help");
        thanks.add("HashPi -Mental Help");

        TextView t = findViewById(R.id.thankstolist);
        String build = "";
        for (String sd : thanks)
            build += sd + "\n";
        t.setText(build);
    }

    public void initQR() {
        // TODO -> CREATE QR
        float[] hsv = new float[3];
        int color = ContextCompat.getColor(this, R.color.colorBackground);
        Color.colorToHSV(color, hsv);
        hsv[2] *= 1.2f;
        color = Color.HSVToColor(hsv);
        Bitmap myBitmap = QRCode.from("sendaro" + "|" + HomeView.address + "||").withSize(100, 100)
                .withColor(color, Color.parseColor("#00000000")).bitmap();
        myBitmap.setHasAlpha(true);
        ImageView myImage = findViewById(R.id.qrimage);
        myImage.setImageBitmap(myBitmap);
        myImage.setAlpha(150);
        initIcons();
    }

    public void initIcons() {
        // TODO-> ICONS


        //TODO -> REFRESH ICON
        ImageView sync = findViewById(R.id.refreshIcon);
        IconicsDrawable syncd = new IconicsDrawable(HomeView.this).icon(GoogleMaterial.Icon.gmd_refresh)
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

        //TODO -> ALIAS SET ICON
        ImageView setAlias = findViewById(R.id.setAlias);
        IconicsDrawable sadr = new IconicsDrawable(HomeView.this).icon(FontAwesome.Icon.faw_ticket_alt)
                .color(Color.WHITE).sizeDp(28);
        sadr.setAlpha(130);
        setAlias.setImageDrawable(sadr);
        setAlias.setClickable(true);
        setAlias.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO -> SET ALIAS IF ALIAS ISN'T SET
                if (alias.equals("none")) {
                    Toasty.error(HomeView.this, "You already own an Alias!", Toast.LENGTH_SHORT, true).show();
                    return;
                }

                new MaterialDialog.Builder(HomeView.this).title("Set Alias ")
                        .content("Enter an Alias (A-Z|0-9)(10 AROs needed + !!NOT REVERSIBLE!!)")
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .input("Alias", "", new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                // Do something
                            }
                        }).positiveText("Set").negativeText("Cancel")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull final MaterialDialog dialog, @NonNull DialogAction which) {
                                String str = dialog.getInputEditText().getText().toString();
                                str = str.toUpperCase();
                                if (str.length() < 4 || str.length() > 25) {
                                    Toasty.error(HomeView.this, "Invalid Alias! 4< && <25 && A-Z|0-9", Toast.LENGTH_SHORT,
                                            true).show();
                                }
                                makeTransaction(getAddress(), 0.00000001, str, "3", new Runnable() {
                                    @Override
                                    public void run() {
                                        dialog.dismiss();
                                    }
                                });

                            }
                        }).show();
            }
        });

        //TODO -> QR RECEIVE ICON
        ImageView closeQRrequest = findViewById(R.id.closeqrrequest);
        IconicsDrawable cqrrid = new IconicsDrawable(HomeView.this).icon(FontAwesome.Icon.faw_times)
                .color(ContextCompat.getColor(instance, R.color.md_black_1000)).sizeDp(28);
        cqrrid.setAlpha(200);
        closeQRrequest.setImageDrawable(cqrrid);
        closeQRrequest.setClickable(true);
        closeQRrequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.qrrequestview).setVisibility(GONE);
            }
        });
    }

    public void initDesign(Bundle savedInstanceState) {
        // TODO-> DESIGN
        createDrawer(savedInstanceState);
        setupPages();

        //TODO -> SCREENSAVER
        findViewById(R.id.ScreenSaver).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFullScreen(false, findViewById(R.id.ScreenSaver));
            }
        });



        // TODO -> ANIMATIONS
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, r.getDisplayMetrics());

        final AnimatorSet animatorSet = new AnimatorSet();
        ValueAnimator valueAnimatorX = ValueAnimator.ofFloat(size.y, px);
        valueAnimatorX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                View view = findViewById(R.id.balancelayout);
                view.getLayoutParams().height = (int) (double) (float) animation.getAnimatedValue();
                view.requestLayout();
            }
        });
        View view = findViewById(R.id.balancelayout);
        view.getLayoutParams().height = (int) (double) (float) size.y;
        view.requestLayout();

        Easing easing = new Easing(1500);
        valueAnimatorX.setEvaluator(easing);
        animatorSet.playTogether(valueAnimatorX);
        animatorSet.setDuration(1500);


        // TODO-> GET BALANCE
        ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
                                       @Override
                                       public void onFeedback(final JSONObject object) throws JSONException {
                                           final Object data = object.get("data");
                                           HomeView.instance.runOnUiThread(new Runnable() {
                                               @Override
                                               public void run() {
                                                   TextView test = findViewById(R.id.balancevalue);
                                                   test.setText(data.toString() + " ARO");
                                               }
                                           });
                                       }
        }, "getBalance", new ApiRequest.Argument("account", HomeView.address));

        // TODO-> GETTRANSACTIONS
        ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
                                       @Override
                                       public void onFeedback(final JSONObject object) throws JSONException {
                                           final JSONArray array = object.getJSONArray("data");
                                           new Thread(new Runnable() {
                                               @Override
                                               public void run() {
                                                   runOnUiThread(new Runnable() {
                                                       @Override
                                                       public void run() {
                                                           findViewById(R.id.waitingtransbar).setVisibility(GONE);
                                                       }
                                                   });
                                                   if (array.length() > 0) {
                                                       sortArrayAndPutInList(array,
                                                               (ListView) findViewById(R.id.transactionlist), new Runnable() {
                                                                   @Override
                                                                   public void run() {
                                                                       HomeView.instance.runOnUiThread(new Runnable() {
                                                                           @Override
                                                                           public void run() {
                                                                               animatorSet.start();
                                                                           }
                                                                       });
                                                                   }
                                                               });
                                                   }

                                               }
                                           }).start();

                                           refreshing = false;
                                       }
        }, "getTransactions", new ApiRequest.Argument("account", HomeView.address), new ApiRequest.Argument("limit", "10"));

        //TODO -> END OF CLASS
    }


    //////////////////////////////////////////
    // TODO -> SETUP PAGES
    //////////////////////////////////////////





    public void setupPages() {
        new ArionumMiner();

        // TODO -> SETUP ABOUT SCREEN
        pages.add(new Page("ABOUT", (RelativeLayout) findViewById(R.id.aboutview)) {
            @Override
            public void onEnable() {
            }
        });

        // TODO -> SETUP BALANCE SCREEN
        pages.add(new Page("BALANCE", (RelativeLayout) findViewById(R.id.balanceview)) {
            @Override
            public void onEnable() {
            }
        });

        // TODO -> SETUP VARS FOR MINER SCREEN
        final FancyButton b = findViewById(R.id.minerToggle);
        final EditText editPool = findViewById(R.id.pool);
        final EditText editHashers = findViewById(R.id.hashers);

        // TODO -> SETUP MINER SCREEN
        pages.add(new Page("MINER", (RelativeLayout) findViewById(R.id.minerview)) {
            @Override
            public void onEnable() {
                b.setText(ArionumMiner.isRunning() ? "Stop Miner" : "Start Miner");
                editPool.setEnabled(!ArionumMiner.isRunning());
                String saved_pool = getString("miner_pool");
                if (saved_pool.isEmpty())
                    saved_pool = "http://aro.cool";
                String pool = ArionumMiner.getInstance().getPool() == null ? saved_pool
                        : ArionumMiner.getInstance().getPool();
                editPool.setText(pool);
                System.gc();
                int max = Runtime.getRuntime().availableProcessors();
                ActivityManager activityManager = (ActivityManager) HomeView.instance
                        .getSystemService(ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);
                double availPercent = (((double) (memoryInfo.availMem / 0x100000L)) * 1.4
                        / (double) ((memoryInfo.totalMem / 0x100000L)));
                int MAXramThreads = (int) ((memoryInfo.totalMem / 0x100000L) / 512);
                if (MAXramThreads <= max) {
                    max = (int) (MAXramThreads * availPercent);
                }
                editHashers.setEnabled(!ArionumMiner.isRunning());
                editHashers.setText(max + "");
            }
        });
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO -> SETUP MINER
                b.setText(!ArionumMiner.isRunning() ? "Stop Miner" : "Start Miner");
                editPool.setEnabled(ArionumMiner.isRunning());
                editHashers.setEnabled(ArionumMiner.isRunning());
                if (!ArionumMiner.isRunning()) {
                    saveString("miner_pool", editPool.getText().toString());
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    if (((Switch) findViewById(R.id.enableScreensaver)).isChecked())
                        toggleFullScreen(true, findViewById(R.id.ScreenSaver));
                    ArionumMiner.getInstance().start(new ArionumMiner.ArionumMinerCallback() {
                                                         // TODO -> MINING VARS
                                                         double hashes = 0;
                                                         long start = System.currentTimeMillis();

                                                         @Override
                                                         public void onAccept(String hash) {
                                                             System.out.println("Share Accepted : " + hash);
                                                             makeNotification("Share accepted: " + hash);
                                                         }

                                                         @Override
                                                         public void onDLChange(final long dl, final String type) {
                                                             if (dl < bestRECORDEDdelay)
                                                                 bestRECORDEDdelay = dl;
                                                             // TODO -> RUN DL CHANGE UI UPDATES
                                                             runOnUiThread(new Runnable() {
                                                                 @Override
                                                                 public void run() {
                                                                     if (start + 1111 < System.currentTimeMillis()) {
                                                                         ArrayList<Double> savedHashes = new ArrayList<>();
                                                                         if (hashTime.size() >= 6) {
                                                                             savedHashes.clear();
                                                                             savedHashes.add(hashTime.get(1));
                                                                             savedHashes.add(hashTime.get(2));
                                                                             savedHashes.add(hashTime.get(3));
                                                                             savedHashes.add(hashTime.get(4));
                                                                             savedHashes.add(hashTime.get(5));
                                                                             hashTime.clear();
                                                                             hashTime.addAll(savedHashes);
                                                                         }
                                                                         hashTime.add(hashes);
                                                                         double currentHashRate = calculateAverage(hashTime);
                                                                         hashTime.set(hashTime.size() - 1, currentHashRate);

                                                                         updateHashGraph(currentHashRate, type);
                                                                         hashes = 0;
                                                                         start = System.currentTimeMillis();
                                                                     }
                                                                     hashes++;
                                                                     final TextView t = findViewById(R.id.currentDur);
                                                                     t.setText(dl + "");
                                                                 }
                                                             });
                                                         }

                                                         @Override
                                                         public void onFind(String hash) {
                                                             System.out.println("Block found with Share : " + hash);
                                                             makeNotification("Found Find: " + hash);
                                                         }

                                                         @Override
                                                         public void onReject(String hash) {
                                                             System.out.println("Rejected Share : " + hash);
                                                             makeNotification("Share rejected: " + hash);
                                                         }

                                                         @Override
                                                         public void onShare(String hash) {
                                                             System.out.println("Found Share : " + hash);
                                                             makeNotification("Share found: " + hash);
                                                         }

                                                     }, editPool.getText().toString(), Integer.parseInt(editHashers.getText().toString()),
                            HomeView.this);

                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    ArionumMiner.getInstance().stop();
                }
            }
        });

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

        final TextView aliasinfo = findViewById(R.id.aliasinfo);
        aliasinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Arionum-Alias", alias);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(instance, "Alias copied to Clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        final TextView publicKey = findViewById(R.id.yourpublickey);
        publicKey.setText(getPublic_key());
        publicKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Arionum-Public-Key", publicKey.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(instance, "PublicKey copied to Clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        final TextView privatekey = findViewById(R.id.yourprivatekey);
        privatekey.setText("*CLICK TO SHOW*");
        privatekey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (privatekey.getText().toString().contains("CLICK TO"))
                    PasswordView.makePasswordPromt(HomeView.this, new PasswordView.PasswordCallback() {
                        @Override
                        public void verification_done(boolean accepted) {
                            if (accepted)
                                privatekey.setText(getPrivate_key());
                        }
                    });
                if (privatekey.getText().toString().contains("CLICK TO"))
                    return;
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Arionum-Private-Key", privatekey.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(instance, "PrivateKey copied to Clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        final RelativeLayout donations = findViewById(R.id.settings);
        donations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(HomeView.this, SettingsView.class);
                HomeView.this.startActivity(i);
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
                                        .from("sendaro" + "|" + address + "|" + doubleVal(amount).replace(",", ".")
                                                + "|")
                                        .withSize(600, 600).withColor(Color.BLACK, Color.parseColor("#00000000"))
                                        .bitmap();
                                qrimage.setImageBitmap(myBitmap);
                                findViewById(R.id.qrrequestview).setVisibility(View.VISIBLE);
                            }
                        }).show();
            }
        });

        // TODO -> SETUP SEND SCREEN
        pages.add(new Page("SEND", (RelativeLayout) findViewById(R.id.send)) {
            @Override
            public void onEnable() {

            }
        });
        final EditText amountedit = findViewById(R.id.amountto);
        final TextView fee = findViewById(R.id.fee);
        amountedit.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    String t = editable.toString();
                    float d = Float.parseFloat(t + "F");
                    double a = d * 0.0025;
                    if (a > 10)
                        a = 10.0;
                    fee.setText("Fee: " + doubleVal(a).replace(",", ".") + " ARO");
                } catch (Exception e) {
                    fee.setText("Fee: 0.000 ARO");
                }
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });
        FancyButton b1 = findViewById(R.id.sendbutton);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                PasswordView.makePasswordPromt(HomeView.this, new PasswordView.PasswordCallback() {
                    @Override
                    public void verification_done(boolean accepted) {
                        if (accepted) {
                            try {
                                final Double amount = Double.parseDouble(amountedit.getText().toString());
                                final String address = ((EditText) findViewById(R.id.addressto)).getText().toString();
                                final String message = ((EditText) findViewById(R.id.messageedit)).getText().toString();
                                DecimalFormat format = new DecimalFormat("0.########");
                                String vals = format.format(amount);
                                if (!vals.contains(","))
                                    vals += ",0";
                                while (vals.split(",")[1].length() < 8)
                                    vals += "0";
                                vals = vals.replace(",", ".");
                                new MaterialDialog.Builder(HomeView.this).title("Transaction")
                                        .content("Are you sure you want to send " + doubleVal(amount).replace(",", ".")
                                                + " ARO " + "\n to: " + address)
                                        .positiveText("Yes").negativeText("No").autoDismiss(false)
                                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog,
                                                                @NonNull DialogAction which) {
                                                dialog.dismiss();
                                                final MaterialDialog d = new MaterialDialog.Builder(HomeView.this)
                                                        .title("Sending").progress(true, 100)
                                                        .progressIndeterminateStyle(true).cancelable(false).show();
                                                String version = "1";
                                                if (address.length() < 26)
                                                    version = "2";
                                                // TODO REQUEST SEND
                                                makeTransaction(address, amount.doubleValue(), message, version,
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                d.dismiss();
                                                            }
                                                        });

                                            }
                                        }).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });

        // TODO -> SETUP RECEIVE SCREEN
        pages.add(new Page("RECEIVE", (RelativeLayout) findViewById(R.id.receiveview)) {
            @Override
            public void onDisable() {
                qrCodeReaderView.stopCamera();
                qrCodeReaderView.setQRDecodingEnabled(false);
            }

            @Override
            public void onEnable() {
                qrCodeReaderView = findViewById(R.id.receivescanner);
                final QRCodeReaderView.OnQRCodeReadListener listener = createQRlistener();
                qrCodeReaderView.setQRDecodingEnabled(false);
                qrCodeReaderView.setOnQRCodeReadListener(listener);
                qrCodeReaderView.setAutofocusInterval(1000L);
                qrCodeReaderView.setBackCamera();
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
        });

        // TODO -> SETUP HISTORY SCREEN
        pages.add(new Page("HISTORY", (RelativeLayout) findViewById(R.id.historyview)) {
            @Override
            public void onEnable() {
                Handler h = new Handler(instance.getMainLooper());
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                    }
                });
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String transactions = getString("transactions");
                            final JSONObject p = new JSONObject(transactions);
                            sortArrayAndPutInList(p.getJSONArray("data"),
                                    (ListView) findViewById(R.id.historylisttransactions), new Runnable() {
                                        @Override
                                        public void run() {
                                        }
                                    });
                        } catch (Exception e) {
                        }

                        checkIfLastTransactionIsSame(new LastTransactionTimer() {
                            @Override
                            public void onDifferect(String id) {
                                System.out.println("DIFFERENT");
                                downloadTransactions(new Call() {
                                    @Override
                                    public void onDone(JSONObject o) {
                                        try {
                                            ListView l = findViewById(R.id.historylisttransactions);
                                            sortArrayAndPutInList(o.getJSONArray("data"),
                                                    (ListView) findViewById(R.id.historylisttransactions), new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Handler h = new Handler(instance.getMainLooper());
                                                            h.post(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    findViewById(R.id.progressBar).setVisibility(GONE);
                                                                }
                                                            });

                                                        }
                                                    });
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onSame(String id) {
                                System.out.println("SAME");
                                try {
                                    String transactions = getString("transactions");
                                    if (transactions.isEmpty()) {
                                        System.out.println("transactions empty");
                                        downloadTransactions(new Call() {
                                            @Override
                                            public void onDone(JSONObject o) {
                                                try {
                                                    ListView l = findViewById(R.id.historylisttransactions);
                                                    sortArrayAndPutInList(o.getJSONArray("data"),
                                                            (ListView) findViewById(R.id.historylisttransactions), new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    Handler h = new Handler(instance.getMainLooper());
                                                                    h.post(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            findViewById(R.id.progressBar).setVisibility(GONE);
                                                                        }
                                                                    });
                                                                }
                                                            });

                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                        return;
                                    }

                                    final JSONObject p = new JSONObject(transactions);
                                    JSONArray a = p.getJSONArray("data");
                                    if (a.length() < 11) {
                                        System.out.println("length to short");
                                        downloadTransactions(new Call() {
                                            @Override
                                            public void onDone(final JSONObject o) {
                                                try {
                                                    sortArrayAndPutInList(o.getJSONArray("data"),
                                                            (ListView) findViewById(R.id.historylisttransactions), new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    Handler h = new Handler(instance.getMainLooper());
                                                                    h.post(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            findViewById(R.id.progressBar).setVisibility(GONE);
                                                                        }
                                                                    });
                                                                }
                                                            });

                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                    }
                                    Handler h = new Handler(instance.getMainLooper());
                                    h.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            findViewById(R.id.progressBar).setVisibility(GONE);
                                        }
                                    });

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }).start();

            }
        });
    }


    //////////////////////////////////////////
    // TODO -> UI RELATED METHODS
    //////////////////////////////////////////

    boolean sspositiveforward = false;
    boolean sspositiveside = false;
    public void updateHashGraph(final double hashrate, final String type) {
        Handler h = new Handler(HomeView.this.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                try {
                    DecimalFormat df = new DecimalFormat("#.00");
                    double d = hashrate;
                    String s = df.format(d);
                    if (s.startsWith(","))
                        s = "0" + s;
                    if (s.startsWith("."))
                        s = "0" + s;

                    String g = df.format(ArionumMiner.getLastHashrate());
                    if (g.startsWith(","))
                        g = "0" + g;
                    if (g.startsWith("."))
                        g = "0" + g;

                    if (((Switch) findViewById(R.id.enableScreensaver)).isChecked()) {
                        String text = ((TextView) findViewById(R.id.screensavertext)).getText().toString();
                        String text_below = "";
                        try {
                            if (text.contains("\n")) text_below = text.split("\n")[1];
                        } catch (Exception e) {

                        }
                        ((TextView) findViewById(R.id.screensavertext))
                                .setText("Miner is running... " + g + "H/s\n" + text_below + "\n" + "(Tap 3 Times to disable SS)");

                        Display display = getWindowManager().getDefaultDisplay();
                        Point size = new Point();
                        display.getSize(size);
                        int width = size.x;
                        int height = size.y;

                        TextView t = findViewById(R.id.screensavertext);

                        int currentY = (int) t.getY();
                        int currentX = (int) t.getX();

                        int twidth = t.getWidth();
                        int theight = t.getHeight();

                        if (currentX + twidth > width)
                            sspositiveside = false;
                        if (currentY + theight > height)
                            sspositiveforward = false;
                        if (currentX < 1)
                            sspositiveside = true;
                        if (currentY < 1)
                            sspositiveforward = true;

                        int speed = 50;

                        if (sspositiveforward)
                            currentY += speed;
                        else
                            currentY -= speed;

                        if (sspositiveside)
                            currentX += speed;
                        else
                            currentX -= speed;

                        t.setY(currentY);
                        t.setX(currentX);

                        ArionumMiner.setLastHashrate(emulateHs(hashrate));
                        return;
                    }

                    if (!(type.equalsIgnoreCase("mn"))) {
                        ((TextView) findViewById(R.id.hashRate))
                                .setText(g + " H/s \n" + s + " H/nds \nBEST DL:" + bestRECORDEDdelay);
                        ArionumMiner.setLastHashrate(emulateHs(hashrate));
                        temp += ArionumMiner.getLastHashrate();
                    } else {
                        ((TextView) findViewById(R.id.hashRate))
                                .setText(0.00 + " H/s \n" + 0.00 + " H/nds \nBEST DL:" + bestRECORDEDdelay);
                    }
                    String text = (ArionumMiner.getMinDL() + "\n" + ArionumMiner.getCurrentBlock() + "\n" + (int) temp);
                    if (!text.equals(((TextView) findViewById(R.id.limitVIEW)).getText())) {
                        findViewById(R.id.limitVIEW)
                                .startAnimation(AnimationUtils.loadAnimation(HomeView.this, android.R.anim.fade_in));
                        ((TextView) findViewById(R.id.limitVIEW)).setText(text);
                    }

                    GraphView graph = findViewById(R.id.graph);
                    if (graph.getSeries().size() <= 0) {
                        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(
                                new DataPoint[]{new DataPoint(0, 0)});
                        graph.addSeries(series);
                        LineGraphSeries<DataPoint> series1 = new LineGraphSeries<>(
                                new DataPoint[]{new DataPoint(0, 0)});
                        graph.addSeries(series1);
                        LineGraphSeries<DataPoint> series2 = new LineGraphSeries<>(
                                new DataPoint[]{new DataPoint(0, 0)});
                        graph.addSeries(series2);
                    } else if (!(type.equalsIgnoreCase("mn"))) {
                        boolean cpu = type.equalsIgnoreCase("cpu");

                        int series = cpu ? 0 : 1;
                        LineGraphSeries<DataPoint> series1 = (LineGraphSeries<DataPoint>) graph.getSeries().get(series);

                        series1.setAnimated(false);
                        series1.setThickness(3);

                        int color = cpu ? R.color.colorAccent : R.color.colorPink;

                        series1.setColor(ContextCompat.getColor(instance, color));

                        int series_backup = !cpu ? 0 : 1;
                        LineGraphSeries<DataPoint> backup = (LineGraphSeries<DataPoint>) graph.getSeries()
                                .get(series_backup);
                        backup.appendData(new DataPoint(backup.getHighestValueX() + 1, 0), false, Integer.MAX_VALUE,
                                false);

                        LineGraphSeries<DataPoint> masterNode = (LineGraphSeries<DataPoint>) graph.getSeries().get(2);
                        masterNode.appendData(new DataPoint(backup.getHighestValueX() + 1, 0), false, Integer.MAX_VALUE,
                                false);

                        graph.getSeries().clear();

                        series1.appendData(new DataPoint(series1.getHighestValueX() + 1, d), false, Integer.MAX_VALUE,
                                false);

                        graph.getViewport().setMinX(series1.getLowestValueX());
                        graph.getViewport().setMaxX(series1.getHighestValueX() + 2);
                        graph.getViewport().setMinY(series1.getLowestValueY());
                        graph.getViewport().setMaxY(bestYList(series1, masterNode, backup) + 2);

                        graph.getViewport().setYAxisBoundsManual(true);
                        graph.getViewport().setXAxisBoundsManual(true);

                        if (series_backup == 0)
                            graph.addSeries(backup);
                        else
                            graph.addSeries(series1);

                        if (series_backup == 0)
                            graph.addSeries(series1);
                        else
                            graph.addSeries(backup);

                        graph.addSeries(masterNode);
                    } else if ((type.equalsIgnoreCase("mn"))) {
                        System.out.println("MASTERNODE STATS");
                        // TODO ADD MASTERNODE STATS
                        LineGraphSeries<DataPoint> series1 = (LineGraphSeries<DataPoint>) graph.getSeries().get(2);
                        series1.setAnimated(false);
                        series1.setThickness(3);
                        series1.setColor(ContextCompat.getColor(instance, R.color.colorYellow));

                        LineGraphSeries<DataPoint> backup = (LineGraphSeries<DataPoint>) graph.getSeries().get(0);
                        backup.appendData(new DataPoint(backup.getHighestValueX() + 1, 0), false, Integer.MAX_VALUE,
                                false);
                        LineGraphSeries<DataPoint> backup1 = (LineGraphSeries<DataPoint>) graph.getSeries().get(1);
                        backup1.appendData(new DataPoint(backup1.getHighestValueX() + 1, 0), false, Integer.MAX_VALUE,
                                false);

                        graph.getSeries().clear();

                        series1.appendData(new DataPoint(series1.getHighestValueX() + 1, 3), false, Integer.MAX_VALUE,
                                false);

                        graph.getViewport().setMinX(series1.getLowestValueX());
                        graph.getViewport().setMaxX(series1.getHighestValueX() + 2);
                        graph.getViewport().setMinY(series1.getLowestValueY());
                        graph.getViewport().setMaxY(bestYList(backup, backup1, series1) + 2);

                        graph.getViewport().setYAxisBoundsManual(true);
                        graph.getViewport().setXAxisBoundsManual(true);

                        graph.addSeries(backup);
                        graph.addSeries(backup1);
                        graph.addSeries(series1);

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void sortArrayAndPutInList(JSONArray array, final ListView view, Runnable done) {
        try {

            final int y = view.getScrollY();
            final float yz = ViewHelper.getScrollY(view);

            int size = array.length();
            ArrayList<String> name = new ArrayList<String>();
            ArrayList<GoogleMaterial.Icon> icon = new ArrayList<GoogleMaterial.Icon>();
            for (int i = 0; i < size; i++) {
                JSONObject o = array.getJSONObject(i);
                name.add(o.get("id").toString() + "," + o.get("val").toString() + "," + o.get("src") + ","
                        + o.get("dst") + "," + o.get("date"));
                if (o.get("type").toString().equals("credit")) {
                    icon.add(GoogleMaterial.Icon.gmd_keyboard_arrow_down);
                } else if (o.get("type").toString().equals("debit")) {
                    icon.add(GoogleMaterial.Icon.gmd_keyboard_arrow_up);

                } else if (o.get("dst").toString().equals(alias)) {
                    icon.add(GoogleMaterial.Icon.gmd_keyboard_arrow_down);
                } else {
                    icon.add(GoogleMaterial.Icon.gmd_keyboard_arrow_up);
                }
            }
            List<String> list = new ArrayList<String>();

            final CustomList adapter = new CustomList(HomeView.this, name, icon, done);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.clearAnimation();
                    try {
                        for (int index = 0; index < view.getChildCount(); ++index) {
                            View child = view.getChildAt(index);
                            child.destroyDrawingCache();
                        }
                    } catch (Exception e) {
                    }

                    view.setAdapter(adapter);

                            for (int index = 0; index < view.getChildCount(); ++index) {
                                View child = view.getChildAt(index);
                                Animation animation = new TranslateAnimation(500, 0, 0, 0);
                                animation.setDuration(1000);
                                animation.setStartOffset(index * 100);
                                child.startAnimation(animation);
                            }
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Drawable convertToGrayscale(Drawable drawable) {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

        drawable.setColorFilter(filter);

        return drawable;
    }
    public void createDrawer(Bundle savedinstance) {
        final IProfile profile = new ProfileDrawerItem().withName(address).withEmail(public_key)
                .withIcon(R.drawable.ic_launcher_round).withSelectedBackgroundAnimated(true);
        if (((boolean) SettingsView.getSettingFromName("blacktheme").getValue()))
            profile.getIcon().setIcon(convertToGrayscale(ContextCompat.getDrawable(this, R.drawable.ic_launcher_round)));

        headerResult = new AccountHeaderBuilder().withActivity(this).withHeaderBackground(R.drawable.colormain)
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
                        if (profile instanceof IDrawerItem && profile.getIdentifier() == PROFILE_SETTING) {

                        }

                        return false;
                    }
                }).withSavedInstance(savedinstance).build();

        int color = ((boolean) SettingsView.getSettingFromName("blacktheme").getValue()) ? Color.parseColor("#ffffff") : ContextCompat.getColor(instance, R.color.material_drawer_primary_icon);

        result = new DrawerBuilder().withActivity(this).withTranslucentStatusBar(true).withAccountHeader(headerResult)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Balance").withIcon(FontAwesome.Icon.faw_money_check)
                                .withIdentifier(1).withSetSelected(true).withIconColor(color),
                        new PrimaryDrawerItem().withName("Send").withIcon(Ionicons.Icon.ion_paper_airplane)
                                .withIdentifier(2).withIconColor(color),
                        new PrimaryDrawerItem().withName("Receive").withIcon(FontAwesome.Icon.faw_qrcode)
                                .withIdentifier(3).withIconColor(color),
                        new PrimaryDrawerItem().withName("Miner").withIcon(FontAwesome.Icon.faw_terminal)
                                .withIdentifier(4).withIconColor(color),
                        new PrimaryDrawerItem().withName("History").withIcon(GoogleMaterial.Icon.gmd_hourglass_full)
                                .withIdentifier(5).withIconColor(color),
                        new PrimaryDrawerItem().withName("About").withIcon(FontAwesome.Icon.faw_book).withIdentifier(6).withIconColor(color))
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
                                            + "\nValue: " + doubleVal(val).replace(",", ".") + "\nMessage: " + message)
                                    .positiveText("Yes").negativeText("No")
                                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog,
                                                            @NonNull DialogAction which) {
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
                                    String version = "1";
                                    if (address.length() < 26)
                                        version = "2";
                                    makeTransaction(address, val.doubleValue(), "", version, new Runnable() {
                                        @Override
                                        public void run() {
                                            qrCodeReaderView = findViewById(R.id.receivescanner);
                                            qrCodeReaderView.startCamera();
                                            qrCodeReaderView.setQRDecodingEnabled(true);
                                        }
                                    });
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


    //////////////////////////////////////////
    // TODO -> STOCK ANDROID OVERRIDE METHODS
    //////////////////////////////////////////




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

    @Override
    public void onConfigurationChanged(Configuration configuration) {

        System.out.println("CONFIG: " + configuration);
    }

    @Override
    public void onLowMemory() {

        System.out.println("LOWMEMORY");

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (qrCodeReaderView != null)
            qrCodeReaderView.stopCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (qrCodeReaderView != null && currentPage.getName().equalsIgnoreCase("RECEIVE"))
            qrCodeReaderView.startCamera();
    }

    @Override
    public void onTrimMemory(int level) {

        switch (level) {

            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                break;

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
                System.out.println("TRIMMEMORY: MODERATE");
                System.gc();
                break;

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
                System.out.println("TRIMMEMORY: LOW");
                System.gc();
                break;

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                System.out.println("TRIMMEMORY: CRITICAL");
                System.gc();
                break;

            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
                break;

            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
                System.out.println("TRIMMEMORY: RUNNING MODERATE");
                System.gc();
                break;

            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                System.out.println("TRIMMEMORY: CRITICAL");
                System.gc();
                break;

            default:
                break;
        }
    }


    //////////////////////////////////////////
    // TODO -> TRANSACTION RELATED METHODS
    //////////////////////////////////////////


    public static void makeTransaction(final String addressTO, double value, String MSG, final String version,
                                       final Runnable run) {
        long UNIX = System.currentTimeMillis() / 1000;
        Base58.getSignature(addressTO, MSG, value, UNIX, version, new Base58.CallBackSigner() {
            @Override
            public void onDone(String signed1, String unix1, String val1, String fee1, String msg1) {
                signature = signed1;
                val = val1;
                fee = fee1;
                unixTime = unix1;
                message = msg1;

                System.out.println("SIGNING DONE VALUES: ");
                System.out.println(val);
                System.out.println(signature);
                System.out.println(fee);
                System.out.println(unixTime);
                System.out.println(message);

                ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
                                               @Override
                                               public void onFeedback(final JSONObject object) throws JSONException {
                                                   run.run();
                                                   final Object data = object.get("data");
                                                   //TODO -> HANDLE ERROR CHECK OF TRANSACTION
                                                   if (object == null || object.toString().contains("error")) {


                                                       //TODO -> DISPLAY DIALOG WITH ERROR AND TRY TO CATCH ERROR
                                                       HomeView.instance.runOnUiThread(new Runnable() {
                                                           @Override
                                                           public void run() {
                                                               new MaterialDialog.Builder(HomeView.instance).title("Error:")
                                                                       .content("Message: " + "\n" + data.toString())
                                                                           .cancelable(true).show();
                                                           }
                                                       });

                                                       //TODO -> HANDLE SUCCESS OF TRANSACTION
                                                   } else {
                                                       HomeView.instance.runOnUiThread(new Runnable() {
                                                           @Override
                                                           public void run() {
                                                               new MaterialDialog.Builder(HomeView.instance)
                                                                           .title("Transaction sent!")
                                                                       .content("Your transaction ID:" + "\n" + data.toString())
                                                                           .cancelable(true).show();
                                                           }
                                                       });

                                                   }

                                               }
                                           }, "send", new ApiRequest.Argument("val", val), new ApiRequest.Argument("dst", addressTO),
                        new ApiRequest.Argument("public_key", public_key),
                        new ApiRequest.Argument("signature", signature), new ApiRequest.Argument("date", unixTime),
                        new ApiRequest.Argument("message", message), new ApiRequest.Argument("version", version));

            }
        });
    }

    public void refreshLastTransactions() {
        refreshing = true;
        findViewById(R.id.waitingtransbar).setVisibility(View.VISIBLE);
        ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
                                       @Override
                                       public void onFeedback(JSONObject object) {
                                           try {

                                               TextView test = findViewById(R.id.balancevalue);
                                               test.setText(object.get("data").toString() + " ARO");
                                           } catch (Exception e) {

                                           }
                                       }
        }, "getBalance", new ApiRequest.Argument("account", address));
        ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {

                                       @Override
                                       public void onFeedback(JSONObject object) {
                                           runOnUiThread(new Runnable() {
                                                   @Override
                                                   public void run() {
                                                       findViewById(R.id.waitingtransbar).setVisibility(GONE);
                                                   }
                                               });
                                           refreshing = false;
                                       }
        }, "getTransactions", new ApiRequest.Argument("account", address), new ApiRequest.Argument("limit", "10"));
    }

    public void checkIfLastTransactionIsSame(final LastTransactionTimer timer) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //TODO ->  GET TRANSACTIONS AND CHECK IF THE SAVED "lastID" STRING IS THE SAME AS THE TRANSACTION ID
                ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
                                               @Override
                                               public void onFeedback(JSONObject object) throws JSONException {
                                                   if (object == null || !object.has("data") || object.getString("data").isEmpty())
                                                       return;
                                                       JSONArray array = object.getJSONArray("data");
                                                   if (array.length() <= 0)
                                                       return;
                                                       String id = ((JSONObject) array.get(0)).get("id").toString();
                                                       if (getString("lastID").equalsIgnoreCase(id))
                                                           timer.onSame(id);
                                                       else
                                                           timer.onDifferect(id);
                                               }
                }, "getTransactions", new ApiRequest.Argument("account", address), new ApiRequest.Argument("limit", "1"));
            }
        }).start();
    }

    public void downloadTransactions(final Call call) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //TODO -> GET TRANSACTIONS
                Handler h = new Handler(getMainLooper());
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                    }
                });
                ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {

                                               @Override
                                               public void onFeedback(JSONObject object) throws JSONException {
                                                   if (!object.has("data") || object.getString("data").isEmpty())
                                                       return;
                                                       saveString("lastID", object.getJSONArray("data").getJSONObject(0).get("id").toString());
                                                       saveString("transactions", object.toString());
                                                       call.onDone(object);
                                               }
                }, "getTransactions", new ApiRequest.Argument("account", getAddress()), new ApiRequest.Argument("limit", "1000"));
            }
        }).start();
    }


    //////////////////////////////////////////
    // TODO -> GETS AND UTIL METHODS
    //////////////////////////////////////////

    void toggleFullScreen(boolean goFullScreen, View v) {
        if (goFullScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            v.requestLayout();
            v.bringToFront();
            findViewById(R.id.crossview).setVisibility(GONE);
            v.setVisibility(View.VISIBLE);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().getDecorView().setSystemUiVisibility(0);
            v.setVisibility(View.GONE);
            findViewById(R.id.crossview).requestLayout();
            findViewById(R.id.crossview).bringToFront();
            findViewById(R.id.crossview).setVisibility(View.VISIBLE);
        }

    }

    public static String getPrivate_key() {
        return private_key;
    }

    public static String getPublic_key() {
        return public_key;
    }

    public void saveString(String key, String string) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, string);
        editor.commit();
    }

    private void checkPasswordEntry() {
        // TODO -> CHECK PASSWORD PROMT ON FIRST LOGIN
        PasswordView.PasswordCallback cp = new PasswordView.PasswordCallback() {
            @Override
            public void verification_done(boolean accepted) {
                if (!accepted)
                    PasswordView.makePasswordPromt(HomeView.this, this);
            }
        };
        if (!PasswordView.hasPassword())
            PasswordView.makePasswordPromt(this, cp);

    }

    public String getString(String key) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String value = sharedPref.getString(key, "");
        return value;
    }

    public double bestYList(LineGraphSeries<DataPoint>... lists) {
        double heighestY = 0;
        for (LineGraphSeries<DataPoint> l : lists) {
            if (l.getHighestValueY() > heighestY)
                heighestY = l.getHighestValueY();
        }
        return heighestY;
    }

    private double calculateAverage(ArrayList<Double> marks) {
        double sum = 0;
        if (!marks.isEmpty()) {
            for (Double mark : marks) {
                sum += mark;
            }
            return sum / marks.size();
        }
        return sum;
    }

    public double emulateHs(double n) {
        // IDK how dan his things work but this is a temporary solution
        return n * 1.4 * 1.3 * 1.12 * 0.99301 * 1.33;
    }

    public void makeNotification(final String contentsmall) {
        Handler h = new Handler(HomeView.this.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(HomeView.this, "ARONOTIFICATIONS")
                        .setSmallIcon(R.drawable.aro).setContentTitle("Arionum Wallet | Miner")
                        .setContentText(contentsmall)
                        .setColor(ContextCompat.getColor(HomeView.this, R.color.colorPrimary)).setColorized(true)
                        .setChannelId("notify_001").setPriority(NotificationCompat.PRIORITY_DEFAULT);

                NotificationManager mNotificationManager = (NotificationManager) HomeView.this
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel("notify_001", "Channel human readable title",
                            NotificationManager.IMPORTANCE_DEFAULT);
                    mNotificationManager.createNotificationChannel(channel);
                }
                mNotificationManager.notify(1047 + new Random().nextInt(1000), mBuilder.build());

                if (((Switch) findViewById(R.id.enableScreensaver)).isChecked()) {
                    TextView t = findViewById(R.id.screensavertext);
                    String text = "\nShares:";
                    String upperText = "";
                    int parsed = 0;
                    if (t.getText().toString().contains("\n")) {
                        try {
                            upperText = t.getText().toString().split("\n")[0];
                            parsed = Integer.parseInt(t.getText().toString().replace("Shares:", "").split("\n")[1]);
                        } catch (Exception e) {
                        }
                    }
                    parsed++;
                    text += parsed;


                    t.setText(upperText + text);
                    return;
                }
                TextView t = findViewById(R.id.shares);
                String text = t.getText().toString();
                int parsed = 0;
                try {
                    parsed = Integer.parseInt(text);
                } catch (Exception e) {
                }
                if (contentsmall.startsWith("Share found"))
                    t.setText((parsed + 1) + "");
            }
        });
    }

    public static String doubleVal(final double d) {
        int afterlength = getDecimals(d);
        String temp = "";
        for (int i = 0; i < afterlength; i++)
            temp += "#";
        DecimalFormat format = new DecimalFormat("0." + temp);

        return format.format(d);
    }

    public static String doubleVal(final Double d) {
        return d == null ? "" : doubleVal(d.doubleValue());
    }

    public static String getAddress() {
        return address;
    }

    public static String getCurrentPeer() {
        return arionum_peer;
    }

    public static int getDecimals(double d) {
        String[] splitt = (d + "").split("\\.");
        if (splitt.length <= 1)
            return 0;
        String after = splitt[1];
        while (after.lastIndexOf("0") == after.length())
            after = after.substring(0, after.length() - 1);
        if (after.length() > 10)
            return 10;
        return after.length();
    }

    private void setupAlias() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
                    @Override
                    public void onFeedback(JSONObject object) throws JSONException {
                        if (object == null)
                            return;
                            alias = object.getString("data");

                        if (alias == null)
                            alias = "none";
                        if (alias.equals("null"))
                            alias = "none";
                            if (alias.equals("false"))
                                alias = "none";
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) findViewById(R.id.aliasinfo)).setText("Your Alias: " + alias);
                            }
                        });
                    }
                }, "getAlias", new ApiRequest.Argument("account", getAddress()));
            }
        }).start();
    }

    public void showPage(String name) {
        for (Page p : pages) {
            if (p.getName().equalsIgnoreCase(name)) {
                currentPage = p;
                p.onEnable();

                Animation fadeIn = new AlphaAnimation(0, 1);
                fadeIn.setInterpolator(new DecelerateInterpolator());
                fadeIn.setDuration(200);
                p.getLayout().startAnimation(fadeIn);

                p.getLayout().setVisibility(View.VISIBLE);
            } else {
                if (p.getLayout().getVisibility() == View.VISIBLE) {

                    Animation fadeOut = new AlphaAnimation(1, 0);
                    fadeOut.setInterpolator(new AccelerateInterpolator());
                    fadeOut.setDuration(200);
                    p.getLayout().startAnimation(fadeOut);

                    p.onDisable();
                }
                p.getLayout().setVisibility(GONE);

            }

        }
    }

    public static int spToPx(float sp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    //////////////////////////////////////////
    // TODO -> CLASSES AND STATIC CLASS CALLS//
    //////////////////////////////////////////

    public static abstract class Page {
        private RelativeLayout layout;
        private String name;

        public Page(String name, RelativeLayout layout) {
            this.name = name;
            this.layout = layout;
        }

        public RelativeLayout getLayout() {
            return layout;
        }

        public String getName() {
            return name;
        }

        public void onDisable() {
        }

        public abstract void onEnable();
    }

    public abstract class Call {
        public abstract void onDone(JSONObject o);
    }


    public class CustomList extends ArrayAdapter<String> {

        private final Activity context;
        private final ArrayList<GoogleMaterial.Icon> imageId;
        private IconicsDrawable drawablenegative;
        private IconicsDrawable drawablepositive;
        private final ArrayList<String> strings;

        public CustomList(Activity context, final ArrayList<String> strings, ArrayList<GoogleMaterial.Icon> imageId, final Runnable done) {
            super(context, R.layout.list_single, strings);
            this.context = context;
            this.strings = strings;
            this.imageId = imageId;
            if (drawablepositive == null) {
                drawablepositive = new IconicsDrawable(HomeView.this).icon(GoogleMaterial.Icon.gmd_keyboard_arrow_down)
                        .color(Color.parseColor("#4cd964")).sizeDp(24);
            }
            if (drawablenegative == null) {
                drawablenegative = new IconicsDrawable(HomeView.this).icon(GoogleMaterial.Icon.gmd_keyboard_arrow_up)
                        .color(Color.parseColor("#ff3b32")).sizeDp(24);
            }
            views.clear();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < strings.size(); i++) {
                        generateView(i, new ListItemEvent() {
                            @Override
                            public void done(View view) {
                                pregeneratedViews.add(view);
                            }
                        });
                    }
                    while (pregeneratedViews.size() != strings.size()) {
                    }
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                    }
                    done.run();
                }
            }).start();

        }

        private ArrayList<View> pregeneratedViews = new ArrayList<>();
        private HashMap<ViewGroup, ArrayList<View>> views = new HashMap<>();

        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            if (position >= pregeneratedViews.size() || pregeneratedViews.isEmpty())
                return generateView(position);
            return pregeneratedViews.get(position);
        }

        public void generateView(final int position, final ListItemEvent runners) {
            LayoutInflater inflater = context.getLayoutInflater();
            final View rowView = inflater.inflate(R.layout.list_single, null, true);
            final TextView txtTitle = rowView.findViewById(R.id.txt);
            final TextView value = rowView.findViewById(R.id.value);
            final TextView from = rowView.findViewById(R.id.from);
            final TextView to = rowView.findViewById(R.id.to);
            final TextView date = rowView.findViewById(R.id.date);
            final ImageView imageView = rowView.findViewById(R.id.img);


            //TODO -> SURE

            long t = Long.parseLong(strings.get(position).split(",")[4]) * 1000;
            Calendar dated = Calendar.getInstance();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            dated.setTimeInMillis(t);
            final String date1 = format1.format(dated.getTime());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtTitle.setText("ID: " + strings.get(position).split(",")[0]);
                    value.setText(strings.get(position).split(",")[1] + " ARO");

                    from.setText("<- " + strings.get(position).split(",")[2]);
                    to.setText("-> " + strings.get(position).split(",")[3]);

                    date.setText(date1);

                    if (imageId.get(position) == GoogleMaterial.Icon.gmd_keyboard_arrow_down) {

                        imageView.setImageDrawable(drawablepositive);
                    } else {

                        imageView.setImageDrawable(drawablenegative);
                    }
                    if (imageId.get(position) == GoogleMaterial.Icon.gmd_keyboard_arrow_down)
                        value.setTextColor(ContextCompat.getColor(HomeView.instance, R.color.colorGreen));
                    else
                        value.setTextColor(ContextCompat.getColor(HomeView.instance, R.color.colorRed));


                    addClickListener(rowView, strings.get(position).split(",")[3], strings.get(position).split(",")[2], strings.get(position).split(",")[0], date1, strings.get(position).split(",")[1] + " ARO");

                    runners.done(rowView);
                }

            });
        }

        public View generateView(final int position) {
            LayoutInflater inflater = context.getLayoutInflater();
            final View rowView = inflater.inflate(R.layout.list_single, null, true);
            final TextView txtTitle = rowView.findViewById(R.id.txt);
            final TextView value = rowView.findViewById(R.id.value);
            final TextView from = rowView.findViewById(R.id.from);
            final TextView to = rowView.findViewById(R.id.to);
            final TextView date = rowView.findViewById(R.id.date);
            final ImageView imageView = rowView.findViewById(R.id.img);


            //TODO -> SURE

            long t = Long.parseLong(strings.get(position).split(",")[4]) * 1000;
            Calendar dated = Calendar.getInstance();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            dated.setTimeInMillis(t);
            final String date1 = format1.format(dated.getTime());

            addClickListener(rowView, strings.get(position).split(",")[3], strings.get(position).split(",")[2], strings.get(position).split(",")[0], date1, strings.get(position).split(",")[1] + " ARO");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtTitle.setText("ID: " + strings.get(position).split(",")[0]);
                    value.setText(strings.get(position).split(",")[1] + " ARO");

                    from.setText("<- " + strings.get(position).split(",")[2]);
                    to.setText("-> " + strings.get(position).split(",")[3]);

                    date.setText(date1);

                    if (imageId.get(position) == GoogleMaterial.Icon.gmd_keyboard_arrow_down) {

                        imageView.setImageDrawable(drawablepositive);

                    } else {

                        imageView.setImageDrawable(drawablenegative);
                    }
                    if (imageId.get(position) == GoogleMaterial.Icon.gmd_keyboard_arrow_down)
                        value.setTextColor(ContextCompat.getColor(HomeView.instance, R.color.colorGreen));
                    else
                        value.setTextColor(ContextCompat.getColor(HomeView.instance, R.color.colorRed));
                }

            });
            return rowView;
        }

        public void addClickListener(final View view, final String from, final String to, final String id, final String date, final String value) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new MaterialDialog.Builder(HomeView.instance).title("Transaction Information")
                            .content("ID: " + id + "\n\n" +
                                    "From: " + to + "\n\n" +
                                    "To: " + from + "\n\n" +
                                    "Value: " + value + "\n\n" +
                                    "Date: " + date + "\n"


                            ).positiveText("Close").show();
                }
            });
        }
    }

    public abstract class ListItemEvent {
        public abstract void done(View view);
    }

    public abstract class LastTransactionTimer {
        public abstract void onDifferect(String id);

        public abstract void onSame(String id);
    }
}