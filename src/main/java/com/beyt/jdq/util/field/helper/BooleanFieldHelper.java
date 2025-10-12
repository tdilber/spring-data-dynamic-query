package com.beyt.jdq.util.field.helper;


/**
 * Created by tdilber at 11/17/2020
 */
public class BooleanFieldHelper implements IFieldHelper<Boolean> {
    @Override
    public Boolean fillRandom() {
        return random.nextBoolean();
    }

    @Override
    public Boolean fillValue(String value) {
        return Boolean.parseBoolean(value);
    }

    @Override
    public String createGeneratorCode(String value) {
        return value.toLowerCase();
    }
}
