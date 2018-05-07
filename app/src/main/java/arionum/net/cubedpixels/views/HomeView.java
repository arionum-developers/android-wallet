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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
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
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;
import com.nineoldandroids.view.ViewHelper;
import com.programmerdan.arionum.arionum_miner.Miner;

import net.glxn.qrgen.android.QRCode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import arionum.net.cubedpixels.MainActivity;
import arionum.net.cubedpixels.R;
import arionum.net.cubedpixels.api.ApiRequest;
import arionum.net.cubedpixels.style.Styler;
import arionum.net.cubedpixels.utils.Base58;
import arionum.net.cubedpixels.utils.CrossfadeWrapper;
import arionum.net.cubedpixels.utils.DoneTask;
import mehdi.sakout.fancybuttons.FancyButton;

import static android.view.View.GONE;

public class HomeView extends AppCompatActivity implements ComponentCallbacks2 {

    //A REALLY LONG AND UNSTRUCTURED CLASS DONT BE MAD AT ME :C


    private static final int PROFILE_SETTING = 1;
    public static HomeView instance;
    public static Miner miner;
    static String signature = "";
    static String fee = "";
    static String val = "";
    static String unixTime = "";
    static String message = "";
    private static ArrayList<String> peers = new ArrayList<>();
    private static ArrayList<Page> pages = new ArrayList<>();
    private static String currentPeer = "";
    private static String public_key = "";
    private static String private_key = "";
    private static String address = "";
    private static QRCodeReaderView.OnQRCodeReadListener upcminglstnr;
    private static QRCodeReaderView qrCodeReaderView;
    private static Thread minerThread;
    boolean upToDate = false;
    private AccountHeader headerResult = null;
    private Drawer result = null;
    private MiniDrawer miniResult = null;
    private Crossfader crossFader;
    private boolean refreshing = true;
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
    private int version = 0;

    public static String getPublic_key() {
        return public_key;
    }

    public static String getAddress() {
        return address;
    }

    public static String getPrivate_key() {
        return private_key;
    }

    public static String doubleVal(final Double d) {
        return d == null ? "" : doubleVal(d.doubleValue());
    }

    public static String doubleVal(final double d) {
        int afterlength = getDecimals(d);
        String temp = "";
        for (int i = 0; i < afterlength; i++)
            temp += "#";
        DecimalFormat format = new DecimalFormat("0." + temp);

        return format.format(d);
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

    public static String getCurrentPeer() {
        return currentPeer;
    }

    public static void makeTransaction(final String addressTO, double value, String MSG, final Runnable run) {
        long UNIX = System.currentTimeMillis() / 1000;
        Base58.getSignature(addressTO, MSG, value, UNIX, new Base58.CallBackSigner() {
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
                                               public void onFeedback(final JSONObject object) {
                                                   run.run();
                                                   if (object == null || object.toString().contains("error")) {
                                                       Handler h = new Handler(HomeView.instance.getMainLooper());
                                                       h.post(new Runnable() {
                                                           @Override
                                                           public void run() {
                                                               try {
                                                                   MaterialDialog d = new MaterialDialog.Builder(HomeView.instance)
                                                                           .title("Error:").content("Message: " + "\n" +
                                                                                   object.get("data") + " <-> ")
                                                                           .cancelable(true).show();
                                                               } catch (Exception e) {
                                                                   e.printStackTrace();
                                                                   MaterialDialog d = new MaterialDialog.Builder(HomeView.instance)
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
                                                                   MaterialDialog d = new MaterialDialog.Builder(HomeView.instance)
                                                                           .title("Transaction sent!")
                                                                           .content("Your transaction ID:" + "\n" +
                                                                                   object.get("data").toString())
                                                                           .cancelable(true).show();
                                                               } catch (final Exception e) {
                                                                   e.printStackTrace();
                                                                   Handler h = new Handler(instance.getMainLooper());
                                                                   h.post(new Runnable() {
                                                                       @Override
                                                                       public void run() {
                                                                           MaterialDialog d = new MaterialDialog.Builder(
                                                                                   HomeView.instance).title("Error:")
                                                                                   .content("Debug: " + e.getMessage())
                                                                                   .cancelable(true).show();
                                                                       }
                                                                   });
                                                               }
                                                           }
                                                       });

                                                   }

                                               }
                                           }, "send", new ApiRequest.Argument("val", val),
                        new ApiRequest.Argument("dst", addressTO),
                        new ApiRequest.Argument("public_key", public_key),
                        new ApiRequest.Argument("signature", signature),
                        new ApiRequest.Argument("date", unixTime),
                        new ApiRequest.Argument("message", message), new ApiRequest.Argument("version", 1));


            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;


        public_key = getString("publickey");
        if (!getString("privatekey").isEmpty())
            try {
                private_key = new String(Base58.decode(getString("privatekey")));
            } catch (Exception e) {
                new MaterialDialog.Builder(HomeView.this).title("D3C0D3 exception!")
                        .content("Your private key couldn't be encrypted!").show();
            }
        address = getString("address");
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new IconicsLayoutInflater2(getDelegate()));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);


        // SETUP
        if (peers.size() > 0)
            currentPeer = peers.get(new Random().nextInt(peers.size()));
        else
            currentPeer = "http://peer1.arionum.com";
        TextView test = findViewById(R.id.connected);
        test.setText(currentPeer.replace("http://", ""));
        TextView address = findViewById(R.id.address);
        address.setText(HomeView.address);
        setupThankyouList();

        // QR
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
                                   }, "getBalance", new ApiRequest.Argument("public_key", public_key),
                new ApiRequest.Argument("account", HomeView.address));

        // GETTRANSACTIONS
        ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {
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
                                                       findViewById(R.id.waitingtransbar).setVisibility(GONE);
                                                   }
                                               });
                                           } catch (Exception e) {
                                               e.printStackTrace();
                                           }
                                           refreshing = false;
                                       }
                                   }, "getTransactions", new ApiRequest.Argument("public_key", public_key),
                new ApiRequest.Argument("account", HomeView.address), new ApiRequest.Argument("limit", "10"));

    }

    public void setupThankyouList() {
        ArrayList<String> thanks = new ArrayList<>();
        thanks.add("AroDev for developing ARIONUM");
        thanks.add("Mercury80 for developing ARIONUM");
        thanks.add("mikepenz for the awesome libs!");
        thanks.add("RehabbeR for being cool");
        thanks.add("ProgrammerDan for his awesome Miner");
        thanks.add("ario");
        thanks.add("hearonLP");
        thanks.add("Nikita_Banane");
        thanks.add("dlazaro66");
        thanks.add("and everyone else!");

        TextView t = findViewById(R.id.thankstolist);
        String build = "";
        for (String sd : thanks)
            build += sd + "\n";
        t.setText(build);
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
                                   }, "getBalance", new ApiRequest.Argument("public_key", public_key),
                new ApiRequest.Argument("account", address));
        ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {

                                       @Override
                                       public void onFeedback(JSONObject object) {
                                           try {
                                               sortArrayAndPutInList(object.getJSONArray("data"), (ListView) findViewById(R.id.transactionlist));
                                               Handler h = new Handler(instance.getMainLooper());
                                               h.post(new Runnable() {
                                                   @Override
                                                   public void run() {
                                                       findViewById(R.id.waitingtransbar).setVisibility(GONE);
                                                   }
                                               });
                                           } catch (Exception e) {
                                               e.printStackTrace();
                                           }
                                           refreshing = false;
                                       }
                                   }, "getTransactions", new ApiRequest.Argument("public_key", public_key),
                new ApiRequest.Argument("account", address), new ApiRequest.Argument("limit", "10"));
    }

    public void setupPages() {


        //SETUP ABOUT SCREEN
        pages.add(new Page("ABOUT", (RelativeLayout) findViewById(R.id.aboutview)) {
            @Override
            public void onEnable() {

            }
        });


        // SETUP BALANCE SCREEN
        pages.add(new Page("BALANCE", (RelativeLayout) findViewById(R.id.balanceview)) {
            @Override
            public void onEnable() {

            }
        });

        final FancyButton b = findViewById(R.id.minerToggle);
        final EditText editPool = findViewById(R.id.pool);
        final EditText editHashers = findViewById(R.id.hashers);
        pages.add(new Page("MINER", (RelativeLayout) findViewById(R.id.minerview)) {
            @Override
            public void onEnable() {
                boolean minerActive = true;
                if (minerThread == null || !minerThread.isAlive())
                    minerActive = false;
                b.setText(minerActive ? "Stop Miner" : "Start Miner");

                editPool.setEnabled(!minerActive);
                editPool.setText("http://aro.cool");
                System.gc();

                int max = Runtime.getRuntime().availableProcessors();

                ActivityManager activityManager = (ActivityManager) HomeView.instance.getSystemService(ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);

                double availPercent = (((double) (memoryInfo.availMem / 0x100000L)) * 1.4 / (double) ((memoryInfo.totalMem / 0x100000L)));


                int MAXramThreads = (int) ((memoryInfo.totalMem / 0x100000L) / 512);
                System.out.println(MAXramThreads + " | " + availPercent);

                if (MAXramThreads <= max) {
                    max = (int) (MAXramThreads * availPercent);
                }

                editHashers.setEnabled(!minerActive);
                editHashers.setText(max + "");
            }
        });
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean minerActive = true;
                if (minerThread == null || !minerThread.isAlive())
                    minerActive = false;

                //SETUP MINER

                b.setText(!minerActive ? "Stop Miner" : "Start Miner");
                editPool.setEnabled(minerActive);
                editHashers.setEnabled(minerActive);
                if (!minerActive) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    minerThread = new Thread(new Runnable() {
                        @Override
                        public void run() {

                            miner = Miner.main(new Miner.callbackMiner() {
                                @Override
                                public void onHashRate(final String hash, final String dur) {
                                    Handler h = new Handler(HomeView.this.getMainLooper());
                                    h.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                DecimalFormat df = new DecimalFormat("#.00");
                                                double d = Double.parseDouble(hash.replace(",", "."));

                                                String s = df.format(d);
                                                if (s.startsWith(","))
                                                    s = "0" + s;
                                                if (s.startsWith("."))
                                                    s = "0" + s;

                                                ((TextView) findViewById(R.id.hashRate)).setText(s + " H/s \nBEST DL:" + dur);
                                                ((TextView) findViewById(R.id.limitVIEW)).setText(Miner.limitDuration + "");

                                                GraphView graph = findViewById(R.id.graph);
                                                if (graph.getSeries().size() <= 0) {
                                                    LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[]{
                                                            new DataPoint(0, 0)
                                                    });
                                                    graph.addSeries(series);
                                                } else {
                                                    LineGraphSeries<DataPoint> series1 = (LineGraphSeries<DataPoint>) graph.getSeries().get(0);
                                                    series1.setAnimated(false);
                                                    series1.setThickness(3);
                                                    series1.setColor(ContextCompat.getColor(instance, R.color.colorAccent));
                                                    graph.getSeries().clear();
                                                    series1.appendData(new DataPoint(series1.getHighestValueX() + 1, d), false, Integer.MAX_VALUE, false);

                                                    graph.getViewport().setMinX(series1.getLowestValueX());
                                                    graph.getViewport().setMaxX(series1.getHighestValueX() + 2);
                                                    graph.getViewport().setMinY(series1.getLowestValueY());
                                                    graph.getViewport().setMaxY(series1.getHighestValueY() + 2);

                                                    graph.getViewport().setYAxisBoundsManual(true);
                                                    graph.getViewport().setXAxisBoundsManual(true);

                                                    graph.addSeries(series1);
                                                }


                                            } catch (Exception e) {

                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onShare(final String hash) {
                                    Handler h = new Handler(HomeView.this.getMainLooper());
                                    h.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            System.out.println("FOUND SHARE = " + hash);

                                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                                                    HomeView.this, "ARONOTIFICATIONS")
                                                    .setSmallIcon(R.drawable.aro)
                                                    .setContentTitle("Arionum Wallet | Miner")
                                                    .setContentText("Found a share")
                                                    .setChannelId("notify_001")
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);


                                            NotificationManager mNotificationManager =
                                                    (NotificationManager) HomeView.this.getSystemService(Context.NOTIFICATION_SERVICE);
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                NotificationChannel channel = new NotificationChannel("notify_001",
                                                        "Channel human readable title",
                                                        NotificationManager.IMPORTANCE_DEFAULT);
                                                mNotificationManager.createNotificationChannel(channel);
                                            }
                                            mNotificationManager.notify(1347, mBuilder.build());

                                            TextView t = findViewById(R.id.shares);
                                            String text = t.getText().toString();
                                            int parsed = 0;
                                            try {
                                                parsed = Integer.parseInt(text);
                                            } catch (Exception e) {
                                            }
                                            t.setText((parsed + 1) + "");
                                        }
                                    });
                                }

                                @Override
                                public void onReject(String hash) {
                                    System.out.println("FOUND REJECT = " + hash);


                                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                                            HomeView.this, "ARONOTIFICATIONS")
                                            .setSmallIcon(R.drawable.aro)
                                            .setContentTitle("Arionum Wallet | Miner")
                                            .setContentText("Share got rejected!")
                                            .setStyle(
                                                    new NotificationCompat.InboxStyle()
                                                            .addLine("Share got rejected")
                                                            .addLine(hash)
                                            )
                                            .setChannelId("notify_001")
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);


                                    NotificationManager mNotificationManager =
                                            (NotificationManager) HomeView.this.getSystemService(Context.NOTIFICATION_SERVICE);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        NotificationChannel channel = new NotificationChannel("notify_001",
                                                "Channel human readable title",
                                                NotificationManager.IMPORTANCE_DEFAULT);
                                        mNotificationManager.createNotificationChannel(channel);
                                    }
                                    mNotificationManager.notify(1357, mBuilder.build());
                                }

                                @Override
                                public void onAccept(String hash) {
                                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                                            HomeView.this, "ARONOTIFICATIONS")
                                            .setSmallIcon(R.drawable.aro)
                                            .setContentTitle("Arionum Wallet | Miner")
                                            .setContentText("Share got accepted!")
                                            .setStyle(
                                                    new NotificationCompat.InboxStyle()
                                                            .addLine("Share got accepted")
                                                            .addLine(hash)
                                            )
                                            .setChannelId("notify_001")
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);


                                    NotificationManager mNotificationManager =
                                            (NotificationManager) HomeView.this.getSystemService(Context.NOTIFICATION_SERVICE);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        NotificationChannel channel = new NotificationChannel("notify_001",
                                                "Channel human readable title",
                                                NotificationManager.IMPORTANCE_DEFAULT);
                                        mNotificationManager.createNotificationChannel(channel);
                                    }
                                    mNotificationManager.notify(1357, mBuilder.build());
                                }

                                @Override
                                public void onFind(String hash) {
                                    System.out.println("FOUND FIND = " + hash);

                                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                                            HomeView.this, "ARONOTIFICATIONS")
                                            .setSmallIcon(R.drawable.aro)
                                            .setContentTitle("Arionum Wallet | Miner")
                                            .setContentText("Share got accepted!")
                                            .setChannelId("notify_001")
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);


                                    NotificationManager mNotificationManager =
                                            (NotificationManager) HomeView.this.getSystemService(Context.NOTIFICATION_SERVICE);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        NotificationChannel channel = new NotificationChannel("notify_001",
                                                "Channel human readable title",
                                                NotificationManager.IMPORTANCE_DEFAULT);
                                        mNotificationManager.createNotificationChannel(channel);
                                    }
                                    mNotificationManager.notify(1357, mBuilder.build());
                                }

                                @Override
                                public void onDurChange(final String dur) {

                                    final TextView t = findViewById(R.id.currentDur);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            t.setText(dur);
                                            try {
                                                long l = Long.parseLong(dur);
                                                if (Miner.finalDuration >= l)
                                                    Miner.finalDuration = l;
                                            } catch (Exception e) {

                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onStop() {
                                    b.setText("Stop Miner");
                                    try {
                                        miner.stop();
                                        Method m = Thread.class.getDeclaredMethod("stop0", Object.class);
                                        m.setAccessible(true);
                                        m.invoke(minerThread, new ThreadDeath());
                                        minerThread.interrupt();
                                    } catch (Exception e) {
                                        minerThread.interrupt();
                                    }
                                }
                            }, editPool.getText().toString(), editHashers.getText().toString());
                        }
                    });
                    minerThread.start();
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    try {
                        Method m = Thread.class.getDeclaredMethod("stop0", Object.class);
                        m.setAccessible(true);
                        m.invoke(minerThread, new ThreadDeath());
                        minerThread.interrupt();
                    } catch (Exception e) {
                        minerThread.interrupt();
                    }
                }
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

        final RelativeLayout donations = findViewById(R.id.donations);
        donations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("DONATION-Address", "1gPvUp3kW6ARDU84b7g8YKssWhdfLadNqrKR81W8RKtYLupbJHimBtMSPkHGLXhFcynkABydovjiRUUCM3SZxCG");
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
                                        .from("sendaro" + "|" + address + "|" + doubleVal(amount).replace(",", ".") + "|")
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
                findViewById(R.id.qrrequestview).setVisibility(GONE);
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
                    float d = Float.parseFloat(t + "F");
                    double a = d * 0.0025;
                    if (a > 10)
                        a = 10.0;
                    fee.setText("Fee: " + doubleVal(a).replace(",", ".") + " ARO");
                } catch (Exception e) {
                    fee.setText("Fee: 0.000 ARO");
                }
            }
        });
        Button b1 = findViewById(R.id.sendbutton);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                            .content("Are you sure you want to send " + doubleVal(amount).replace(",", ".") + " ARO " + "\n to: " + address)
                            .cancelable(false).positiveText("Yes").negativeText("No").autoDismiss(false)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog.dismiss();
                                    final MaterialDialog d = new MaterialDialog.Builder(HomeView.this).title("Sending")
                                            .progress(true, 100).progressIndeterminateStyle(true).cancelable(false)
                                            .show();


                                    //TODO REQUEST SEND

                                    makeTransaction(address, amount.doubleValue(), message, new Runnable() {
                                        @Override
                                        public void run() {
                                            d.dismiss();
                                        }
                                    });


                                }
                            }).show();
                } catch (Exception e) {

                }
            }
        });
        qrCodeReaderView = findViewById(R.id.receivescanner);
        final QRCodeReaderView.OnQRCodeReadListener listener = createQRlistener();
        qrCodeReaderView.setQRDecodingEnabled(false);
        qrCodeReaderView.setOnQRCodeReadListener(listener);
        qrCodeReaderView.setAutofocusInterval(1000L);
        qrCodeReaderView.setBackCamera();

        // SETUP RECEIVE SCREEN
        pages.add(new Page("RECEIVE", (RelativeLayout) findViewById(R.id.receiveview)) {
            @Override
            public void onEnable() {
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
                            if (transactions.isEmpty()) {
                                downloadTransactions(new Call() {
                                    @Override
                                    public void onDone(JSONObject o) {
                                        try {
                                            ListView l = findViewById(R.id.historylisttransactions);
                                            sortArrayAndPutInList(o.getJSONArray("data"),
                                                    (ListView) findViewById(R.id.historylisttransactions));
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
                                return;
                            }


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
                                                    findViewById(R.id.progressBar).setVisibility(GONE);
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
                                                        findViewById(R.id.progressBar).setVisibility(GONE);
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
                                            findViewById(R.id.progressBar).setVisibility(GONE);
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
                                    ListView l = findViewById(R.id.historylisttransactions);
                                    sortArrayAndPutInList(o.getJSONArray("data"),
                                            (ListView) findViewById(R.id.historylisttransactions));
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
                });
            }
        });
    }

    public void sortArrayAndPutInList(JSONArray array, final ListView view) {
        try {

            final int y = view.getScrollY();
            final float yz = ViewHelper.getScrollY(view);

            int size = array.length();
            ArrayList<String> name = new ArrayList<String>();
            ArrayList<GoogleMaterial.Icon> icon = new ArrayList<GoogleMaterial.Icon>();
            for (int i = 0; i < size; i++) {
                JSONObject o = array.getJSONObject(i);
                name.add(o.get("id").toString() + "," + o.get("val").toString() + "," + o.get("src") + "," +
                        o.get("dst") + "," + o.get("date"));
                // type <-
                if (o.get("type").toString().equals("credit")) {
                    icon.add(GoogleMaterial.Icon.gmd_long_arrow_down);
                } else {
                    icon.add(GoogleMaterial.Icon.gmd_long_arrow_up);

                }
            }
            System.out.println(size + " | " + name.size());

            List<String> list = new ArrayList<String>();
            final ArrayAdapter emptyAdapter = new ArrayAdapter<String>(HomeView.this,
                    android.R.layout.simple_list_item_1,
                    list.toArray(new String[0]));

            final CustomList adapter = new CustomList(HomeView.this, name, icon);
            Handler h = new Handler(instance.getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {
                    view.clearChoices();
                    view.clearAnimation();
                    for (int index = 0; index < view.getChildCount(); ++index) {
                        View child = view.getChildAt(index);
                        child.setVisibility(GONE);
                    }


                    view.setAdapter(emptyAdapter);
                    view.setAdapter(adapter);
                    view.setScrollY(y);

                    view.post(new Runnable() {
                        @Override
                        public void run() {
                            for (int index = 0; index < view.getChildCount(); ++index) {
                                View child = view.getChildAt(index);
                                Animation animation = new TranslateAnimation(500, 0, 0, 0);
                                animation.setDuration(1000);
                                animation.setStartOffset(index * 100);
                                child.startAnimation(animation);
                                view.setScrollY(y);
                            }
                            view.post(new Runnable() {
                                @Override
                                public void run() {
                                    System.out.println("SCROLLING TO: " + y + " | " + yz);
                                    view.scrollTo(0, y);
                                }
                            });
                        }
                    });
                    view.setScrollY(y);
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
                Handler h = new Handler(getMainLooper());
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                    }
                });
                ApiRequest.requestFeedback(new ApiRequest.RequestFeedback() {

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
                                           }, "getTransactions", new ApiRequest.Argument("public_key", public_key),
                        new ApiRequest.Argument("account", address),
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
                                    .content("Do you want to accept the QR request?" + "\nAddress to: " + address +
                                            "\nValue: " + doubleVal(val).replace(",", ".") + "\nMessage: " + message)
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
                                    makeTransaction(address, val.doubleValue(), "", new Runnable() {
                                        @Override
                                        public void run() {
                                            qrCodeReaderView = findViewById(
                                                    R.id.receivescanner);
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

    public void showPage(String name) {
        for (Page p : pages) {
            if (p.getName().equalsIgnoreCase(name)) {
                p.onEnable();
                p.getLayout().setVisibility(View.VISIBLE);
            } else {
                if (p.getLayout().getVisibility() == View.VISIBLE)
                    p.onDisable();
                p.getLayout().setVisibility(GONE);
            }

        }
    }

    public void createDrawer(Bundle savedinstance) {
        final IProfile profile = new ProfileDrawerItem().withName(address).withEmail(public_key)
                .withIcon(R.drawable.ic_launcher_round).withSelectedBackgroundAnimated(true);

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
                        if (profile instanceof IDrawerItem &&
                                profile.getIdentifier() == PROFILE_SETTING) {

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
                        new PrimaryDrawerItem().withName("Miner").withIcon(FontAwesome.Icon.faw_terminal)
                                .withIdentifier(4),
                        new PrimaryDrawerItem().withName("History").withIcon(FontAwesome.Icon.faw_hourglass_start)
                                .withIdentifier(5),
                        new SecondaryDrawerItem().withName("About").withIcon(FontAwesome.Icon.faw_info)
                                .withIdentifier(6))
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
                                           }, "getTransactions", new ApiRequest.Argument("public_key", public_key),
                        new ApiRequest.Argument("account", address),
                        new ApiRequest.Argument("limit", "1"));
            }
        }).start();
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

    public String getString(String key) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String value = sharedPref.getString(key, "");
        return value;
    }

    @Override
    public void onTrimMemory(int level) {


        // Determine which lifecycle or system event was raised.
        switch (level) {

            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:

                /*
                   Release any UI objects that currently hold memory.

                   The user interface has moved to the background.
                */

                break;

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
                System.out.println("TRIMMEMORY: MODERATE");
                System.gc();
                Miner.setSleep(System.currentTimeMillis());
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
                System.out.println("TRIMMEMORY: LOW");
                System.gc();
                Miner.setSleep(System.currentTimeMillis());
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                System.out.println("TRIMMEMORY: CRITICAL");
                Miner.setSleep(System.currentTimeMillis());
                System.gc();

                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */

                break;

            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                System.out.println("TRIMMEMORY: CRITICAL");
                Miner.setSleep(System.currentTimeMillis());
                System.gc();
                /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */

                break;

            default:
                /*
                  Release any non-critical data structures.

                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {

        System.out.println("CONFIG: " + configuration);
    }

    @Override
    public void onLowMemory() {

        System.out.println("LOWMEMORY");

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
        }
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
                        .color(ContextCompat.getColor(HomeView.instance, R.color.colorGreen)).sizeDp(24);
            }
            if (drawablenegative == null) {
                drawablenegative = new IconicsDrawable(HomeView.this).icon(GoogleMaterial.Icon.gmd_long_arrow_up)
                        .color(ContextCompat.getColor(HomeView.instance, R.color.colorRed)).sizeDp(24);
            }
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            LayoutInflater inflater = context.getLayoutInflater();
            View rowView = inflater.inflate(R.layout.list_single, null, true);
            TextView txtTitle = rowView.findViewById(R.id.txt);
            TextView value = rowView.findViewById(R.id.value);
            TextView from = rowView.findViewById(R.id.from);
            TextView to = rowView.findViewById(R.id.to);
            TextView date = rowView.findViewById(R.id.date);
            ImageView imageView = rowView.findViewById(R.id.img);
            txtTitle.setText("ID: " + strings.get(position).split(",")[0]);
            value.setText(strings.get(position).split(",")[1] + " ARO");

            if (imageId.get(position) == GoogleMaterial.Icon.gmd_long_arrow_down)
                value.setTextColor(ContextCompat.getColor(HomeView.instance, R.color.colorGreen));
            else
                value.setTextColor(ContextCompat.getColor(HomeView.instance, R.color.colorRed));

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

    public abstract class LastTransactionTimer {
        public abstract void onSame(String id);

        public abstract void onDifferect(String id);
    }

    public abstract class Call {
        public abstract void onDone(JSONObject o);
    }
}