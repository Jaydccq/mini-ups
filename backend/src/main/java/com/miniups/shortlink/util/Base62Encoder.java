package com.miniups.shortlink.util;

public final class Base62Encoder {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int BASE = ALPHABET.length;

    private Base62Encoder() {
    }

    public static String encode(long value) {
        if (value == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        long current = value;
        while (current > 0) {
            int index = (int) (current % BASE);
            sb.append(ALPHABET[index]);
            current /= BASE;
        }
        return sb.reverse().toString();
    }
}

