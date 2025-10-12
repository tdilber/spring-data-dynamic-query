package com.beyt.jdq.dto;

import com.beyt.jdq.dto.enums.CriteriaOperator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by tdilber at 11/18/2020
 */
public class CriteriaList extends ArrayList<Criteria> implements Serializable {

    public static CriteriaList of(Criteria... criteria) {
        CriteriaList criteriaList = new CriteriaList();
        criteriaList.addAll(Arrays.asList(criteria));
        return criteriaList;
    }

    public static CriteriaList of(Collection<Criteria> criteria) {
        CriteriaList criteriaList = new CriteriaList();
        criteriaList.addAll(criteria);
        return criteriaList;
    }

    public List<Criteria> getCriteriaListByKeyAndOperation(String key, CriteriaOperator operation) {
        ArrayList<Criteria> criterias = new ArrayList<>();

        for (Criteria criteria : this) {
            if (criteria.key.equalsIgnoreCase(key) && criteria.operation.equals(operation)) {
                criterias.add(criteria);
            }
        }

        return criterias;
    }


    public List<Criteria> getCriteriaListByKey(String key) {
        ArrayList<Criteria> criterias = new ArrayList<>();

        for (Criteria criteria : this) {
            if (criteria.key.equalsIgnoreCase(key)) {
                criterias.add(criteria);
            }
        }

        return criterias;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        for (Criteria criteria : this) {
            result.append("{").append(criteria.toString()).append("}");
        }

        return result.toString();
    }
}
