package com.beyt.jdq.query;

import com.beyt.jdq.dto.Criteria;
import com.beyt.jdq.dto.enums.CriteriaOperator;
import com.beyt.jdq.dto.enums.JoinType;
import com.beyt.jdq.exception.*;
import com.beyt.jdq.util.SpecificationUtil;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;

import jakarta.persistence.criteria.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tdilber at 24-Aug-19
 */

public class DynamicSpecification<Entity> implements Specification<Entity> {

    protected List<Criteria> criteriaList;
    protected Map<Triple<From<?, ?>, String, JoinType>, Join<?, ?>> joinMap = new ConcurrentHashMap<>();
    protected RepositoryContext context;

    public DynamicSpecification(List<Criteria> criteriaList, RepositoryContext context) {
        this.criteriaList = criteriaList;
        this.joinMap = new ConcurrentHashMap<>();
        this.context = context;
    }

    public DynamicSpecification(List<Criteria> criteriaList, Map<Triple<From<?, ?>, String, JoinType>, Join<?, ?>> joinMap, RepositoryContext context) {
        this.criteriaList = criteriaList;
        this.joinMap = joinMap;
        this.context = context;
    }

    @Override
    public Predicate toPredicate(Root<Entity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        List<Predicate> predicateAndList = new ArrayList<>();
        List<Predicate> predicateOrList = new ArrayList<>();
        for (int i = 0; i < criteriaList.size(); i++) {
            if (criteriaList.get(i).getOperation() == CriteriaOperator.PARENTHES) {
                SpecificationUtil.checkHasFirstValue(criteriaList.get(i));
                try {
                    predicateAndList.add(new DynamicSpecification<Entity>(((List<Criteria>) (criteriaList.get(i).getValues().get(0))), joinMap, context).toPredicate(root, query, builder));
                } catch (Exception e) {
                    throw new DynamicQueryNoAvailableParenthesesOperationUsageException(
                            "There is No Available Paranthes Operation Usage in Criteria Key: " + criteriaList.get(i).getKey());
                }
            } else if (criteriaList.get(i).getOperation() == CriteriaOperator.OR) {
                if (i == 0 || i + 1 == criteriaList.size()) {
                    throw new DynamicQueryNoAvailableOrOperationUsageException(
                            "There is No Available OR Operation Usage in Criteria Key: " + criteriaList.get(i).getKey());
                }

                predicateOrList.add(builder.and(predicateAndList.toArray(new Predicate[0])));
                predicateAndList.clear();
            } else {
                predicateAndList.add(getPredicate(root, builder, criteriaList.get(i)));
            }
        }

        predicateOrList.add(builder.and(predicateAndList.toArray(new Predicate[0])));

        return builder.or(predicateOrList.toArray(new Predicate[0]));
    }

    private Predicate getPredicate(Root<Entity> root, CriteriaBuilder builder, Criteria criteria) {
        From<?, ?> localFrom = createLocalFrom(root, criteria.getKey());
        return addPredicate(localFrom, builder, new Criteria(getFieldName(criteria.getKey()), criteria.getOperation(), criteria.getValues().toArray(new Object[0])));
    }

    public From<?, ?> createLocalFrom(Root<?> root, String key) {
        From<?, ?> localFrom = root;
        List<Pair<String, JoinType>> fieldJoins = getFieldJoins(key);

        for (Pair<String, JoinType> fieldJoin : fieldJoins) {
            localFrom = getJoin(localFrom, fieldJoin.getFirst(), fieldJoin.getSecond());
        }

        return localFrom;
    }

    public static List<Pair<String, JoinType>> getFieldJoins(String key) {
        List<Pair<String, JoinType>> fieldJoins = new ArrayList<>();

        String subKey = key;
        JoinType joinType = null;
        int index = -1;


        while (subKey.chars().anyMatch(c -> Arrays.stream(JoinType.values()).anyMatch(j -> j.getSeparator().charValue() == c))) {

            for (JoinType value : JoinType.values()) {
                int indexOf = subKey.indexOf(value.getSeparator());
                if (indexOf > -1 && (index == -1 || indexOf < index)) {
                    index = indexOf;
                    joinType = value;
                }
            }

            if (Objects.nonNull(joinType)) {
                fieldJoins.add(Pair.of(subKey.substring(0, index), joinType));
                subKey = subKey.substring(index + 1);
            }

            index = -1;
            joinType = null;
        }

        return fieldJoins;
    }

    public static String getFieldName(String key) {
        String[] splitedKey = key.split(">|<|\\.");
        return splitedKey[splitedKey.length - 1];
    }

    protected Predicate addPredicate(Path<?> root, CriteriaBuilder builder, Criteria criteria) {
        if (!criteria.getOperation().equals(CriteriaOperator.SPECIFIED)) {
            try {
                criteria.setValues(deserialize(root.get(criteria.getKey()).getJavaType(), criteria.getValues()));
            } catch (Exception e) {
                throw new DynamicQueryNoAvailableEnumException("There is a "
                        + root.get(criteria.getKey()).getJavaType().getSimpleName() + "  Problem in Criteria Key: "
                        + criteria.getKey(), e);
            }
        }

        if (DynamicQueryManager.specificationRuleMap.containsKey(criteria.getOperation())) {
            return DynamicQueryManager.specificationRuleMap
                    .get(criteria.getOperation()).generatePredicate(root, builder, criteria);
        } else {
            throw new DynamicQueryNoAvailableOperationException("There is No Available Operation in Criteria Key: "
                    + criteria.getKey());
        }
    }

    protected Join<?, ?> getJoin(From<?, ?> from, String key, JoinType joinType) {
        Triple<From<?, ?>, String, JoinType> joinMapKey = new ImmutableTriple<>(from, key, joinType);
        if (joinMap.containsKey(joinMapKey)) {
            return joinMap.get(joinMapKey);
        }
        Join<?, ?> join = from.join(key, joinType.getJoinType());
        joinMap.put(joinMapKey, join);
        return join;
    }

    protected List<Object> deserialize(Class<?> clazz, List<Object> objects) throws Exception {
        List<Object> result = new ArrayList<>();
        for (Object object : objects) {
            Object deserialized = null;
            try {
                deserialized = context.getDeserializer().deserialize(object.toString(), clazz);
            } catch (Exception e) {
                throw new DynamicQueryValueSerializeException("There is a "
                        + clazz.getSimpleName() + " Deserialization Problem in Criteria Value: "
                        + object.toString());
            }
            result.add(deserialized);
        }
        return result;
    }
}
