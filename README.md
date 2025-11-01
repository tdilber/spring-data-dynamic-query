# Java Dynamic Query (JDQ)

This project is designed to bring powerful, unified dynamic query creation to Spring Data JPA, Spring Data MongoDB, and 
Spring Data Elasticsearch. It tackles the complexity and boilerplate code traditionally required to write queries, 
especially at runtime, across these different data stores.

**SPOILER:** 

```java
//Model
@JdqModel
public record AdminUserSummary(
        @JdqField("id") Long id,
        @JdqField("username") String username,
        @JdqField("roles.name") String roleName,
        @JdqField("roles.roleAuthorizations.authorization.name") String authorizationName) {
}
//Query
var query = DynamicQuery.of(List.of(Criteria.of("roles.id", CriteriaOperator.GREATER_THAN, 5L)));
//Usage
Page<AdminUserSummary> adminUsers = adminUserRepository.findAll(query, AdminUserSummary.class);
// Update Example
adminUserRepository.update(updatedAdminUser);
```
From Jpa Criteria Api Generated Sql:
```sql
select adminuser0_.id       as col_0_0_,
       adminuser0_.username as col_1_0_,
       role2_.name          as col_2_0_,
       authorizat4_.name    as col_3_0_
from admin_user adminuser0_
         inner join admin_user_role roles1_ on adminuser0_.id = roles1_.admin_user_id
         inner join role role2_ on roles1_.role_id = role2_.id
         inner join role_authorization roleauthor3_ on role2_.id = roleauthor3_.role_id
         inner join my_authorization authorizat4_ on roleauthor3_.authorization_id = authorizat4_.id
where role2_.id > 5
```

**For more spoiler visit:** https://tdilber.com/dynamic-query-demo

This project is designed to overcome the complexity and boilerplate of writing separate code for each query, especially 
across different data stores. At its core, it simplifies the native query-building mechanisms for each data source:
- For JPA, it builds on the Criteria API (introduced in Jpa 2).
- For MongoDB, it builds on the MongoDB Criteria API.
- For Elasticsearch, it builds on the Elasticsearch Query DSL.
This enables a unified approach to programmatic or dynamic runtime query creation.

The query creation capabilities include 9 different field operators, AND-OR conjunctions, SCOPE support, and (for JPA 
and MongoDB) single or multi JOIN features. The SELECT, DISTINCT, ORDER BY clauses in the query are also supported with 
Joined Column. In addition, all query results can be returned as both List<?> and Page<?>. After SELECT clause, PROJECTION 
support is also available. This PROJECTION works with ignore missing fields rules for more flexibility. In Elasticsearch, 
I didn't use JOIN because AI don't recommend JOIN in Elasticsearch architecture, so actually **still all Join operations are 
working for nested objects on Elasticsearch**.

You don't need to write any lines of code to create these queries, all operations including all JOIN operations are done
dynamically at runtime. It is sufficient to create just one Repository interface. The objects required to build and call 
the dynamic queries can be taken from within your program or from external DTOs. Since all query creation and database 
query operations are done with Criteria Api during these operations, it is as secure as Spring Data (JPA, MongoDB, Elasticsearch), and works as fast 
and effectively as Spring Data (JPA, MongoDB, Elasticsearch).

**Note:**

- #### **The project have a live Demo. I suggest you to visit and try it from:** https://tdilber.com/dynamic-query-demo
- The project have a Turkish introduction video on Youtube. You can watch it from: https://youtu.be/kY3UGLKXgmo

## Introduction

**This is the unified base Models for Spring Dynamic Query. Models same for all Databases (JPA, MongoDB, Elasticsearch).**

```java
public enum CriteriaOperator {
    //    String Operators
    CONTAIN, DOES_NOT_CONTAIN, END_WITH, START_WITH,
    //    Null Check Operator
    SPECIFIED,
    //    String and Comparable Operators
    EQUAL, NOT_EQUAL,
    //    Comparable Operators
    GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL,
    //    OR Operator
    OR,
    //    SCOPE Operator
    PARENTHES
}


@Getter
@Setter
public class Criteria implements Serializable {
    protected String key;
    protected CriteriaOperator operation;
    protected List<Object> values;
}

@Getter
@Setter
public class DynamicQuery implements Serializable {
    protected boolean distinct = false;
    protected Integer pageSize = null;
    protected Integer pageNumber = null;
    protected List<Pair<String, String>> select = new ArrayList<>();
    protected List<Criteria> where = new CriteriaList();
    protected List<Pair<String, Order>> orderBy = new ArrayList<>();
}
```

**This is the base unified Methods for BaseDynamicQueryRepository.**

```java

@NoRepositoryBean
public interface BaseDynamicQueryRepository<T, ID> {
    // List Entity with WHERE Clause (JOIN supported)
    List<T> findAll(List<Criteria> criteriaList);

    // List Entity with WHERE Clause (JOIN supported) and Pageable
    Page<T> findAll(List<Criteria> criteriaList, Pageable pageable);

    // List Entity with SELECT, DISTINCT, WHERE, ORDER BY Clause (JOIN supported) Page Supported 
    List<T> findAll(DynamicQuery dynamicQuery);

    // List Entity with SELECT, DISTINCT, WHERE, ORDER BY Clause (JOIN supported) Page Supported
    Page<T> findAllAsPage(DynamicQuery dynamicQuery);

    // List Entity with SELECT, DISTINCT, WHERE, ORDER BY Clause (JOIN supported) Page Supported With Projection
    <ResultType> List<ResultType> findAll(DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass);

    // List Entity with SELECT, DISTINCT, WHERE, ORDER BY Clause (JOIN supported) Page Supported With Projection
    <ResultType> Page<ResultType> findAllAsPage(DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass);
    
    // Count with WHERE Clause (JOIN supported)
    long count(List<Criteria> criteriaList);

    // Look like Query DSL Builder, you can create simple queries with this builder in code. 
    BaseQueryBuilder<T, ID> queryBuilder();
}
```

## Writing the Code


### Getting Started For Spring Data JPA


#### 1- Setting up the project with Maven

**for Spring Boot 3.*:**
```maven
<dependency>
    <groupId>io.github.tdilber</groupId>
    <artifactId>spring-boot-starter-jpa-dynamic-query</artifactId>
    <version>0.8.0</version>
</dependency>
```

**for Spring Boot 2.*:**
```maven
<dependency>
    <groupId>io.github.tdilber</groupId>
    <artifactId>spring-data-jpa-dynamic-query-sb2</artifactId>
    <version>0.8.0</version>
</dependency>
```

#### 2- Enable Annotation

Add the `@EnableJpaDynamicQuery` annotation to the main class of your project. This annotation is used to enable the
dynamic query feature.

```java

@EnableJpaDynamicQuery
@SpringBootApplication
public class SpringJpaDynamicQueryDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringJpaDynamicQueryDemoApplication.class, args);
    }
}

```

#### 3- Create a Repository

**Create a Repository for Existing Entity.**

```java
 public interface AdminUserRepository extends JpaDynamicQueryRepository<AdminUser, Long> {

}
```

### Getting Started For Spring Data MongoDB

#### 1- Setting up the project with Maven

**for Spring Boot 3.*:**
```maven
<dependency>
    <groupId>io.github.tdilber</groupId>
    <artifactId>spring-data-mongodb-dynamic-query</artifactId>
    <version>0.8.0</version>
</dependency>
```

**for Spring Boot 2.*:**
```maven
<dependency>
    <groupId>io.github.tdilber</groupId>
    <artifactId>spring-data-mongodb-dynamic-query-sb2</artifactId>
    <version>0.8.0</version>
</dependency>
```

#### 2- Enable Annotation
Add the `@EnableMongoDbDynamicQuery` annotation to the main class of your project. This annotation is used to enable the
dynamic query feature.

```java
@EnableMongoDbDynamicQuery
@SpringBootApplication
public class SpringJpaDynamicQueryDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringJpaDynamicQueryDemoApplication.class, args);
    }
}

```

#### 3- Create a Repository

**Create a Repository for Existing Entity.**

```java
 public interface AdminUserMongoRepository extends MongoDynamicQueryRepository<AdminUser, Long> {

}
```

### Getting Started For Spring Data Elasticsearch

#### 1- Setting up the project with Maven

**for Spring Boot 3.*:**
```maven
<dependency>
    <groupId>io.github.tdilber</groupId>
    <artifactId>spring-data-elasticsearch-dynamic-query</artifactId>
    <version>0.8.0</version>
</dependency>
```

**for Spring Boot 2.*:**
```maven
<dependency>
    <groupId>io.github.tdilber</groupId>
    <artifactId>spring-data-elasticsearch-dynamic-query-sb2</artifactId>
    <version>0.8.0</version>
</dependency>
```

#### 2- Enable Annotation
Add the `@EnableElasticsearchDynamicQuery` annotation to the main class of your project. This annotation is used to
enable the dynamic query feature.

```java
@EnableElasticsearchDynamicQuery
@SpringBootApplication
public class SpringJpaDynamicQueryDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringJpaDynamicQueryDemoApplication.class, args);
    }
}

```

#### 3- Create a Repository

**Create a Repository for Existing Entity.**

```java
 public interface AdminUserElasticsearchRepository extends ElasticsearchDynamicQueryRepository<AdminUser, Long> {

}
```


### Coding Examples

#### 1- Operator Examples

###### What is Criteria?

At the beginning we must understand what is Criteria. Criteria is SQL Query WHERE Clause item.

For example `SELECT * FROM user WHERE id > 5 AND name like 'Ali%' AND surname = 'DILBER' AND age IN (29, 30, 31) AND status is not null`

- `id > 5` is a Criteria => `Criteria.of("id", CriteriaOperator.GREATER_THAN, 5)`
- `name like 'Ali%'` is a Criteria => `Criteria.of("name", CriteriaOperator.START_WITH, "Ali")`
- `surname = 'DILBER'` is a Criteria => `Criteria.of("name", CriteriaOperator.EQUAL, "DILBER")`
- `age IN (29, 30, 31)` is a Criteria => `Criteria.of("age", CriteriaOperator.EQUAL, 29, 30, 31)`
- `status is not null` is a Criteria => `Criteria.of("age", CriteriaOperator.SPECIFIED, true)`

this is it :)

###### Multi Value Supported Criteria Operators

Some operators have **multi value support**. This means that you can use multiple values for the same field.

* Multi Value **support** is available for the following operators: _EQUAL, NOT_EQUAL, CONTAIN, DOES_NOT_CONTAIN,
  START_WITH, END_WITH_
* Multi Value **not supported** for the following operators: _SPECIFIED, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN,
  LESS_THAN_OR_EQUAL_

**Note:** As you know Sql Where Clause Some operators have multi value input, for example **IN, NOT IN**. We develop
more multi value operators with java code touches.

##### Comparable Operators

This operator is used to compare numbers and dates. Available Java Types are **Timestamp,  Date, Double, Long, LocalDate,
ZonedDateTime, Instant, Integer**.

The following operators are available:

`EQUAL, NOT_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL`

Enums supported for `EQUAL, NOT_EQUAL` operators.

```java
userRepository.findAll(CriteriaList.of(Criteria.of("status", CriteriaOperator.EQUAL, User.Status.ACTIVE)));
customerRepository.findAll(CriteriaList.of(Criteria.of("age", CriteriaOperator.NOT_EQUAL, 23,24,25)));
```

_Hibernate Query:_

```sql
-- CriteriaOperator.EQUAL => User.Status.ACTIVE
select user0_.id        as id1_1_,
       user0_.age       as age2_1_,
       user0_.birthdate as birthdat3_1_,
       user0_.name      as name4_1_,
       user0_.status    as status5_1_,
       user0_.surname   as surname6_1_,
       user0_.type      as type7_1_
from test_user user0_
where user0_.status = ?

-- Multi Value CriteriaOperator.NOT_EQUAL => 23, 24, 25
select customer0_.id        as id1_0_,
       customer0_.age       as age2_0_,
       customer0_.birthdate as birthdat3_0_,
       customer0_.name      as name4_0_,
       customer0_.user_id   as user_id5_0_
from customer customer0_
where customer0_.age <> 23
  and customer0_.age <> 24
  and customer0_.age <> 25
```

##### String Operators

This operator is used to compare strings. The following operators are available:

`EQUAL, NOT_EQUAL, CONTAIN, DOES_NOT_CONTAIN, END_WITH, START_WITH`

```java
customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.CONTAIN, "Customer")));
```

_Hibernate Query:_

```sql
select customer0_.id        as id1_0_,
       customer0_.age       as age2_0_,
       customer0_.birthdate as birthdat3_0_,
       customer0_.name      as name4_0_,
       customer0_.user_id   as user_id5_0_
from customer customer0_
where customer0_.name like ?
```

_Multi Value Support Examples:_

```java
customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.DOES_NOT_CONTAIN, "5","4")));
customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.START_WITH, "Customer 3","Customer 4")));
```

_Hibernate Query:_

```sql
-- Multi Value CriteriaOperator.DOES_NOT_CONTAIN
select customer0_.id        as id1_0_,
       customer0_.age       as age2_0_,
       customer0_.birthdate as birthdat3_0_,
       customer0_.name      as name4_0_,
       customer0_.user_id   as user_id5_0_
from customer customer0_
where (customer0_.name not like ?)
  and (customer0_.name not like ?)

-- Multi Value CriteriaOperator.START_WITH
select customer0_.id        as id1_0_,
       customer0_.age       as age2_0_,
       customer0_.birthdate as birthdat3_0_,
       customer0_.name      as name4_0_,
       customer0_.user_id   as user_id5_0_
from customer customer0_
where customer0_.name like ?
   or customer0_.name like ?
```

##### Null Check Operator

This operator is used to check if the field is null or not. The following operators are available: `SPECIFIED`

```java
customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.SPECIFIED, true)));
customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.SPECIFIED, false)));
```

_Hibernate Query:_

```sql
-- CriteriaOperator.SPECIFIED true
select customer0_.id        as id1_0_,
       customer0_.age       as age2_0_,
       customer0_.birthdate as birthdat3_0_,
       customer0_.name      as name4_0_,
       customer0_.user_id   as user_id5_0_
from customer customer0_
where customer0_.name is not null

-- CriteriaOperator.SPECIFIED false
select customer0_.id        as id1_0_,
       customer0_.age       as age2_0_,
       customer0_.birthdate as birthdat3_0_,
       customer0_.name      as name4_0_,
       customer0_.user_id   as user_id5_0_
from customer customer0_
where customer0_.name is null
```

#### 2- AND-OR Operator Examples

Sequentially, all criteria are evaluated with the AND operator. If you want to evaluate the criteria with the `OR`
operator, you can use the `Criteria.OR()` method.

```java
customerRepository.findAll(CriteriaList.of(
        Criteria.of("name", CriteriaOperator.EQUAL, "Customer 1"), 
        Criteria.OR(),
        Criteria.of("name", CriteriaOperator.EQUAL, "Customer 2")));

customerRepository.findAll(CriteriaList.of(
        Criteria.of("age", CriteriaOperator.EQUAL, 23, 24),
                        Criteria.of("age", CriteriaOperator.NOT_EQUAL, 20, 21),
                        Criteria.OR(), // ( [ (23 or 24) AND (not 20 and not 21) ] "OR" [ (not 24) AND (25 or 26) ])
                        Criteria.of("age", CriteriaOperator.NOT_EQUAL, 24),
                        Criteria.of("age", CriteriaOperator.EQUAL, 25, 26)));
```

_Hibernate Query:_

```sql
-- Criteria.OR() First Example
select customer0_.id        as id1_0_,
       customer0_.age       as age2_0_,
       customer0_.birthdate as birthdat3_0_,
       customer0_.name      as name4_0_,
       customer0_.user_id   as user_id5_0_
from customer customer0_
where customer0_.name = ?
   or customer0_.name = ?

-- Criteria.OR() Second Example
select customer0_.id        as id1_0_,
       customer0_.age       as age2_0_,
       customer0_.birthdate as birthdat3_0_,
       customer0_.name      as name4_0_,
       customer0_.user_id   as user_id5_0_
from customer customer0_
where (customer0_.age = 23 or customer0_.age = 24) and customer0_.age <> 20 and customer0_.age <> 21
   or customer0_.age <> 24 and (customer0_.age = 25 or customer0_.age = 26)
```

#### 3- SCOPE Operator Examples

Just And-Or operators are not enough for complex queries. For Example: you cannot this simple query with just AND-OR
operators: `(A OR B) AND (C OR D)`. For this reason, the `CriteriaOperator.PARENTHES` operator is used to create a
scope. You can use PARENTHES in PARENTHES.

```java
    //     (A OR B) AND (C OR D)
var criteriaList = CriteriaList.of(
        Criteria.of("", CriteriaOperator.PARENTHES,
                CriteriaList.of(Criteria.of(Course.Fields.id, CriteriaOperator.EQUAL, 1),
                        Criteria.OR(),
                        Criteria.of(Course.Fields.id, CriteriaOperator.EQUAL, 2))),
        Criteria.of("", CriteriaOperator.PARENTHES,
                CriteriaList.of(Criteria.of(Course.Fields.id, CriteriaOperator.EQUAL, 2),
                        Criteria.OR(),
                        Criteria.of(Course.Fields.id, CriteriaOperator.EQUAL, 3)))
);
List<Course> courseList = courseRepository.findAll(criteriaList);
```

_Hibernate Query:_

```sql
select course0_.id                as id1_3_,
       course0_.active            as active2_3_,
       course0_.description       as descript3_3_,
       course0_.max_student_count as max_stud4_3_,
       course0_.name              as name5_3_,
       course0_.start_date        as start_da6_3_
from course course0_
where (course0_.id = 1 or course0_.id = 2)
  and (course0_.id = 2 or course0_.id = 3)
```

#### 4- JOIN Examples

**The strongest feature of this project is JOIN**. Joins work dynamically. If you use it, it automatically performs a
JOIN. It understands which columns to match between two tables through the Join Annotations you specify in the entity (
ManyToMany, OneToMany, OneToOne, ManyToOne).

**Inner join and Left join are supported**. Although our system supports Right join, it is not supported because
Hibernate does not support it.

When performing a join, the field names inside the entity object are used. If you put a **dot (.)** after the field
name, it performs an **inner join**, if you put a less than **sign (<)**, it performs a **left join**. Then you need to
write the field name of the object you joined.

Also **you can use multiple join in the same query.**

Also **you can use (multiple) join in select clause and order by clause.**

```java
// Inner Join
var criteriaList = CriteriaList.of(
        Criteria.of("department.name", CriteriaOperator.START_WITH, "P"),
        Criteria.of("name", CriteriaOperator.START_WITH, "Robert")
);
List<Student> students = studentRepository.findAll(criteriaList);


// Left Join
var criteriaList = CriteriaList.of(
        Criteria.of("department<id", CriteriaOperator.SPECIFIED, false),
        Criteria.of("id", CriteriaOperator.GREATER_THAN, 3)
);
List<Student> students = studentRepository.findAll(criteriaList);

// Multi Inner Join
var criteriaList = CriteriaList.of(Criteria.of("roles.roleAuthorizations.authorization.menuIcon", CriteriaOperator.START_WITH, "icon"));
List<AdminUser> courseList = adminUserRepository.findAll(criteriaList);

// Multi Left Join
var criteriaList = CriteriaList.of(Criteria.of("roles<roleAuthorizations<authorization<menuIcon", CriteriaOperator.START_WITH, "icon"));
List<AdminUser> courseList = adminUserRepository.findAll(criteriaList);
```

_Hibernate Query:_

```sql
-- Inner Join
select student0_.id            as id1_8_,
       student0_.address_id    as address_3_8_,
       student0_.department_id as departme4_8_,
       student0_.name          as name2_8_
from student student0_
         inner join department department1_ on student0_.department_id = department1_.id
where (department1_.name like ?)
  and (student0_.name like ?)

-- Left Join
select student0_.id            as id1_8_,
       student0_.address_id    as address_3_8_,
       student0_.department_id as departme4_8_,
       student0_.name          as name2_8_
from student student0_
         left outer join department department1_ on student0_.department_id = department1_.id
where (department1_.id is null)
  and student0_.id > 3

-- Multi Inner Join
select adminuser0_.id as id1_1_, adminuser0_.password as password2_1_, adminuser0_.username as username3_1_
from admin_user adminuser0_
         inner join admin_user_role roles1_ on adminuser0_.id = roles1_.admin_user_id
         inner join role role2_ on roles1_.role_id = role2_.id
         inner join role_authorization roleauthor3_ on role2_.id = roleauthor3_.role_id
         inner join my_authorization authorizat4_ on roleauthor3_.authorization_id = authorizat4_.id
where authorizat4_.menu_icon like ?

-- Multi Left Join
select adminuser0_.id as id1_1_, adminuser0_.password as password2_1_, adminuser0_.username as username3_1_
from admin_user adminuser0_
         left outer join admin_user_role roles1_ on adminuser0_.id = roles1_.admin_user_id
         left outer join role role2_ on roles1_.role_id = role2_.id
         left outer join role_authorization roleauthor3_ on role2_.id = roleauthor3_.role_id
         left outer join my_authorization authorizat4_ on roleauthor3_.authorization_id = authorizat4_.id
where authorizat4_.menu_icon like ?
```

#### 5- Projection Examples

Spring Data projections always boring. But this project projections are very simple. 
There are two ways to use projections. I suggested using the second way. Because the second way is easier and more reusable.

**Note:** Record class is supported for projection. You can use record class for projection.

##### A- Manual Projection
When you want to use specific fields in the result, you can add selected fields on select list on `DynamicQuery` object. You can add multiple fields to the
select clause. You can also use the `Pair` class to give an alias to the field.

Why we are using Pair class? Because we want to use the same field name in the select clause. But we want to use
different field names in the result. For example, we want to use `id` in the select clause, but we want to use `adminId`
in the result.

When you are using this select clause, queries are created with the select clause. For this reason, your query will be
faster and more efficient.

This method also support **joined column** pass value to result object. Like this following example:

```java
DynamicQuery dynamicQuery = new DynamicQuery();
dynamicQuery.getSelect().add(Pair.of("id", "adminId"));
dynamicQuery.getSelect().add(Pair.of("username", "adminUsername"));
dynamicQuery.getSelect().add(Pair.of("roles.id", "roleId"));
dynamicQuery.getSelect().add(Pair.of("roles.name", "roleName"));
dynamicQuery.getSelect().add(Pair.of("roles.roleAuthorizations.authorization.id", "authorizationId"));
dynamicQuery.getSelect().add(Pair.of("roles.roleAuthorizations.authorization.name", "authorizationName"));
dynamicQuery.getSelect().add(Pair.of("roles.roleAuthorizations.authorization.menuIcon", "menuIcon"));
var criteriaList = CriteriaList.of(Criteria.of("roles.roleAuthorizations.authorization.menuIcon", CriteriaOperator.START_WITH, "icon"));
dynamicQuery.getWhere().addAll(criteriaList);
List<AuthorizationSummary> result = adminUserRepository.findAll(dynamicQuery, AuthorizationSummary.class);
```

_Hibernate Query:_

```sql
select adminuser0_.id         as col_0_0_,
       adminuser0_.username   as col_1_0_,
       role2_.id              as col_2_0_,
       role2_.name            as col_3_0_,
       authorizat4_.id        as col_4_0_,
       authorizat4_.name      as col_5_0_,
       authorizat4_.menu_icon as col_6_0_
from admin_user adminuser0_
         inner join admin_user_role roles1_ on adminuser0_.id = roles1_.admin_user_id
         inner join role role2_ on roles1_.role_id = role2_.id
         inner join role_authorization roleauthor3_ on role2_.id = roleauthor3_.role_id
         inner join my_authorization authorizat4_ on roleauthor3_.authorization_id = authorizat4_.id
where authorizat4_.menu_icon like ?
```

_Note: you can find the example on demo github repository._

##### B- Auto Projection with Annotated Model
Model Annotations: `@JdqModel`, `@JdqField`, `@JdqIgnoreField`, `@JdqSubModel`

We are discovering select clause if model has `@JdqModel` annotation AND select clause is empty.
Autofill Rules are Simple:
- If field has `@JdqField` annotation, we are using this field name in the select clause.
- If field has not any annotation, we are using field name in the select clause.
- If field has `@JdqIgnoreField` annotation, we are ignoring this field in the select clause.
- If field has `@JdqSubModel` annotation, we are including the sub-model fields in the select clause.

**Usage of `@JdqField` annotation:**

`@JdqField` annotation has a parameter. This parameter is a string. This string is a field name in the select clause. If you want to use different field name in the select clause, you can use this annotation. And also If you need to use joined column in the select clause, you can use this annotation.

**Usage of `@JdqSubModel` annotation:**

`@JdqSubModel` annotation is used to include fields from a nested model in the select clause. This allows for more complex projections involving nested objects.

There are 2 usage of `@JdqSubModel` annotation: 
- If you want to use nested model fields without join support, Use `@JdqSubModel()` annotation without any parameter.
- If you want to use nested model fields with join support, Use `@JdqSubModel("joined_column_name")` annotation with joined column name parameter.

_Examples:_

```java
@JdqModel // This annotation is required for using projection with joined column
@Data
public static class UserJdqModel {
  @JdqField("name") // This annotation is not required. But if you want to use different field name in the result, you can use this annotation.
  private String nameButDifferentFieldName;
  @JdqField("team.name") // This annotation is required for using joined column in the projection
  private String teamNameWithJoin;

  private Integer age; // This field is in the select clause. Because this field has not any annotation.

  @JdqIgnoreField // This annotation is required for ignoring this field in the select clause.
  private String surname;

  @JdqSubModel // This annotation is used to include fields from a nested model without join support
  private AddressJdqModel address;
  
  @JdqSubModel("department") // This annotation is used to include fields from a nested model with join support
  private DepartmentJdqModel departmentJdqModel;
}

@JdqModel
@Data
public static class AddressJdqModel {
  @JdqField("address.street")
  private String street;
  @JdqField("address.city")
  private String city;
}

@JdqModel
public record DepartmentJdqModel(@JdqField("id") Long departmentId, @JdqField String name) {
    
}        

// USAGE EXAMPLE
List<UserJdqModel> result = userRepository.findAll(dynamicQuery, UserJdqModel.class);
```
_Autofilled select Result If you fill Manuel:_
```java
select.add(Pair.of("name", "nameButDifferentFieldName")); 
select.add(Pair.of("user.name", "userNameWithJoin")); 
select.add(Pair.of("age", "age")); 
select.add(Pair.of("address.street", "address.street"));
select.add(Pair.of("address.city", "address.city"));
select.add(Pair.of("department.id", "departmentJdqModel.departmentId"));
select.add(Pair.of("department.name", "departmentJdqModel.name"));
```

_Hibernate Query:_

```sql
select user0_.name as col_0_0_,
       team3_.name as col_1_0_,
       user0_.age as col_2_0_,
       address1_.street as col_3_0_,
       address1_.city as col_4_0_,
       department2_.id as col_5_0_,
       department2_.name as col_6_0_
from test_user user0_
       inner join team team3_ on user0_.team_id = team3_.id
       inner join address address1_ on user0_.address_id = address1_.id
       inner join department department2_ on user0_.department_id = department2_.id
where user0_.age > 25
```

#### 6- JdqModel Update Operations

The JdqModel update feature enables powerful, type-safe partial entity updates with minimal code. Using the same `@JdqModel`, `@JdqField`, and `@JdqSubModel` annotations you already use for projections, you can now perform updates on main entities and their joined relations within a single transaction. This eliminates the need for verbose manual update code while maintaining full control over which fields to update. The system automatically generates optimized SQL UPDATE statements using JPA Criteria API, supporting simple fields, partial updates, nested entities, records, and even deep multi-level joins (4+ levels). All operations are transactional, type-safe, and leverage your existing entity relationships.

##### Key Strengths

- **Reuse Projection Models**: The same `@JdqModel` annotated classes used for queries can be used for updates
- **Partial Updates**: Only non-null fields in your model are updated - perfect for PATCH operations
- **Join Support**: Update multiple related entities in a single transaction using the same dot notation as queries (`roles.name`, `roles.roleAuthorizations.authorization.menuIcon`)
- **Type Safety**: Compile-time checking with strongly typed models (classes or records)
- **Zero Boilerplate**: No need to write custom update queries or manage entity loading/merging
- **Transaction Safe**: All updates execute within the same transaction with automatic rollback on failure
- **Flexible Structure**: Supports flat models (dot notation), nested `@JdqSubModel` structures, and Java records

##### Limitations

- **ID Required**: Every entity being updated (main or joined) must include its ID field in the model
- **No Collections**: Cannot update collection fields directly - only scalar fields and single relations
- **No Cascading**: Updates apply only to specified entities; no automatic cascade to related entities unless explicitly included
- **JPA Only**: Currently available only for Spring Data JPA (not MongoDB or Elasticsearch)
- **No Partial Joins**: When updating joined entities, you must provide the complete join path (e.g., cannot skip intermediate entities in a chain)

##### Model Examples

Here's a comprehensive example showing different update model patterns:

```java
// Deep 4-level join update - Record style (most complex example)
@JdqModel
public record FourLevelDeepRecordUpdateModel(
    @JdqField("id") Long adminId,
    @JdqField("username") String username,
    @JdqSubModel("roles") RoleLevel2Record role
) {
    @JdqModel
    public record RoleLevel2Record(
        @JdqField("id") Long roleId,
        @JdqField("name") String roleName,
        @JdqField("description") String roleDescription,
        @JdqSubModel("roleAuthorizations") RoleAuthorizationLevel3Record roleAuthorization
    ) {
        @JdqModel
        public record RoleAuthorizationLevel3Record(
            @JdqField("id") Long roleAuthorizationId,
            @JdqSubModel("authorization") AuthorizationLevel4Record authorization
        ) {
            @JdqModel
            public record AuthorizationLevel4Record(
                @JdqField("id") Long authorizationId,
                @JdqField("name") String authorizationName,
                @JdqField("menuUrl") String menuUrl,
                @JdqField("menuIcon") String menuIcon
            ) {}
        }
    }
}
```

##### Repository Usage

The update method is available on all `JpaDynamicQueryRepository` instances:

```java
// Deep 4-level update
FourLevelDeepRecordUpdateModel deepUpdate = new FourLevelDeepRecordUpdateModel(
    3L, "adminName",
    new FourLevelDeepRecordUpdateModel.RoleLevel2Record(
        3L, "roleName", "roleDesc",
        new FourLevelDeepRecordUpdateModel.RoleLevel2Record.RoleAuthorizationLevel3Record(
            3L,
            new FourLevelDeepRecordUpdateModel.RoleLevel2Record.RoleAuthorizationLevel3Record.AuthorizationLevel4Record(
                3L, "authName", "/url", "icon"
            )
        )
    )
);
adminUserRepository.update(deepUpdate);
```

_Generated Hibernate Queries:_

```sql
-- Deep 4-level update generates UPDATE for each level
update admin_user set username=? where id=?
update role set name=?, description=? where id=?
update my_authorization set name=?, menu_url=?, menu_icon=? where id=?
```

#### 7- Pagination Examples

You can find all pagination methods in the `JpaDynamicQueryRepository` interface. You can use the `findAllAsPage` method
to get the result as a page.

```java
DynamicQuery dynamicQuery = new DynamicQuery();
dynamicQuery.setWhere(CriteriaList.of(Criteria.of(Course.Fields.id, CriteriaOperator.GREATER_THAN, 3)));
dynamicQuery.setPageSize(2);
dynamicQuery.setPageNumber(1);

Page<Course> result = courseRepository.findAllAsPage(dynamicQuery);
```

_Note: you can find the example on demo github repository._

#### 8- Query Builder Examples

When you want to use Dynamic Query on programmatic way, you can use Query Builder. Query Builder is a fluent API. You
can use it to create a dynamic query. I inspired from `QueryDSL` project but it is just easy to use DTO create builder
for `DynamicQuery` object.

```java
Page<AuthorizationSummary> result = adminUserRepository.queryBuilder()
        .select(Select("id", "adminId"),
                Select("username", "adminUsername"),
                Select("roles.id", "roleId"),
                Select("roles.name", "roleName"),
                Select("roles.roleAuthorizations.authorization.id", "authorizationId"),
                Select("roles.roleAuthorizations.authorization.name", "authorizationName"),
                Select("roles.roleAuthorizations.authorization.menuIcon", "menuIcon"))
        .distinct(false)
        .where(Field("roles.roleAuthorizations.authorization.menuIcon").startWith("icon"), Parantesis(Field("id").eq(3), OR, Field("roles.id").eq(4), OR, Field("id").eq(5)), Parantesis(Field("id").eq(5), OR, Field("id").eq(4), OR, Field("roles.id").eq(3)))
        .orderBy(OrderBy("roles.id", Order.DESC))
        .page(1, 2)
        .getResultAsPage(AuthorizationSummary.class);
```

_Note: you can find the example on demo github repository._

#### 9- Argument Resolver Examples

Argument resolvers are used to automatically create dynamic queries from the request parameters. You can use the
`@EnableJpaDynamicQueryArgumentResolvers` annotation to enable this feature.

Your controller methods can take `CriteriaList` and `DynamicQuery` objects as parameters.

```java

@GetMapping("/course")
public ResponseEntity<List<Course>> getCourseWithCriteria(CriteriaList criteriaList) {
    List<Course> customerList = courseRepository.findAll(criteriaList);
    return ResponseEntity.ok().body(customerList);
}

@GetMapping("/course/as-list")
public ResponseEntity<List<AuthorizationSummary>> getCourseWithDynamicQueryAsList(DynamicQuery dynamicQuery) {
    List<AuthorizationSummary> customerList = adminUserRepository.findAll(dynamicQuery, AuthorizationSummary.class);
    return ResponseEntity.ok().body(customerList);
}
```

_Test the API with the following request:_

```java
printRequestedResult(COURSE_CRITERIA_API_URL, "key0=name&operation0=CONTAIN&values0=Calculus",Course[].class);

printRequestedResult(COURSE_SEARCH_LIST_API_URL,
            "select0=id&select1=username&select2=roles.id&select3=roles.name&select4=roles.roleAuthorizations.authorization.id&select5=roles.roleAuthorizations.authorization.name&select6=roles.roleAuthorizations.authorization.menuIcon&"+
                    "selectAs0=adminId&selectAs1=adminUsername&selectAs2=roleId&selectAs3=roleName&selectAs4=authorizationId&selectAs5=authorizationName&selectAs6=menuIcon&"+
                    "orderBy0=roles.id&orderByDirection0=desc&"+
                    "page=1&"+
                    "pageSize=2&"+
                    "key0=roles.roleAuthorizations.authorization.menuIcon&operation0=START_WITH&values0=icon",S9_Query_Builder.AuthorizationSummary[].class);
```

_Note: you can find the example on demo github repository._

```sql
-- COURSE_CRITERIA_API_URL Example
select course0_.id                as id1_3_,
       course0_.active            as active2_3_,
       course0_.description       as descript3_3_,
       course0_.max_student_count as max_stud4_3_,
       course0_.name              as name5_3_,
       course0_.start_date        as start_da6_3_
from course course0_
where course0_.name like ?

-- COURSE_SEARCH_LIST_API_URL Example
select adminuser0_.id         as col_0_0_,
       adminuser0_.username   as col_1_0_,
       role2_.id              as col_2_0_,
       role2_.name            as col_3_0_,
       authorizat4_.id        as col_4_0_,
       authorizat4_.name      as col_5_0_,
       authorizat4_.menu_icon as col_6_0_
from admin_user adminuser0_
         inner join admin_user_role roles1_ on adminuser0_.id = roles1_.admin_user_id
         inner join role role2_ on roles1_.role_id = role2_.id
         inner join role_authorization roleauthor3_ on role2_.id = roleauthor3_.role_id
         inner join my_authorization authorizat4_ on roleauthor3_.authorization_id = authorizat4_.id
where authorizat4_.menu_icon like ?
order by role2_.id desc limit ?
offset ?
```

#### 10- Custom Converter

When you start using this library, there will be things that will really trouble you. But don't worry, we have a
solution for you.

Firstly, `BasicDeserializer` is our default deserializer.

When you want to change Date object deserialization, You can create a custom deserializer and override the `deserialize`
method. **Filter the class what you want to change the deserialization process**. After that, **pass the other object
types to the default deserializer.**

```java

@Component
public class DateTimeDeserializer extends BasicDeserializer {

    @Override
    public <T> T deserialize(Object value, Class<T> clazz) throws Exception {
        if (clazz.isAssignableFrom(java.util.Date.class)) { // only Date class changing to deserialization
            if (value instanceof Date date) {
                return (T) date;
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return (T) simpleDateFormat.parse(value.toString()); // Date deserialization complete
        }

        return super.deserialize(value, clazz); // other deserialization is supported from our project default deserializer
    }
}
```

#### 11- Custom Entity Manager Provider

#### 12- Additional Features

## More Potential(Future) Features

- Policy Management: For ensure security, we can add policy management for each query. For example, we can add a policy
  for each query. If the user does not have permission to access the query, we can throw an exception.

## Performance

## Security
## Is Production Ready?
**This project is production ready.** But you must be careful when you are using this project. Because if you are using Argument Resolvers on customer api, then bad people can access unauthorized data using with select and join features. This project is fully secured for sql injection but careful to unauthorized queries. You can use programatic queries safely with QueryBuilder or DynamicQuery object.

You can easily use this project on your backoffice or admin panel. But you must be careful when you are using this project on customer api. **Don't miss the use Spring Security or JWT token or another security mechanism for secure your api.**

**Not:** Don't worry when you are using Argument Resolver feature if your entities don't have `@ManyToMany`, `@OneToMany`, `@OneToOne`, `@ManyToOne` annotations. Because this project is not support join without these annotations. Just check unauthorized queries with `OR` and/or `PARENTHES` operators. 

## License
Apache License 2.0

## Conclusion

In conclusion, this project is designed to overcome the
sluggishness of Spring Data JPA's query creation and the need to write separate code for each query. It simplifies the
Criteria API, enabling programmatic or dynamic runtime query creation. No operation cancels the previous operation. You can use many different operations together such as Operators with Join,
Join with Or-Scope, Or and Scope with Projection, Projection with OrderBy, OrderBy with Pageable. You can use all of
these with Query Builder or dto with Argument Resolver or creating new DynamicQuery object. You can create a custom deserializer for avoiding bad times.
Finally, I want to say this: They say Java is a cumbersome language, but if you design and use Java in a cumbersome way, it becomes cumbersome. In this project, I tried to eliminate the clumsiness of Spring Data JPA, which is cumbersome. I hope I have been successful. Good work.



