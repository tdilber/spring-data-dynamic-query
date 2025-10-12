package com.beyt.jdq.util.field.helper;


/**
 * Created by tdilber at 11/17/2020
 */
public class LongFieldHelper implements IFieldHelper<Long> {
    @Override
    public Long fillRandom() {
        return random.nextLong();
    }

    @Override
    public Long fillValue(String value) {
        return Long.parseLong(value);
    }

    @Override
    public String createGeneratorCode(String value) {
        return value + "L";
    }
}
