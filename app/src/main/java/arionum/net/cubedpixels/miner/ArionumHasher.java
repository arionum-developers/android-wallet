package arionum.net.cubedpixels.miner;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.wuthoehle.argon2jni.Argon2;
import de.wuthoehle.argon2jni.EncodedArgon2Result;

public class ArionumHasher implements Thread.UncaughtExceptionHandler {

    private boolean active = false;
    private boolean forceStop = false;
    private boolean paused = false;
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

    private ArionumMiner minerInstance;

    public ArionumHasher() {
        minerInstance = ArionumMiner.getInstance();
    }

    public void initiate() {
        System.out.println("Starting Hasher -> " + this);
        //TODO -> SETUP ARGON2
        argon2 = new Argon2(Argon2.SecurityParameterTemplates.OFFICIAL_DEFAULT, 32, Argon2.TypeIdentifiers.ARGON2I, Argon2.VersionIdentifiers.VERSION_13);
        //TODO -> REGISTER HASHER AND ENVIROMENT
        try {
            messageDigest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            //TODO -> CATCH OLD PHONES WITHOUT SHA-512
        }

        Thread.setDefaultUncaughtExceptionHandler(this);

        active = true;
        forceStop = false;
        doCycle();
    }

    public void doCycle() {
        //TODO -> GENERATE NONCE

        while (active && !forceStop) {
            if (forceStop)
                return;
            if (doPause) {
                paused = true;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.runFinalization();
                Runtime.getRuntime().gc();
                System.gc();
                continue;
            } else paused = false;

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
            ArionumMiner.getInstance().getCallback().onDLChange(finalDuration);

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
    public void updateHasher(String data, String pool_key, long difficulty, long neededDL, long height) {
        this.data = data;
        this.difficulty = difficulty;
        this.neededDL = neededDL;
        this.pool_key = pool_key;


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

    public boolean isPaused() {
        return paused;
    }

    public void doPause(boolean doPause) {
        this.doPause = doPause;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        throwable.printStackTrace();

        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();

        System.out.println("HASHER " + this + " FAILED FOR " + throwable.getMessage());
        doCycle();
    }
}
