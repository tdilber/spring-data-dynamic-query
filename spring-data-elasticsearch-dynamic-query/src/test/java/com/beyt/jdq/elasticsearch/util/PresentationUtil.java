package com.beyt.jdq.elasticsearch.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PresentationUtil {

    public static void prettyPrint(Object object) {
        try {
            var objectMapper = new ObjectMapper();
            System.out.println("______________________________________________________________________________");
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object));
            System.out.println("========================================================================");
        } catch (JsonProcessingException e) {
            // ignore
        }
    }
}

