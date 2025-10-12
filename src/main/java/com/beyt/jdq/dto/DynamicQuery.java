package com.beyt.jdq.dto;

import com.beyt.jdq.dto.enums.Order;
import org.springframework.data.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tdilber at 30-Dec-20
 */
public class DynamicQuery implements Serializable {

    protected boolean distinct = false;
    protected Integer pageSize = null;
    protected Integer pageNumber = null;
    protected List<Pair<String, String>> select = new ArrayList<>();
    protected List<Criteria> where = new CriteriaList();
    protected List<Pair<String, Order>> orderBy = new ArrayList<>();


    public static DynamicQuery of(List<Criteria> where) {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.getWhere().addAll(where);
        return dynamicQuery;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public List<Pair<String, String>> getSelect() {
        return select;
    }

    public void setSelect(List<Pair<String, String>> select) {
        this.select = select;
    }

    public List<Criteria> getWhere() {
        return where;
    }

    public void setWhere(List<Criteria> where) {
        this.where = where;
    }

    public List<Pair<String, Order>> getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(List<Pair<String, Order>> orderBy) {
        this.orderBy = orderBy;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }
}
