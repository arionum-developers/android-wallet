package arionum.net.cubedpixels.miner;

public class Report {
    long reportTime = System.currentTimeMillis();

    long streams;

    long runs;

    long hashes;

    double hashPerRun;

    double curHashPerSecond;
    double curTimeInCore;
    double curWaitLoss;

    double shaEff;
    double argonEff;

    long totalTime;
    long argonTime;
    long nonArgontime;
    long shaTime;
    long shares;
    long finds;
    long rejects;

}
