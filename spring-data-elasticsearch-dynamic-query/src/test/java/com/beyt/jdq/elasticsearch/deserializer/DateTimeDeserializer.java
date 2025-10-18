package com.beyt.jdq.elasticsearch.deserializer;

import com.beyt.jdq.core.deserializer.BasicDeserializer;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class DateTimeDeserializer extends BasicDeserializer {

    @Override
    public <T> T deserialize(Object value, Class<T> clazz) throws Exception {
        if (clazz.isAssignableFrom(Date.class)) {
            if (value instanceof Date date) {
                return (T) date;
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date parsedDate = simpleDateFormat.parse(value.toString());
            return (T) parsedDate;
        }

        if (clazz.isAssignableFrom(Timestamp.class)) {
            if (value instanceof Timestamp date) {
                return (T) date;
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date parsedDate = simpleDateFormat.parse(value.toString());
            return (T) new Timestamp(parsedDate.getTime());
        }



        return super.deserialize(value, clazz);
    }
}

