package com.programmerdan.arionum.arionum_miner;


import android.app.ActivityManager;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import arionum.net.cubedpixels.views.HomeView;
import de.wuthoehle.argon2jni.Argon2;
import de.wuthoehle.argon2jni.EncodedArgon2Result;

import static android.content.Context.ACTIVITY_SERVICE;

public class MappedHasher extends Hasher {

    /*Local Hasher vars*/

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456789";
    static SecureRandom rnd = new SecureRandom();
    private final Argon2 context;
    private final int iterations = 1;
    private final int parallelism = 1;
    private final int hashLen = 32;
    private SecureRandom random = new SecureRandom();
    private Random insRandom = new Random(random.nextLong());
    private String rawHashBase;
    private byte[] nonce = new byte[32];
    private String rawNonce;
    private byte[] hashBaseBuffer;
    private byte[] m_hashBaseBuffer;
    private byte[] fullHashBaseBuffer;
    private Miner.callbackMiner caller;
    private String hashBase_Done = "";
    private int hashLENGTH = 0;
    private int salt_length = 16;

    public MappedHasher(Miner parent, String id, long target, long maxTime) {
        super(parent, id, target, maxTime);
        genNonce();
        // SET UP ARGON FOR DIRECT-TO-JNA-WRAPPER-EXEC

        context = new Argon2(Argon2.SecurityParameterTemplates.OFFICIAL_DEFAULT, 32, Argon2.TypeIdentifiers.ARGON2I, Argon2.VersionIdentifiers.VERSION_13);
    }

    @Override
    public void update(BigInteger difficulty, String data, long limit, String publicKey, long blockHeight, Miner.callbackMiner caller) {
        super.update(difficulty, data, limit, publicKey, blockHeight, caller);
        this.caller = caller;
        genNonce();
    }

    @Override
    public void newHeight(long oldBlockHeight, long newBlockHeight) {
        // no-op, we are locked into 10800 > territory now
    }

    int argos = 0;

    String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    private ActivityManager.MemoryInfo getAvailableMemory() {
        ActivityManager activityManager = (ActivityManager) HomeView.instance.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    private void genNonce() {
        if (caller != null)
            caller.onDurChange("Generating Nonce...");
        insRandom = new Random(random.nextLong());
        String encNonce = null;
        StringBuilder hashBase;
        random.nextBytes(nonce);

        encNonce = new String(android.util.Base64.encode(nonce, android.util.Base64.DEFAULT));

        char[] nonceChar = encNonce.toCharArray();

        // shaves a bit off vs regex -- for this operation, about 50% savings
        StringBuilder nonceSb = new StringBuilder(encNonce.length());
        for (char ar : nonceChar) {
            if (ar >= '0' && ar <= '9' || ar >= 'a' && ar <= 'z' || ar >= 'A' && ar <= 'Z') {
                nonceSb.append(ar);
            }
        }

        // prealloc probably saves us 10% on this op sequence
        hashBase = new StringBuilder(hashBufferSize); // size of key + nonce + difficult + argon + data + spacers
        hashBase.append(this.publicKey).append("-");
        hashBase.append(nonceSb).append("-");
        hashBase.append(this.data).append("-");
        hashBase.append(this.difficultyString);

        hashBase_Done = hashBase.toString();

        rawNonce = nonceSb.toString();
        rawHashBase = hashBase.toString();

        hashBaseBuffer = rawHashBase.getBytes();
        hashLENGTH = hashBaseBuffer.length;
        m_hashBaseBuffer = hashBaseBuffer;
        fullHashBaseBuffer = new byte[hashBaseBuffer.length + 32];
        System.arraycopy(hashBaseBuffer, 0, fullHashBaseBuffer, 0, hashBaseBuffer.length);
    }

    @Override
    public void go() {
        boolean doLoop = true;
        this.hashBegin = System.currentTimeMillis();

        this.parent.hasherCount.getAndIncrement();
        byte[] byteBase = null;

        MessageDigest sha512 = null;
        try {
            sha512 = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e1) {
            System.err.println("Unable to find SHA-512 algorithm! Fatal error.");
            e1.printStackTrace();
            active = false;
            doLoop = false;
        }
        if (active) {
            parent.workerInit(id);
        }

        long statCycle = 0l;
        long statBegin = 0l;
        long statArgonBegin = 0l;
        long statArgonEnd = 0l;
        long statShaBegin = 0l;
        long statShaEnd = 0l;
        long statEnd = 0l;

        try {
            boolean bound = true;
            while (doLoop && active) {
                statCycle = System.currentTimeMillis();
                statBegin = System.nanoTime();
                try {
                    //insRandom.nextBytes(salt); // 47 ns

                    statArgonBegin = System.nanoTime();

                    String base = hashBase_Done;


                    EncodedArgon2Result result = context.argon2_hash(base.getBytes());
                    argos++;

                    String hash = result.getEncoded();



                    String hashed_done = base + hash;

                    fullHashBaseBuffer = hashed_done.getBytes();
                    statShaBegin = System.nanoTime();


                    byteBase = sha512.digest(fullHashBaseBuffer);
                    for (int i = 0; i < 5; i++) {
                        byteBase = sha512.digest(byteBase);
                    }
                    statShaEnd = System.nanoTime();
                    // shas total 4900-5000ns for all 6 digests, or < 1000ns ea

                    StringBuilder duration = new StringBuilder(25);
                    duration.append(byteBase[10] & 0xFF).append(byteBase[15] & 0xFF).append(byteBase[20] & 0xFF)
                            .append(byteBase[23] & 0xFF).append(byteBase[31] & 0xFF).append(byteBase[40] & 0xFF)
                            .append(byteBase[45] & 0xFF).append(byteBase[55] & 0xFF);

                    long finalDuration = new BigInteger(duration.toString()).divide(this.difficulty).longValue();

                    if (Miner.finalDuration >= finalDuration)
                        Miner.finalDuration = finalDuration;
                    Miner.limitDuration = this.limit;

                    caller.onDurChange(finalDuration + "");

                    //SUBMIT:

                    // parent.submit(base, hash, finalDuration, this.difficulty.longValue(), this.getType());

                    // 385 ns for duration

                    if (finalDuration <= this.limit) {
                        Miner.finalDuration = Long.MAX_VALUE;
                        System.out.println("SUBMITTING!!");
                        parent.submit(rawNonce, hash, finalDuration, this.difficulty.longValue(), this.getType());
                        if (finalDuration <= 240) {
                            finds++;
                            caller.onFind(finalDuration + "");
                            System.out.println("FOUND +1 = FOUND");
                        } else {
                            shares++;
                            System.out.println("FOUND +1 = SHARE");
                            caller.onShare(finalDuration + "");
                        }
                        argos = 0;
                        genNonce(); // only gen a new nonce once we exhaust the one we had
                    }
                    if (argos > 300) {
                        argos = 0;
                        genNonce();
                    }
                    hashCount++;
                    statEnd = System.nanoTime();

                    if (finalDuration < this.bestDL) {
                        this.bestDL = finalDuration;
                    }

                    this.argonTime += statArgonEnd - statArgonBegin;
                    this.shaTime += statShaEnd - statShaBegin;
                    this.nonArgonTime += (statArgonBegin - statBegin) + (statEnd - statArgonEnd);


                } catch (Exception e) {
                    System.err.println("WORKER FAILED! " + e.getMessage() + " at " + e.getStackTrace()[0]);
                    e.printStackTrace();
                    doLoop = false;
                }
                this.loopTime += System.currentTimeMillis() - statCycle;

                if (this.hashCount > this.targetHashCount || this.loopTime > this.maxTime) {
                    if (!bound) {                        //System.out.println("Ending worker " + this.id);
                        doLoop = false;
                    } else {
                        //System.out.println("Ending a session for worker " + this.id);
                        this.hashEnd = System.currentTimeMillis();
                        this.hashTime = this.hashEnd - this.hashBegin;
                        this.hashBegin = System.currentTimeMillis();
                        completeSession();
                        this.loopTime = 0l;
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        System.gc();
        Runtime.getRuntime().gc();
        this.hashEnd = System.currentTimeMillis();
        this.hashTime = this.hashEnd - this.hashBegin;
        this.parent.hasherCount.decrementAndGet();
    }


    public String getType() {
        return "CPU";
    }
}