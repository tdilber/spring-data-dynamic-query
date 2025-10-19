# Elasticsearch Nested Query Implementation

## Overview
This document describes the implementation of nested query support for Elasticsearch in the dynamic query framework.

## Implementation Status

### ✅ Completed - All Tests Passing!
1. **Nested Query Builder** - Added support for detecting and building nested queries in `ElasticsearchSearchQueryTemplate`
2. **Multi-level Nesting** - Supports deep nested paths like `roles.roleAuthorizations.authorization.menuIcon`
3. **Left Join Support** - Handles the `<` operator syntax for optional nested relationships
4. **Entity Structure** - Created proper entity structure with nested documents avoiding circular references:
   - `Student` has nested `Department` (without students)
   - `Department` has nested `StudentInfo` (simplified student without department)
   - `AdminUser` -> `Role` -> `RoleAuthorization` -> `Authorization` chain
5. **Test Enablement** - Enabled `S5_Join` and `S6_Advanced_Join` test classes
6. **Index Mapping Solution** - Implemented automatic index recreation with proper mappings in `BaseElasticsearchJoinTestInstance`
7. **Embedded Object Support** - Added detection for embedded (Object type) vs nested fields
8. **Test Results:**
   - ✅ S5_Join: 7/7 tests passing
   - ✅ S6_Advanced_Join: 4/4 tests passing
   - ✅ Total: 11/11 nested query tests passing

**Generated Query (Correct):**
```json
{
  "nested" : {
    "query" : {
      "prefix" : {
        "department.name" : {
          "value" : "p",
          "case_insensitive" : true,
          "boost" : 1.0
        }
      }
    },
    "path" : "department",
    "ignore_unmapped" : false,
    "score_mode" : "none",
    "boost" : 1.0
  }
}
```

## Implementation Details

### 1. Nested Path Detection
The `ElasticsearchSearchQueryTemplate` now analyzes field paths to detect nested structures:
- `department.name` → nested path: `[department]`
- `roles.roleAuthorizations.authorization.menuIcon` → nested paths: `[roles, roles.roleAuthorizations, roles.roleAuthorizations.authorization]`

### 2. Nested Query Building
For each nested level, the implementation wraps the inner query with a `NestedQueryBuilder`:
```java
QueryBuilders.nestedQuery(nestedPath, innerQuery, ScoreMode.None)
```

### 3. Left Join Support
When the field path contains `<` (e.g., `department<id`), the implementation:
1. Converts it to dot notation (`department.id`)
2. Wraps the nested query to include documents where the nested field doesn't exist:
```java
BoolQueryBuilder leftJoinQuery = QueryBuilders.boolQuery();
leftJoinQuery.should(QueryBuilders.nestedQuery(nestedPath, currentQuery, scoreMode));
leftJoinQuery.should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(nestedPath)));
leftJoinQuery.minimumShouldMatch(1);
```

### 4. Circular Reference Prevention
Elasticsearch nested documents require careful structure to avoid circular references:

**Problem:** 
- `Student` contains nested `Department`
- `Department` contains nested `Student` (circular!)

**Solution:**
- `Student` has full nested `Department` (without students field)
- `Department` has nested `StudentInfo` (simplified student data without department reference)

## Code Changes

### Modified Files
1. **`ElasticsearchSearchQueryTemplate.java`**
   - Added `analyzeNestedPath()` method to detect nested field paths
   - Added `buildNestedQuery()` method to wrap queries in nested query builders
   - Added `buildFieldQuery()` method for building field-level queries
   - Added `NestedPathInfo` helper class

2. **`Student.java`**
   - Enabled nested `Department` field
   - Enabled nested `Course` list field
   - Enabled embedded `Address` object field

3. **`Department.java`**
   - Changed nested students to use `StudentInfo` instead of `Student`

4. **`StudentInfo.java`** (New)
   - Simplified student information for storage in Department
   - Avoids circular reference by not including department

### Test Files
1. **`S5_Join.java`** - Enabled (removed `@Disabled`)
2. **`S6_Advanced_Join.java`** - Enabled (removed `@Disabled`)
3. **`BaseElasticsearchJoinTestInstance.java`** - Updated to use `StudentInfo`

## Next Steps

### Immediate (To Fix Current Issue)
1. **Option A: Delete and Recreate Indices**
   ```bash
   # Delete existing indices
   curl -X DELETE "localhost:9200/students"
   curl -X DELETE "localhost:9200/departments"
   
   # Restart tests - Spring Data Elasticsearch will recreate with correct mappings
   mvn test -Dtest=S5_Join
   ```

2. **Option B: Explicitly Create Index Mapping**
   Add a configuration bean to create indices on startup:
   ```java
   @Configuration
   public class ElasticsearchIndexConfig {
       @Autowired
       private ElasticsearchOperations elasticsearchOperations;
       
       @PostConstruct
       public void setupIndices() {
           IndexOperations indexOps = elasticsearchOperations.indexOps(Student.class);
           if (indexOps.exists()) {
               indexOps.delete();
           }
           indexOps.create();
           indexOps.putMapping();
       }
   }
   ```

3. **Option C: Use Test Containers**
   Consider using Testcontainers to start a fresh Elasticsearch instance for each test run.

### Future Enhancements
1. Add support for aggregations on nested fields
2. Add support for sorting on nested fields
3. Optimize nested query performance with proper score modes
4. Add caching for nested path analysis

## Testing

### Test Coverage
- **S5_Join Tests:**
  - `innerJoin()` - Basic nested field query
  - `innerJoin2()` - Multiple criteria on nested fields
  - `innerJoin3()` - Mixed parent and nested criteria
  - `innerJoin4()` - Reverse nesting (Department -> Students)
  - `innerJoin5()` - Many-to-many nested relationships
  - `leftJoin()` - Optional nested relationships
  - `embeddedDocumentQuery()` - Embedded object queries

- **S6_Advanced_Join Tests:**
  - `roleJoin()` - Multi-level deep nesting
  - `roleLeftJoin()` - Multi-level optional nesting
  - `mixedJoinTypes()` - Mixed inner and left joins
  - `complexNestedQuery()` - Complex queries at multiple nesting levels

### Running Tests
```bash
# Single test
mvn test -Dtest="S5_Join#innerJoin"

# All join tests
mvn test -Dtest="S5_Join,S6_Advanced_Join"

# With debug output
mvn test -Dtest="S5_Join" -X
```

## Differences from MongoDB Implementation

| Feature | MongoDB | Elasticsearch |
|---------|---------|---------------|
| Join Mechanism | `$lookup` aggregation pipeline | Nested documents stored together |
| Performance | Slower (requires lookups) | Faster (denormalized) |
| Circular References | Handled by separate collections | Must avoid in entity design |
| Query Syntax | Aggregation pipeline | Nested query DSL |
| Left Joins | `preserveNullAndEmptyArrays` | Bool query with should clauses |

## Known Limitations

1. **Index Mapping Required:** Nested fields must be properly mapped in Elasticsearch - auto-mapping may not work in all cases
2. **No Reverse Lookups:** Unlike MongoDB's `$lookup`, Elasticsearch can't efficiently query "parent by child" without denormalization
3. **Update Complexity:** When a nested document changes, the entire parent document must be reindexed
4. **Size Limits:** Very large nested arrays can impact performance

## References

- [Elasticsearch Nested Query Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-nested-query.html)
- [Spring Data Elasticsearch - Nested Objects](https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/#elasticsearch.mapping.meta-model.annotations)
- [MongoDB Dynamic Query Implementation](../spring-data-mongodb-dynamic-query/MONGODB_IMPLEMENTATION_SUMMARY.md)

