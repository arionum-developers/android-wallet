package arionum.net.cubedpixels.miner;

import java.util.Arrays;

public class Profile implements Comparable<Profile> {
    AdvMode[] workerTypes;
    long hashes;
    long sampleBegin;
    long samplePlannedEnd;
    long sampleTime;
    double rate;
    double TiC;

    public Profile(int workers) {
        workerTypes = new AdvMode[workers];
        hashes = 0l;
        sampleTime = 0;
        rate = 0.0d;
        this.sampleBegin = 0l;
        this.samplePlannedEnd = 0l;
    }

    public Profile(AdvMode... workers) {
        workerTypes = workers;
        hashes = 0l;
        sampleTime = 0;
        rate = 0.0d;
        this.sampleBegin = 0;
        this.samplePlannedEnd = 0;
    }

    public void begin() {
        long now = System.currentTimeMillis();
        this.sampleBegin = now + Miner.INIT_DELAY;
        this.samplePlannedEnd = this.sampleBegin + Miner.TEST_PERIOD;
    }

    public Status getStatus() {
        if (this.sampleBegin == 0) {
            return Status.QUEUED;
        }
        long now = System.currentTimeMillis();
        if (now < this.sampleBegin) {
            return Status.WAITING;
        } else if (now > this.samplePlannedEnd) {
            return Status.DONE;
        } else {
            return Status.PROFILING;
        }
    }

    public AdvMode[] getWorkers() {
        return workerTypes;
    }

    public void register(int idx, AdvMode worker) {
        this.workerTypes[idx] = worker;
    }

    public void update(long hashes, long time) {
        this.hashes += hashes;
        if (time > this.sampleTime)
            this.sampleTime = time;
    }

    public double getHashSec() {
        return getRawRate() / 1000d;
    }

    public double getRawRate() {
        /*long accumulate = 0l;
		for (long hash : hashes) {
			accumulate += hash;
		}*/
        if (this.sampleTime > 0) {
            rate = (double) hashes / (double) this.sampleTime;
        } else {
            rate = 0d;
        }
        return rate;
    }

    public long getHashes() {
        return hashes;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Profile) {
            Profile p = (Profile) o;
            return p == this || (Arrays.deepEquals(p.workerTypes, this.workerTypes));
        }

        return false;
    }

    @Override
    public int compareTo(Profile o) {
        return Double.compare(this.getRawRate(), o.getRawRate());
    }

    enum Status {
        QUEUED,
        WAITING,
        PROFILING,
        DONE
    }
}
