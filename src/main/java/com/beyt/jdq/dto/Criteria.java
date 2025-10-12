package com.beyt.jdq.dto;


import com.beyt.jdq.dto.enums.CriteriaOperator;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by tdilber at 24-Aug-19
 */
public class Criteria implements Serializable {
    protected String key;
    protected CriteriaOperator operation;
    protected List<Object> values;

    public static Criteria of(String key, CriteriaOperator operation, Collection<Object> values) {
        return new Criteria(key, operation, values);
    }

    public static Criteria of(String key, CriteriaOperator operation, Object... values) {
        return new Criteria(key, operation, values);
    }

    public Criteria(String key, CriteriaOperator operation, Object... values) {
        this.key = key;
        this.operation = operation;
        this.values = values != null ? Arrays.asList(values) : null;
    }

    public static Criteria OR() {
        return Criteria.of("", CriteriaOperator.OR);
    }

    public Criteria() {

    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public CriteriaOperator getOperation() {
        return operation;
    }

    public void setOperation(CriteriaOperator operation) {
        this.operation = operation;
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("key: " + key + " Criteria Operation: " + operation.name() + " ");
        for (int i = 0; i < values.size(); i++) {
            result.append("value").append(i).append(": ").append(values.get(i));
        }
        return result.toString();
    }
}
