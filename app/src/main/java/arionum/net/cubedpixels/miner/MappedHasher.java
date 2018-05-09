package arionum.net.cubedpixels.miner;


import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;

import de.wuthoehle.argon2jni.Argon2;
import de.wuthoehle.argon2jni.EncodedArgon2Result;

public class MappedHasher extends Hasher {



    private final Argon2 context;
    private SecureRandom random = new SecureRandom();
    private Nonce currentNonce;
    private Miner.callbackMiner caller;

    public MappedHasher(Miner parent, String id, long target, long maxTime) {
        super(parent, id, target, maxTime);
        context = new Argon2(Argon2.SecurityParameterTemplates.OFFICIAL_DEFAULT, 32, Argon2.TypeIdentifiers.ARGON2I, Argon2.VersionIdentifiers.VERSION_13);
    }

    private ArrayList<Nonce> nonces = new ArrayList<>();
    private ArrayList<Share> sharePool = new ArrayList<>();

    @Override
    public void newHeight(long oldBlockHeight, long newBlockHeight) {

    }


    int argos = 0;
    private byte[] temporaryHashBuffer;

    public Nonce getCurrentNonce() {
        return currentNonce;
    }

    @Override
    public void update(BigInteger difficulty, String data, long limit, String publicKey, long blockHeight, Miner.callbackMiner caller) {
        if (this.limit != limit) {
            for (Share s : sharePool) {
                if (s.getDuration() < this.limit) {
                    System.out.println("SUBMITTING!!");
                    parent.submit(s.getRawNonce(), s.getArgonHash() + "SHAREPOOL", s.getDuration(), this.difficulty.longValue(), this.getType());
                    if (s.getDuration() <= 240) {
                        finds++;
                        caller.onFind(s.getDuration() + "//SHARE POOL");
                        System.out.println("FOUND +1 = FOUND");
                    } else {
                        shares++;
                        System.out.println("FOUND +1 = SHARE");
                        caller.onShare(s.getDuration() + "//SHARE POOL");
                    }
                    argos = 0;
                }
            }
        }
        super.update(difficulty, data, limit, publicKey, blockHeight, caller);
        this.caller = caller;
    }

    private Nonce genNonce() {
        Nonce nonce = new Nonce(32);
        currentNonce = nonce;
        return nonce;
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
                    //GENERATE NONCEPOOL
                    if (nonces.size() <= 0) {
                        for (int i = 0; i < 15; i++) {
                            nonces.add(genNonce());
                        }
                    }

                    //GET FIRST NONCE OF NONCEPOOL
                    Nonce nonce = nonces.get(0);

                    statArgonBegin = System.nanoTime();
                    String base = nonce.getNonce();

                    EncodedArgon2Result result = context.argon2_hash(base.getBytes());
                    argos++;

                    String hash = result.getEncoded();
                    String hashed_done = base + hash;

                    temporaryHashBuffer = hashed_done.getBytes();
                    statShaBegin = System.nanoTime();

                    byteBase = sha512.digest(temporaryHashBuffer);
                    for (int i = 0; i < 5; i++) {
                        byteBase = sha512.digest(byteBase);
                    }
                    statShaEnd = System.nanoTime();

                    StringBuilder duration = new StringBuilder(25);
                    duration.append(byteBase[10] & 0xFF).append(byteBase[15] & 0xFF).append(byteBase[20] & 0xFF)
                            .append(byteBase[23] & 0xFF).append(byteBase[31] & 0xFF).append(byteBase[40] & 0xFF)
                            .append(byteBase[45] & 0xFF).append(byteBase[55] & 0xFF);


                    long finalDuration = new BigInteger(duration.toString()).divide(this.difficulty).longValue();

                    if (finalDuration > 4000000)
                        nonces.remove(nonce);
                    else
                        sharePool.add(new Share(nonce.getNonceRaw(), hash, difficulty.longValue(), finalDuration));

                    if (finalDuration < Miner.finalDuration) {
                        Miner.finalDuration = finalDuration;
                        caller.onDLChange(this.parent.speed(), finalDuration);
                    }

                    Miner.limitDuration = this.limit;
                    caller.onDurChange(finalDuration + "");

                    if (finalDuration <= this.limit) {
                        System.out.println("SUBMITTING!!");
                        parent.submit(nonce.getNonceRaw(), hash, finalDuration, this.difficulty.longValue(), this.getType());
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
                    }
                    if (argos > 135) {
                        argos = 0;
                        System.out.println("RECREATE");
                        doLoop = false;
                        this.hashEnd = System.currentTimeMillis();
                        this.hashTime = this.hashEnd - this.hashBegin;
                        this.hashBegin = System.currentTimeMillis();
                        completeSession();
                        this.loopTime = 0l;
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
                        doLoop = false;
                    } else {
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

    public static class Share {
        private long duration;
        private long difficulty;
        private String rawNonce;
        private String argonHash;

        public Share(String rawNonce, String argonHash, long difficulty, long duration) {
            this.duration = duration;
            this.rawNonce = rawNonce;
            this.argonHash = argonHash;
            this.difficulty = difficulty;
        }

        public long getDifficulty() {
            return difficulty;
        }

        public long getDuration() {
            return duration;
        }

        public String getArgonHash() {
            return argonHash;
        }

        public String getRawNonce() {
            return rawNonce;
        }
    }

    public class Nonce {
        private String nonce;
        private String nonceRaw;
        private byte[] nonceBYTE = new byte[16];

        public Nonce(int length) {
            if (caller != null)
                caller.onDurChange("Generating Nonce...");
            String encNonce = null;
            StringBuilder hashBase;
            random.nextBytes(nonceBYTE);
            encNonce = new String(android.util.Base64.encode(nonceBYTE, android.util.Base64.DEFAULT));
            char[] nonceChar = encNonce.toCharArray();
            StringBuilder nonceSb = new StringBuilder(encNonce.length());

            for (char ar : nonceChar) {
                if (ar >= '0' && ar <= '9' || ar >= 'a' && ar <= 'z' || ar >= 'A' && ar <= 'Z') {
                    nonceSb.append(ar);
                }
            }

            hashBase = new StringBuilder(length);
            hashBase.append(MappedHasher.this.publicKey).append("-");
            hashBase.append(nonceSb).append("-");
            hashBase.append(MappedHasher.this.data).append("-");
            hashBase.append(MappedHasher.this.difficultyString);

            nonce = hashBase.toString();
            nonceRaw = nonceSb.toString();
        }

        public byte[] getNonceBYTE() {
            return nonceBYTE;
        }

        public String getNonce() {
            return nonce;
        }

        public String getNonceRaw() {
            return nonceRaw;
        }
    }
}