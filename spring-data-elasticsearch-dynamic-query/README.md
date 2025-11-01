# Java Dynamic Query - Elasticsearch Support

This package provides Elasticsearch support for the Java Dynamic Query library, allowing you to use the same dynamic query API with Elasticsearch repositories.

## Features

- All CriteriaOperator types supported:
  - `EQUAL`, `NOT_EQUAL`
  - `CONTAIN`, `DOES_NOT_CONTAIN`, `START_WITH`, `END_WITH`
  - `GREATER_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN`, `LESS_THAN_OR_EQUAL`
  - `SPECIFIED` (check for field existence)
  - `OR` (logical OR operations)
  - `PARENTHES` (nested query grouping)
- Pagination support
- Sorting support
- Batch processing with `consumePartially`
- Query Builder fluent API

## Setup

### 1. Add Elasticsearch Dependencies

Ensure you have Spring Data Elasticsearch in your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

### 2. Enable Elasticsearch Dynamic Query

Add the `@EnableElasticsearchDynamicQuery` annotation to your configuration class:

```java
@Configuration
@EnableElasticsearchDynamicQuery
public class ElasticsearchConfig {
    // Your Elasticsearch configuration
}
```

This annotation will:
- Enable Elasticsearch repositories with dynamic query support
- Configure the `ElasticsearchSearchQueryTemplate` bean
- Set up the custom repository factory

### 3. Create Your Repository

Extend `ElasticsearchDynamicQueryRepository` instead of `ElasticsearchRepository`:

```java
public interface UserRepository extends ElasticsearchDynamicQueryRepository<User, String> {
    // Your custom methods (optional)
}
```

### 4. Annotate Your Entities

Use Spring Data Elasticsearch annotations:

```java
@Document(indexName = "users")
public class User {
    @Id
    private String id;
    
    @Field(type = FieldType.Text)
    private String name;
    
    @Field(type = FieldType.Integer)
    private Integer age;
    
    @Field(type = FieldType.Boolean)
    private Boolean active;
    
    // getters and setters
}
```

## Usage Examples

### Basic Query with Criteria

```java
@Autowired
private UserRepository userRepository;

// Find users with name containing "John"
List<User> users = userRepository.findAll(
    CriteriaList.of(
        Criteria.of("name", CriteriaOperator.CONTAIN, "John")
    )
);
```

### Multiple Criteria (AND operation)

```java
// Find active users with age greater than 18
List<User> users = userRepository.findAll(
    CriteriaList.of(
        Criteria.of("active", CriteriaOperator.EQUAL, true),
        Criteria.of("age", CriteriaOperator.GREATER_THAN, 18)
    )
);
```

### OR Operations

```java
// Find users with name "John" OR age > 30
List<User> users = userRepository.findAll(
    CriteriaList.of(
        Criteria.of("name", CriteriaOperator.EQUAL, "John"),
        Criteria.OR(),  // Everything after this OR until next OR is part of OR group
        Criteria.of("age", CriteriaOperator.GREATER_THAN, 30)
    )
);
```

### Parentheses/Scopes - Complex Nested Logic

```java
// (name="John" OR name="Jane") AND (age > 18 AND age < 65)
List<User> users = userRepository.findAll(
    CriteriaList.of(
        Criteria.of("", CriteriaOperator.PARENTHES,
            CriteriaList.of(
                Criteria.of("name", CriteriaOperator.EQUAL, "John"),
                Criteria.OR(),
                Criteria.of("name", CriteriaOperator.EQUAL, "Jane")
            )
        ),
        Criteria.of("", CriteriaOperator.PARENTHES,
            CriteriaList.of(
                Criteria.of("age", CriteriaOperator.GREATER_THAN, 18),
                Criteria.of("age", CriteriaOperator.LESS_THAN, 65)
            )
        )
    )
);
```

### Dynamic Query with Pagination and Sorting

```java
DynamicQuery query = new DynamicQuery();

// Add criteria
query.getWhere().add(Criteria.of("status", CriteriaOperator.EQUAL, "ACTIVE"));

// Add sorting
query.getOrderBy().add(Pair.of("createdDate", Order.DESC));

// Add pagination
query.setPageNumber(0);
query.setPageSize(10);

// Execute
Page<User> userPage = userRepository.findAllAsPage(query);
```

### Using SPECIFIED Operator

```java
// Find users where email field exists
List<User> usersWithEmail = userRepository.findAll(
    CriteriaList.of(
        Criteria.of("email", CriteriaOperator.SPECIFIED, "true")
    )
);

// Find users where email field doesn't exist
List<User> usersWithoutEmail = userRepository.findAll(
    CriteriaList.of(
        Criteria.of("email", CriteriaOperator.SPECIFIED, "false")
    )
);
```

### String Operations

```java
// Start with
List<User> users = userRepository.findAll(
    CriteriaList.of(
        Criteria.of("email", CriteriaOperator.START_WITH, "admin")
    )
);

// End with
users = userRepository.findAll(
    CriteriaList.of(
        Criteria.of("email", CriteriaOperator.END_WITH, "@example.com")
    )
);

// Does not contain
users = userRepository.findAll(
    CriteriaList.of(
        Criteria.of("username", CriteriaOperator.DOES_NOT_CONTAIN, "test")
    )
);
```

### Batch Processing

```java
// Process all users in batches of 100
userRepository.consumePartially(users -> {
    // Process batch
    users.forEach(user -> {
        // Your processing logic
        System.out.println(user.getName());
    });
}, 100);

// Process with criteria
userRepository.consumePartially(
    CriteriaList.of(
        Criteria.of("status", CriteriaOperator.EQUAL, "ACTIVE")
    ),
    users -> {
        // Process batch of active users
    },
    100
);
```

### Count Operations

```java
// Count users matching criteria
long count = userRepository.count(
    CriteriaList.of(
        Criteria.of("status", CriteriaOperator.EQUAL, "ACTIVE")
    )
);
```

### Query Builder Fluent API

```java
List<User> users = userRepository.queryBuilder()
    .where(Field("status").eq("ACTIVE"), Field("age").gt(18))
    .orderBy(OrderBy("createdDate", Order.DESC))
    .page(0, 10)
    .getResult();
```

## Comparison Operations

All comparison operators work with numbers, dates, and strings:

```java
// Numbers
userRepository.findAll(
    CriteriaList.of(
        Criteria.of("age", CriteriaOperator.GREATER_THAN_OR_EQUAL, 18),
        Criteria.of("age", CriteriaOperator.LESS_THAN, 65)
    )
);

// Dates (as ISO strings or Date objects)
userRepository.findAll(
    CriteriaList.of(
        Criteria.of("createdDate", CriteriaOperator.GREATER_THAN, "2023-01-01")
    )
);
```

## Advanced: Direct ElasticsearchSearchQueryTemplate Usage

If you need more control, you can inject `ElasticsearchSearchQueryTemplate` directly:

```java
@Autowired
private ElasticsearchSearchQueryTemplate elasticsearchSearchQueryTemplate;

public List<User> customQuery() {
    DynamicQuery query = new DynamicQuery();
    query.getWhere().add(Criteria.of("status", CriteriaOperator.EQUAL, "ACTIVE"));
    
    return elasticsearchSearchQueryTemplate.findAll(User.class, query);
}
```

## API Methods

### ElasticsearchDynamicQueryRepository Interface

- `List<T> findAll(List<Criteria> criteriaList)` - Find all matching criteria
- `List<T> findAll(DynamicQuery dynamicQuery)` - Find all with dynamic query
- `<R> List<R> findAll(DynamicQuery dynamicQuery, Class<R> resultClass)` - Find all with projection
- `Page<T> findAllAsPage(DynamicQuery dynamicQuery)` - Find all as page
- `<R> Page<R> findAllAsPage(DynamicQuery dynamicQuery, Class<R> resultClass)` - Find all as page with projection
- `Page<T> findAll(List<Criteria> criteriaList, Pageable pageable)` - Find all with pagination
- `long count(List<Criteria> criteriaList)` - Count matching criteria
- `void consumePartially(ListConsumer<T> processor, int pageSize)` - Batch processing
- `void consumePartially(List<Criteria> criteriaList, ListConsumer<T> processor, int pageSize)` - Batch processing with criteria
- `void consumePartially(DynamicQuery dynamicQuery, ListConsumer<T> processor, int pageSize)` - Batch processing with dynamic query
- `ElasticsearchQueryBuilder<T, ID> queryBuilder()` - Get fluent query builder

## Elasticsearch-Specific Considerations

### Field Types and Queries

For exact matches on text fields, use the `.keyword` subfield:

```java
// For analyzed text fields, use CONTAIN or wildcards
Criteria.of("name", CriteriaOperator.CONTAIN, "john")

// For exact matches on keyword fields
Criteria.of("status.keyword", CriteriaOperator.EQUAL, "ACTIVE")
```

### Wildcard Query Performance

Wildcard queries (CONTAIN, START_WITH, END_WITH) can be slow on large datasets. Consider:
1. Using proper field analyzers
2. Creating keyword fields for exact matches
3. Using edge n-gram tokenizers for prefix matching

### Index Refresh

Elasticsearch indexes are near real-time. After saving documents, they may not be immediately searchable. For tests, wait or manually refresh:

```java
// In tests - wait for indexing
Thread.sleep(1000);
```

## Limitations and Known Issues

1. **Joins**: Elasticsearch doesn't support traditional SQL-like joins. Use:
   - Nested documents for one-to-many relationships
   - Parent-child relationships for more complex scenarios
   - Denormalization (embedding data)

2. **Transactions**: Elasticsearch doesn't support ACID transactions like relational databases

3. **Aggregations**: Complex aggregations are not yet implemented in the dynamic query API

4. **Full-Text Search**: Advanced full-text search features (fuzzy, phrase, boosting) are not exposed through the simple criteria API

## Architecture

```
@EnableElasticsearchDynamicQuery
    ↓
ElasticsearchSearchQueryTemplateConfig
    ↓
ElasticsearchSearchQueryTemplate (query builder)
    ↓
ElasticsearchDynamicQueryRepositoryFactoryBean
    ↓
ElasticsearchDynamicQueryRepositoryImpl
    ↓
Your Repository Interface → ElasticsearchDynamicQueryRepository
```

## Notes

- String queries use Elasticsearch wildcard queries (case-sensitive)
- Wildcard special characters (`*`, `?`) are automatically escaped
- The library is compatible with Spring Data Elasticsearch 4.x and 5.x
- Thread-safe for concurrent usage

## Troubleshooting

### Repository Not Found

Make sure you have `@EnableElasticsearchDynamicQuery` on a configuration class in your component scan path.

### ElasticsearchSearchQueryTemplate Bean Not Found

The `ElasticsearchSearchQueryTemplate` bean should be auto-configured. If not, ensure:
1. You have an `ElasticsearchOperations` bean configured
2. Your Elasticsearch configuration is correct
3. The `@EnableElasticsearchDynamicQuery` annotation is present

### Connection Issues

Ensure your Elasticsearch instance is running and accessible:

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
```

### Query Not Returning Results

1. Check if documents are indexed (refresh delay)
2. Verify field names match your document mapping
3. For text fields, consider using `.keyword` for exact matches
4. Check Elasticsearch logs for query errors

## Performance Tips

1. **Use Keyword Fields**: For exact matching and sorting, use keyword type fields
2. **Avoid Leading Wildcards**: Queries like `*term` are slow
3. **Use Filters**: Boolean filters (EQUAL, range queries) are cached
4. **Limit Result Size**: Use pagination to avoid loading large result sets
5. **Index Design**: Design your index schema for your query patterns

## Testing

For integration tests, consider using Testcontainers:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>elasticsearch</artifactId>
    <scope>test</scope>
</dependency>
```

```java
@SpringBootTest
@Testcontainers
class UserRepositoryTest {
    
    @Container
    static ElasticsearchContainer elasticsearch = 
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.0");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress);
    }
}
```

## License

Same as parent project.


