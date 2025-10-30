package com.beyt.jdq.jpa.query;

import com.beyt.jdq.core.model.annotation.JdqField;
import com.beyt.jdq.core.model.annotation.JdqModel;
import com.beyt.jdq.core.model.annotation.JdqSubModel;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Handles updating entities based on JdqModel annotated objects
 * Supports partial updates and nested entity updates within a single transaction
 */
public class JdqModelUpdater {
    
    private final EntityManager entityManager;
    
    public JdqModelUpdater(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    /**
     * Updates entities based on JdqModel annotated object
     * @param jdqModel The annotated model containing update data
     * @param domainClass The main entity class
     * @throws IllegalArgumentException if jdqModel is not annotated with @JdqModel or main entity ID is missing
     */
    public void update(Object jdqModel, Class<?> domainClass) {
        if (jdqModel == null) {
            throw new IllegalArgumentException("JdqModel cannot be null");
        }
        
        Class<?> jdqModelClass = jdqModel.getClass();
        if (!jdqModelClass.isAnnotationPresent(JdqModel.class)) {
            throw new IllegalArgumentException("Object must be annotated with @JdqModel");
        }
        
        // Parse the model and perform updates
        UpdateContext context = new UpdateContext(jdqModel, domainClass);
        processModel(context);
        
        // Validate main entity has ID
        if (context.mainEntityId == null) {
            throw new IllegalArgumentException("Main entity ID is required for update operation");
        }
        
        // Execute all updates in the same transaction
        executeUpdates(context);
    }
    
    private void processModel(UpdateContext context) {
        processModelFields(context.jdqModel, context.jdqModelClass, context, "");
    }
    
    private void processModelFields(Object modelInstance, Class<?> modelClass, UpdateContext context, String pathPrefix) {
        // Check if it's a record
        if (modelClass.isRecord()) {
            processRecordComponents(modelInstance, modelClass, context, pathPrefix);
        } else {
            processClassFields(modelInstance, modelClass, context, pathPrefix);
        }
    }
    
    private void processRecordComponents(Object recordInstance, Class<?> recordClass, UpdateContext context, String pathPrefix) {
        RecordComponent[] components = recordClass.getRecordComponents();
        for (RecordComponent component : components) {
            try {
                Object value = component.getAccessor().invoke(recordInstance);
                if (value == null) {
                    continue; // Skip null values
                }
                
                JdqField jdqField = component.getAnnotation(JdqField.class);
                JdqSubModel jdqSubModel = component.getAnnotation(JdqSubModel.class);
                
                if (jdqField != null) {
                    String fieldPath = jdqField.value();
                    processFieldValue(fieldPath, value, context, pathPrefix);
                } else if (jdqSubModel != null) {
                    String subModelPath = jdqSubModel.value();
                    String newPrefix = pathPrefix.isEmpty() ? subModelPath : 
                                      (subModelPath.isEmpty() ? pathPrefix : pathPrefix + "." + subModelPath);
                    Class<?> componentType = component.getType();
                    if (componentType.isRecord() || componentType.isAnnotationPresent(JdqModel.class)) {
                        processModelFields(value, componentType, context, newPrefix);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to process record component: " + component.getName(), e);
            }
        }
    }
    
    private void processClassFields(Object modelInstance, Class<?> modelClass, UpdateContext context, String pathPrefix) {
        Field[] fields = getAllFields(modelClass);
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(modelInstance);
                if (value == null) {
                    continue; // Skip null values
                }
                
                JdqField jdqField = field.getAnnotation(JdqField.class);
                JdqSubModel jdqSubModel = field.getAnnotation(JdqSubModel.class);
                
                if (jdqField != null) {
                    String fieldPath = jdqField.value();
                    processFieldValue(fieldPath, value, context, pathPrefix);
                } else if (jdqSubModel != null) {
                    String subModelPath = jdqSubModel.value();
                    String newPrefix = pathPrefix.isEmpty() ? subModelPath : 
                                      (subModelPath.isEmpty() ? pathPrefix : pathPrefix + "." + subModelPath);
                    processModelFields(value, field.getType(), context, newPrefix);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to process field: " + field.getName(), e);
            }
        }
    }
    
    private void processFieldValue(String fieldPath, Object value, UpdateContext context, String pathPrefix) {
        // Handle empty fieldPath (can occur with records when @JdqField is not used properly)
        if (fieldPath == null || fieldPath.trim().isEmpty()) {
            return;
        }
        
        String fullPath = pathPrefix.isEmpty() ? fieldPath : 
                         (fieldPath.startsWith(pathPrefix) ? fieldPath : pathPrefix + "." + fieldPath);
        
        // Parse the path to determine entity and field
        String[] pathParts = fullPath.split("\\.");
        
        if (pathParts.length == 1) {
            // Main entity field
            String fieldName = pathParts[0];
            if (isIdField(context.domainClass, fieldName)) {
                context.mainEntityId = value;
            } else {
                context.addMainEntityUpdate(fieldName, value);
            }
        } else {
            // Joined entity field
            String relationPath = String.join(".", Arrays.copyOfRange(pathParts, 0, pathParts.length - 1));
            String fieldName = pathParts[pathParts.length - 1];
            
            Class<?> relatedEntityClass = getRelatedEntityClass(context.domainClass, relationPath);
            if (relatedEntityClass != null) {
                if (isIdField(relatedEntityClass, fieldName)) {
                    context.addJoinedEntityId(relationPath, relatedEntityClass, value);
                } else {
                    context.addJoinedEntityUpdate(relationPath, relatedEntityClass, fieldName, value);
                }
            }
        }
    }
    
    /**
     * Check if a field name corresponds to an ID field in the entity.
     * This works for both regular classes (checks @Id annotation) and any field name that is "id".
     */
    private boolean isIdField(Class<?> entityClass, String fieldName) {
        // Common convention: if field name is "id", treat it as ID field
        if ("id".equalsIgnoreCase(fieldName)) {
            return true;
        }
        
        // Also check for @Id annotation on the actual entity field
        try {
            Field field = findField(entityClass, fieldName);
            return field != null && field.isAnnotationPresent(Id.class);
        } catch (Exception e) {
            return false;
        }
    }
    
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
    
    private Field[] getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }
    
    private Class<?> getRelatedEntityClass(Class<?> entityClass, String relationPath) {
        try {
            String[] parts = relationPath.split("\\.");
            Class<?> currentClass = entityClass;
            
            for (String part : parts) {
                Field field = findField(currentClass, part);
                if (field == null) {
                    return null;
                }
                
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    // Handle collections (List, Set, etc.)
                    ParameterizedType paramType = (ParameterizedType) genericType;
                    currentClass = (Class<?>) paramType.getActualTypeArguments()[0];
                } else {
                    currentClass = field.getType();
                }
            }
            
            // Verify it's an entity
            if (currentClass.getAnnotation(javax.persistence.Entity.class) != null) {
                return currentClass;
            }
        } catch (Exception e) {
            // Ignore and return null
        }
        return null;
    }
    
    private void executeUpdates(UpdateContext context) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        
        // Update main entity
        if (!context.mainEntityUpdates.isEmpty()) {
            executeUpdateForEntity(cb, context.domainClass, context.mainEntityUpdates, context.mainEntityId, true);
        }
        
        // Update joined entities
        for (JoinedEntityUpdate joinedUpdate : context.joinedEntityUpdates) {
            if (joinedUpdate.entityId == null) {
                // Skip if no ID provided for joined entity
                continue;
            }
            
            // Skip if no fields to update (only ID was provided)
            if (joinedUpdate.updates.isEmpty()) {
                continue;
            }
            
            executeUpdateForEntity(cb, joinedUpdate.entityClass, joinedUpdate.updates, joinedUpdate.entityId, false);
        }
        
        entityManager.flush();
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void executeUpdateForEntity(CriteriaBuilder cb, Class<?> entityClass, Map<String, Object> updates, 
                                       Object entityId, boolean throwIfNotFound) {
        CriteriaUpdate criteriaUpdate = cb.createCriteriaUpdate(entityClass);
        Root root = criteriaUpdate.from(entityClass);
        
        // Set all fields to update
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            criteriaUpdate.set(root.get(entry.getKey()), entry.getValue());
        }
        
        // Where clause with ID
        Field idField = getIdField(entityClass);
        if (idField != null) {
            criteriaUpdate.where(cb.equal(root.get(idField.getName()), entityId));
        }
        
        int updated = entityManager.createQuery(criteriaUpdate).executeUpdate();
        if (throwIfNotFound && updated == 0) {
            throw new IllegalArgumentException("No entity found with ID: " + entityId);
        }
    }
    
    private Field getIdField(Class<?> entityClass) {
        for (Field field : getAllFields(entityClass)) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        return null;
    }
    
    /**
     * Context class to hold update information
     */
    private static class UpdateContext {
        final Object jdqModel;
        final Class<?> jdqModelClass;
        final Class<?> domainClass;
        Object mainEntityId;
        final Map<String, Object> mainEntityUpdates = new LinkedHashMap<>();
        final List<JoinedEntityUpdate> joinedEntityUpdates = new ArrayList<>();
        final Map<String, JoinedEntityUpdate> joinedEntityMap = new HashMap<>();
        
        UpdateContext(Object jdqModel, Class<?> domainClass) {
            this.jdqModel = jdqModel;
            this.jdqModelClass = jdqModel.getClass();
            this.domainClass = domainClass;
        }
        
        void addMainEntityUpdate(String fieldName, Object value) {
            mainEntityUpdates.put(fieldName, value);
        }
        
        void addJoinedEntityId(String relationPath, Class<?> entityClass, Object idValue) {
            JoinedEntityUpdate update = joinedEntityMap.computeIfAbsent(
                relationPath, 
                k -> {
                    JoinedEntityUpdate jeu = new JoinedEntityUpdate(relationPath, entityClass);
                    joinedEntityUpdates.add(jeu);
                    return jeu;
                }
            );
            update.entityId = idValue;
        }
        
        void addJoinedEntityUpdate(String relationPath, Class<?> entityClass, String fieldName, Object value) {
            JoinedEntityUpdate update = joinedEntityMap.computeIfAbsent(
                relationPath, 
                k -> {
                    JoinedEntityUpdate jeu = new JoinedEntityUpdate(relationPath, entityClass);
                    joinedEntityUpdates.add(jeu);
                    return jeu;
                }
            );
            update.updates.put(fieldName, value);
        }
    }
    
    /**
     * Represents an update to a joined entity
     */
    private static class JoinedEntityUpdate {
        final String relationPath;
        final Class<?> entityClass;
        Object entityId;
        final Map<String, Object> updates = new LinkedHashMap<>();
        
        JoinedEntityUpdate(String relationPath, Class<?> entityClass) {
            this.relationPath = relationPath;
            this.entityClass = entityClass;
        }
    }
}

