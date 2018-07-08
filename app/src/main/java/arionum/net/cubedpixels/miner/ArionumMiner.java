package arionum.net.cubedpixels.miner;

import android.os.Handler;
import android.provider.Settings;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Scanner;

import arionum.net.cubedpixels.MainActivity;
import arionum.net.cubedpixels.views.HomeView;

public class ArionumMiner {

    private static boolean running = false;
    private static int overallHashes;
    private static int bestDL;
    private static double lastHashrate = 0;
    private static ArionumMiner instance;
    public String pool;
    public String minerName;
    public String publicKey;
    public String address;
    public int threads = 0;
    private ArionumMinerCallback callback;


    //TODO -> HASHERS AND LISTS
    private ArrayList<ArionumHasher> hashers = new ArrayList<>();
    private ArrayList<Thread> threadCollection = new ArrayList<>();

    public ArionumMiner() {
        instance = this;
    }

    public static void makeStormieHappy() {
        //TODO -> GET SOMETHING TO MAKE STORMIE HAPPY
    }

    public static int getBestDL() {
        return bestDL;
    }

    public static int getOverallHashes() {
        return overallHashes;
    }

    public static boolean isRunning() {
        return running;
    }

    public static ArionumMiner getInstance() {
        return instance;
    }

    private int hasherClock = 0;

    public void startHashers() {
        for (int i = 0; i < threads; i++) {
            //TODO -> SETUP HASHER
            ArionumHasher hasher = new ArionumHasher();
            hashers.add(hasher);
        }
        updateHashers();
    }

    public static double getLastHashrate() {
        return lastHashrate;
    }

    public static void setLastHashrate(double lastHashrate) {
        ArionumMiner.lastHashrate = lastHashrate;
    }

    public void start(ArionumMinerCallback callback, String pool, int threads) {
        this.pool = pool;
        this.threads = threads;
        this.callback = callback;

        //TODO: -> USER DATA
        String deviceId = Settings.Secure.getString(MainActivity.getInstance().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        minerName = "Cuby's Android Miner " + deviceId.substring(0, 5);
        address = HomeView.getAddress();
        publicKey = address;

        System.out.println("\n" + "================================================" + "\n" +
                ("---------STARTING CUBY'S ANDROID MINER----------") + "\n" +
                ("--Miner Name: " + minerName) + "\n" +
                ("--Address: " + address) + "\n" +
                ("--Pool: " + pool) + "\n" +
                ("--Threads: " + threads) + "\n" +
                ("================================================"));
        running = true;
        makeStormieHappy();
        createUpdateThread();
    }

    public void submitShare(final String nonce, String argon, final long submitDL, final long difficulty, long height) {
        //TODO -> SEND URL REQUEST TO POOL
        System.out.println("Submitting Share...");

        try {

            URL url = new URL(getPool() + "?q=submitNonce");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            con.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(con.getOutputStream());

            StringBuilder data = new StringBuilder();

            data.append(URLEncoder.encode("argon", "UTF-8")).append("=")
                    .append(URLEncoder.encode(argon.substring(30), "UTF-8")).append("&");
            data.append(URLEncoder.encode("nonce", "UTF-8")).append("=")
                    .append(URLEncoder.encode(nonce, "UTF-8")).append("&");
            data.append(URLEncoder.encode("private_key", "UTF-8")).append("=")
                    .append(URLEncoder.encode(publicKey, "UTF-8")).append("&");
            data.append(URLEncoder.encode("public_key", "UTF-8")).append("=")
                    .append(URLEncoder.encode(publicKey, "UTF-8")).append("&");
            data.append(URLEncoder.encode("address", "UTF-8")).append("=")
                    .append(URLEncoder.encode(address, "UTF-8")).append("&");

            data.append(URLEncoder.encode("height", "UTF-8")).append("=")
                    .append(height);

            System.out.println("MAKING REQUEST WITH DATA: " + data);

            out.writeBytes(data.toString());

            out.flush();
            out.close();

            BufferedReader b = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String content = b.readLine();
            //TODO -> HANDLE CALLBACK

            JSONObject jsO = new JSONObject(content);

            String status = jsO.getString("status");
            callback.onShare(argon);
            if (status.equalsIgnoreCase("ok")) {
                //TODO -> ACCEPT
                callback.onAccept(argon);

                if (submitDL <= 240) {
                    //TODO -> FOUND
                    callback.onFind(argon);
                }
            } else {
                //TODO -> REJECT
                System.out.println("REJECT: " + jsO);
                callback.onReject(argon);

            }
        } catch (Exception e) {
            //TODO -> HANDLE EXCEPTION
        }
    }

    public void stop() {
        //TODO -> STOP ALL ACTIVIES AND RECOURSES OF MINER
        running = false;
        for (ArionumHasher hasher : hashers) {
            hasher.setForceStop(true);
        }

        threadCollection.clear();
        hashers.clear();
    }

    public ArionumMinerCallback getCallback() {
        return callback;
    }

    public String getAddress() {
        return address;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public int getThreads() {
        return threads;
    }

    public void createUpdateThread() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                startHashers();
                while (running) {
                    try {
                        Thread.sleep(1000 * 20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!running)
                        return;
                    updateHashers();
                }
            }
        });
        thread.setDaemon(true);
        thread.setName("Arionum Miner Update Thread");
        thread.setPriority(10);
        thread.start();
        threadCollection.add(thread);
    }

    public void updateHashers() {
        //TODO -> UPDATE HASHERS WITH NEW VARS
        hasherClock++;
        //TODO -> GET DATA FROM POOL!

        String data = "";
        String pool_key = "";
        long difficulty = 0;
        long neededDL = 0;
        long height = 0;

        String url = null;
        try {
            url = getPool() + "?q=info&worker=" + URLEncoder.encode(getMinerName(), "UTF-8");
            if (hasherClock > 3) {
                hasherClock = 0;
                url += "&address=" + HomeView.getAddress() + "&hashrate=" + lastHashrate;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        try {
            URL u = new URL(url);
            URLConnection uc = u.openConnection();
            String content = new Scanner(uc.getInputStream()).nextLine();
            JSONObject o = new JSONObject(content);
            JSONObject jsonData = (JSONObject) o.get("data");
            String localData = (String) jsonData.get("block");
            data = localData;
            BigInteger localDifficulty = new BigInteger((String) jsonData.get("difficulty"));
            difficulty = localDifficulty.longValue();
            long limitDL = Long.parseLong(jsonData.get("limit").toString());
            neededDL = limitDL;
            long localHeight = jsonData.getLong("height");
            height = localHeight;
            String publicpoolkey = jsonData.getString("public_key");
            pool_key = publicpoolkey;
        } catch (Exception e) {
            //TODO -> HANDLE FAILED URL RESPONSE
            e.printStackTrace();
            final Handler h = new Handler(HomeView.instance.getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {
                    new MaterialDialog.Builder(HomeView.instance).title("ERROR").content("The Pool URL could not get resolved!").positiveText("OH NO!").show();
                }
            });
            return;
        }

        for (final ArionumHasher hasher : hashers) {
            hasher.updateHasher(data, pool_key, difficulty, neededDL, height);
            if (!hasher.isActive()) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(hashers.indexOf(hasher) * 1250);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        hasher.initiate();
                    }
                });
                threadCollection.add(thread);
                thread.setDaemon(true);
                thread.setName("Arionum Hasher Thread");
                thread.setPriority(10);
                thread.start();
            }
        }
    }

    public void setOverallHashes(int overallHashes) {
        ArionumMiner.overallHashes = overallHashes;
    }

    public String getMinerName() {
        return minerName;
    }

    public String getPool() {
        if (pool == null)
            return null;
        if (pool.contains(".php"))
            return pool;
        else
            return pool + "/mine.php";
    }

    public abstract static class ArionumMinerCallback {
        public abstract void onShare(String hash);

        public abstract void onAccept(String hash);

        public abstract void onFind(String hash);

        public abstract void onReject(String hash);

        public abstract void onDLChange(long dl);

    }
}
