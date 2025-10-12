package com.beyt.jdq.util.field.helper;


/**
 * Created by tdilber at 11/17/2020
 */
public class IntegerFieldHelper implements IFieldHelper<Integer> {
    @Override
    public Integer fillRandom() {
        return random.nextInt();
    }

    @Override
    public Integer fillValue(String value) {
        return Integer.parseInt(value);
    }

    @Override
    public String createGeneratorCode(String value) {
        return value;
    }
}
