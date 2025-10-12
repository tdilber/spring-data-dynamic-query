package com.beyt.jdq.util.field.helper;


/**
 * Created by tdilber at 11/17/2020
 */
public class DoubleFieldHelper implements IFieldHelper<Double> {
    @Override
    public Double fillRandom() {
        return random.nextDouble();
    }

    @Override
    public Double fillValue(String value) {
        return Double.parseDouble(value);
    }

    @Override
    public String createGeneratorCode(String value) {
        return value + "d";
    }
}
