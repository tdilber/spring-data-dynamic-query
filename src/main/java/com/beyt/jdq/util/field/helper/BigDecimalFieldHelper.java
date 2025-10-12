package com.beyt.jdq.util.field.helper;


import java.math.BigDecimal;

/**
 * Created by tdilber at 8/18/2024
 */
public class BigDecimalFieldHelper implements IFieldHelper<BigDecimal> {
    @Override
    public BigDecimal fillRandom() {
        return BigDecimal.valueOf(random.nextDouble());
    }

    @Override
    public BigDecimal fillValue(String value) {
        return new BigDecimal(value);
    }

    @Override
    public String createGeneratorCode(String value) {
        return "new BigDecimal(\"" + value + "\");";
    }
}
