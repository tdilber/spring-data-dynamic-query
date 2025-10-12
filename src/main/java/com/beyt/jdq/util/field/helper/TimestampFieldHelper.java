package com.beyt.jdq.util.field.helper;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.sql.Timestamp;

/**
 * Created by tdilber at 11/17/2020
 */
public class TimestampFieldHelper implements IFieldHelper<Timestamp> {
    @Override
    public Timestamp fillRandom() {
        return new Timestamp(System.currentTimeMillis() + random.nextInt(1000000000));
    }

    @Override
    public Timestamp fillValue(String value) {
        DateFormat dateFormat = new SimpleDateFormat();
        try {
            return new Timestamp(dateFormat.parse(value).getTime());
        } catch (ParseException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public String createGeneratorCode(String value) {
        return "new Timestamp(" + fillValue(value).getTime() + "L)";
    }
}
