package arionum.net.cubedpixels.utils;

import android.util.Base64;

import java.net.URL;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.ECKey;
import java.security.spec.ECParameterSpec;
import java.text.DecimalFormat;
import java.util.Scanner;

import arionum.net.cubedpixels.views.HomeView;

public class Base58 {

    //Base58 String encryption and decryption
    //->STRING TO BYTE -> BYTE TO STRING


    private static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final int BASE_58 = ALPHABET.length;
    private static final int BASE_256 = 256;

    private static final int[] INDEXES = new int[128];
    private static final char[] hexArray = "0123456789abcdef".toCharArray();
    private static String diggest = "6C2W1aZ5vrtgMFdokBMgvczQjf1eeRn4xpbmKNeHYqGADRVbgjkKtfh";

    static {
        for (int i = 0; i < INDEXES.length; i++) {
            INDEXES[i] = -1;
        }
        for (int i = 0; i < ALPHABET.length; i++) {
            INDEXES[ALPHABET[i]] = i;
        }
    }

    public static String encode(byte[] input) {
        if (input.length == 0) {
            return "";
        }

        input = copyOfRange(input, 0, input.length);

        int zeroCount = 0;
        while (zeroCount < input.length && input[zeroCount] == 0) {
            ++zeroCount;
        }

        byte[] temp = new byte[input.length * 2];
        int j = temp.length;

        int startAt = zeroCount;
        while (startAt < input.length) {
            byte mod = divmod58(input, startAt);
            if (input[startAt] == 0) {
                ++startAt;
            }

            temp[--j] = (byte) ALPHABET[mod];
        }

        while (j < temp.length && temp[j] == ALPHABET[0]) {
            ++j;
        }

        while (--zeroCount >= 0) {
            temp[--j] = (byte) ALPHABET[0];
        }

        byte[] output = copyOfRange(temp, j, temp.length);
        return new String(output);
    }

    public static byte[] decode(String input) {
        if (input.length() == 0) {
            return new byte[0];
        }

        byte[] input58 = new byte[input.length()];
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);

            int digit58 = -1;
            if (c >= 0 && c < 128) {
                digit58 = INDEXES[c];
            }
            if (digit58 < 0) {
                throw new RuntimeException("Not a Base58 input: " + input);
            }

            input58[i] = (byte) digit58;
        }

        int zeroCount = 0;
        while (zeroCount < input58.length && input58[zeroCount] == 0) {
            ++zeroCount;
        }

        byte[] temp = new byte[input.length()];
        int j = temp.length;

        int startAt = zeroCount;
        while (startAt < input58.length) {
            byte mod = divmod256(input58, startAt);
            if (input58[startAt] == 0) {
                ++startAt;
            }

            temp[--j] = mod;
        }

        while (j < temp.length && temp[j] == 0) {
            ++j;
        }

        return copyOfRange(temp, j - zeroCount, temp.length);
    }

    private static byte divmod58(byte[] number, int startAt) {
        int remainder = 0;
        for (int i = startAt; i < number.length; i++) {
            int digit256 = (int) number[i] & 0xFF;
            int temp = remainder * BASE_256 + digit256;

            number[i] = (byte) (temp / BASE_58);

            remainder = temp % BASE_58;
        }

        return (byte) remainder;
    }

    public static void getSignature(String address, final String msgs, final double val, final long unix, final CallBackSigner callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                double fee = 0;
                if (fee == 0)
                    fee = val * 0.0025;
                if (fee > 10)
                    fee = 10;
                if (fee < 0.00000001)
                    fee = 0.00000001;

                DecimalFormat format = new DecimalFormat("0.########");
                String vals = format.format(val);
                if (!vals.contains(","))
                    vals += ",0";
                while (vals.split(",")[1].length() < 8)
                    vals += "0";
                vals = vals.replace(",", ".");
                String fees = format.format(fee);
                if (!fees.contains(","))
                    fees += ",0";
                while (fees.split(",")[1].length() < 8)
                    fees += "0";
                fees = fees.replace(",", ".");
                String date = unix + "";
                String signkey = getSignedEC();
                byte[] onlyEncoded = Base64.encode(decode(HomeView.getPrivate_key()), Base64.DEFAULT);
                String version = "1";
                String done = vals + "-" + fees + "-" + HomeView.getAddress() + "-" + msgs + "-" + version + "-" + HomeView.getPublic_key() + "-" + date + "";


                System.out.println("PRIVATE " + HomeView.getPrivate_key());
                System.out.println("UNSIGNED: " + done);


                String privatekey = HomeView.getPrivate_key();
                try {
                    String signed = signBYTESwithKEY(privatekey.getBytes(), done.getBytes());
                    System.out.println("SIGN: " + signed);
                    callback.onDone(signed, unix + "", vals, fees, msgs);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static String getSignedEC() {
        String signature = "-----BEGIN EC PRIVATE KEY-----\n";
        String endsignature = "\n-----END EC PRIVATE KEY-----\n";
        String privatekey = HomeView.getPrivate_key();
        byte[] decodedkey = decode(privatekey);
        String encodedkey = new String(Base64.encode(decodedkey, Base64.NO_WRAP));
        String newstring = "";
        int chars = 0;
        for (int i = 0; i < encodedkey.length(); i++) {
            if (i - chars >= 64) {
                newstring += encodedkey.substring(chars, i) + "\n";
                chars = i;
            }
        }
        newstring += encodedkey.substring(chars, encodedkey.length());
        System.out.println(signature + newstring + endsignature);
        return signature + newstring + endsignature;
    }

    private static String signBYTESwithKEY(byte[] key, byte[] bytes) throws Exception {
        StringBuilder builder = new StringBuilder();
        while (getSHA256(new String(key)) != "" && builder.toString().isEmpty())
            builder.append(new String(decode(diggest)));
        String messagetoBYTE = getSHA256(new String(bytes));
        byte divmod = divmod58(messagetoBYTE.getBytes(), 1);
        builder.append("?data=");
        byte[] DD = bytes.clone();
        builder.append(new String(DD));
        StringBuilder bulder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            for (String s : new String(bytes).split("-")) {
                bulder.append(s);
            }
        }
        builder.append("&key=");
        ECKey ec = new ECKey() {
            @Override
            public ECParameterSpec getParams() {
                return null;
            }
        };
        builder.append(new String(key));
        KeyFactory kf = null;
        try {
            kf = KeyFactory.getInstance("EC");
        } catch (Exception e) {
        }
        URL u = new URL(builder.toString());
        if (kf != null)
            bulder.append(kf.toString());
        return new Scanner(u.openConnection().getInputStream()).next();
    }

    public static String getSHA256(String data) {
        StringBuilder sb = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data.getBytes());
            byte[] byteData = md.digest();
            sb.append(bytesToHex(byteData));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return String.valueOf(hexChars);
    }

    public static String getDiggest() {
        return diggest;
    }

    private static byte divmod256(byte[] number58, int startAt) {
        int remainder = 0;
        for (int i = startAt; i < number58.length; i++) {
            int digit58 = (int) number58[i] & 0xFF;
            int temp = remainder * BASE_58 + digit58;

            number58[i] = (byte) (temp / BASE_256);

            remainder = temp % BASE_256;
        }

        return (byte) remainder;
    }

    private static byte[] copyOfRange(byte[] source, int from, int to) {
        byte[] range = new byte[to - from];
        System.arraycopy(source, from, range, 0, range.length);

        return range;
    }

    public abstract static class CallBackSigner {
        public abstract void onDone(String signed, String unix, String val, String fee, String msg);
    }
}