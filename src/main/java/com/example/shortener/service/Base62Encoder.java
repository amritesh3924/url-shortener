package com.example.shortener.service;

public class Base62Encoder {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    // Base 62 keeps an increasing database ID short while remaining URL-safe.
    public String encode(long value) {
        if (value < 0) throw new IllegalArgumentException("ID cannot be negative");
        if (value == 0) return "0";
        StringBuilder code = new StringBuilder();
        while (value > 0) {
            code.append(ALPHABET.charAt((int) (value % 62)));
            value /= 62;
        }
        return code.reverse().toString();
    }
}
