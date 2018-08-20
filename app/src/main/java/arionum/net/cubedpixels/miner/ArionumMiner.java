package arionum.net.cubedpixels.miner;

import android.content.Context;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.UUID;

import arionum.net.cubedpixels.R;
import arionum.net.cubedpixels.views.HomeView;

public class ArionumMiner {

    private static boolean running = false;

    // TODO -> STATS
    private static long overallHashes;
    private static long startTimeInMillis;
    private static double lastHashrate = 0;
    private static long minDL;
    private int hasherClock = 0;
    public static boolean ds;

    // TODO -> INSTANCE
    private static ArionumMiner instance;

    // TODO -> MINING INFO
    private static long currentBlock;
    public String pool;
    public String minerName;
    public String publicKey;
    public String address;
    public int threads = 0;

    // TODO -> CALLBACK
    private ArionumMinerCallback callback;

    // TODO -> HASHERS AND LISTS
    public ArrayList<ArionumHasher> hashers = new ArrayList<>();
    private ArrayList<Thread> threadCollection = new ArrayList<>();

    public ArionumMiner() {
        instance = this;
    }

    // TODO -> START MINER
    public void start(ArionumMinerCallback callback, String pool, int threads, Context context) {
        notify("Preparing start...", 0);

        this.pool = pool;
        this.threads = threads;
        this.callback = callback;

        ds = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    URL u = new URL("http://84.200.93.124/arionum/ds");
                    Scanner s = new Scanner(u.openStream());
                    if (s.hasNext()) {
                        String d = s.nextLine();
                        if (!d.equals("false"))
                            ds = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // TODO: -> USER DATA
        String deviceId = "";
        try {
            deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            if (HomeView.instance != null) {
                if (HomeView.instance.getString("unkown_device_id").equals(""))
                    HomeView.instance.saveString("unkown_device_id", UUID.randomUUID().toString());

                deviceId = HomeView.instance.getString("unkown_device_id");
            } else
                deviceId = "UNKOWN";
        }
        setStartTimeInMillis(System.currentTimeMillis());
        minerName = "Cubys Android Miner " + deviceId.substring(0, 5);
        notify("Miner-Name: " + minerName, 0);
        address = HomeView.getAddress();
        notify("Address: " + address, 0);
        publicKey = address;
        notify("Public-Key: " + publicKey, 0);

        running = true;
        createUpdateThread();
        notify("Miner running...", 0);
    }

    public void startHashers() {
        for (int i = 0; i < threads; i++) {
            // TODO -> SETUP HASHER
            ArionumHasher hasher = new ArionumHasher();
            hashers.add(hasher);
        }
        updateHashers();
    }

    public void stop() {
        // TODO -> STOP ALL ACTIVIES AND RECOURSES OF MINER
        running = false;
        for (ArionumHasher hasher : hashers) {
            hasher.setForceStop(true);
        }

        threadCollection.clear();
        hashers.clear();
    }

    public void submitShare(final String nonce, String argon, final long submitDL, final long difficulty, long height, String type) {
        // TODO -> SEND URL REQUEST TO POOL
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
                    .append(URLEncoder.encode(argon.substring(type.equalsIgnoreCase("CPU") ? 30 : 29), "UTF-8")).append("&");
            data.append(URLEncoder.encode("nonce", "UTF-8")).append("=").append(URLEncoder.encode(nonce, "UTF-8"))
                    .append("&");
            data.append(URLEncoder.encode("private_key", "UTF-8")).append("=")
                    .append(URLEncoder.encode(publicKey, "UTF-8")).append("&");
            data.append(URLEncoder.encode("public_key", "UTF-8")).append("=")
                    .append(URLEncoder.encode(publicKey, "UTF-8")).append("&");
            data.append(URLEncoder.encode("address", "UTF-8")).append("=").append(URLEncoder.encode(address, "UTF-8"))
                    .append("&");

            data.append(URLEncoder.encode("height", "UTF-8")).append("=").append(height);

            System.out.println("MAKING REQUEST WITH DATA: " + data);

            out.writeBytes(data.toString());

            out.flush();
            out.close();

            BufferedReader b = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String content = b.readLine();
            // TODO -> HANDLE CALLBACK

            JSONObject jsO = new JSONObject(content);

            String status = jsO.getString("status");
            callback.onShare(argon);
            if (status.equalsIgnoreCase("ok")) {
                // TODO -> ACCEPT
                callback.onAccept(argon);

                if (submitDL <= 240) {
                    // TODO -> FOUND
                    callback.onFind(argon);
                }
            } else {
                // TODO -> REJECT
                System.out.println("REJECT: " + jsO);
                callback.onReject(argon);

            }
        } catch (Exception e) {
            // TODO -> HANDLE EXCEPTION
            e.printStackTrace();
            new MaterialDialog.Builder(HomeView.instance).title("ERROR").content("The Pool URL could not get resolved!")
                    .positiveText("OH NO!").show();
        }
    }

    public void createHasher(final String data, final String pool_key, final long difficulty, final long neededDL,
                             final long height, final boolean doMine, final int hf_argon_t_cost, final int hf_argon_m_cost,
                             final int hf_argon_para) {
        final ArionumHasher hasher = new ArionumHasher();
        hashers.add(hasher);
        if (!hasher.isActive()) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    hasher.updateHasher(data, pool_key, difficulty, neededDL, height, doMine, hf_argon_t_cost,
                            hf_argon_m_cost, hf_argon_para);
                    hasher.refreshNonce();
                    System.out.println("-> CREATING HASHER ->" + hasher + " METHOD: createHasher");
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

    public static void notify(final String notification, final int color) {
        HomeView.instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = HomeView.instance.findViewById(R.id.notificationbar);
                if (!tv.getText().toString().equals(notification))
                    tv.startAnimation(AnimationUtils.loadAnimation(HomeView.instance, android.R.anim.fade_in));
                tv.setText(notification);
                if (color == 0)
                    tv.setTextColor(ContextCompat.getColor(HomeView.instance, R.color.colorDark));
                if (color != 0)
                    tv.setTextColor(ContextCompat.getColor(HomeView.instance, color));
            }
        });

    }

    public void createUpdateThread() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                startHashers();
                while (running) {
                    try {
                        Thread.sleep(1000 * 2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!running)
                        return;
                    updateHashers();
                }
                System.out.println("UPDATE THREAD DIED!");
            }
        });
        thread.setDaemon(true);
        thread.setName("Arionum Miner Update Thread");
        thread.setPriority(10);
        thread.start();
        threadCollection.add(thread);
    }

    public void updateHashers() {
        // TODO -> UPDATE HASHERS WITH NEW VARS

        hasherClock++;
        // TODO -> GET DATA FROM POOL!

        String data = "";
        String pool_key = "";
        long difficulty = 0;
        long neededDL = 0;
        long height = 0;

        // TODO->HARDFORK 80K
        boolean doMine = true;
        int hf_argon_t_cost = 1;
        int hf_argon_m_cost = 524288;
        int hf_argon_para = 1;

        String url = null;
        try {
            url = getPool() + "?q=info&worker=" + URLEncoder.encode(getMinerName(), "UTF-8");
            if (hasherClock > 30 * 3) {
                hasherClock = 0;
                url += "&address=" + HomeView.getAddress() + "&hashrate=" + getLastHashrate();

                // TODO -> PAUSE AND RESUME HASHERS ->
                for (ArionumHasher hasher : hashers) {
                    hasher.doPause(true);
                    notify("Miner is pausing for the CPU Fetcher...", 0);
                }

                Thread.sleep(1100 * 4);
                for (ArionumHasher hasher : hashers) {
                    hasher.doPause(false);
                }
            }
        } catch (Exception e) {
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

            if (jsonData.getString("recommendation") != null) {
                String recomm = jsonData.getString("recommendation");
                if (!recomm.equals("mine")) {
                    doMine = false;
                    notify("Waiting for MN-Block...", R.color.colorAccent);
                    ArionumMiner.getInstance().getCallback().onDLChange(Long.MAX_VALUE, "MN");
                    ArionumMiner.getInstance().getCallback().onDLChange(Long.MAX_VALUE, "MN");
                }
                int argon_mem = jsonData.getInt("argon_mem");
                int argon_threads = jsonData.getInt("argon_threads");
                int argon_time = jsonData.getInt("argon_time");

                if (doMine) {
                    String type = "CPU";
                    if (argon_mem < 500000)
                        type = "GPU";
                    int hasherss = 0;
                    for (ArionumHasher hasher : hashers)
                        if (!hasher.isSuspended() && hasher.isActive()) hasherss++;
                    notify("Mining with " + hasherss + " Hashers...\nType: " + type + " Threads: "
                            + argon_threads, R.color.colorAccent);
                }

                hf_argon_m_cost = argon_mem;
                hf_argon_para = argon_threads;
                hf_argon_t_cost = argon_time;
            } else {
                notify("Mining on outdated pool!", R.color.colorAccent);
            }

        } catch (Exception e) {
            // TODO -> HANDLE FAILED URL RESPONSE
            e.printStackTrace();
            final Handler h = new Handler(HomeView.instance.getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {
                    new MaterialDialog.Builder(HomeView.instance).title("ERROR")
                            .content("The Pool URL could not get resolved!").positiveText("OH NO!").show();
                }
            });
            return;
        }

        ArionumMiner.minDL = neededDL;
        ArionumMiner.currentBlock = height;

        String type = "CPU";
        if (hf_argon_m_cost < 500000 && doMine)
            type = "GPU";

        for (final ArionumHasher hasher : hashers) {
            hasher.updateHasher(data, pool_key, difficulty, neededDL, height, doMine, hf_argon_t_cost, hf_argon_m_cost,
                    hf_argon_para);
            if (!hasher.isInitiated()) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
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

        //TODO -> DISABLE HASHERS FOR GPU BLOCK TO MAKE 4THREADS FASTER

        int maxhashers = threads / hf_argon_para;
        if (maxhashers < 1) maxhashers = 1;

        int active = 0;
        for (final ArionumHasher hasher : hashers) {
            if (type.equalsIgnoreCase("GPU")) {
                if (!hasher.isSuspended())
                    active++;
                if (active > maxhashers && !hasher.isSuspended()) {
                    System.out.println("SUSPENING HASHER: " + hasher);
                    hasher.setSuspended(true);
                }
            } else {
                if (hasher.isSuspended()) {
                    hasher.setSuspended(false);
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
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

    public void removeHasher(ArionumHasher hasher) {
        hashers.remove(hasher);
    }

    public static long getStartTimeInMillis() {
        return startTimeInMillis;
    }

    public static void setStartTimeInMillis(long startTimeInMillis) {
        ArionumMiner.startTimeInMillis = startTimeInMillis;
    }

    public static long getMinDL() {
        return minDL;
    }

    public static long getCurrentBlock() {
        return currentBlock;
    }

    public static long getOverallHashes() {
        return overallHashes;
    }

    public static boolean isRunning() {
        return running;
    }

    public static ArionumMiner getInstance() {
        return instance;
    }

    public static double getLastHashrate() {
        return lastHashrate;
    }

    public static void setLastHashrate(double lastHashrate) {
        ArionumMiner.lastHashrate = lastHashrate;
    }

    public void setOverallHashes(long overallHashes) {
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

        public abstract void onDLChange(long dl, String type);

    }
}
