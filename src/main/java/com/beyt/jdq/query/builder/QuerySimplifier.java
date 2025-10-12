package com.beyt.jdq.query.builder;

import com.beyt.jdq.dto.Criteria;
import com.beyt.jdq.dto.enums.CriteriaOperator;
import com.beyt.jdq.dto.enums.Order;
import com.beyt.jdq.query.DynamicSpecification;

import java.util.Arrays;

public class QuerySimplifier {
    public static final Criteria OR = Criteria.OR();

    public static Field Field(String name) {
        return new Field(name);
    }

    public static Criteria Parantesis(Criteria... criterias) {
        return Criteria.of("", CriteriaOperator.PARENTHES, Arrays.asList(criterias));
    }

    public static OrderByRule OrderBy(String fieldName, Order orderType) {
        return new OrderByRule(fieldName, orderType);
    }

    public static SelectRule Select(String fieldName, String alias) {
        return new SelectRule(fieldName, alias);
    }

    public static SelectRule Select(String fieldName) {
        return new SelectRule(fieldName, DynamicSpecification.getFieldName(fieldName));
    }

    public static class OrderByRule {
        private String fieldName;
        private Order orderType;

        public OrderByRule() {
        }

        public OrderByRule(String fieldName, Order orderType) {
            this.fieldName = fieldName;
            this.orderType = orderType;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public Order getOrderType() {
            return orderType;
        }

        public void setOrderType(Order orderType) {
            this.orderType = orderType;
        }

        @Override
        public String toString() {
            return "OrderByRule{" +
                    "fieldName='" + fieldName + '\'' +
                    ", orderType=" + orderType +
                    '}';
        }
    }

    public static class SelectRule {
        private String fieldName;
        private String alias;

        public SelectRule() {
        }

        public SelectRule(String fieldName, String alias) {
            this.fieldName = fieldName;
            this.alias = alias;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        @Override
        public String toString() {
            return "SelectRule{" +
                    "fieldName='" + fieldName + '\'' +
                    ", alias='" + alias + '\'' +
                    '}';
        }
    }

    public static class Field {
        private String name;

        public Field(String name) {
            this.name = name;
        }

        public Criteria eq(Object... values) {
            return Criteria.of(name, CriteriaOperator.EQUAL, values);
        }

        public Criteria notEq(Object... values) {
            return Criteria.of(name, CriteriaOperator.NOT_EQUAL, values);
        }

        public Criteria contain(Object... values) {
            return Criteria.of(name, CriteriaOperator.CONTAIN, values);
        }

        public Criteria doesNotContain(Object... values) {
            return Criteria.of(name, CriteriaOperator.DOES_NOT_CONTAIN, values);
        }

        public Criteria endWith(Object... values) {
            return Criteria.of(name, CriteriaOperator.END_WITH, values);
        }

        public Criteria startWith(Object... values) {
            return Criteria.of(name, CriteriaOperator.START_WITH, values);
        }

        public Criteria specified(boolean value) {
            return Criteria.of(name, CriteriaOperator.SPECIFIED, value);
        }

        public Criteria isNull() {
            return Criteria.of(name, CriteriaOperator.SPECIFIED, false);
        }

        public Criteria nonNull() {
            return Criteria.of(name, CriteriaOperator.SPECIFIED, true);
        }

        public Criteria greaterThan(Object value) {
            return Criteria.of(name, CriteriaOperator.GREATER_THAN, value);
        }

        public Criteria greaterThanOrEqual(Object value) {
            return Criteria.of(name, CriteriaOperator.GREATER_THAN_OR_EQUAL, value);
        }

        public Criteria lessThan(Object value) {
            return Criteria.of(name, CriteriaOperator.LESS_THAN, value);
        }

        public Criteria lessThanOrEqual(Object value) {
            return Criteria.of(name, CriteriaOperator.LESS_THAN_OR_EQUAL, value);
        }
    }
}
