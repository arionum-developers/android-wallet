package com.programmerdan.arionum.arionum_miner;


import android.util.Base64;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import at.gadermaier.argon2.Argon2;
import at.gadermaier.argon2.Argon2Factory;
import at.gadermaier.argon2.model.Argon2Type;

public class MappedHasher extends Hasher {

	/*Local Hasher vars*/

    private final Argon2 context;

    private final int iterations = 1;
    private final int parallelism = 1;
    private final int hashLen = 32;
    private byte[] encoded;
    private SecureRandom random = new SecureRandom();
    private Random insRandom = new Random(random.nextLong());
    private String rawHashBase;
    private byte[] nonce = new byte[32];
    private byte[] salt = new byte[16];
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

        context = Argon2Factory.create();
        context.setClearMemory(true);
        context.setSalt(salt);
        context.setSecret(null);
        context.setAdditional(null);
        context.setIterations(iterations);
        context.setParallelism(parallelism);
        context.setPassword(m_hashBaseBuffer);
        context.setOutputLength(hashLen);
        context.setType(Argon2Type.Argon2i);
        String hash = context.hash();
        encoded = new byte[hash.length()];
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

    private void genNonce() {
        insRandom = new Random(random.nextLong());
        String encNonce = null;
        StringBuilder hashBase;
        random.nextBytes(nonce);

        encNonce = android.util.Base64.encodeToString(nonce, android.util.Base64.DEFAULT);

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
        context.setClearMemory(true);

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
            System.exit(1);
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
                    random.nextBytes(salt);
                    statArgonBegin = System.nanoTime();

					/*argonlib.argon2i_hash_encoded(iterations, memory, parallelism, hashBaseBuffer, hashBaseBufferSize, salt,
							saltLen, hashLen, encoded, encLen); // refactor saves like 30,000-200,000 ns per hash // 34.2 ms
					*/
                    // -- 34,200,000 ns
                    // set argon params in context..
                    context.setOutputLength(32);
                    context.setSalt(salt);
                    context.setType(Argon2Type.Argon2i);
                    context.setPassword(m_hashBaseBuffer);

                    String hash = context.hash();


                    //byte[] method1 = new byte[encoded.length];
                    //System.arraycopy(encoded, 0, method1, 0, encoded.length);

                    //argonlib.argon2i_hash_encoded(iterations, memory, parallelism, hashBaseBuffer, hashBaseBufferSize, salt,
                    //		saltLen, hashLen, encoded, encLen); // refactor saves like 30,000-200,000 ns per hash // 34.2 ms

                    statArgonEnd = System.nanoTime();


                    String hashed_done = hashBase_Done + "$argon2i$v=19$m=524288,t=1,p=1$" + Base64.encodeToString(salt, Base64.NO_CLOSE).replace("=", "") + "$" +
                            Base64.encodeToString(hash.getBytes(), Base64.NO_CLOSE).replace("=", "");


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

                    // 385 ns for duration

                    if (finalDuration <= this.limit) {
                        caller.onShare(finalDuration + "");
                        System.out.println("SUBMITTING!!");
                        parent.submit(rawNonce, hashed_done, finalDuration, this.difficulty.longValue(), this.getType(), this.blockHeight);
                        if (finalDuration <= 240) {
                            finds++;
                            caller.onFind(finalDuration + "");
                            System.out.println("FOUND +1 = FOUND");
                        } else {
                            shares++;
                            System.out.println("FOUND +1 = SHARE");
                            caller.onShare(finalDuration + "");
                        }
                        genNonce(); // only gen a new nonce once we exhaust the one we had
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
                    if (!bound) {
                        //System.out.println("Ending worker " + this.id);
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
        this.hashEnd = System.currentTimeMillis();
        this.hashTime = this.hashEnd - this.hashBegin;
        this.parent.hasherCount.decrementAndGet();
    }


    public String getType() {
        return "CPU";
    }
}
