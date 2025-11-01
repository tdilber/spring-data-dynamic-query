package com.beyt.jdq.mongodb.core;

import com.beyt.jdq.core.model.DynamicQuery;
import org.springframework.data.mongodb.core.query.Criteria;

/**
 * Functional interface for converting core Criteria to MongoDB Criteria.
 */
public interface CriteriaBuilderFunction {
    /**
     * Applies the search criteria to create MongoDB Criteria.
     * 
     * @param searchCriteria the core search criteria
     * @param searchQuery the dynamic query containing additional context
     * @return the MongoDB Criteria object
     */
    Criteria apply(com.beyt.jdq.core.model.Criteria searchCriteria, DynamicQuery searchQuery);
}
