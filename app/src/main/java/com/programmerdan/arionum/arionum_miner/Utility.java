package com.programmerdan.arionum.arionum_miner;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.LinkedList;

public class Utility {
    public static String base58_chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    public static byte[] baseConvert(byte[] _source, int sourceBase, int targetBase) {
        LinkedList<Byte> source = new LinkedList<Byte>();
        for (byte sourceByte : _source) {
            source.add(sourceByte);
        }
        LinkedList<Byte> result = new LinkedList<Byte>();

        int count = 0;

        while ((count = source.size()) > 0) {
            LinkedList<Byte> quotient = new LinkedList<Byte>();
            int remainder = 0;
            for (int i = 0; i != count; i++) {
                int accum = source.get(i) + remainder * sourceBase;
                int digit = accum / targetBase;
                remainder = accum % targetBase;
                if (quotient.size() > 0 || digit != 0) {
                    quotient.add((byte) digit);
                }
            }
            result.addFirst((byte) remainder);
            source = quotient;
        }

        byte[] _result = new byte[result.size()];
        int i = 0;
        for (Byte resultByte : result) {
            _result[i] = resultByte;
            i++;
        }

        return _result;
    }

    public static String base58_decode(String data) {
        char[] dat = data.toCharArray();
        byte[] map = new byte[dat.length];
        for (int i = 0; i < dat.length; i++) {
            map[i] = (byte) base58_chars.indexOf(dat[i]);
        }

        byte[] converted = baseConvert(map, 58, 256);
        return new String(converted, Charset.forName("ASCII"));
    }


    public static BigInteger base58_decodeInt(String data) {
        char[] dat = data.toCharArray();
        byte[] map = new byte[dat.length];
        for (int i = 0; i < dat.length; i++) {
            map[i] = (byte) base58_chars.indexOf(dat[i]);
        }

        byte[] converted = baseConvert(map, 58, 10);
        StringBuilder sb = new StringBuilder();
        for (byte a : converted) {
            sb.append(a);
        }
        return new BigInteger(sb.toString());
    }
}
