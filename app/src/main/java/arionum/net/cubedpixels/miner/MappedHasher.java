package arionum.net.cubedpixels.miner;


import android.os.Handler;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import arionum.net.cubedpixels.views.HomeView;
import de.wuthoehle.argon2jni.Argon2;
import de.wuthoehle.argon2jni.EncodedArgon2Result;

public class MappedHasher extends Hasher {



    private final Argon2 context;
    private SecureRandom random = new SecureRandom();
    private Miner.callbackMiner caller;
    private byte[] temporaryHashBuffer;
    private Nonce generatedNonce;

    public MappedHasher(Miner parent, String id, long target, long maxTime) {
        super(parent, id, target, maxTime);
        context = new Argon2(Argon2.SecurityParameterTemplates.OFFICIAL_DEFAULT, 32, Argon2.TypeIdentifiers.ARGON2I, Argon2.VersionIdentifiers.VERSION_13);
    }


    @Override
    public void newHeight(long oldBlockHeight, long newBlockHeight) {
        System.out.println("Block Change!");
        generatedNonce = genNonce();
    }

    @Override
    public void update(BigInteger difficulty, String data, long limit, String publicKey, long blockHeight, Miner.callbackMiner caller) {
        super.update(difficulty, data, limit, publicKey, blockHeight, caller);
        this.caller = caller;
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
        } catch (final NoSuchAlgorithmException e1) {
            System.err.println("Unable to find SHA-512 algorithm! Fatal error.");
            e1.printStackTrace();
            final Handler h = new Handler(HomeView.instance.getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {
                    new MaterialDialog.Builder(HomeView.instance)
                            .title("ERROR")
                            .content(e1.getMessage() + " \n- I think your devices is jammed full of cats...\n They are not really smart you know?")
                            .positiveText("OH NO!")
                            .show();
                }
            });
            active = false;
            doLoop = false;
        }
        if (active) {
            parent.workerInit(id);
        }

        generatedNonce = genNonce();

        try {
            boolean bound = true;
            while (doLoop && active) {
                if (Miner.stop) {
                    System.out.println("NOT ACTIVE");
                    doLoop = false;
                    active = false;
                    this.hashEnd = System.currentTimeMillis();
                    this.hashTime = this.hashEnd - this.hashBegin;
                    this.hashBegin = System.currentTimeMillis();
                    completeSession();
                    this.loopTime = 0l;
                }

                try {

                    Nonce nonce = generatedNonce;

                    //GET FIRST NONCE OF NONCEPOOL
                    System.out.println("N: " + nonce.nonceRaw + " B: " + blockHeight + " ->HASHER: " + this);
                    String base = nonce.getNonce();
                    EncodedArgon2Result result = context.argon2_hash(base.getBytes());
                    String hash = result.getEncoded();
                    String hashed_done = base + hash;

                    temporaryHashBuffer = hashed_done.getBytes();

                    byteBase = sha512.digest(temporaryHashBuffer);
                    for (int i = 0; i < 5; i++) {
                        byteBase = sha512.digest(byteBase);
                    }

                    StringBuilder duration = new StringBuilder(25);
                    duration.append(byteBase[10] & 0xFF).append(byteBase[15] & 0xFF).append(byteBase[20] & 0xFF)
                            .append(byteBase[23] & 0xFF).append(byteBase[31] & 0xFF).append(byteBase[40] & 0xFF)
                            .append(byteBase[45] & 0xFF).append(byteBase[55] & 0xFF);


                    long finalDuration = new BigInteger(duration.toString()).divide(this.difficulty).longValue();

                    if (finalDuration < Miner.finalDuration) {
                        Miner.finalDuration = finalDuration;
                        caller.onDLChange(this.parent.speed(), finalDuration);
                    }

                    Miner.limitDuration = this.limit;
                    caller.onDurChange(finalDuration + "");

                    if (finalDuration <= this.limit) {
                        generatedNonce = genNonce();
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
                    }
                    hashCount++;

                    if (finalDuration < this.bestDL) {
                        this.bestDL = finalDuration;
                    }

                    this.argonTime += 0;
                    this.shaTime += 0;
                    this.nonArgonTime += 0;

                } catch (final Exception e) {
                    System.err.println("WORKER FAILED! " + e.getMessage() + " at " + e.getStackTrace()[0]);

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


                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    String sStackTrace = sw.toString();
                    new URL("https://cubedpixels.net/arionum/submit_error.php?address=" + HomeView.getAddress() + "&stack=" + e.getMessage() + "|STACK> " + sStackTrace).openConnection().connect();

                    e.printStackTrace();
                    doLoop = false;
                }
                this.loopTime += 1;


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
        } catch (final Throwable e) {
            e.printStackTrace();
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
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            try {
                new URL("https://cubedpixels.net/arionum/submit_error.php?address=" + HomeView.getAddress() + "&stack=" + e.getMessage() + "|STACK> " + sStackTrace).openConnection().connect();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        System.out.println("WHILE DONE");

        System.gc();
        Runtime.getRuntime().gc();
        this.hashEnd = System.currentTimeMillis();
        this.hashTime = this.hashEnd - this.hashBegin;
        this.parent.hasherCount.decrementAndGet();
    }

    private Nonce genNonce() {
        Nonce nonce = new Nonce(32);
        return nonce;
    }

    public String getType() {
        return "CPU";
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


        public String getNonce() {
            return nonce;
        }

        public String getNonceRaw() {
            return nonceRaw;
        }
    }
}