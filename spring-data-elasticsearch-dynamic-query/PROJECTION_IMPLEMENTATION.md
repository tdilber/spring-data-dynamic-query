# Elasticsearch Projection Implementation

## Overview
This document describes the implementation of SELECT, DISTINCT, and PROJECTION features for the Elasticsearch Dynamic Query library.

## Features Implemented

### 1. SELECT Projection Support
The library now supports field-level projection, allowing you to select specific fields from entities and map them to result classes.

**Example - Manual Field Mapping:**
```java
DynamicQuery dynamicQuery = new DynamicQuery();
dynamicQuery.getSelect().add(Pair.of("name", "name"));
dynamicQuery.getSelect().add(Pair.of("description", "description"));
List<CourseName> result = courseRepository.findAll(dynamicQuery, CourseName.class);
```

**Example - Field Alias:**
```java
DynamicQuery dynamicQuery = new DynamicQuery();
dynamicQuery.getSelect().add(Pair.of("description", "name")); // Map description to name field
List<CourseName> result = courseRepository.findAll(dynamicQuery, CourseName.class);
```

### 2. DISTINCT Support
The library now supports removing duplicate results using the `distinct` flag.

**Example:**
```java
DynamicQuery dynamicQuery = new DynamicQuery();
dynamicQuery.setDistinct(true);
dynamicQuery.setWhere(CriteriaList.of(
    Criteria.of("courses.id", CriteriaOperator.GREATER_THAN, 1),
    Criteria.of("id", CriteriaOperator.EQUAL, 2)
));
List<Student> studentList = studentRepository.findAll(dynamicQuery);
// Returns distinct students only
```

### 3. @JdqModel and @JdqField Annotation Support
Use annotations to automatically map entity fields to projection class fields without manually specifying select clauses.

**Example:**
```java
@JdqModel
public class AnnotatedCourseName {
    @JdqField("name")
    private String courseName;
    
    @JdqField("description")
    private String courseDescription;
    
    // getters/setters...
}

DynamicQuery dynamicQuery = new DynamicQuery();
// No need to specify select fields - they're extracted from annotations
List<AnnotatedCourseName> result = courseRepository.findAll(dynamicQuery, AnnotatedCourseName.class);
```

### 4. @JdqSubModel Support for Nested Projections
Support for hierarchical projections using nested models.

**Example - Simple SubModel:**
```java
@JdqModel
public class StudentWithDepartment {
    @JdqField("id")
    private Long studentId;
    
    @JdqField("name")
    private String studentName;
    
    @JdqSubModel("department")
    private DepartmentInfo department;
    
    @JdqModel
    public static class DepartmentInfo {
        @JdqField("id")
        private Long departmentId;
        
        @JdqField("name")
        private String departmentName;
    }
}
```

**Example - Empty SubModel Annotation:**
```java
@JdqModel
public class StudentProjection {
    @JdqField("id")
    private Long studentId;
    
    @JdqSubModel() // Empty annotation - uses full paths in nested fields
    private DepartmentInfo department;
    
    @JdqModel
    public static class DepartmentInfo {
        @JdqField("department.id")  // Full path specified
        private Long departmentId;
        
        @JdqField("department.name") // Full path specified
        private String departmentName;
    }
}
```

### 5. Record Support
Full support for Java records in projections.

**Example:**
```java
public record CourseNameRecord(String name) {}

DynamicQuery dynamicQuery = new DynamicQuery();
dynamicQuery.getSelect().add(Pair.of("name", "name"));
List<CourseNameRecord> result = courseRepository.findAll(dynamicQuery, CourseNameRecord.class);
```

**Example with Annotations:**
```java
@JdqModel
public record AnnotatedCourseRecord(
    @JdqField("name") String courseName,
    @JdqField("description") String courseDescription
) {}
```

### 6. Pagination with Projection
Projection works seamlessly with pagination.

**Example:**
```java
DynamicQuery dynamicQuery = new DynamicQuery();
dynamicQuery.getSelect().add(Pair.of("name", "name"));
dynamicQuery.setPageSize(10);
dynamicQuery.setPageNumber(0);

Page<CourseName> page = courseRepository.findAllAsPage(dynamicQuery, CourseName.class);
```

## Implementation Details

### Architecture
The projection implementation consists of several key components:

1. **Field Extraction**: `extractIfJdqModel()` - Extracts field mappings from @JdqModel annotated classes
2. **Recursive Processing**: `recursiveSubModelFiller()` - Handles nested @JdqSubModel annotations
3. **Entity Conversion**: `convertEntityToResultType()` - Maps entity fields to result class
4. **Value Extraction**: `getFieldValue()` - Extracts values from entities using reflection
5. **Instance Creation**: `createInstance()`, `createRecordInstance()`, `createClassInstance()` - Creates result instances
6. **Type Conversion**: `convertValue()` - Handles type conversions (numeric, date, enum, etc.)

### Type Conversions Supported
- Numeric types (Integer, Long, Double, Float, Short, Byte)
- Date/Instant conversions
- String conversions
- Enum conversions
- Direct assignments for matching types

### How It Works

1. **Annotation Processing**: When a result class with `@JdqModel` is provided, the library automatically scans for `@JdqField` and `@JdqSubModel` annotations and builds the select clause.

2. **Query Execution**: The query is executed against Elasticsearch with the standard entity class.

3. **Result Mapping**: Each entity result is converted to the target result type by:
   - Extracting field values based on the select clause
   - Creating nested objects for `@JdqSubModel` fields
   - Converting types as needed
   - Constructing the result instance (using constructors for records, setters for classes)

4. **Distinct Processing**: If distinct is enabled, the results are deduplicated using Java streams.

## Testing

The implementation was verified with comprehensive tests covering:
- ✓ Simple field projection
- ✓ Multi-field projection
- ✓ Field aliasing
- ✓ Record projection
- ✓ Annotated projection (@JdqModel, @JdqField)
- ✓ Nested projection (@JdqSubModel)
- ✓ Distinct filtering
- ✓ Pagination with projection

All tests passed successfully.

## Usage Notes

1. **Performance**: Projection can improve performance by reducing data transfer, but the entity is still fully loaded from Elasticsearch before projection is applied.

2. **Nested Fields**: Use dot notation for nested field access (e.g., "department.name").

3. **Null Handling**: The implementation gracefully handles null values in nested paths.

4. **Equals/HashCode**: For distinct to work properly, ensure your entity classes have proper `equals()` and `hashCode()` implementations that compare all relevant fields.

5. **Compatibility**: The implementation follows the same patterns as JPA and MongoDB implementations for consistency across the library.

## Future Enhancements

Possible future improvements:
- Elasticsearch-native field filtering using `_source` includes/excludes
- Aggregation-based projections
- Script-based field transformations
- Performance optimizations for large result sets

## Related Files

Key files modified:
- `ElasticsearchSearchQueryTemplate.java` - Main implementation
- `Course.java` - Updated equals() method for proper distinct support

