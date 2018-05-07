package com.programmerdan.arionum.arionum_miner;

import java.math.BigInteger;

public abstract class Hasher implements Runnable {

    protected Miner parent;
    protected boolean active;
    protected String id;
    protected long hashCount;
    protected long targetHashCount;
    protected long hashBegin;
    protected long hashEnd;
    protected long hashTime;
    protected long maxTime;
    protected long blockHeight;
    protected BigInteger difficulty;
    protected String difficultyString;
    protected String data;
    protected int hashBufferSize;
    protected long limit;
    protected String publicKey;
    protected long bestDL;
    protected long shares;
    protected long finds;
    protected long loopTime;
    protected long argonTime;
    protected long shaTime;
    protected long nonArgonTime;

    public Hasher(Miner parent, String id, long target, long maxTime) {
        super();
        this.parent = parent;
        this.id = id;
        this.active = false;
        this.hashCount = 0l;
        this.targetHashCount = target;
        this.maxTime = maxTime;
    }

    public void run() {
        try {
            active = true;
            go();
            active = false;
        } catch (Throwable e) {
            System.err.println("Detected thread " + Thread.currentThread().getName() + " death due to error: " + e.getMessage());
            e.printStackTrace();

            System.err.println("\n\nThis is probably fatal, so exiting now.");
        }
        HasherStats stats = new HasherStats(id, argonTime, shaTime, nonArgonTime, hashTime, hashCount, bestDL, shares, finds, getType());
        parent.workerFinish(stats, this);
    }

    public abstract void go();

    public void completeSession() {
        HasherStats stats = new HasherStats(id, argonTime, shaTime, nonArgonTime, hashTime, hashCount, bestDL, shares, finds, getType());
        argonTime = 0l;
        shaTime = 0l;
        nonArgonTime = 0l;
        hashTime = 0l;
        hashCount = 0l;
        bestDL = Long.MAX_VALUE;
        shares = 0l;
        finds = 0l;
        long[] sessionUpd = parent.sessionFinish(stats, this);
        this.targetHashCount = sessionUpd[0];
        this.maxTime = sessionUpd[1];
    }

    public String getID() {
        return this.id;
    }


    public long getBestDL() {
        return bestDL;
    }

    public long getShares() {
        return shares;
    }

    public long getFinds() {
        return finds;
    }

    public long getArgonTime() {
        return argonTime;
    }

    public long getShaTime() {
        return shaTime;
    }

    public long getNonArgonTime() {
        return nonArgonTime;
    }

    public long getLoopTime() {
        return loopTime;
    }

    public long getHashTime() {
        return this.hashTime;
    }

    public void update(BigInteger difficulty, String data, long limit, String publicKey, long blockHeight, Miner.callbackMiner caller) {
        this.difficulty = difficulty;
        this.difficultyString = difficulty.toString();
        if (!data.equals(this.data)) {
            bestDL = Long.MAX_VALUE;
        }
        this.data = data;
        this.hashBufferSize = 280 + this.data.length();
        this.limit = limit;
        this.publicKey = publicKey;
        if (blockHeight != this.blockHeight) {
            newHeight(this.blockHeight, blockHeight);
        }
        this.blockHeight = blockHeight;
    }

    public abstract void newHeight(long oldBlockHeight, long newBlockHeight);

    public abstract String getType();

    public long getHashes() {
        return this.hashCount;
    }

    public boolean isActive() {
        return active;
    }


}
