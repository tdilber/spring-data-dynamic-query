package com.beyt.jdq.query;

import com.beyt.jdq.deserializer.IDeserializer;
import javax.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Context object that holds dependencies required for dynamic query execution
 */
@Getter
@AllArgsConstructor
public class RepositoryContext {
    private final EntityManager entityManager;
    private final IDeserializer deserializer;
}

