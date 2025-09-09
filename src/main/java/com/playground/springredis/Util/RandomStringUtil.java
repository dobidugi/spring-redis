package com.playground.springredis.Util;

import org.springframework.stereotype.Component;

@Component
public class RandomStringUtil {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int DEFAULT_LENGTH = 10;

    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int) (CHARACTERS.length() * Math.random());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    public String generate() {
        return generate(DEFAULT_LENGTH);
    }
}
