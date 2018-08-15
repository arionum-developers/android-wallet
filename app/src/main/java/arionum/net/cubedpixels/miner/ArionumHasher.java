package arionum.net.cubedpixels.miner;

import android.app.ActivityManager;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import arionum.net.cubedpixels.utils.Base58;
import arionum.net.cubedpixels.views.HomeView;
import de.wuthoehle.argon2jni.Argon2;
import de.wuthoehle.argon2jni.EncodedArgon2Result;
import de.wuthoehle.argon2jni.SecurityParameters;

import static android.content.Context.ACTIVITY_SERVICE;

public class ArionumHasher implements Thread.UncaughtExceptionHandler {

    private boolean active = false;
    private boolean forceStop = false;
    private boolean isInitiated = false;
    private boolean doPause = false;
    private Argon2 argon2;
    private MessageDigest messageDigest;
    private Nonce nonce;

    //TODO-> HASHER VARS FOR NONCE / SHARE UPDATES
    private String data;
    private long difficulty;
    private long neededDL;
    private long height;
    private String pool_key;

    //TODO->  HARDFORK 80K
    private boolean doMine = true;

    //TODO-> NEW ARGON2 PARAMS
    private int hf_argon_t_cost = 1;
    private int hf_argon_m_cost = 524288;
    private int hf_argon_para = 1;


    private ArionumMiner minerInstance;

    public ArionumHasher() {
        minerInstance = ArionumMiner.getInstance();
    }

    public void initiate() {
        //TODO -> SETUP ARGON2
        active = true;
        isInitiated = true;

        System.out.println("Intiting Hasher -> " + this);


        argon2 = new Argon2(new SecurityParameters(hf_argon_t_cost, hf_argon_m_cost, hf_argon_para), 32, Argon2.TypeIdentifiers.ARGON2I, Argon2.VersionIdentifiers.VERSION_13);
        //TODO -> REGISTER HASHER AND ENVIROMENT
        try {
            messageDigest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            //TODO -> CATCH OLD PHONES WITHOUT SHA-512
        }

        Thread.setDefaultUncaughtExceptionHandler(this);

        forceStop = false;
        doCycle();
    }

    public void doCycle() {
        //TODO -> GENERATE NONCE
        while (active && !forceStop) {
            if (forceStop)
                return;
            if (doPause || !doMine) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            //TODO -> GENERATE ARGON2 HASH
            String base = nonce.getNonce();
            EncodedArgon2Result result = argon2.argon2_hash(base.getBytes());
            String encoded = result.getEncoded();

            /*
            System.out.println("================================================================");
            System.out.println("Generated: "+encoded+" ->HASHER "+this+" ");
            System.out.println("Nonce RAWR: "+nonce.getNonce()+" ->R "+nonce.getNonceRaw()+" ");
            System.out.println("================================================================");
            */

            //TODO -> LOOP ARGON2 HASH + NONCE 5 TIMES WITH SHA512
            String hash = base + encoded;
            byte[] hashBytes = null;
            hashBytes = messageDigest.digest(hash.getBytes());
            for (int i = 0; i < 5; i++) {
                hashBytes = messageDigest.digest(hashBytes);
            }

            //TODO -> GET DL FROM BYES
            StringBuilder duration = new StringBuilder(25);
            duration.append(hashBytes[10] & 0xFF).append(hashBytes[15] & 0xFF).append(hashBytes[20] & 0xFF)
                    .append(hashBytes[23] & 0xFF).append(hashBytes[31] & 0xFF).append(hashBytes[40] & 0xFF)
                    .append(hashBytes[45] & 0xFF).append(hashBytes[55] & 0xFF);


            long finalDuration = new BigInteger(duration.toString()).divide(new BigInteger(difficulty + "")).longValue();

            ArionumMiner.getInstance().setOverallHashes(ArionumMiner.getOverallHashes() + 1);
            String type = "CPU";
            if (hf_argon_m_cost < 500000)
                type = "GPU";
            ArionumMiner.getInstance().getCallback().onDLChange(finalDuration, type);
            //TODO -> MAKE SOLO MINER
            String signature = Base58.generateSoloSignature(nonce.getNonceRaw(), encoded, finalDuration, difficulty, height, neededDL, ArionumMiner.getInstance().getCallback());

            if (finalDuration <= neededDL) {
                System.out.println("NONCE: " + nonce.getNonce());
                System.out.println("ENCODED: " + encoded);
                minerInstance.submitShare(nonce.getNonceRaw(), encoded, finalDuration, difficulty, height);
                refreshNonce();
            }

        }
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();
        active = false;
    }

    public void refreshNonce() {
        nonce = new Nonce(getPool_key(), data, difficulty + "", 32);
    }

    //TODO -> UPDATE HASHER
    public void updateHasher(String data, String pool_key, long difficulty, long neededDL, long height, boolean doMine, int hf_argon_t_cost, int hf_argon_m_cost, int hf_argon_para) {
        this.data = data;
        this.difficulty = difficulty;
        this.neededDL = neededDL;
        this.pool_key = pool_key;

        this.doMine = doMine;

        if (!(this.hf_argon_m_cost == hf_argon_m_cost && this.hf_argon_para == hf_argon_para && this.hf_argon_t_cost == hf_argon_t_cost)) {
            //TODO-> SET SECURITY PARAMS
            argon2 = new Argon2(new SecurityParameters(hf_argon_t_cost, hf_argon_m_cost, hf_argon_para), 32, Argon2.TypeIdentifiers.ARGON2I, Argon2.VersionIdentifiers.VERSION_13);
        }

        this.hf_argon_t_cost = hf_argon_t_cost;
        this.hf_argon_m_cost = hf_argon_m_cost;
        this.hf_argon_para = hf_argon_para;

        if (this.height != height) {
            this.height = height;
            refreshNonce();
        }

        this.height = height;
    }

    public void setForceStop(boolean forceStop) {
        this.forceStop = forceStop;
    }

    public String getPool_key() {
        return pool_key;
    }


    public void doPause(boolean doPause) {
        this.doPause = doPause;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isInitiated() {
        return isInitiated;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();

        ActivityManager activityManager = (ActivityManager) HomeView.instance.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        System.out.println("HASHER " + this + " FAILED FOR " + throwable.getMessage());
        System.out.println("HASHER MEMORY: " + (memoryInfo.availMem / 0x100000L));

        active = false;
        forceStop = true;

        boolean contains = ArionumMiner.getInstance().hashers.contains(this);

        ArionumMiner.getInstance().removeHasher(this);
        if (!throwable.getMessage().contains("Memory allocation error") && contains)
            ArionumMiner.getInstance().createHasher(data, pool_key, difficulty, neededDL, height, doMine, hf_argon_t_cost, hf_argon_m_cost, hf_argon_para);
    }
}
