package com.programmerdan.arionum.arionum_miner;

public enum AdvMode {

    standard(true),
    legacy(false),
    experimental(false),
    basic(false),
    stable(false),
    auto(false),
    gpu(false),
    mixed(false);

    boolean use;

    AdvMode(boolean use) {
        this.use = use;
    }

    public boolean useThis() {
        return use;
    }
}
