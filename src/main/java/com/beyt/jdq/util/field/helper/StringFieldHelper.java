package com.beyt.jdq.util.field.helper;

import java.util.Random;

/**
 * Created by tdilber at 11/17/2020
 */
public class StringFieldHelper implements IFieldHelper<String> {
    int lastIndex = 0;

    @Override
    public String fillRandom() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 8;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        return generatedString
                + Long.toString(System.currentTimeMillis()).substring(4)
                + (lastIndex++);
    }

    @Override
    public String fillValue(String value) {
        return value;
    }

    @Override
    public String createGeneratorCode(String value) {
        return "\"" + value + "\"";
    }
}
