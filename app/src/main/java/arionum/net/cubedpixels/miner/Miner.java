package arionum.net.cubedpixels.miner;

import android.os.AsyncTask;
import android.os.Handler;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import arionum.net.cubedpixels.views.HomeView;


@SuppressWarnings({"unused"})
public class Miner implements UncaughtExceptionHandler {

    public static final long UPDATING_DELAY = 2000l;
    public static final long UPDATING_REPORT = 45000l;
    public static final long UPDATING_STATS = 7500l;
    protected static final long TEST_PERIOD = 270000;
    protected static final long INIT_DELAY = 30000;
    protected static final long REPORTING_INTERVAL = 60000l;
    private static final long MIN_HASHES_PER_SESSION = 1l;
    private static final long MIN_SESSION_LENGTH = 5000l;
    private static final long MAX_SESSION_LENGTH = 14000l;
    private static final long REBALANCE_DELAY = 300000l;
    public static long finalDuration = Long.MAX_VALUE;
    public static long limitDuration = 0;
    private static callbackMiner callbackMiner;
    private static long sleep;
    protected final AtomicInteger hasherCount;
    protected final AtomicLong hashes;
    protected final AtomicLong bestDL;
    protected final AtomicLong sessionSubmits;
    protected final AtomicLong sessionRejects;
    protected final AtomicLong blockShares;
    protected final AtomicLong blockFinds;
    protected final long wallClockBegin;
    private final ExecutorService submitters;
    private final ConcurrentLinkedQueue<HasherStats> deadWorkerSociety;
    private final AtomicLong deadWorkers;
    private final ConcurrentHashMap<String, Long> deadWorkerLives;
    private final ExecutorService hashers;
    private final ConcurrentHashMap<String, Hasher> workers;
    private final ExecutorService updaters;
    private final AtomicLong updateTimeAvg;
    private final AtomicLong updateTimeMax;
    private final AtomicLong updateTimeMin;
    private final AtomicLong updateParseTimeAvg;
    private final AtomicLong updateParseTimeMax;
    private final AtomicLong updateParseTimeMin;
    private final AtomicLong submitTimeAvg;
    private final AtomicLong submitTimeMax;
    private final AtomicLong submitTimeMin;
    private final AtomicLong submitParseTimeAvg;
    private final AtomicLong submitParseTimeMax;
    private final AtomicLong submitParseTimeMin;
    private final ExecutorService stats;
    protected boolean active = false;
    protected boolean colors = false;
    private int maxHashers;
    private long hashesPerSession = 10l;
    private long sessionLength = 5000l;
    private long lastRebalance;
    private double lastRebalanceHashRate = Double.MAX_VALUE;
    private double lastRebalanceTiC = 0.0d;
    private long lastRebalanceSessionLength = 0l;
    private long lastWorkerReport;
    private MinerType type;
    private AdvMode hasherMode;
    public static String node;
    private String worker;
    private String publicKey;
    private String privateKey;
    private String data;
    private BigInteger difficulty;
    private long limit;
    private long height;
    private long lastBlockUpdate;
    private AtomicLong lastSpeed;
    private AtomicLong speedAccrue;
    private long lastUpdate;
    private long lastReport;
    private int cycles;
    private int supercycles;
    private int skips;
    private int failures;
    private int updates;
    private Profile activeProfile;
    private TreeSet<Profile> evaluatedProfiles;
    private ConcurrentLinkedQueue<Profile> profilesToEvaluate;
    private int coreCap;
    private long nextProfileSwap;
    private long profilesTested;
    private long nextReassess;
    private Profile toReassess;
    private String statsHost;
    private String statsInvoke;
    private String statsToken;
    private boolean post;
    private ConcurrentHashMap<String, HasherStats> statsStage;
    private ConcurrentLinkedQueue<HasherStats> statsReport;

    boolean sendSpeed = false;

    public static void setSleep(long sleep) {
        Miner.sleep = sleep;
    }

    public static Miner main(callbackMiner callback, String pool, String hashers) {
        Miner miner = null;
        callbackMiner = callback;
        int defaultHashers = Runtime.getRuntime().availableProcessors();
        if (!hashers.isEmpty())
            defaultHashers = Integer.parseInt(hashers);
        String workerName = Miner.php_uniqid();
        miner = new Miner(pool, defaultHashers, workerName);
        miner.start();
        return miner;
    }

    public static String php_uniqid() {
        double m = ((double) (System.nanoTime() / 10)) / 10000d;
        return String.format("%8x%05x", (long) Math.floor(m), (long) ((m - Math.floor(m)) * 1000000)).trim();
    }

    public boolean shouldSleep() {
        return (System.currentTimeMillis() - sleep) < 2000;
    }

    long lastSendSpeed = 0;
    public Miner(String node, int cores, String worker) {
        this.hasherMode = AdvMode.standard;
        this.worker = php_uniqid();

        this.updaters = Executors.newSingleThreadExecutor();
        this.submitters = Executors.newCachedThreadPool();
        this.hasherCount = new AtomicInteger();

        this.workers = new ConcurrentHashMap<String, Hasher>();

        this.deadWorkerSociety = new ConcurrentLinkedQueue<>();
        this.deadWorkers = new AtomicLong(0l);
        this.deadWorkerLives = new ConcurrentHashMap<String, Long>();

        this.blockFinds = new AtomicLong();
        this.blockShares = new AtomicLong();

        sendSpeed = false;
        lastSendSpeed = System.currentTimeMillis();


        activeProfile = null;
        TreeSet<Profile> evaluatedProfiles = new TreeSet<Profile>();
        ConcurrentLinkedQueue<Profile> profilesToEvaluate = new ConcurrentLinkedQueue<Profile>();
        int coreCap = Runtime.getRuntime().availableProcessors();
        long nextProfileSwap = 0;
        long profilesTested = 0;

        this.statsHost = null;
        this.statsInvoke = "report.php";
        this.statsToken = php_uniqid();
        this.post = false;
        this.statsStage = new ConcurrentHashMap<String, HasherStats>();
        this.statsReport = new ConcurrentLinkedQueue<HasherStats>();
        this.stats = Executors.newCachedThreadPool();

        this.hashes = new AtomicLong();
        this.bestDL = new AtomicLong(Long.MAX_VALUE);
        this.sessionSubmits = new AtomicLong();
        this.sessionRejects = new AtomicLong();
        this.lastSpeed = new AtomicLong();
        this.speedAccrue = new AtomicLong();

        this.updateTimeAvg = new AtomicLong();
        this.updateTimeMax = new AtomicLong(Long.MIN_VALUE);
        this.updateTimeMin = new AtomicLong(Long.MAX_VALUE);
        this.updateParseTimeAvg = new AtomicLong();
        this.updateParseTimeMax = new AtomicLong(Long.MIN_VALUE);
        this.updateParseTimeMin = new AtomicLong(Long.MAX_VALUE);

        this.submitTimeAvg = new AtomicLong();
        this.submitTimeMax = new AtomicLong(Long.MIN_VALUE);
        this.submitTimeMin = new AtomicLong(Long.MAX_VALUE);
        this.submitParseTimeAvg = new AtomicLong();
        this.submitParseTimeMax = new AtomicLong(Long.MIN_VALUE);
        this.submitParseTimeMin = new AtomicLong(Long.MAX_VALUE);


        try {
            Miner.node = node;
            this.type = MinerType.pool;
            this.publicKey = HomeView.getAddress();
            this.privateKey = publicKey;
            Miner.node = node;
            this.hasherMode = AdvMode.standard;
            this.maxHashers = cores;
            this.worker = worker;


            System.out.println(" type: " + this.type);
            System.out.println(" node: " + this.type);
            System.out.println(" public-key: " + this.publicKey);
            System.out.println(" private-key: " + this.privateKey);
            System.out.println(" hasher-count: " + this.maxHashers);
            System.out.println(" hasher-mode: " + this.hasherMode);

        } catch (Exception e) {
            System.err.println("Invalid configuration: " + (e.getMessage()));
            System.err.println("  type: " + this.type);
            System.err.println("  node: " + Miner.node);
            System.err.println("  public-key: " + this.publicKey);
            System.err.println("  private-key: " + this.privateKey);
            System.err.println("  hasher-count: " + this.maxHashers);
            System.err.println("  hasher-mode: " + this.hasherMode);
            System.err.println("  colors: " + this.colors);
            System.err.println("  worker-name: " + this.worker);
        }



        this.hashers = Executors.newFixedThreadPool(
                this.maxHashers > 0 ? this.maxHashers : Runtime.getRuntime().availableProcessors());

        this.limit = 240;
        this.wallClockBegin = System.currentTimeMillis();
    }

    public void start() {
        if (MinerType.test.equals(this.type)) {
            startTest();
            return;
        }

        active = true;
        this.lastUpdate = wallClockBegin;
        final AtomicBoolean firstRun = new AtomicBoolean(true);
        cycles = 0;
        supercycles = 0;
        final AtomicBoolean sentSpeed = new AtomicBoolean(false);
        skips = 0;
        failures = 0;
        updates = 0;
        System.out.println("STARTING");


        while (active) {
            System.out.println("ACTIVE");
            final boolean[] updateLoop = {true};
            int firstAttempts = 0;
            while (updateLoop[0]) {
                System.out.println("LOOP");
                Future<Boolean> update = this.updaters.submit(new Callable<Boolean>() {
                    public Boolean call() throws JSONException {
                        long executionTimeTracker = System.currentTimeMillis();
                        try {
                            if (cycles > 0 && (System.currentTimeMillis() - lastUpdate) < (UPDATING_DELAY * .5)) {
                                skips++;
                                return Boolean.FALSE;
                            }
                            boolean endline = false;

                            String cummSpeed = speed();
                            StringBuilder extra = new StringBuilder(node);
                            extra.append("/mine.php?q=info");
                            if (MinerType.pool.equals(type)) {
                                extra.append("&worker=").append(URLEncoder.encode(worker, "UTF-8"));

                                if (firstRun.get() || (!sentSpeed.get() && supercycles > 15)) {
                                    extra.append("&address=").append(privateKey);
                                }

                                if (!sendSpeed && lastSendSpeed + 1000 * 20 < System.currentTimeMillis()) {
                                    extra.append("&hashrate=").append(cummSpeed);
                                    lastSendSpeed = System.currentTimeMillis();
                                    sendSpeed = true;
                                } else if (sendSpeed && lastSendSpeed + 1000 * 60 * 6 < System.currentTimeMillis()) {
                                    extra.append("&hashrate=").append(cummSpeed);
                                    lastSendSpeed = System.currentTimeMillis();
                                }

                            }

                            URL url = new URL(extra.toString());
                            URLConnection connect = url.openConnection();
                            connect.setConnectTimeout(cycles < 10 ? 10000 : 1000);
                            updateLoop[0] = false;

                            lastUpdate = System.currentTimeMillis();



                            long parseTimeTracker = System.currentTimeMillis();

                            BufferedReader s = new BufferedReader(new InputStreamReader(connect.getInputStream()));
                            String st = new String(s.readLine());


                            JSONObject obj = new JSONObject(st);

                            if (!"ok".equals(obj.get("status"))) {
                                failures++;
                                updateTime(System.currentTimeMillis(), executionTimeTracker, parseTimeTracker);
                                return Boolean.FALSE;
                            }

                            JSONObject jsonData = (JSONObject) obj.get("data");
                            String localData = (String) jsonData.get("block");
                            if (!localData.equals(data)) {
                                if (lastBlockUpdate > 0) {
                                }
                                data = localData;
                                lastBlockUpdate = System.currentTimeMillis();
                                long bestDLLastBlock = bestDL.getAndSet(Long.MAX_VALUE);

                                endline = true;
                            }
                            BigInteger localDifficulty = new BigInteger((String) jsonData.get("difficulty"));
                            if (!localDifficulty.equals(difficulty)) {
                                difficulty = localDifficulty;
                                endline = true;
                            }
                            updateLoop[0] = false;
                            long localLimit = 0;
                            if (MinerType.pool.equals(type)) {
                                localLimit = Long.parseLong(jsonData.get("limit").toString());
                                publicKey = (String) jsonData.get("public_key");
                            } else {
                                localLimit = 240;
                            }

                            if (limit != localLimit) {
                                limit = localLimit;
                            }
                            long localHeight = jsonData.getLong("height");
                            if (localHeight != height) {
                                height = localHeight;
                                updateWorkers();
                            }


                            localHeight = (Long) jsonData.get("height");
                            if (localHeight != height) {
                                height = localHeight;
                                endline = true;
                            }

                            if (endline) {
                                updateWorkers();
                            }

                            System.out.println("REPORTED SPEED: " + cummSpeed);
                            System.out.println("Shares: " + sessionSubmits.get());
                            long sinceLastReport = System.currentTimeMillis() - lastReport;
                            if (sinceLastReport > UPDATING_REPORT) {
                                lastReport = System.currentTimeMillis();
                                System.out.println("REPORTED SPEED: " + cummSpeed);
                                System.out.println("Shares: " + sessionSubmits.get());

                                printWorkerHeader();

                                updateTimeAvg.set(0);
                                updateTimeMax.set(Long.MIN_VALUE);
                                updateTimeMin.set(Long.MAX_VALUE);
                                updateParseTimeAvg.set(0);
                                updateParseTimeMax.set(Long.MIN_VALUE);
                                updateParseTimeMin.set(Long.MAX_VALUE);
                                submitParseTimeAvg.set(0);
                                submitParseTimeMax.set(Long.MIN_VALUE);
                                submitParseTimeMin.set(Long.MAX_VALUE);
                                skips = 0;
                                failures = 0;
                                updates = 0;
                                endline = true;
                                clearSpeed();
                            }
                            updates++;
                            updateTime(System.currentTimeMillis(), executionTimeTracker, parseTimeTracker);
                            if (endline) {
                                System.out.println();
                            }
                            if ((sinceLastReport % UPDATING_STATS) < UPDATING_DELAY && sinceLastReport < 5000000000l) {
                                printWorkerStats();
                            }

                            updateLoop[0] = false;
                            return Boolean.TRUE;
                        } catch (IOException e) {
                            lastUpdate = System.currentTimeMillis();
                            updateLoop[0] = false;
                            e.printStackTrace();
                            failures++;
                            updateTime(System.currentTimeMillis() - executionTimeTracker);
                            return Boolean.FALSE;
                        }
                    }
                });
                if (firstRun.get()) {
                    try {
                        if (update.get().booleanValue()) {
                            firstRun.set(false);
                            updateLoop[0] = false;
                        } else {
                            firstAttempts++;
                        }
                    } catch (InterruptedException | ExecutionException e) {

                    } finally {
                        if (firstRun.get() && firstAttempts > 15) {
                            System.out.println("ACTIVE == FALSE");
                            active = false;
                            firstRun.set(false);
                            updateLoop[0] = false;
                            break;
                        } else if (firstRun.get()) {

                            try {
                                Thread.sleep(5000l);
                            } catch (InterruptedException ie) {
                                System.out.println("INTERRUPT == FALSE");
                                active = false;
                                firstRun.set(false);
                                updateLoop[0] = false;
                            }

                        }
                    }
                    lastWorkerReport = System.currentTimeMillis();
                } else {
                    updateLoop[0] = false;
                }
            }
            System.out.println("Hashrate: " + speed());
            callbackMiner.onHashRate(speed(), finalDuration);
            if (this.hasherCount.get() < maxHashers) {
                String workerId = this.deadWorkers.getAndIncrement() + "]" + php_uniqid();
                this.deadWorkerLives.put(workerId, System.currentTimeMillis());
                Hasher hasher = HasherFactory.createHasher(hasherMode, this, workerId, this.hashesPerSession, this.sessionLength * 2l);
                updateWorker(hasher);
                this.hashers.submit(hasher);
                addWorker(workerId, hasher);
            }


            try {
                Thread.sleep(UPDATING_DELAY);
            } catch (InterruptedException ie) {
                active = false;
            }

            if (cycles == 30) {
                cycles = 0;
            }

            if (supercycles == 300) {
                supercycles = 0;
                sentSpeed.set(false);
            }

            if (cycles % 2 == 0) {
                refreshFromWorkers();
            }

            updateStats();

            cycles++;
            supercycles++;
        }

        this.updaters.shutdown();
        this.hashers.shutdown();
        this.submitters.shutdown();
    }

    protected void addWorker(String workerId, Hasher hasher) {
        workers.put(workerId, hasher);
    }

    protected void releaseWorker(String workerId) {
        workers.remove(workerId);
    }

    protected void updateWorkers() {
        for (Hasher h : workers.values()) {
            if (h != null && h.isActive()) {
                updateWorker(h);
            }
        }
    }



    protected void updateWorker(Hasher hasher) {
        hasher.update(getDifficulty(), getBlockData(), getLimit(), getPublicKey(), getHeight(), callbackMiner);
    }

    protected void workerInit(final String workerId) {
    }

    protected void workerFinish(HasherStats stats, Hasher worker) {
        this.deadWorkerSociety.offer(stats);
        releaseWorker(worker.getID());
        try {
            stats.scheduledTime = System.currentTimeMillis() - this.deadWorkerLives.remove(stats.id);
        } catch (NullPointerException npe) {
            stats.scheduledTime = stats.hashTime;
        }
        String workerId = this.deadWorkers.getAndIncrement() + "]" + php_uniqid();
        this.deadWorkerLives.put(workerId, System.currentTimeMillis());
        Hasher hasher = HasherFactory.createHasher(hasherMode, this, workerId, this.hashesPerSession, this.sessionLength * 2l);
        updateWorker(hasher);
        this.hashers.submit(hasher);
        addWorker(workerId, hasher);
    }

    protected long[] sessionFinish(HasherStats stats, Hasher worker) {
        this.deadWorkerSociety.offer(stats);
        try {
            stats.scheduledTime = System.currentTimeMillis() - this.deadWorkerLives.put(stats.id, System.currentTimeMillis());
        } catch (NullPointerException npe) {
            stats.scheduledTime = stats.hashTime;
        }
        return new long[]{this.hashesPerSession, this.sessionLength * 2l};
    }

    protected void refreshFromWorkers() {
        long wallTime = System.currentTimeMillis() - lastWorkerReport;
        lastWorkerReport = System.currentTimeMillis();
        try {
            AtomicLong newHashes = new AtomicLong();
            AtomicLong adjust = new AtomicLong();
            AtomicLong offload = new AtomicLong();
            HasherStats worker = null;
            Report report = new Report();
            while ((worker = this.deadWorkerSociety.poll()) != null) {
                try {
                    report.runs++;

                    long allHashes = worker.hashes;
                    newHashes.addAndGet(allHashes);
                    hashes.getAndAdd(allHashes);

                    report.hashes += allHashes;

                    long localDL = worker.bestDL;

                    report.shares += worker.shares;
                    report.finds += worker.finds;

                    blockShares.addAndGet(worker.shares);
                    blockFinds.addAndGet(worker.finds);

                    long argonTime = worker.argonTime;
                    long shaTime = worker.shaTime;
                    long nonArgonTime = worker.nonArgonTime;
                    long fullTime = argonTime + nonArgonTime;
                    double lastRatio = 0.0d;
                    if (fullTime > 0) {
                        lastRatio = (double) argonTime / (double) fullTime;
                    }

                    double lastShaRatio = 0.0;
                    if (shaTime > 0 || fullTime > 0) {
                        lastShaRatio = (double) shaTime / (double) fullTime;
                    }
                    report.argonEff += lastRatio;
                    report.shaEff += lastShaRatio;

                    report.argonTime += argonTime;
                    report.nonArgontime += nonArgonTime;
                    report.shaTime += shaTime;

                    long totalTime = worker.hashTime;

                    report.totalTime += totalTime;

                    report.curWaitLoss += (double) (worker.scheduledTime - worker.hashTime) / (double) worker.scheduledTime;

                    long seconds = fullTime / 1000000000l;

                    report.curHashPerSecond += (double) allHashes / (totalTime / 1000d);

                    report.curTimeInCore += ((double) seconds * 1000d) / ((double) totalTime);

                    if (totalTime < this.sessionLength) {
                        double gap = (double) (this.sessionLength - totalTime) / (double) this.sessionLength;
                        long recom = (long) (((double) allHashes) * .5 * gap);

                        adjust.addAndGet(recom);
                        offload.incrementAndGet();
                    } else if (totalTime > this.sessionLength) {
                        double gap = (double) (totalTime - this.sessionLength) / (double) totalTime;
                        long recom = (long) -(((double) allHashes) * .5 * gap);

                        adjust.addAndGet(recom);
                        offload.incrementAndGet();
                    } else {
                        offload.incrementAndGet();
                    }

                    /* reporting stats */
                    if (this.statsHost != null) {
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            if (offload.get() > 0) {
                this.hashesPerSession += (long) (adjust.doubleValue() / offload.doubleValue());
                if (this.hashesPerSession < MIN_HASHES_PER_SESSION) {
                    this.hashesPerSession = MIN_HASHES_PER_SESSION;
                }
            }
            this.lastSpeed.addAndGet((long) (((newHashes.doubleValue() * 10000000d) / (double) (wallTime))));
            this.speedAccrue.incrementAndGet();
        } catch (Exception e) {
        }
    }

    private void printWorkerHeader() {

    }

    private void printWorkerStats() {
        int recentSize = 0;
        long runs = 0;
        double avgRate = 0.0d;
        double coreEff = 0.0d;
        double waitEff = 0.0d;
        double argEff = 0.0d;
        double shaEff = 0.0d;
        long shares = this.blockShares.get();
        long finds = this.blockFinds.get();
        long failures = this.sessionRejects.get();
        int grabReports = (int) (15000.0 / (Miner.UPDATING_DELAY * 2d));
        if (grabReports < 3) grabReports = 3;

        try {
            LinkedList<Report> recent = new LinkedList<Report>();


            for (Report report : recent) {
                runs += report.runs;
                avgRate += report.curHashPerSecond;
                waitEff += report.curWaitLoss * 100d;
                coreEff += report.curTimeInCore * 100d;
                argEff += report.argonEff * 100d;
                shaEff += report.shaEff * 100d;
            }

            if (waitEff < 0.0d) waitEff = 0.0d;


        } catch (Throwable t) {
            t.printStackTrace();
        }

        if (AdvMode.auto.equals(this.hasherMode)) {
        }
    }

    protected void updateStats() {
        String a = "1";
        if (a.equalsIgnoreCase("1"))
            return;
        this.stats.submit(new Runnable() {
            public void run() {
                HasherStats latest = null;
                while ((latest = statsReport.poll()) != null) {
                    try {
                        StringBuilder to = new StringBuilder(statsHost);
                        to.append("/").append(statsInvoke).append("?q=report");
                        to.append("&token=").append(URLEncoder.encode(statsToken, "UTF-8"));
                        to.append("&id=").append(URLEncoder.encode(latest.id, "UTF-8")).append("&type=").append(latest.type);
                        if (!post) {
                            to.append("&hashes=").append(latest.hashes)
                                    .append("&elapsed=").append(latest.hashTime);
                        }


                        URL url = new URL(to.toString());
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        if (post) {
                            con.setRequestMethod("POST");
                            con.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                            con.setDoOutput(true);
                            DataOutputStream out = new DataOutputStream(con.getOutputStream());

                            StringBuilder data = new StringBuilder();

                            data.append(URLEncoder.encode("hashes", "UTF-8")).append("=")
                                    .append(URLEncoder.encode(Long.toString(latest.hashes), "UTF-8")).append("&");
                            data.append(URLEncoder.encode("elapsed", "UTF-8")).append("=")
                                    .append(URLEncoder.encode(Long.toString(latest.hashTime), "UTF-8"));

                            out.writeBytes(data.toString());

                            out.flush();
                            out.close();
                        } else {
                            con.setRequestMethod("GET");
                        }

                        int status = con.getResponseCode();
                        if (status != HttpURLConnection.HTTP_OK) {
                            System.err.println("Failed to report stats: " + status);
                        }
                    } catch (IOException ioe) {
                        System.err.println("Failed to report stats: " + ioe.getMessage());
                    }
                }
            }
        });
    }

    protected void submitStats(final String nonce, final String argon, final long submitDL, final long difficulty, final String type, final int retries, final boolean accepted) {
        this.stats.submit(new Runnable() {
            public void run() {
                try {
                    StringBuilder to = new StringBuilder(statsHost);
                    to.append("/").append(statsInvoke).append("?q=discovery");
                    to.append("&token=").append(URLEncoder.encode(statsToken, "UTF-8"));
                    to.append("&id=").append(URLEncoder.encode(worker, "UTF-8")).append("&type=").append(type);
                    if (!post) {
                        to.append("&nonce=").append(URLEncoder.encode(nonce, "UTF-8"))
                                .append("&argon=").append(URLEncoder.encode(argon, "UTF-8"))
                                .append("&difficulty=").append(difficulty)
                                .append("&dl=").append(submitDL)
                                .append("&retries=").append(retries);
                        if (accepted) {
                            to.append("&confirmed");
                        }
                    }


                    URL url = new URL(to.toString());
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    if (post) {
                        con.setRequestMethod("POST");
                        con.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                        con.setDoOutput(true);
                        DataOutputStream out = new DataOutputStream(con.getOutputStream());

                        StringBuilder data = new StringBuilder();

                        data.append(URLEncoder.encode("nonce", "UTF-8")).append("=")
                                .append(URLEncoder.encode(nonce, "UTF-8")).append("&");
                        data.append(URLEncoder.encode("argon", "UTF-8")).append("=")
                                .append(URLEncoder.encode(argon, "UTF-8")).append("&");
                        data.append(URLEncoder.encode("difficulty", "UTF-8")).append("=")
                                .append(URLEncoder.encode(Long.toString(difficulty), "UTF-8")).append("&");
                        data.append(URLEncoder.encode("dl", "UTF-8")).append("=")
                                .append(URLEncoder.encode(Long.toString(submitDL), "UTF-8")).append("&");
                        data.append(URLEncoder.encode("retries", "UTF-8")).append("=")
                                .append(URLEncoder.encode(Long.toString(retries), "UTF-8")).append("&");
                        if (accepted) {
                            data.append(URLEncoder.encode("confirmed", "UTF-8"));
                        }

                        out.writeBytes(data.toString());

                        out.flush();
                        out.close();
                    } else {
                        con.setRequestMethod("GET");
                    }

                    int status = con.getResponseCode();
                    if (status != HttpURLConnection.HTTP_OK) {
                        System.err.println("Failed to report submit: " + status);
                    }
                } catch (IOException ioe) {
                    System.err.println("Failed to report submit: " + ioe.getMessage());
                }
            }
        });
    }

    protected void submit(final String nonce, String argon1, final long submitDL, final long difficulty, final String workerType) {
        if (height == 0)
            return;
        boolean sharepool = argon1.contains("SHAREPOOL");
        final String argon = argon1.replace("SHAREPOOL", "");

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                StringBuilder extra = new StringBuilder(node);
                extra.append("/mine.php?q=submitNonce");
                int failures = 0;
                long executionTimeTracker = System.currentTimeMillis();
                try {
                    URL url = new URL(extra.toString());
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
                            .append(URLEncoder.encode(privateKey, "UTF-8")).append("&");
                    data.append(URLEncoder.encode("public_key", "UTF-8")).append("=")
                            .append(URLEncoder.encode(publicKey, "UTF-8")).append("&");
                    data.append(URLEncoder.encode("address", "UTF-8")).append("=")
                            .append(URLEncoder.encode(privateKey, "UTF-8")).append("&");

                    data.append(URLEncoder.encode("height", "UTF-8")).append("=")
                            .append(height);

                    System.out.println("MAKING REQUEST WITH DATA: " + data);

                    out.writeBytes(data.toString());

                    out.flush();
                    out.close();

                    sessionSubmits.incrementAndGet();

                    int status = con.getResponseCode();
                    if (status != HttpURLConnection.HTTP_OK) {
                        con.disconnect();
                        failures++;
                        submitTime(System.currentTimeMillis() - executionTimeTracker);
                    } else {
                        long parseTimeTracker = System.currentTimeMillis();
                        BufferedReader b = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String s = b.readLine();
                        JSONObject obj = new JSONObject(s);

                        if (!"ok".equals(obj.get("status"))) {
                            sessionRejects.incrementAndGet();
                            System.out.println(" Raw Failure: " + obj.toString());
                            callbackMiner.onReject(obj.toString() + "//SHAREPOOL");
                            submitStats(nonce, argon, submitDL, difficulty, workerType, failures, false);

                        } else {
                            System.out.println("DONE ACCEPTED SHARE " + obj.toString());
                            callbackMiner.onAccept(obj.toString());
                            submitStats(nonce, argon, submitDL, difficulty, workerType, failures, true);
                        }

                        con.disconnect();

                        submitTime(System.currentTimeMillis(), executionTimeTracker, parseTimeTracker);
                    }
                } catch (IOException e) {
                    failures++;


                    submitTime(System.currentTimeMillis() - executionTimeTracker);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(null, null, null);
    }

    String speed() {
        return String.format("%.3f", (((double) this.lastSpeed.get() / 10000d) / (double) this.speedAccrue.get()));
    }

    private void clearSpeed() {
        this.lastSpeed.set(0);
        this.speedAccrue.set(0);
    }

    private String avgSpeed(long clockBegin) {
        return String.format("%.3f",
                this.hashes.doubleValue() / (((double) (System.currentTimeMillis() - clockBegin)) / 1000d));
    }

    private void updateTime(long duration) {
        this.updateTimeAvg.addAndGet(duration);
    }

    private void updateTime(long instance, long total, long parse) {
        updateTime(instance - total);
        long duration = instance - parse;

        this.updateParseTimeAvg.addAndGet(duration);
    }

    private void submitTime(long duration) {
        this.submitTimeAvg.addAndGet(duration);
    }

    private void submitTime(long instance, long total, long parse) {
        submitTime(instance - total);
        long duration = instance - parse;
        this.submitParseTimeAvg.addAndGet(duration);
    }

    protected BigInteger getDifficulty() {
        return this.difficulty;
    }

    protected String getBlockData() {
        return this.data;
    }

    protected long getLimit() {
        return this.limit;
    }

    protected String getPublicKey() {
        return this.publicKey;
    }

    protected long getHeight() {
        return this.height;
    }

    private void startTest() {
        System.out.println("Static tests using " + this.maxHashers + " iterations as cap");

        System.out.println("Utility Test on " + this.publicKey);
        String refKey = this.publicKey;


        System.out.println("Done static testing.");
    }

    @Override
    public void uncaughtException(Thread t, final Throwable e) {
        e.printStackTrace();
        callbackMiner.onStop();
        final Handler h = new Handler(HomeView.instance.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                new MaterialDialog.Builder(HomeView.instance)
                        .title("ERROR")
                        .content(e.getMessage() + " \n- I think your devices is jammed full of cats...\n They are not really smart you know?")
                        .positiveText("OH NO!")
                        .show();
            }
        });
    }

    public void stop() {
        this.updaters.shutdown();
        this.hashers.shutdown();
        this.submitters.shutdown();
    }

    public static abstract class callbackMiner {
        public abstract void onHashRate(String hash, long bestDelay);

        public abstract void onDLChange(String hash, long bestDelay);

        public abstract void onShare(String hash);

        public abstract void onReject(String hash);

        public abstract void onAccept(String hash);

        public abstract void onFind(String hash);

        public abstract void onDurChange(String dur);

        public abstract void onStop();
    }

}