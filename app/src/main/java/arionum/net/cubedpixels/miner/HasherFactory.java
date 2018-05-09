package arionum.net.cubedpixels.miner;

public class HasherFactory {

    public static Hasher createHasher(AdvMode mode, Miner parent, String id, long lifeTime, long maxSession) {
        switch (mode) {
            case experimental:
                return new MappedHasher(parent, id, lifeTime, maxSession);
            case standard:
                return new MappedHasher(parent, id, lifeTime, maxSession);
            default:
                return new MappedHasher(parent, id, lifeTime, maxSession);
        }
    }
}
