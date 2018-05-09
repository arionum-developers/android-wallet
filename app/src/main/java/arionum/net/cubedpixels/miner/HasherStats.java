package arionum.net.cubedpixels.miner;

public class HasherStats {
    public long argonTime;
    public long shaTime;
    public long nonArgonTime;
    public long hashes;
    public long bestDL;
    public long shares;
    public long finds;
    public long hashTime;
    public long scheduledTime;
    public String id;
    public String type;

    public HasherStats(String id, long argonTime, long shaTime, long nonArgonTime, long hashTime, long hashes, long bestDL, long shares, long finds, String type) {
        this.id = id;
        this.argonTime = argonTime;
        this.shaTime = shaTime;
        this.nonArgonTime = nonArgonTime;
        this.hashTime = hashTime;
        this.hashes = hashes;
        this.bestDL = bestDL;
        this.shares = shares;
        this.finds = finds;
        this.type = type;
    }
}
