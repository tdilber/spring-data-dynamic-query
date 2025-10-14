package com.beyt.jdq.mongo;

import com.beyt.jdq.annotation.model.JdqField;
import com.beyt.jdq.annotation.model.JdqModel;
import com.beyt.jdq.annotation.model.JdqSubModel;
import com.beyt.jdq.deserializer.IDeserializer;
import com.beyt.jdq.dto.DynamicQuery;
import com.beyt.jdq.dto.enums.CriteriaOperator;
import com.beyt.jdq.exception.DynamicQueryIllegalArgumentException;
import com.beyt.jdq.exception.DynamicQueryValueSerializeException;
import com.beyt.jdq.util.field.FieldUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.data.util.Pair;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Template class for building and executing MongoDB queries from DynamicQuery objects.
 * Provides methods similar to JPA DynamicQueryManager but for MongoDB.
 */
public class MongoSearchQueryTemplate {

    private final MongoTemplate mongoTemplate;
    private final IDeserializer deserializer;

    public MongoSearchQueryTemplate(MongoTemplate mongoTemplate, IDeserializer deserializer) {
        this.mongoTemplate = mongoTemplate;
        this.deserializer = deserializer;
    }

    /**
     * Find all entities matching the criteria list
     */
    public <Entity> List<Entity> findAll(Class<Entity> entityClass, List<com.beyt.jdq.dto.Criteria> searchCriteriaList) {
        DynamicQuery dynamicQuery = DynamicQuery.of(searchCriteriaList);
        return findAll(entityClass, dynamicQuery);
    }

    /**
     * Find all entities matching the dynamic query
     */
    public <Entity> List<Entity> findAll(Class<Entity> entityClass, DynamicQuery dynamicQuery) {
        // Check if we need aggregation pipeline (joins, distinct, or projection)
        if (requiresAggregation(entityClass, dynamicQuery)) {
            return executeAggregation(entityClass, dynamicQuery);
        } else {
            Query query = prepareQuery(entityClass, dynamicQuery);
            applyOrderBy(query, dynamicQuery);
            return mongoTemplate.find(query, entityClass);
        }
    }

    /**
     * Find all entities with projection matching the dynamic query
     */
    public <Entity, ResultType> List<ResultType> findAll(Class<Entity> entityClass, DynamicQuery dynamicQuery, Class<ResultType> resultClass) {
        // Extract @JdqModel annotations if present
        extractIfJdqModel(dynamicQuery, resultClass);
        
        // Check if projection is needed
        if (CollectionUtils.isEmpty(dynamicQuery.getSelect())) {
            // No projection, just return as result type
            if (requiresJoin(entityClass, dynamicQuery)) {
                return executeAggregationWithProjection(entityClass, dynamicQuery, resultClass);
            } else {
                Query query = prepareQuery(entityClass, dynamicQuery);
                applyOrderBy(query, dynamicQuery);
                return mongoTemplate.find(query, entityClass).stream()
                    .map(e -> mongoTemplate.getConverter().read(resultClass, new Document()))
                    .collect(Collectors.toList());
            }
        }
        
        // Execute with projection
        return executeAggregationWithProjection(entityClass, dynamicQuery, resultClass);
    }

    /**
     * Find all entities as page matching the criteria list
     */
    public <Entity> Page<Entity> findAllAsPage(Class<Entity> entityClass, List<com.beyt.jdq.dto.Criteria> searchCriteriaList, Pageable pageable) {
        DynamicQuery dynamicQuery = DynamicQuery.of(searchCriteriaList);
        dynamicQuery.setPageNumber(pageable.getPageNumber());
        dynamicQuery.setPageSize(pageable.getPageSize());
        return findAllAsPage(entityClass, dynamicQuery);
    }

    /**
     * Find all entities as page matching the dynamic query
     */
    public <Entity> Page<Entity> findAllAsPage(Class<Entity> entityClass, DynamicQuery dynamicQuery) {
        Pageable pageable = getPageable(dynamicQuery);
        
        if (requiresJoin(entityClass, dynamicQuery)) {
            List<Entity> all = executeAggregation(entityClass, dynamicQuery, pageable);
            long count = countWithAggregation(entityClass, dynamicQuery);
            return PageableExecutionUtils.getPage(all, pageable, () -> count);
        } else {
            Criteria criteria = applyCriteria(entityClass, dynamicQuery);
            Query query = Query.query(criteria).with(pageable);
            List<Entity> all = mongoTemplate.find(query, entityClass);
            return PageableExecutionUtils.getPage(
                all, 
                pageable, 
                () -> mongoTemplate.count(Query.query(criteria), entityClass)
            );
        }
    }

    /**
     * Find all entities as page with projection matching the dynamic query
     */
    public <Entity, ResultType> Page<ResultType> findAllAsPage(Class<Entity> entityClass, DynamicQuery dynamicQuery, Class<ResultType> resultClass) {
        // Extract @JdqModel annotations if present
        extractIfJdqModel(dynamicQuery, resultClass);
        
        Pageable pageable = getPageable(dynamicQuery);
        
        // Execute with projection
        List<ResultType> all = executeAggregationWithProjection(entityClass, dynamicQuery, resultClass, pageable);
        long count = countWithAggregation(entityClass, dynamicQuery);
        return PageableExecutionUtils.getPage(all, pageable, () -> count);
    }

    /**
     * Count entities matching the criteria list
     */
    public <Entity> long count(Class<Entity> entityClass, List<com.beyt.jdq.dto.Criteria> searchCriteriaList) {
        DynamicQuery dynamicQuery = DynamicQuery.of(searchCriteriaList);
        
        if (requiresJoin(entityClass, dynamicQuery)) {
            return countWithAggregation(entityClass, dynamicQuery);
        } else {
            Criteria criteria = applyCriteria(entityClass, dynamicQuery);
            return mongoTemplate.count(Query.query(criteria), entityClass);
        }
    }

    /**
     * Prepare a MongoDB Query from DynamicQuery
     */
    public <Entity> Query prepareQuery(Class<Entity> entityClass, DynamicQuery searchQuery) {
        Criteria criteria = applyCriteria(entityClass, searchQuery);
        Query query = Query.query(criteria);
        
        if (searchQuery.getPageSize() != null && searchQuery.getPageNumber() != null) {
            query.with(getPageable(searchQuery));
        }
        
        return query;
    }

    /**
     * Get pageable from DynamicQuery
     */
    private Pageable getPageable(DynamicQuery searchQuery) {
        int pageNumber = searchQuery.getPageNumber() != null ? searchQuery.getPageNumber() : 0;
        int pageSize = searchQuery.getPageSize() != null ? searchQuery.getPageSize() : 20;
        
        // Include sort in Pageable using original field names (not MongoDB paths)
        // We handle the actual sorting in aggregation pipeline with MongoDB field paths,
        // but the Pageable should reflect what was requested using the original field names
        if (CollectionUtils.isNotEmpty(searchQuery.getOrderBy())) {
            List<org.springframework.data.domain.Sort.Order> orders = new ArrayList<>();
            for (Pair<String, com.beyt.jdq.dto.enums.Order> orderPair : searchQuery.getOrderBy()) {
                orders.add(new org.springframework.data.domain.Sort.Order(
                    orderPair.getSecond().getDirection(),
                    orderPair.getFirst()  // Use original field name
                ));
            }
            return PageRequest.of(pageNumber, pageSize, org.springframework.data.domain.Sort.by(orders));
        }
        
        return PageRequest.of(pageNumber, pageSize);
    }

    /**
     * Apply criteria from DynamicQuery and build MongoDB Criteria
     * Logic: Collects criteria into groups, splits groups on OR operator, then combines:
     * - Each group's criteria are combined with AND
     * - All groups are combined with OR
     * Example: (c1 AND c2) OR (c3 AND c4)
     */
    private <Entity> Criteria applyCriteria(Class<Entity> entityClass, DynamicQuery searchQuery) {
        return applyCriteriaList(entityClass, searchQuery.getWhere(), searchQuery);
    }

    /**
     * Recursively process a criteria list and build MongoDB Criteria
     * This method handles PARENTHES operator by recursively processing nested criteria lists
     */
    @SuppressWarnings("unchecked")
    private <Entity> Criteria applyCriteriaList(Class<Entity> entityClass, List<com.beyt.jdq.dto.Criteria> criteriaList, DynamicQuery searchQuery) {
        List<com.beyt.jdq.dto.Criteria> issuedCriteriaList = new ArrayList<>();
        List<Criteria> searchCriteriaList = new ArrayList<>();
        List<Criteria> andCriteriaList = new ArrayList<>();
        List<Criteria> orGroupList = new ArrayList<>();

        // Validate OR operator usage
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            // Check if OR is at the start
            if (criteriaList.get(0).getOperation() == CriteriaOperator.OR) {
                throw new com.beyt.jdq.exception.DynamicQueryNoAvailableOrOperationUsageException(
                    "OR operator cannot be at the start of criteria list");
            }
            // Check if OR is at the end
            if (criteriaList.get(criteriaList.size() - 1).getOperation() == CriteriaOperator.OR) {
                throw new com.beyt.jdq.exception.DynamicQueryNoAvailableOrOperationUsageException(
                    "OR operator cannot be at the end of criteria list");
            }
            // Check if only OR operator exists
            if (criteriaList.size() == 1 && criteriaList.get(0).getOperation() == CriteriaOperator.OR) {
                throw new com.beyt.jdq.exception.DynamicQueryNoAvailableOrOperationUsageException(
                    "OR operator cannot be used alone");
            }
        }

        Map<CriteriaOperator, CriteriaBuilderFunction> criteriaBuilderMap = buildCriteriaMap(entityClass, issuedCriteriaList, searchQuery);

        for (com.beyt.jdq.dto.Criteria c : criteriaList) {
            // Handle PARENTHES operator - recursively process nested criteria
            if (CriteriaOperator.PARENTHES.equals(c.getOperation())) {
                if (c.getValues() == null || c.getValues().isEmpty()) {
                    continue;
                }
                try {
                    List<com.beyt.jdq.dto.Criteria> nestedCriteria = (List<com.beyt.jdq.dto.Criteria>) c.getValues().get(0);
                    Criteria nestedMongoCriteria = applyCriteriaList(entityClass, nestedCriteria, searchQuery);
                    andCriteriaList.add(nestedMongoCriteria);
                } catch (Exception e) {
                    throw new RuntimeException("Error processing PARENTHES operator for key: " + c.getKey(), e);
                }
                continue;
            }

            // Handle OR operator - finalize current AND group and start new one
            if (CriteriaOperator.OR.equals(c.getOperation())) {
                if (!andCriteriaList.isEmpty()) {
                    // Combine current group with AND and add to OR list
                    orGroupList.add(new Criteria().andOperator(andCriteriaList));
                    andCriteriaList = new ArrayList<>();
                }
                continue;
            }

            if (issuedCriteriaList.contains(c) || !criteriaBuilderMap.containsKey(c.getOperation())) {
                continue;
            }

            Criteria builtCriteria = criteriaBuilderMap.get(c.getOperation()).apply(c, searchQuery);
            
            if (c.getKey().startsWith("search.")) {
                searchCriteriaList.add(builtCriteria);
            } else {
                andCriteriaList.add(builtCriteria);
            }
        }

        // Add the final AND group to OR list
        if (!andCriteriaList.isEmpty()) {
            orGroupList.add(new Criteria().andOperator(andCriteriaList));
        }

        // Build final criteria
        Criteria finalCriteria;
        if (orGroupList.isEmpty()) {
            finalCriteria = new Criteria();
        } else if (orGroupList.size() == 1) {
            finalCriteria = orGroupList.get(0);
        } else {
            finalCriteria = new Criteria().orOperator(orGroupList);
        }

        // Add search criteria as additional OR condition if present
        if (CollectionUtils.isNotEmpty(searchCriteriaList)) {
            Criteria searchCriteria = new Criteria().orOperator(searchCriteriaList);
            if (orGroupList.isEmpty()) {
                finalCriteria = searchCriteria;
            } else {
                finalCriteria = new Criteria().andOperator(finalCriteria, searchCriteria);
            }
        }

        return finalCriteria;
    }

    /**
     * Build the criteria builder map for all operators
     */
    private <Entity> Map<CriteriaOperator, CriteriaBuilderFunction> buildCriteriaMap(
            Class<Entity> entityClass,
            List<com.beyt.jdq.dto.Criteria> issuedCriteriaList, 
            DynamicQuery searchQuery) {
        
        Map<CriteriaOperator, CriteriaBuilderFunction> criteriaBuilderMap = new HashMap<>();

        // EQUAL - supports multiple values (IN query)
        criteriaBuilderMap.put(CriteriaOperator.EQUAL, (sc, sq) -> {
            String fieldKey = sc.getKey().replace("<", ".");  // Convert left join syntax
            String mongoField = toMongoFieldPath(entityClass, fieldKey);
            if (sc.getValues() == null || sc.getValues().isEmpty()) {
                return where(mongoField).is(null);
            }
            List<Object> deserializedValues = deserializeValues(entityClass, fieldKey, sc.getValues());
            if (deserializedValues.size() == 1) {
                return where(mongoField).is(deserializedValues.get(0));
            }
            return where(mongoField).in(deserializedValues);
        });

        // NOT_EQUAL
        criteriaBuilderMap.put(CriteriaOperator.NOT_EQUAL, (sc, sq) -> {
            String fieldKey = sc.getKey().replace("<", ".");  // Convert left join syntax
            String mongoField = toMongoFieldPath(entityClass, fieldKey);
            if (sc.getValues() == null || sc.getValues().isEmpty()) {
                return where(mongoField).ne(null);
            }
            List<Object> deserializedValues = deserializeValues(entityClass, fieldKey, sc.getValues());
            if (deserializedValues.size() == 1) {
                return where(mongoField).ne(deserializedValues.get(0));
            }
            return where(mongoField).nin(deserializedValues);
        });

        // CONTAIN - case insensitive regex, supports multiple values with OR logic
        criteriaBuilderMap.put(CriteriaOperator.CONTAIN, (sc, sq) -> {
            String fieldKey = sc.getKey().replace("<", ".");  // Convert left join syntax
            String mongoField = toMongoFieldPath(entityClass, fieldKey);
            if (sc.getValues() == null || sc.getValues().isEmpty()) {
                return where(mongoField).is(null);
            }
            if (sc.getValues().size() == 1) {
                return where(mongoField).regex(".*" + escapeRegex(String.valueOf(sc.getValues().get(0))) + ".*", "i");
            }
            // Multiple values - OR logic
            List<Criteria> orCriteria = new ArrayList<>();
            for (Object value : sc.getValues()) {
                orCriteria.add(where(mongoField).regex(".*" + escapeRegex(String.valueOf(value)) + ".*", "i"));
            }
            return new Criteria().orOperator(orCriteria);
        });

        // DOES_NOT_CONTAIN - supports multiple values with AND logic (none of the values should be contained)
        // Also excludes null values since null doesn't "contain" or "not contain" anything
        criteriaBuilderMap.put(CriteriaOperator.DOES_NOT_CONTAIN, (sc, sq) -> {
            String fieldKey = sc.getKey().replace("<", ".");  // Convert left join syntax
            String mongoField = toMongoFieldPath(entityClass, fieldKey);
            if (sc.getValues() == null || sc.getValues().isEmpty()) {
                return where(mongoField).ne(null);
            }
            // Build criteria: field must not be null AND must not match any of the patterns
            List<Criteria> andCriteria = new ArrayList<>();
            andCriteria.add(where(mongoField).ne(null)); // Exclude nulls
            
            if (sc.getValues().size() == 1) {
                andCriteria.add(where(mongoField).not().regex(".*" + escapeRegex(String.valueOf(sc.getValues().get(0))) + ".*", "i"));
            } else {
                // Multiple values - AND logic (must not contain any of them)
                for (Object value : sc.getValues()) {
                    andCriteria.add(where(mongoField).not().regex(".*" + escapeRegex(String.valueOf(value)) + ".*", "i"));
                }
            }
            return new Criteria().andOperator(andCriteria);
        });

        // START_WITH - supports multiple values with OR logic
        criteriaBuilderMap.put(CriteriaOperator.START_WITH, (sc, sq) -> {
            String fieldKey = sc.getKey().replace("<", ".");  // Convert left join syntax
            String mongoField = toMongoFieldPath(entityClass, fieldKey);
            if (sc.getValues() == null || sc.getValues().isEmpty()) {
                return where(mongoField).is(null);
            }
            if (sc.getValues().size() == 1) {
                return where(mongoField).regex("^" + escapeRegex(String.valueOf(sc.getValues().get(0))) + ".*", "i");
            }
            // Multiple values - OR logic
            List<Criteria> orCriteria = new ArrayList<>();
            for (Object value : sc.getValues()) {
                orCriteria.add(where(mongoField).regex("^" + escapeRegex(String.valueOf(value)) + ".*", "i"));
            }
            return new Criteria().orOperator(orCriteria);
        });

        // END_WITH - supports multiple values with OR logic
        criteriaBuilderMap.put(CriteriaOperator.END_WITH, (sc, sq) -> {
            String fieldKey = sc.getKey().replace("<", ".");  // Convert left join syntax
            String mongoField = toMongoFieldPath(entityClass, fieldKey);
            if (sc.getValues() == null || sc.getValues().isEmpty()) {
                return where(mongoField).is(null);
            }
            if (sc.getValues().size() == 1) {
                return where(mongoField).regex(".*" + escapeRegex(String.valueOf(sc.getValues().get(0))) + "$", "i");
            }
            // Multiple values - OR logic
            List<Criteria> orCriteria = new ArrayList<>();
            for (Object value : sc.getValues()) {
                orCriteria.add(where(mongoField).regex(".*" + escapeRegex(String.valueOf(value)) + "$", "i"));
            }
            return new Criteria().orOperator(orCriteria);
        });

        // SPECIFIED - check if field is not null (true) or is null (false)
        // Also handles left join syntax with '<' (e.g., "department<id" means check if department is null)
        criteriaBuilderMap.put(CriteriaOperator.SPECIFIED, (sc, sq) -> {
            String fieldKey = sc.getKey().replace("<", ".");  // Convert left join syntax
            String mongoField = toMongoFieldPath(entityClass, fieldKey);
            boolean isSpecified = Boolean.parseBoolean(String.valueOf(sc.getValues().get(0)));
            if (isSpecified) {
                return where(mongoField).ne(null);
            } else {
                return where(mongoField).is(null);
            }
        });

        // Comparison operators
        criteriaBuilderMap.put(CriteriaOperator.GREATER_THAN, (sc, sq) -> {
            String fieldKey = sc.getKey().replace("<", ".");  // Convert left join syntax
            String mongoField = toMongoFieldPath(entityClass, fieldKey);
            Object value = deserializeValue(entityClass, fieldKey, sc.getValues().get(0));
            return where(mongoField).gt(value);
        });

        criteriaBuilderMap.put(CriteriaOperator.GREATER_THAN_OR_EQUAL, (sc, sq) -> {
            String fieldKey = sc.getKey().replace("<", ".");  // Convert left join syntax
            String mongoField = toMongoFieldPath(entityClass, fieldKey);
            Object value = deserializeValue(entityClass, fieldKey, sc.getValues().get(0));
            return where(mongoField).gte(value);
        });

        criteriaBuilderMap.put(CriteriaOperator.LESS_THAN, (sc, sq) -> {
            String fieldKey = sc.getKey().replace("<", ".");  // Convert left join syntax
            String mongoField = toMongoFieldPath(entityClass, fieldKey);
            Object value = deserializeValue(entityClass, fieldKey, sc.getValues().get(0));
            return where(mongoField).lt(value);
        });

        criteriaBuilderMap.put(CriteriaOperator.LESS_THAN_OR_EQUAL, (sc, sq) -> {
            String fieldKey = sc.getKey().replace("<", ".");  // Convert left join syntax
            String mongoField = toMongoFieldPath(entityClass, fieldKey);
            Object value = deserializeValue(entityClass, fieldKey, sc.getValues().get(0));
            return where(mongoField).lte(value);
        });

        return criteriaBuilderMap;
    }

    /**
     * Escape special regex characters
     */
    private String escapeRegex(String str) {
        return str.replaceAll("([\\\\\\[\\]{}()*+?.^$|])", "\\\\$1");
    }

    /**
     * Convert Java field path to MongoDB field path for aggregation pipelines
     * Maps @Id annotated fields to _id
     * For DBRef fields in aggregation pipelines, uses the lookup field names
     * 
     * Logic:
     * - When we encounter a DBRef field, we use the lookup field name (path_to_field_lookup)
     * - For nested DBRef fields, the lookup creates a new root-level field in the aggregation
     * - After $addFields for non-collection DBRefs, fields are accessible via the parent lookup field
     * 
     * Example: roles.roleAuthorizations.authorization.menuIcon
     * - roles (DBRef collection) -> uses roles_lookup at root
     * - roleAuthorizations (DBRef collection in Role) -> uses roles_roleAuthorizations_lookup at root  
     * - authorization (DBRef single in RoleAuthorization) -> after $addFields, accessible as roles_roleAuthorizations_lookup.authorization
     * - Result: roles_roleAuthorizations_lookup.authorization.menuIcon
     */
    private String toMongoFieldPath(Class<?> entityClass, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        Class<?> currentClass = entityClass;
        StringBuilder logicalPath = new StringBuilder();
        StringBuilder resultPath = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            // Build the logical path so far
            if (logicalPath.length() > 0) {
                logicalPath.append(".");
            }
            logicalPath.append(part);
            
            try {
                Field field = currentClass.getDeclaredField(part);
                
                // Check if this is a DBRef field
                if (field.isAnnotationPresent(DBRef.class)) {
                    boolean isCollection = Collection.class.isAssignableFrom(field.getType());
                    
                    if (i < parts.length - 1) {
                        // This is a DBRef that leads to more nested fields
                        // The lookup field name
                        String lookupField = logicalPath.toString().replace(".", "_") + "_lookup";
                        
                        if (isCollection) {
                            // For collection DBRefs, the lookup field is at the root level
                            resultPath = new StringBuilder(lookupField);
                        } else {
                            // For non-collection DBRefs, after $addFields, it's accessible via the parent lookup
                            if (resultPath.length() > 0) {
                                resultPath.append(".");
                            }
                            resultPath.append(part);
                        }
                    } else {
                        // This is the final DBRef field (shouldn't happen in practice for queries on nested fields)
                        if (resultPath.length() > 0) {
                            resultPath.append(".");
                        }
                        resultPath.append(part);
                    }
                    
                    currentClass = getTargetClassFromField(field);
                } else if (field.isAnnotationPresent(org.springframework.data.annotation.Id.class)) {
                    // Check if this field is annotated with @Id - if so, use _id in MongoDB
                    if (resultPath.length() > 0) {
                        resultPath.append(".");
                    }
                    resultPath.append("_id");
                } else {
                    // Regular field
                    if (resultPath.length() > 0) {
                        resultPath.append(".");
                    }
                    resultPath.append(part);
                    
                    // Move to next class for nested paths
                    if (i < parts.length - 1 && !Collection.class.isAssignableFrom(field.getType())) {
                        currentClass = field.getType();
                    }
                }
            } catch (NoSuchFieldException e) {
                // Field not found, keep original name
                if (resultPath.length() > 0) {
                    resultPath.append(".");
                }
                resultPath.append(part);
            }
        }
        
        return resultPath.toString();
    }

    /**
     * Get the field type for a given field key in an entity class
     * Supports nested fields using dot notation (e.g. "address.city")
     * Also handles DBRef fields by looking up the referenced entity
     */
    private Class<?> getFieldType(Class<?> entityClass, String fieldKey) {
        try {
            String[] fieldParts = fieldKey.split("\\.");
            Class<?> currentClass = entityClass;
            
            for (String fieldName : fieldParts) {
                Field field = currentClass.getDeclaredField(fieldName);
                
                // If this field is a DBRef, get the target class
                if (field.isAnnotationPresent(DBRef.class)) {
                    currentClass = getTargetClassFromField(field);
                } else {
                    currentClass = field.getType();
                }
            }
            
            return currentClass;
        } catch (NoSuchFieldException e) {
            // If field is not found, return String as fallback
            return String.class;
        }
    }

    /**
     * Deserialize a list of values to the target type using the configured deserializer
     */
    private List<Object> deserializeValues(Class<?> entityClass, String fieldKey, List<Object> values) {
        if (values == null || values.isEmpty()) {
            return values;
        }

        Class<?> fieldType = getFieldType(entityClass, fieldKey);
        List<Object> result = new ArrayList<>();
        
        for (Object value : values) {
            try {
                Object deserialized = deserializer.deserialize(value.toString(), fieldType);
                result.add(deserialized);
            } catch (Exception e) {
                throw new DynamicQueryValueSerializeException(
                    "There is a " + fieldType.getSimpleName() + 
                    " Deserialization Problem in Criteria Key: " + fieldKey + 
                    ", Value: " + value.toString());
            }
        }
        
        return result;
    }

    /**
     * Deserialize a single value to the target type using the configured deserializer
     */
    private Object deserializeValue(Class<?> entityClass, String fieldKey, Object value) {
        if (value == null) {
            return null;
        }

        Class<?> fieldType = getFieldType(entityClass, fieldKey);
        
        try {
            return deserializer.deserialize(value.toString(), fieldType);
        } catch (Exception e) {
            throw new DynamicQueryValueSerializeException(
                "There is a " + fieldType.getSimpleName() + 
                " Deserialization Problem in Criteria Key: " + fieldKey + 
                ", Value: " + value.toString());
        }
    }

    /**
     * Check if the query requires aggregation pipeline
     * Required for:
     * 1. Joins (DBRef lookups) in WHERE, SELECT, or ORDER BY
     * 2. Projection (SELECT fields)
     * 3. Distinct queries
     * 4. Complex ordering on joined fields
     */
    private <Entity> boolean requiresAggregation(Class<Entity> entityClass, DynamicQuery searchQuery) {
        // Check for joins in WHERE criteria
        boolean hasJoinsInWhere = searchQuery.getWhere().stream().anyMatch(c -> isJoinCriteria(entityClass, c.getKey()));
        
        // Check for joins in SELECT fields
        boolean hasJoinsInSelect = false;
        if (CollectionUtils.isNotEmpty(searchQuery.getSelect())) {
            hasJoinsInSelect = searchQuery.getSelect().stream()
                .anyMatch(p -> isJoinCriteria(entityClass, p.getFirst()));
        }
        
        // Check for joins in ORDER BY fields
        boolean hasJoinsInOrderBy = false;
        if (CollectionUtils.isNotEmpty(searchQuery.getOrderBy())) {
            hasJoinsInOrderBy = searchQuery.getOrderBy().stream()
                .anyMatch(p -> isJoinCriteria(entityClass, p.getFirst()));
        }
        
        // Check for projection
        boolean hasProjection = CollectionUtils.isNotEmpty(searchQuery.getSelect());
        
        // Check for distinct
        boolean hasDistinct = searchQuery.isDistinct();
        
        return hasJoinsInWhere || hasJoinsInSelect || hasJoinsInOrderBy || hasProjection || hasDistinct;
    }

    /**
     * Check if the query requires join operations (DBRef lookups)
     * Detects:
     * 1. Nested field paths (e.g., "department.name") where first part is a DBRef field
     * 2. Left join syntax with '<' (e.g., "department<id")
     * Checks WHERE, SELECT, and ORDER BY fields
     */
    private <Entity> boolean requiresJoin(Class<Entity> entityClass, DynamicQuery searchQuery) {
        // Check WHERE criteria
        boolean hasJoinsInWhere = searchQuery.getWhere().stream().anyMatch(c -> isJoinCriteria(entityClass, c.getKey()));
        if (hasJoinsInWhere) return true;
        
        // Check SELECT fields
        if (CollectionUtils.isNotEmpty(searchQuery.getSelect())) {
            boolean hasJoinsInSelect = searchQuery.getSelect().stream()
                .anyMatch(p -> isJoinCriteria(entityClass, p.getFirst()));
            if (hasJoinsInSelect) return true;
        }
        
        // Check ORDER BY fields
        if (CollectionUtils.isNotEmpty(searchQuery.getOrderBy())) {
            boolean hasJoinsInOrderBy = searchQuery.getOrderBy().stream()
                .anyMatch(p -> isJoinCriteria(entityClass, p.getFirst()));
            if (hasJoinsInOrderBy) return true;
        }
        
        return false;
    }

    /**
     * Check if a field key represents a join criteria
     * Handles both '.' and '<' syntax for nested paths
     */
    private boolean isJoinCriteria(Class<?> entityClass, String fieldKey) {
        // Convert left join syntax to dot notation for analysis
        String pathToAnalyze = fieldKey.replace("<", ".");
        
        // Handle nested path: "department.name"
        if (pathToAnalyze.contains(".")) {
            String firstPart = pathToAnalyze.substring(0, pathToAnalyze.indexOf("."));
            return isDBRefField(entityClass, firstPart);
        }
        
        return false;
    }

    /**
     * Check if a field is annotated with @DBRef
     */
    private boolean isDBRefField(Class<?> entityClass, String fieldName) {
        try {
            Field field = entityClass.getDeclaredField(fieldName);
            return field.isAnnotationPresent(DBRef.class);
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    /**
     * Execute aggregation pipeline for queries with joins
     */
    private <Entity> List<Entity> executeAggregation(Class<Entity> entityClass, DynamicQuery dynamicQuery) {
        return executeAggregation(entityClass, dynamicQuery, null);
    }

    /**
     * Execute aggregation pipeline for queries with joins with pagination
     */
    private <Entity> List<Entity> executeAggregation(Class<Entity> entityClass, DynamicQuery dynamicQuery, Pageable pageable) {
        Aggregation aggregation = buildAggregation(entityClass, dynamicQuery, pageable);
        AggregationResults<Entity> results = mongoTemplate.aggregate(aggregation, getCollectionName(entityClass), entityClass);
        return results.getMappedResults();
    }

    /**
     * Count with aggregation for queries with joins
     */
    @SuppressWarnings("unchecked")
    private <Entity> long countWithAggregation(Class<Entity> entityClass, DynamicQuery dynamicQuery) {
        Aggregation aggregation = buildAggregation(entityClass, dynamicQuery, null, true);
        AggregationResults<Map<String, Object>> results = mongoTemplate.aggregate(aggregation, getCollectionName(entityClass), (Class<Map<String, Object>>)(Class<?>)Map.class);
        List<Map<String, Object>> mappedResults = results.getMappedResults();
        if (mappedResults.isEmpty()) {
            return 0;
        }
        Object count = mappedResults.get(0).get("count");
        return count != null ? ((Number) count).longValue() : 0;
    }

    /**
     * Build aggregation pipeline with $lookup stages for joins
     */
    private <Entity> Aggregation buildAggregation(Class<Entity> entityClass, DynamicQuery dynamicQuery, Pageable pageable) {
        return buildAggregation(entityClass, dynamicQuery, pageable, false, null);
    }

    /**
     * Build aggregation pipeline with $lookup stages for joins
     */
    private <Entity> Aggregation buildAggregation(Class<Entity> entityClass, DynamicQuery dynamicQuery, Pageable pageable, boolean isCount) {
        return buildAggregation(entityClass, dynamicQuery, pageable, isCount, null);
    }

    /**
     * Build aggregation pipeline with $lookup stages for joins and optional projection
     */
    private <Entity, ResultType> Aggregation buildAggregation(Class<Entity> entityClass, DynamicQuery dynamicQuery, Pageable pageable, boolean isCount, Class<ResultType> resultClass) {
        List<org.springframework.data.mongodb.core.aggregation.AggregationOperation> operations = new ArrayList<>();
        
        // Collect all nested paths that need lookups (including deep nested paths)
        Set<String> allNestedPaths = collectAllNestedPaths(entityClass, dynamicQuery);
        
        // Build lookup stages for all paths, starting from the root
        Set<String> processedPaths = new HashSet<>();
        buildLookupsForPaths(entityClass, allNestedPaths, operations, processedPaths);
        
        // Build match criteria from DynamicQuery
        Criteria criteria = applyCriteria(entityClass, dynamicQuery);
        operations.add(match(criteria));
        
        // Add distinct if specified (using $group - must come before projection)
        if (dynamicQuery.isDistinct()) {
            // For distinct with joins, we need to group by document root to get unique documents
            operations.add(Aggregation.group("$_id")
                .first("$$ROOT").as("doc"));
            // Replace root with the grouped document
            operations.add(Aggregation.replaceRoot("doc"));
        }
        
        if (isCount) {
            // For count, just return the count
            operations.add(Aggregation.count().as("count"));
        } else {
            // Add order by BEFORE projection (because projection changes field names)
            if (CollectionUtils.isNotEmpty(dynamicQuery.getOrderBy())) {
                List<org.springframework.data.domain.Sort.Order> orders = new ArrayList<>();
                for (Pair<String, com.beyt.jdq.dto.enums.Order> orderPair : dynamicQuery.getOrderBy()) {
                    String fieldPath = orderPair.getFirst();
                    // Use MongoDB field path (before projection)
                    String orderByField = toMongoFieldPath(entityClass, fieldPath);
                    
                    orders.add(new org.springframework.data.domain.Sort.Order(
                        orderPair.getSecond().getDirection(),
                        orderByField
                    ));
                }
                operations.add(sort(org.springframework.data.domain.Sort.by(orders)));
            }
            
            // Add projection if select fields are specified (AFTER order by)
            if (CollectionUtils.isNotEmpty(dynamicQuery.getSelect())) {
                operations.add(buildProjectionStage(entityClass, dynamicQuery));
            }
            
            // Add pagination if provided
            if (pageable != null) {
                // Note: Sort is already handled above, don't add from pageable
                operations.add(skip((long) pageable.getPageNumber() * pageable.getPageSize()));
                operations.add(limit(pageable.getPageSize()));
            } else if (dynamicQuery.getPageSize() != null && dynamicQuery.getPageNumber() != null) {
                Pageable pageableFromQuery = getPageable(dynamicQuery);
                // Note: Sort is already handled above, don't add from pageable
                operations.add(skip((long) pageableFromQuery.getPageNumber() * pageableFromQuery.getPageSize()));
                operations.add(limit(pageableFromQuery.getPageSize()));
            }
        }
        
        return Aggregation.newAggregation(operations);
    }

    /**
     * Build $project stage for field selection
     */
    private <Entity> org.springframework.data.mongodb.core.aggregation.ProjectionOperation buildProjectionStage(
            Class<Entity> entityClass, 
            DynamicQuery dynamicQuery) {
        org.springframework.data.mongodb.core.aggregation.ProjectionOperation projection = Aggregation.project();
        
        // Check if _id is explicitly selected
        boolean hasIdSelection = dynamicQuery.getSelect().stream()
            .anyMatch(p -> "id".equals(p.getFirst()) || "_id".equals(p.getFirst()));
        
        // Exclude _id if not explicitly selected
        if (!hasIdSelection) {
            projection = projection.andExclude("_id");
        }
        
        for (Pair<String, String> selectPair : dynamicQuery.getSelect()) {
            String sourceField = selectPair.getFirst();  // Database field name
            String targetField = selectPair.getSecond(); // Result field name (alias)
            
            // Convert to MongoDB field path
            String mongoField = toMongoFieldPath(entityClass, sourceField);
            
            // Add field with alias
            projection = projection.and(mongoField).as(targetField);
        }
        
        return projection;
    }

    /**
     * Build all $lookup stages needed for nested paths in the correct order
     */
    private void buildLookupsForPaths(Class<?> currentClass, Set<String> allNestedPaths, 
                                     List<org.springframework.data.mongodb.core.aggregation.AggregationOperation> operations,
                                     Set<String> processedPaths) {
        buildLookupsRecursive(currentClass, "", "", allNestedPaths, operations, processedPaths);
    }

    /**
     * Recursively build $lookup stages for nested paths
     * 
     * @param currentClass The current entity class being processed
     * @param currentPath The logical path (e.g., "roles.roleAuthorizations")
     * @param currentLookupPath The actual MongoDB path after lookups (e.g., "roles_lookup.roles_roleAuthorizations_lookup")
     * @param allNestedPaths All nested paths that need lookup
     * @param operations List of aggregation operations to add to
     * @param processedPaths Set of already processed paths
     */
    private void buildLookupsRecursive(Class<?> currentClass, String currentPath, String currentLookupPath, 
                                       Set<String> allNestedPaths,
                                       List<org.springframework.data.mongodb.core.aggregation.AggregationOperation> operations,
                                       Set<String> processedPaths) {
        // Find all direct children paths that need lookup
        for (String nestedPath : allNestedPaths) {
            String pathToCheck = nestedPath;
            
            // Handle left join syntax
            if (pathToCheck.contains("<")) {
                pathToCheck = pathToCheck.substring(0, pathToCheck.indexOf("<"));
            }
            
            // Skip if already processed
            if (processedPaths.contains(pathToCheck)) {
                continue;
            }
            
            // Get the first unprocessed segment
            String relativePath = currentPath.isEmpty() ? pathToCheck : 
                (pathToCheck.startsWith(currentPath + ".") ? pathToCheck.substring(currentPath.length() + 1) : null);
            
            if (relativePath == null) {
                continue;
            }
            
            // Check if this is a direct child (no more dots)
            String firstSegment = relativePath.contains(".") ? relativePath.substring(0, relativePath.indexOf(".")) : relativePath;
            String fullPathSoFar = currentPath.isEmpty() ? firstSegment : currentPath + "." + firstSegment;
            
            if (processedPaths.contains(fullPathSoFar)) {
                continue;
            }
            
            // Check if this field is a DBRef
            try {
                Field field = currentClass.getDeclaredField(firstSegment);
                if (field.isAnnotationPresent(DBRef.class)) {
                    // Add lookup operations
                    String targetCollection = getCollectionNameFromField(field);
                    String lookupAs = fullPathSoFar.replace(".", "_") + "_lookup";
                    
                    // Determine the local field to lookup from
                    // After previous lookups, we need to reference the looked-up field
                    String localField;
                    if (currentLookupPath.isEmpty()) {
                        localField = firstSegment;
                    } else {
                        localField = currentLookupPath + "." + firstSegment;
                    }
                    
                    // Check if it's a collection field
                    boolean isCollection = Collection.class.isAssignableFrom(field.getType());
                    
                    // Perform $lookup
                    // Both single references and collections use DBRef format with $id field
                    // Single: {$ref: "...", $id: ObjectId(...)}
                    // Array: [{$ref: "...", $id: ObjectId(...)}, ...]
                    operations.add(lookup(targetCollection, localField + ".$id", "_id", lookupAs));
                    
                    // Unwind the lookup result
                    operations.add(unwind(lookupAs, true));
                    
                    // For non-collection fields, replace the original DBRef with the looked up document
                    // For collections, we keep using the lookup field name in queries
                    String nextLookupPath;
                    if (!isCollection) {
                        operations.add(Aggregation.addFields()
                            .addField(localField).withValue("$" + lookupAs)
                            .build());
                        nextLookupPath = localField;
                    } else {
                        nextLookupPath = lookupAs;
                    }
                    
                    processedPaths.add(fullPathSoFar);
                    
                    // Get the target class and recurse for deeper paths
                    Class<?> targetClass = getTargetClassFromField(field);
                    buildLookupsRecursive(targetClass, fullPathSoFar, nextLookupPath, allNestedPaths, operations, processedPaths);
                }
            } catch (NoSuchFieldException e) {
                // Skip if field not found
            }
        }
    }

    /**
     * Get the target class from a field (handling collections)
     */
    private Class<?> getTargetClassFromField(Field field) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            ParameterizedType genericType = (ParameterizedType) field.getGenericType();
            return (Class<?>) genericType.getActualTypeArguments()[0];
        } else {
            return field.getType();
        }
    }

    /**
     * Collect all nested paths from criteria and select fields (including deep nested paths)
     * Handles both '.' and '<' syntax for nested paths
     */
    private <Entity> Set<String> collectAllNestedPaths(Class<Entity> entityClass, DynamicQuery dynamicQuery) {
        Set<String> allPaths = new HashSet<>();
        
        // Collect paths from WHERE criteria
        for (com.beyt.jdq.dto.Criteria c : dynamicQuery.getWhere()) {
            String fieldKey = c.getKey();
            
            // Convert left join syntax to dot notation for analysis
            // "roles<roleAuthorizations<authorization<menuIcon" -> "roles.roleAuthorizations.authorization.menuIcon"
            String pathToAnalyze = fieldKey.replace("<", ".");
            
            // For nested paths like "department.name" or "roles.roleAuthorizations.authorization.menuIcon"
            if (pathToAnalyze.contains(".")) {
                // Add all parent paths that need lookup
                String[] segments = pathToAnalyze.split("\\.");
                for (int i = 0; i < segments.length - 1; i++) {
                    StringBuilder pathBuilder = new StringBuilder();
                    for (int j = 0; j <= i; j++) {
                        if (j > 0) pathBuilder.append(".");
                        pathBuilder.append(segments[j]);
                    }
                    allPaths.add(pathBuilder.toString());
                }
            }
        }
        
        // Collect paths from SELECT fields
        if (CollectionUtils.isNotEmpty(dynamicQuery.getSelect())) {
            for (Pair<String, String> selectPair : dynamicQuery.getSelect()) {
                String fieldKey = selectPair.getFirst();
                
                // Convert left join syntax to dot notation for analysis
                String pathToAnalyze = fieldKey.replace("<", ".");
                
                // For nested paths like "user.name"
                if (pathToAnalyze.contains(".")) {
                    // Add all parent paths that need lookup
                    String[] segments = pathToAnalyze.split("\\.");
                    for (int i = 0; i < segments.length - 1; i++) {
                        StringBuilder pathBuilder = new StringBuilder();
                        for (int j = 0; j <= i; j++) {
                            if (j > 0) pathBuilder.append(".");
                            pathBuilder.append(segments[j]);
                        }
                        allPaths.add(pathBuilder.toString());
                    }
                }
            }
        }
        
        // Collect paths from ORDER BY fields
        if (CollectionUtils.isNotEmpty(dynamicQuery.getOrderBy())) {
            for (Pair<String, com.beyt.jdq.dto.enums.Order> orderPair : dynamicQuery.getOrderBy()) {
                String fieldKey = orderPair.getFirst();
                
                // Convert left join syntax to dot notation for analysis
                String pathToAnalyze = fieldKey.replace("<", ".");
                
                // For nested paths
                if (pathToAnalyze.contains(".")) {
                    // Add all parent paths that need lookup
                    String[] segments = pathToAnalyze.split("\\.");
                    for (int i = 0; i < segments.length - 1; i++) {
                        StringBuilder pathBuilder = new StringBuilder();
                        for (int j = 0; j <= i; j++) {
                            if (j > 0) pathBuilder.append(".");
                            pathBuilder.append(segments[j]);
                        }
                        allPaths.add(pathBuilder.toString());
                    }
                }
            }
        }
        
        return allPaths;
    }

    /**
     * Get the MongoDB collection name from a DBRef field
     */
    private String getCollectionNameFromField(Field field) {
        Class<?> refType;
        
        // Check if it's a List/Collection type
        if (Collection.class.isAssignableFrom(field.getType())) {
            ParameterizedType genericType = (ParameterizedType) field.getGenericType();
            refType = (Class<?>) genericType.getActualTypeArguments()[0];
        } else {
            refType = field.getType();
        }
        
        return getCollectionName(refType);
    }

    /**
     * Get the MongoDB collection name for an entity class
     */
    private String getCollectionName(Class<?> entityClass) {
        org.springframework.data.mongodb.core.mapping.Document docAnnotation = 
            entityClass.getAnnotation(org.springframework.data.mongodb.core.mapping.Document.class);
        
        if (docAnnotation != null && !docAnnotation.collection().isEmpty()) {
            return docAnnotation.collection();
        }
        
        // Default: convert class name to lowercase plural
        String className = entityClass.getSimpleName();
        return className.toLowerCase() + "s";
    }

    /**
     * Apply order by to query from DynamicQuery
     */
    private void applyOrderBy(Query query, DynamicQuery dynamicQuery) {
        if (CollectionUtils.isNotEmpty(dynamicQuery.getOrderBy())) {
            dynamicQuery.getOrderBy().forEach(orderPair -> {
                query.with(org.springframework.data.domain.Sort.by(
                    orderPair.getSecond().getDirection(),
                    orderPair.getFirst()
                ));
            });
        }
    }

    /**
     * Extract field mappings from @JdqModel annotated class
     */
    private <ResultType> void extractIfJdqModel(DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass) {
        if (!resultTypeClass.isAnnotationPresent(JdqModel.class)) {
            return;
        }

        List<Pair<String, String>> select = new ArrayList<>();
        recursiveSubModelFiller(resultTypeClass, select, new ArrayList<>(), "");
        dynamicQuery.setSelect(select);
    }

    /**
     * Recursively process @JdqSubModel and @JdqField annotations
     */
    private <ResultType> void recursiveSubModelFiller(Class<ResultType> resultTypeClass, 
                                                      List<Pair<String, String>> select, 
                                                      List<String> dbPrefixList, 
                                                      String entityPrefix) {
        Field[] fields = resultTypeClass.isRecord() ? 
            Arrays.stream(resultTypeClass.getRecordComponents())
                .map(rc -> {
                    try {
                        return resultTypeClass.getDeclaredField(rc.getName());
                    } catch (NoSuchFieldException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toArray(Field[]::new) :
            resultTypeClass.getDeclaredFields();

        for (Field declaredField : fields) {
            // Check if field should be ignored
            if (declaredField.isAnnotationPresent(com.beyt.jdq.annotation.model.JdqIgnoreField.class)) {
                // For records, we cannot ignore fields because all constructor parameters must be provided
                if (resultTypeClass.isRecord()) {
                    throw new DynamicQueryIllegalArgumentException(
                        "Record doesn't support @JdqIgnoreField annotation on component: " + declaredField.getName()
                    );
                }
                continue;  // Skip this field for non-record classes
            }
            
            if (declaredField.isAnnotationPresent(JdqSubModel.class)) {
                String subModelValue = declaredField.getAnnotation(JdqSubModel.class).value();
                ArrayList<String> newPrefixList = new ArrayList<>(dbPrefixList);
                if (StringUtils.isNotBlank(subModelValue)) {
                    newPrefixList.add(subModelValue);
                }
                recursiveSubModelFiller(declaredField.getType(), select, newPrefixList, 
                    entityPrefix + declaredField.getName() + ".");
            } else if (FieldUtil.isSupportedType(declaredField.getType())) {
                if (declaredField.isAnnotationPresent(JdqField.class)) {
                    select.add(Pair.of(
                        prefixCreator(dbPrefixList) + declaredField.getAnnotation(JdqField.class).value(), 
                        entityPrefix + declaredField.getName()
                    ));
                } else {
                    select.add(Pair.of(
                        prefixCreator(dbPrefixList) + declaredField.getName(), 
                        entityPrefix + declaredField.getName()
                    ));
                }
            } else {
                if (resultTypeClass.isRecord()) {
                    throw new DynamicQueryIllegalArgumentException(
                        "Record doesn't support nested model type: " + declaredField.getType().getName()
                    );
                }
            }
        }
    }

    /**
     * Create prefix string from list
     */
    private String prefixCreator(List<String> prefixList) {
        String collect = String.join(".", prefixList);
        if (StringUtils.isNotBlank(collect)) {
            collect += ".";
        }
        return collect;
    }

    /**
     * Execute aggregation with projection
     */
    private <Entity, ResultType> List<ResultType> executeAggregationWithProjection(
            Class<Entity> entityClass, 
            DynamicQuery dynamicQuery, 
            Class<ResultType> resultClass) {
        return executeAggregationWithProjection(entityClass, dynamicQuery, resultClass, null);
    }

    /**
     * Execute aggregation with projection and pagination
     */
    private <Entity, ResultType> List<ResultType> executeAggregationWithProjection(
            Class<Entity> entityClass, 
            DynamicQuery dynamicQuery, 
            Class<ResultType> resultClass,
            Pageable pageable) {
        Aggregation aggregation = buildAggregation(entityClass, dynamicQuery, pageable, false, resultClass);
        AggregationResults<Document> results = mongoTemplate.aggregate(
            aggregation, 
            getCollectionName(entityClass), 
            Document.class
        );
        
        return results.getMappedResults().stream()
            .map(doc -> convertDocumentToResultType(doc, resultClass, dynamicQuery))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Convert MongoDB Document to result type using field mappings
     */
    private <ResultType> ResultType convertDocumentToResultType(
            Document doc, 
            Class<ResultType> resultClass, 
            DynamicQuery dynamicQuery) {
        try {
            if (CollectionUtils.isEmpty(dynamicQuery.getSelect())) {
                // No projection, direct conversion
                return mongoTemplate.getConverter().read(resultClass, doc);
            }

            // Build a map of field names to values from the document
            Map<String, Object> fieldValues = new HashMap<>();
            for (Pair<String, String> selectPair : dynamicQuery.getSelect()) {
                String targetField = selectPair.getSecond(); // The result class field name
                
                // Get value from document - handle nested paths
                Object value = getNestedValue(doc, targetField);
                fieldValues.put(targetField, value);
            }

            return createInstance(resultClass, fieldValues, dynamicQuery);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get value from document using nested path (e.g., "role.roleId" -> doc.role.roleId)
     */
    private Object getNestedValue(Document doc, String path) {
        String[] parts = path.split("\\.");
        Object current = doc;
        
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            if (current instanceof Document) {
                current = ((Document) current).get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }

    /**
     * Create instance of result class using field values
     */
    private <ResultType> ResultType createInstance(
            Class<ResultType> resultClass, 
            Map<String, Object> fieldValues,
            DynamicQuery dynamicQuery) throws Exception {
        
        if (resultClass.isRecord()) {
            return createRecordInstance(resultClass, fieldValues, dynamicQuery);
        } else {
            return createClassInstance(resultClass, fieldValues, dynamicQuery);
        }
    }

    /**
     * Create record instance using constructor
     */
    private <ResultType> ResultType createRecordInstance(
            Class<ResultType> resultClass, 
            Map<String, Object> fieldValues,
            DynamicQuery dynamicQuery) throws Exception {
        
        RecordComponent[] components = resultClass.getRecordComponents();
        Class<?>[] paramTypes = new Class<?>[components.length];
        Object[] args = new Object[components.length];
        
        for (int i = 0; i < components.length; i++) {
            paramTypes[i] = components[i].getType();
            String fieldName = components[i].getName();
            
            // Get the field to check for @JdqSubModel annotation
            Field field = resultClass.getDeclaredField(fieldName);
            
            // Check if it's a sub-model
            if (field.isAnnotationPresent(JdqSubModel.class)) {
                // Recursively create sub-model
                Map<String, Object> subFieldValues = new HashMap<>();
                String prefix = fieldName + ".";
                for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
                    if (entry.getKey().startsWith(prefix)) {
                        subFieldValues.put(entry.getKey().substring(prefix.length()), entry.getValue());
                    }
                }
                args[i] = createInstance(components[i].getType(), subFieldValues, dynamicQuery);
            } else {
                Object value = fieldValues.get(fieldName);
                args[i] = convertValue(value, components[i].getType());
            }
        }
        
        Constructor<ResultType> constructor = resultClass.getDeclaredConstructor(paramTypes);
        return constructor.newInstance(args);
    }

    /**
     * Create class instance using no-arg constructor and setters
     */
    private <ResultType> ResultType createClassInstance(
            Class<ResultType> resultClass, 
            Map<String, Object> fieldValues,
            DynamicQuery dynamicQuery) throws Exception {
        
        ResultType instance = resultClass.getDeclaredConstructor().newInstance();
        
        for (Field field : resultClass.getDeclaredFields()) {
            String fieldName = field.getName();
            
            // Check if it's a sub-model
            if (field.isAnnotationPresent(JdqSubModel.class)) {
                Map<String, Object> subFieldValues = new HashMap<>();
                String prefix = fieldName + ".";
                for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
                    if (entry.getKey().startsWith(prefix)) {
                        subFieldValues.put(entry.getKey().substring(prefix.length()), entry.getValue());
                    }
                }
                Object subModel = createInstance(field.getType(), subFieldValues, dynamicQuery);
                field.setAccessible(true);
                field.set(instance, subModel);
            } else if (fieldValues.containsKey(fieldName)) {
                Object value = fieldValues.get(fieldName);
                Object convertedValue = convertValue(value, field.getType());
                
                // Find setter method
                String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                try {
                    Method setter = resultClass.getMethod(setterName, field.getType());
                    setter.invoke(instance, convertedValue);
                } catch (NoSuchMethodException e) {
                    // Try direct field access
                    field.setAccessible(true);
                    field.set(instance, convertedValue);
                }
            }
        }
        
        return instance;
    }
    
    /**
     * Convert value to target type
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        // If types already match, return as is
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        // Handle numeric conversions
        if (value instanceof Number) {
            Number numValue = (Number) value;
            if (targetType == Integer.class || targetType == int.class) {
                return numValue.intValue();
            } else if (targetType == Long.class || targetType == long.class) {
                return numValue.longValue();
            } else if (targetType == Double.class || targetType == double.class) {
                return numValue.doubleValue();
            } else if (targetType == Float.class || targetType == float.class) {
                return numValue.floatValue();
            } else if (targetType == Short.class || targetType == short.class) {
                return numValue.shortValue();
            } else if (targetType == Byte.class || targetType == byte.class) {
                return numValue.byteValue();
            }
        }
        
        // Handle Date/Instant conversions
        if (value instanceof java.util.Date && targetType == java.time.Instant.class) {
            return ((java.util.Date) value).toInstant();
        }
        if (value instanceof java.time.Instant && targetType == java.util.Date.class) {
            return java.util.Date.from((java.time.Instant) value);
        }
        
        // Handle String conversions
        if (targetType == String.class) {
            return value.toString();
        }
        
        // Handle enum conversions
        if (targetType.isEnum() && value instanceof String) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object enumValue = Enum.valueOf((Class<? extends Enum>) targetType, (String) value);
            return enumValue;
        }
        
        // Default: return as is and let reflection handle it
        return value;
    }
}
