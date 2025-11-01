# Java Dynamic Query - MongoDB Support

This package provides MongoDB support for the Java Dynamic Query library, allowing you to use the same dynamic query API with MongoDB repositories.

## Features

- All CriteriaOperator types supported:
  - `EQUAL`, `NOT_EQUAL`
  - `CONTAIN`, `DOES_NOT_CONTAIN`, `START_WITH`, `END_WITH`
  - `GREATER_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN`, `LESS_THAN_OR_EQUAL`
  - `SPECIFIED` (check for null/not null)
  - `OR` (logical OR operations)
- Pagination support
- Sorting support
- Batch processing with `consumePartially`
- Type conversion (automatic number type conversion)

## Setup

### 1. Add MongoDB Dependencies

Ensure you have Spring Data MongoDB in your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

### 2. Enable Java Dynamic Query MongoDB

Add the `@EnableJpaDynamicQueryMongo` annotation to your configuration class:

```java
@Configuration
@EnableJpaDynamicQueryMongo
public class MongoConfig {
    // Your MongoDB configuration
}
```

This annotation will:
- Enable MongoDB repositories with dynamic query support
- Configure the `MongoSearchQueryTemplate` bean
- Set up the custom repository factory

### 3. Create Your Repository

Extend `JpaDynamicQueryMongoRepository` instead of `MongoRepository`:

```java
public interface UserRepository extends JpaDynamicQueryMongoRepository<User, String> {
    // Your custom methods (optional)
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
// Find users where email is NOT null
List<User> usersWithEmail = userRepository.findAll(
    CriteriaList.of(
        Criteria.of("email", CriteriaOperator.SPECIFIED, "true")
    )
);

// Find users where email IS null
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

// Dates (as ISO strings or timestamps)
userRepository.findAll(
    CriteriaList.of(
        Criteria.of("createdDate", CriteriaOperator.GREATER_THAN, "2023-01-01")
    )
);
```

## Type Conversion

The library automatically converts number types:
- String numbers are converted to appropriate numeric types (Integer, Long, Double)
- Supports both integer and floating-point numbers

```java
// These all work correctly
Criteria.of("age", CriteriaOperator.EQUAL, "25")      // String
Criteria.of("age", CriteriaOperator.EQUAL, 25)        // int
Criteria.of("age", CriteriaOperator.EQUAL, 25L)       // Long
Criteria.of("price", CriteriaOperator.EQUAL, 19.99)   // double
```

## Advanced: Direct MongoSearchQueryTemplate Usage

If you need more control, you can inject `MongoSearchQueryTemplate` directly:

```java
@Autowired
private MongoSearchQueryTemplate mongoSearchQueryTemplate;

public List<User> customQuery() {
    DynamicQuery query = new DynamicQuery();
    query.getWhere().add(Criteria.of("status", CriteriaOperator.EQUAL, "ACTIVE"));
    
    return mongoSearchQueryTemplate.findAll(User.class, query);
}
```

## API Methods

### JpaDynamicQueryMongoRepository Interface

- `List<T> findAll(List<Criteria> criteriaList)` - Find all matching criteria
- `List<T> findAll(DynamicQuery dynamicQuery)` - Find all with dynamic query
- `Page<T> findAllAsPage(DynamicQuery dynamicQuery)` - Find all as page
- `Page<T> findAll(List<Criteria> criteriaList, Pageable pageable)` - Find all with pagination
- `long count(List<Criteria> criteriaList)` - Count matching criteria
- `void consumePartially(ListConsumer<T> processor, int pageSize)` - Batch processing
- `void consumePartially(List<Criteria> criteriaList, ListConsumer<T> processor, int pageSize)` - Batch processing with criteria

## Differences from JPA Version

1. **No Tuple Support**: MongoDB version doesn't support Tuple results (since MongoDB doesn't use JPA Tuples)
2. **No QueryBuilder**: The fluent QueryBuilder is not yet implemented for MongoDB
3. **Type Conversion**: MongoDB handles types differently, so automatic type conversion is provided
4. **Implementation**: Uses `MongoTemplate` instead of `EntityManager`

## Architecture

```
@EnableJpaDynamicQueryMongo
    ↓
MongoSearchQueryTemplateConfig
    ↓
MongoSearchQueryTemplate (query builder)
    ↓
JpaDynamicQueryMongoRepositoryFactoryBean
    ↓
JpaDynamicQueryMongoRepositoryImpl
    ↓
Your Repository Interface → JpaDynamicQueryMongoRepository
```

## Notes

- All string comparisons are **case-insensitive** by default
- Regex special characters are automatically escaped in string operations
- The library is compatible with Spring Data MongoDB 2.x and 3.x
- Thread-safe for concurrent usage

## Troubleshooting

### Repository Not Found

Make sure you have `@EnableJpaDynamicQueryMongo` on a configuration class in your component scan path.

### MongoSearchQueryTemplate Bean Not Found

The `MongoSearchQueryTemplate` bean should be auto-configured. If not, ensure:
1. You have a `MongoTemplate` bean configured
2. Your MongoDB configuration is correct
3. The `@EnableJpaDynamicQueryMongo` annotation is present

### Type Conversion Issues

If you encounter type conversion issues, ensure your values are in the correct format:
- Numbers: Use actual number types or numeric strings
- Dates: Use ISO format strings or Date objects
- Booleans: Use "true"/"false" strings or boolean values

