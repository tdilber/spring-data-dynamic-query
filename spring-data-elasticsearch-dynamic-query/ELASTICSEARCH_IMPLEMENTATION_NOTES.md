# Elasticsearch Dynamic Query Implementation Notes

This document describes the implementation details, design decisions, limitations, and differences compared to the JPA and MongoDB versions of the Dynamic Query library.

## Implementation Overview

The Elasticsearch implementation follows the same pattern as the MongoDB version, adapted for Elasticsearch's query DSL and data model.

### Architecture Components

1. **ElasticsearchSearchQueryTemplate**: Core query builder that converts `DynamicQuery` objects to Elasticsearch's `NativeSearchQuery`
2. **ElasticsearchDynamicQueryRepository**: Extended repository interface combining Spring Data Elasticsearch's `ElasticsearchRepository` with dynamic query capabilities
3. **ElasticsearchDynamicQueryRepositoryImpl**: Implementation that delegates to `ElasticsearchSearchQueryTemplate`
4. **ElasticsearchDynamicQueryRepositoryFactoryBean**: Custom factory bean for creating repository instances
5. **ElasticsearchQueryBuilder**: Fluent API for building queries
6. **@EnableElasticsearchDynamicQuery**: Configuration annotation to enable the feature

## Operator Mapping to Elasticsearch Queries

### Basic Equality

- **EQUAL (single value)**: Maps to `termQuery(field, value)`
- **EQUAL (multiple values)**: Maps to `termsQuery(field, values)` - equivalent to SQL IN
- **NOT_EQUAL (single value)**: Maps to `boolQuery().mustNot(termQuery(field, value))`
- **NOT_EQUAL (multiple values)**: Maps to `boolQuery().mustNot(termsQuery(field, values))`

**Note on Text Fields**: Elasticsearch analyzes text fields by default. For exact matches on text fields, use the `.keyword` subfield (e.g., `status.keyword`). The library doesn't automatically append `.keyword` to maintain flexibility.

### String Operations

- **CONTAIN**: Maps to `wildcardQuery(field, "*value*")`
- **DOES_NOT_CONTAIN**: Maps to `boolQuery().mustNot(wildcardQuery(field, "*value*"))`
- **START_WITH**: Maps to `prefixQuery(field, value)` - more efficient than wildcard
- **END_WITH**: Maps to `wildcardQuery(field, "*value")`

**Escaping**: Special wildcard characters (`*`, `?`, `\`) are automatically escaped in values.

**Performance Warning**: Wildcard queries, especially with leading wildcards (CONTAIN, END_WITH), can be slow on large datasets. Consider using:
- Proper analyzers with edge n-grams for prefix matching
- Dedicated keyword fields for exact matching
- Alternative query strategies for contains operations

### Range Operations

- **GREATER_THAN**: Maps to `rangeQuery(field).gt(value)`
- **GREATER_THAN_OR_EQUAL**: Maps to `rangeQuery(field).gte(value)`
- **LESS_THAN**: Maps to `rangeQuery(field).lt(value)`
- **LESS_THAN_OR_EQUAL**: Maps to `rangeQuery(field).lte(value)`

Works with numbers, dates, and any comparable types supported by Elasticsearch.

### Field Existence

- **SPECIFIED(true)**: Maps to `existsQuery(field)` - checks if field exists in document
- **SPECIFIED(false)**: Maps to `boolQuery().mustNot(existsQuery(field))` - checks if field doesn't exist

**Note**: In Elasticsearch, "exists" means the field is present and has a non-null value. This is different from SQL's IS NULL.

### Logical Operations

#### AND Operations
Multiple criteria without OR operators are combined using `BoolQuery` with `must` clauses:
```json
{
  "bool": {
    "must": [
      { "term": { "status": "ACTIVE" }},
      { "range": { "age": { "gt": 18 }}}
    ]
  }
}
```

#### OR Operations
Criteria are split into groups by OR operators. Groups are combined using `BoolQuery` with `should` clauses:
```json
{
  "bool": {
    "should": [
      { "bool": { "must": [criteria_group_1] }},
      { "bool": { "must": [criteria_group_2] }}
    ],
    "minimum_should_match": 1
  }
}
```

Example: `[A, B, OR, C, D]` becomes `(A AND B) OR (C AND D)`

#### PARENTHES Operations (Nested Logic)
The PARENTHES operator creates nested query structures:
```java
Criteria.of("", CriteriaOperator.PARENTHES, 
    CriteriaList.of(
        Criteria.of("field1", CriteriaOperator.EQUAL, "value1"),
        Criteria.OR(),
        Criteria.of("field2", CriteriaOperator.EQUAL, "value2")
    )
)
```

Maps to:
```json
{
  "bool": {
    "should": [
      { "term": { "field1": "value1" }},
      { "term": { "field2": "value2" }}
    ],
    "minimum_should_match": 1
  }
}
```

This allows complex Boolean logic like `(A OR B) AND (C OR D)`.

## Features Status

### ✅ Fully Implemented

- **Basic Operators**: EQUAL, NOT_EQUAL, GREATER_THAN, LESS_THAN, etc.
- **Multi-value Operators**: Multiple values for EQUAL/NOT_EQUAL (IN/NOT IN queries)
- **String Operations**: CONTAIN, START_WITH, END_WITH, DOES_NOT_CONTAIN
- **Logical Operations**: AND and OR
- **Nested Logic**: PARENTHES operator for complex Boolean expressions
- **Field Existence**: SPECIFIED operator
- **Pagination**: Page number and page size
- **Sorting**: Single and multiple field sorting with ASC/DESC
- **Counting**: Count queries with criteria
- **Batch Processing**: `consumePartially` methods for memory-efficient processing
- **Projection**: Field selection (partial implementation)

### ⚠️ Partially Implemented

- **Projection/Selection**: Basic field selection works, but complex transformations are limited
- **Query Builder**: Basic fluent API implemented but not as feature-rich as JPA version
- **Nested Field Queries**: Simple dot notation works (e.g., `user.name`), but complex nested document queries need enhancement

### ❌ Not Implemented / Limitations

- **Joins**: Elasticsearch doesn't support SQL-like joins
  - Use nested documents for one-to-many relationships (requires nested query support)
  - Use parent-child relationships (requires specific implementation)
  - Denormalize data (embed related data in documents)
- **Transactions**: Elasticsearch doesn't support ACID transactions
- **Complex Aggregations**: No support for groupBy, having, etc.
- **Full-Text Search Features**: Fuzzy matching, phrase queries, boosting not exposed
- **Dynamic Specifications**: Not implemented (JPA-specific feature)
- **Tuple Results**: Not applicable to Elasticsearch

## Query Building Process

### 1. DynamicQuery → NativeSearchQuery Conversion

```java
NativeSearchQuery prepareQuery(Class<Entity> entityClass, DynamicQuery dynamicQuery)
```

Steps:
1. Convert criteria list to Elasticsearch QueryBuilder
2. Add sorting from orderBy list
3. Add pagination (pageNumber, pageSize)
4. Build and return NativeSearchQuery

### 2. Criteria List → QueryBuilder Conversion

```java
QueryBuilder buildQueryFromCriteria(List<Criteria> criteriaList)
```

Process:
1. Split criteria list by OR operators into groups
2. For each group, build an AND query (`buildAndQuery`)
3. If multiple groups, combine with OR (BoolQuery with should clauses)
4. Return final QueryBuilder

### 3. Single Criteria → QueryBuilder Conversion

```java
QueryBuilder buildCriteriaQuery(Criteria criteria)
```

Maps each CriteriaOperator to appropriate Elasticsearch query type (term, range, wildcard, etc.)

## Elasticsearch-Specific Considerations

### Text Analysis

Elasticsearch analyzes text fields by default, breaking them into tokens. This affects queries:

```java
// This might not work as expected on analyzed fields
Criteria.of("name", CriteriaOperator.EQUAL, "John Doe")

// Use keyword subfield for exact match
Criteria.of("name.keyword", CriteriaOperator.EQUAL, "John Doe")

// Or use CONTAIN for analyzed fields
Criteria.of("name", CriteriaOperator.CONTAIN, "John")
```

### Index Mapping

Proper index mapping is crucial. Example:

```json
{
  "properties": {
    "name": {
      "type": "text",
      "fields": {
        "keyword": {
          "type": "keyword"
        }
      }
    },
    "status": {
      "type": "keyword"
    },
    "age": {
      "type": "integer"
    },
    "createdDate": {
      "type": "date"
    }
  }
}
```

- Use `text` for full-text search
- Use `keyword` for exact matching, sorting, aggregations
- Use multi-field mapping to support both use cases

### Near Real-Time Search

Elasticsearch is near real-time, not real-time. After indexing:
- Documents are immediately available via GET API
- Documents appear in search results after refresh (default 1 second)

For tests, consider:
```java
// Wait for refresh
Thread.sleep(1000);

// Or force refresh (don't use in production)
elasticsearchOperations.indexOps(IndexCoordinates.of("index_name")).refresh();
```

### Performance Implications

1. **Wildcard Queries**: Especially leading wildcards (`*term`) scan all terms
2. **Sorting**: Requires field data or doc values - use keyword fields
3. **Aggregations**: Not yet implemented, but would use bucket aggregations
4. **Deep Pagination**: Use search_after instead of from/size for deep paging (not implemented)

## Differences from JPA Version

| Feature | JPA | Elasticsearch |
|---------|-----|---------------|
| Joins | ✅ Full support | ❌ Not supported (use nested docs) |
| Transactions | ✅ ACID | ❌ No transactions |
| Aggregations | ✅ GROUP BY, HAVING | ❌ Not implemented |
| Full-text search | ⚠️ Limited | ⚠️ Powerful but not exposed |
| Projection | ✅ Full support | ⚠️ Basic support |
| Tuple results | ✅ Supported | ❌ Not applicable |
| Query Builder | ✅ Full featured | ⚠️ Basic implementation |
| Type safety | ✅ Strong | ⚠️ Weaker (JSON-based) |

## Differences from MongoDB Version

| Feature | MongoDB | Elasticsearch |
|---------|---------|---------------|
| Query Language | Aggregation Pipeline | Query DSL |
| Text Search | $text, $regex | Analyzed fields, wildcards |
| Joins | $lookup | None (nested docs) |
| Transactions | ✅ Multi-document | ❌ Single document |
| Consistency | Strong | Eventual |
| Use Case | Operational DB | Search & Analytics |

## Testing Recommendations

### Use Testcontainers

```java
@Testcontainers
class RepositoryTest {
    @Container
    static ElasticsearchContainer elasticsearch = 
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.0");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", 
            elasticsearch::getHttpHostAddress);
    }
}
```

### Wait for Indexing

```java
@BeforeEach
void setup() {
    // Save documents
    repository.saveAll(testData);
    
    // Wait for indexing
    Thread.sleep(1000);
}
```

### Test Different Field Types

Test queries against:
- Text fields (analyzed)
- Keyword fields (not analyzed)
- Numeric fields
- Date fields
- Boolean fields
- Nested documents (if supported)

## Future Enhancements

### Planned

1. **Nested Document Support**: Add support for querying nested documents
2. **Parent-Child Relationships**: Support parent-child document relationships
3. **Aggregations**: Expose bucket and metric aggregations
4. **Search After**: Implement deep pagination with search_after
5. **Highlighting**: Support result highlighting for text fields
6. **Fuzzy Queries**: Expose fuzzy matching capabilities

### Under Consideration

1. **Full-Text Search DSL**: Rich API for full-text search features
2. **Geo Queries**: Support for geo-point and geo-shape queries
3. **Scripting**: Support for Elasticsearch scripts in queries
4. **Multi-Index Queries**: Query across multiple indices
5. **Index Aliases**: Support for index aliases

## Known Issues

1. **Date Format Handling**: Date parsing relies on Elasticsearch's default formats. Custom formats may need manual handling.

2. **Null Values**: Elasticsearch doesn't index null values by default. SPECIFIED(false) finds documents where field doesn't exist OR is null.

3. **Array Fields**: Queries on array fields work, but may not behave exactly like MongoDB or SQL arrays.

4. **Case Sensitivity**: Keyword fields are case-sensitive. Text fields depend on analyzer configuration.

5. **Sorting on Text Fields**: Requires keyword subfield. Sorting on analyzed text fields will fail.

## Contributing

When contributing to the Elasticsearch implementation:

1. **Follow the pattern** established by JPA and MongoDB versions
2. **Test thoroughly** with different data types and query combinations
3. **Document limitations** specific to Elasticsearch
4. **Consider performance** implications of query structures
5. **Maintain compatibility** with Spring Data Elasticsearch versions

## References

- [Elasticsearch Query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html)
- [Spring Data Elasticsearch](https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/)
- [Elasticsearch Mapping](https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html)
- [Bool Query](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-bool-query.html)

## Version Compatibility

- **Spring Boot**: 2.7.x, 3.x
- **Spring Data Elasticsearch**: 4.x, 5.x
- **Elasticsearch**: 7.x, 8.x
- **Java**: 11, 17, 21

## License

Same as parent project - see LICENSE file in root directory.

