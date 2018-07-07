package arionum.net.cubedpixels.miner;

import java.security.SecureRandom;

public class Nonce {

    private String nonce;
    private String nonceRaw;
    private byte[] nonceBYTE = new byte[16];

    public Nonce(String publicKey, String data, String difficultyString, int length) {

        System.out.println("data " + data);
        System.out.println("publicKey " + publicKey);
        System.out.println("difficultyString " + difficultyString);

        SecureRandom random = new SecureRandom();
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
        hashBase.append(publicKey).append("-");
        hashBase.append(nonceSb).append("-");
        hashBase.append(data).append("-");
        hashBase.append(difficultyString);

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
