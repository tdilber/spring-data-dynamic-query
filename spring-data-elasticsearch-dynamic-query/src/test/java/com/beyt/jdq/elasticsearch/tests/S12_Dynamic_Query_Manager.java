package com.beyt.jdq.elasticsearch.tests;

import com.beyt.jdq.core.model.annotation.JdqField;
import com.beyt.jdq.core.model.annotation.JdqIgnoreField;
import com.beyt.jdq.core.model.annotation.JdqModel;
import com.beyt.jdq.core.model.Criteria;
import com.beyt.jdq.core.model.CriteriaList;
import com.beyt.jdq.core.model.DynamicQuery;
import com.beyt.jdq.core.model.enums.CriteriaOperator;
import com.beyt.jdq.core.model.enums.Order;
import com.beyt.jdq.core.model.exception.DynamicQueryNoAvailableOrOperationUsageException;
import com.beyt.jdq.elasticsearch.ElasticsearchTestApplication;
import com.beyt.jdq.elasticsearch.entity.Customer;
import com.beyt.jdq.elasticsearch.entity.User;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import static com.beyt.jdq.core.model.builder.QuerySimplifier.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Elasticsearch tests for DynamicQueryManager functionality.
 * Tests complex query scenarios, projections, pagination, and JdqModel annotations.
 */
@SpringBootTest(classes = ElasticsearchTestApplication.class)
@ActiveProfiles("estest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Disabled("TODO: Enable after previous tests pass")
class S12_Dynamic_Query_Manager extends BaseElasticsearchTestInstance {

    private SimpleDateFormat dateFormat;

    @Test
    void test() {
        assertEquals(toList(customer4, customer5, customer6, customer7),
                customerRepository.findAll(CriteriaList.of(
                        Criteria.of("age", CriteriaOperator.EQUAL, 23, 24),
                        Criteria.of("age", CriteriaOperator.NOT_EQUAL, 20, 21),
                        Criteria.OR(), // ( [ (23 or 24) AND (not 20 and not 21) ] "OR" [ (not 24) AND (25 or 26) ])
                        Criteria.of("age", CriteriaOperator.NOT_EQUAL, 24),
                        Criteria.of("age", CriteriaOperator.EQUAL, 25, 26))));
    }

    @Test
    void findAll() {
        // SINGLE CRITERIA TESTS

        //Support Single Input => GREATER_THAN_OR_EQUAL, GREATER_THAN, LESS_THAN_OR_EQUAL, LESS_THAN
        assertEquals(toList(user2, user4, user6, user7),
                userRepository.findAll(CriteriaList.of(Criteria.of("status", CriteriaOperator.EQUAL, User.Status.ACTIVE))));
        assertEquals(toList(user1, user2, user3, user4, user5, user6, user7, user8),
                userRepository.findAll(CriteriaList.of(Criteria.of("status", CriteriaOperator.EQUAL, User.Status.ACTIVE, User.Status.PASSIVE))));
        assertEquals(toList(user2, user5, user6, user8),
                userRepository.findAll(CriteriaList.of(Criteria.of("type", CriteriaOperator.EQUAL, User.Type.ADMIN))));
        assertEquals(toList(user2, user6),
                userRepository.findAll(CriteriaList.of(Criteria.of("type", CriteriaOperator.EQUAL, User.Type.ADMIN), Criteria.of("status", CriteriaOperator.EQUAL, User.Status.ACTIVE))));
        assertEquals(toList(customer5, customer6, customer7, customer8),
                customerRepository.findAll(CriteriaList.of(Criteria.of("age", CriteriaOperator.GREATER_THAN_OR_EQUAL, 24))));
        assertEquals(toList(customer6, customer7, customer8),
                customerRepository.findAll(CriteriaList.of(Criteria.of("age", CriteriaOperator.GREATER_THAN, 24))));
        assertEquals(toList(customer1, customer2, customer3, customer4, customer5),
                customerRepository.findAll(CriteriaList.of(Criteria.of("age", CriteriaOperator.LESS_THAN_OR_EQUAL, 24))));
        assertEquals(toList(customer1, customer2, customer3, customer4),
                customerRepository.findAll(CriteriaList.of(Criteria.of("age", CriteriaOperator.LESS_THAN, 24))));

        INSTANCE.add(Calendar.MONTH, 3);
        dateFormat = new SimpleDateFormat();
        assertEquals(toList(customer5, customer6, customer7, customer8),
                customerRepository.findAll(CriteriaList.of(Criteria.of("birthdate", CriteriaOperator.LESS_THAN_OR_EQUAL, INSTANCE.toInstant()))));
        assertEquals(toList(customer6, customer7, customer8),
                customerRepository.findAll(CriteriaList.of(Criteria.of("birthdate", CriteriaOperator.LESS_THAN, INSTANCE.toInstant()))));
        assertEquals(toList(customer1, customer2, customer3, customer4, customer5),
                customerRepository.findAll(CriteriaList.of(Criteria.of("birthdate", CriteriaOperator.GREATER_THAN_OR_EQUAL, INSTANCE.toInstant()))));
        assertEquals(toList(customer1, customer2, customer3, customer4),
                customerRepository.findAll(CriteriaList.of(Criteria.of("birthdate", CriteriaOperator.GREATER_THAN, INSTANCE.toInstant()))));

        // Support Multi Input => EQUAL, NOT_EQUAL
        assertEquals(toList(customer5),
                customerRepository.findAll(CriteriaList.of(Criteria.of("age", CriteriaOperator.EQUAL, 24))));
        assertEquals(toList(customer5, customer6, customer7),
                customerRepository.findAll(CriteriaList.of(Criteria.of("age", CriteriaOperator.EQUAL, 24, 25, 26))));
        assertEquals(toList(customer4, customer5, customer6),
                customerRepository.findAll(CriteriaList.of(Criteria.of("age", CriteriaOperator.EQUAL, 23, 24, 25))));
        assertEquals(toList(),
                customerRepository.findAll(CriteriaList.of(Criteria.of("age", CriteriaOperator.EQUAL, 24), Criteria.of("age", CriteriaOperator.EQUAL, 25))));
        assertEquals(toList(customer6),
                customerRepository.findAll(CriteriaList.of(Criteria.of("age", CriteriaOperator.EQUAL, 23, 24, 25), Criteria.of("age", CriteriaOperator.EQUAL, 25, 26))));

        assertEquals(toList(customer1, customer2, customer3, customer4, customer6, customer7, customer8),
                customerRepository.findAll(CriteriaList.of(Criteria.of("age", CriteriaOperator.NOT_EQUAL, 24))));
        assertEquals(toList(customer1, customer2, customer3, customer6, customer7, customer8),
                customerRepository.findAll(CriteriaList.of(Criteria.of("age", CriteriaOperator.NOT_EQUAL, 23, 24))));
        assertEquals(toList(customer1, customer2, customer3, customer7, customer8),
                customerRepository.findAll(CriteriaList.of(Criteria.of("age", CriteriaOperator.NOT_EQUAL, 23, 24, 25))));

        // Support Multi Input => CONTAIN, DOES_NOT_CONTAIN, START_WITH, END_WITH
        assertEquals(toList(customer1, customer2, customer3, customer4, customer5, customer6, customer7),
                customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.CONTAIN, "Customer"))));
        assertEquals(toList(customer1, customer2),
                customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.CONTAIN, "1", "2"))));
        assertEquals(toList(customer1, customer2, customer3, customer4, customer6, customer7),
                customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.DOES_NOT_CONTAIN, "5"))));
        assertEquals(toList(customer1, customer2, customer3, customer6, customer7),
                customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.DOES_NOT_CONTAIN, "5", "4"))));
        assertEquals(toList(customer1, customer2, customer3, customer4, customer5, customer6, customer7),
                customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.START_WITH, "Customer"))));
        assertEquals(toList(customer3, customer4),
                customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.START_WITH, "Customer 3", "Customer 4"))));
        assertEquals(toList(customer7),
                customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.END_WITH, " 7"))));
        assertEquals(toList(customer5, customer6),
                customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.END_WITH, "5", "6"))));

        // Null, Not Null check. Support Single Input => SPECIFIED
        assertEquals(toList(customer1, customer2, customer3, customer4, customer5, customer6, customer7),
                customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.SPECIFIED, true))));
        assertEquals(toList(customer8),
                customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.SPECIFIED, false))));

        // AND CRITERIA TESTs

        assertEquals(toList(), // Empty Result
                customerRepository.findAll(CriteriaList.of(
                        Criteria.of("age", CriteriaOperator.EQUAL, 24),
                        Criteria.of("age", CriteriaOperator.EQUAL, 25))));

        assertEquals(toList(customer5),
                customerRepository.findAll(CriteriaList.of(
                        Criteria.of("age", CriteriaOperator.EQUAL, 24),
                        Criteria.of("name", CriteriaOperator.EQUAL, "Customer 5"))));

        assertEquals(toList(customer1, customer2, customer5),
                customerRepository.findAll(CriteriaList.of(
                        Criteria.of("age", CriteriaOperator.EQUAL, 20, 21, 24, 25, 26), // OR 20, 21, 24, 25
                        Criteria.of("age", CriteriaOperator.NOT_EQUAL, 25, 26)))); // AND NOT 25 AND NOT 26


        assertEquals(toList(customer1, customer2),
                customerRepository.findAll(CriteriaList.of(
                        Criteria.of("age", CriteriaOperator.EQUAL, 20, 21, 24, 25, 26), // OR 20, 21, 24, 25
                        Criteria.of("age", CriteriaOperator.NOT_EQUAL, 25, 26),// AND NOT 25 AND NOT 26
                        Criteria.of("age", CriteriaOperator.LESS_THAN, 24)))); // AND LESS THAN 24


        assertEquals(toList(customer1),
                customerRepository.findAll(CriteriaList.of(
                        Criteria.of("age", CriteriaOperator.EQUAL, 20, 21, 24, 25, 26), // OR 20, 21, 24, 25
                        Criteria.of("age", CriteriaOperator.NOT_EQUAL, 25, 26), // AND NOT 25 AND NOT 26
                        Criteria.of("age", CriteriaOperator.LESS_THAN, 24), // AND LESS THAN 24
                        Criteria.of("name", CriteriaOperator.CONTAIN, "1")))); // AND CONTAIN name 1


        // OR CRITERIA TESTs
        assertThrows(DynamicQueryNoAvailableOrOperationUsageException.class, () ->
                customerRepository.findAll(CriteriaList.of(Criteria.OR())));
        assertThrows(DynamicQueryNoAvailableOrOperationUsageException.class, () ->
                customerRepository.findAll(CriteriaList.of(Criteria.of("name", CriteriaOperator.EQUAL, "Customer 1"), Criteria.OR())));
        assertThrows(DynamicQueryNoAvailableOrOperationUsageException.class, () ->
                customerRepository.findAll(CriteriaList.of(Criteria.OR(), Criteria.of("name", CriteriaOperator.EQUAL, "Customer 1"))));

        assertEquals(toList(customer1, customer2),
                customerRepository.findAll(CriteriaList.of(
                        Criteria.of("name", CriteriaOperator.EQUAL, "Customer 1"), Criteria.OR(),
                        Criteria.of("name", CriteriaOperator.EQUAL, "Customer 2"))));

        assertEquals(toList(customer1, customer2, customer3),
                customerRepository.findAll(CriteriaList.of(
                        Criteria.of("name", CriteriaOperator.EQUAL, "Customer 1"),
                        Criteria.OR(),
                        Criteria.of("name", CriteriaOperator.EQUAL, "Customer 2"),
                        Criteria.OR(),
                        Criteria.of("name", CriteriaOperator.EQUAL, "Customer 3")
                )));

        assertEquals(toList(customer1, customer2, customer3, customer4, customer5, customer6, customer7, customer8),
                customerRepository.findAll(CriteriaList.of(
                        Criteria.of("age", CriteriaOperator.EQUAL, 24),
                        Criteria.OR(),
                        Criteria.of("age", CriteriaOperator.NOT_EQUAL, 24))));

        assertEquals(toList(customer4, customer5, customer6, customer7),
                customerRepository.findAll(CriteriaList.of(
                        Criteria.of("age", CriteriaOperator.EQUAL, 23, 24),
                        Criteria.of("age", CriteriaOperator.NOT_EQUAL, 20, 21),
                        Criteria.OR(), // ( [ (23 or 24) AND (not 20 and not 21) ] "OR" [ (not 24) AND (25 or 26) ])
                        Criteria.of("age", CriteriaOperator.NOT_EQUAL, 24),
                        Criteria.of("age", CriteriaOperator.EQUAL, 25, 26))));
    }

    @SafeVarargs
    public static <T> List<T> toList(T... values) {
        return List.of(values);
    }

    @Test
    void searchQuery() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.getWhere().add(Criteria.of("age", CriteriaOperator.GREATER_THAN, 20));
        dynamicQuery.setPageSize(10);
        dynamicQuery.setPageNumber(0);
        dynamicQuery.setDistinct(true);
        dynamicQuery.getOrderBy().add(Pair.of("age", Order.ASC));

        DynamicQuery dynamicQuery1 = new DynamicQuery();
        dynamicQuery1.getSelect().add(Pair.of("id", "id"));
        dynamicQuery1.getSelect().add(Pair.of("name", "name"));
        DynamicQuery dynamicQuery2 = new DynamicQuery();

        List<Customer> allWithSearchQuery1 = customerRepository.findAll(dynamicQuery1, Customer.class);
        List<User> allWithSearchQuery2 = customerRepository.findAll(dynamicQuery1, User.class);
        List<User> allWithSearchQuery3 = customerRepository.findAll(dynamicQuery2, User.class);
        List<Customer> allWithSearchQuery4 = customerRepository.findAll(dynamicQuery2, Customer.class);
    }

    @JdqModel
    public static class UserJdqModel {
        @JdqField("name")
        private String nameButDifferentFieldName;
        @JdqField("user.name")
        private String userNameWithJoin;

        private Integer age;
        @JdqIgnoreField
        private String surname;

        public UserJdqModel() {
        }

        public UserJdqModel(String nameButDifferentFieldName, String userNameWithJoin, Integer age, String surname) {
            this.nameButDifferentFieldName = nameButDifferentFieldName;
            this.userNameWithJoin = userNameWithJoin;
            this.age = age;
            this.surname = surname;
        }

        public String getNameButDifferentFieldName() {
            return nameButDifferentFieldName;
        }

        public void setNameButDifferentFieldName(String nameButDifferentFieldName) {
            this.nameButDifferentFieldName = nameButDifferentFieldName;
        }

        public String getUserNameWithJoin() {
            return userNameWithJoin;
        }

        public void setUserNameWithJoin(String userNameWithJoin) {
            this.userNameWithJoin = userNameWithJoin;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }

        @Override
        public boolean equals(Object object) {
            if (object == null || getClass() != object.getClass()) return false;
            UserJdqModel that = (UserJdqModel) object;
            return Objects.equals(nameButDifferentFieldName, that.nameButDifferentFieldName) && Objects.equals(userNameWithJoin, that.userNameWithJoin) && Objects.equals(age, that.age) && Objects.equals(surname, that.surname);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nameButDifferentFieldName, userNameWithJoin, age, surname);
        }
    }

    @Test
    void JdqModels() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.getWhere().add(Criteria.of("age", CriteriaOperator.GREATER_THAN, 25));

        List<UserJdqModel> result = customerRepository.findAll(dynamicQuery, UserJdqModel.class);
        assertEquals(toList(new UserJdqModel(customer7.getName(),  customer7.getUser().getName(), customer7.getAge(), null), new UserJdqModel(customer8.getName(),   customer8.getUser().getName(), customer8.getAge(), null)), result);
    }

    @JdqModel
    public record UserJdqModelRecord(@JdqField("name") String nameButDifferentFieldName, @JdqField("user.name") String userNameWithJoin, Integer age) {
    }

    @JdqModel
    public record UserJdqModelRecordNotValid(@JdqField("name") String nameButDifferentFieldName, @JdqField("user.name") String userNameWithJoin, @JdqIgnoreField Integer age) {
    }

    @Test
    void JdqModelRecord() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.getWhere().add(Criteria.of("age", CriteriaOperator.GREATER_THAN, 25));

        List<UserJdqModelRecord> result = customerRepository.findAll(dynamicQuery, UserJdqModelRecord.class);
        assertEquals(toList(new UserJdqModelRecord(customer7.getName(),  customer7.getUser().getName(), customer7.getAge()), new UserJdqModelRecord(customer8.getName(),   customer8.getUser().getName(), customer8.getAge())), result);
        assertThrows(Exception.class, () -> customerRepository.findAll(dynamicQuery, UserJdqModelRecordNotValid.class));
    }

    @Test
    void simplifiedSearchQuery() {
        List<User> result = customerRepository.queryBuilder()
                .select(Select("user.name", "name"), Select("user.age"), Select("name", "surname"), Select("birthdate", "birthdate"))
                .distinct(false)
                .where(Parantesis(Field("id").eq(3), OR, Field("user.id").eq(4), OR, Field("id").eq(5)), OR, Parantesis(Field("id").eq(6), OR, Field("id").eq(4), OR, Field("user.id").eq(5)))
                .orderBy(OrderBy("user.id", Order.ASC))
                .page(0, 2)
                .getResult(User.class);

        assertEquals(toList(new User(null, user3.getName(), customer3.getName(), user3.getAge(), customer3.getBirthdate(), null, null),
                new User(null, user4.getName(), customer4.getName(), user4.getAge(), customer4.getBirthdate(), null, null)), result);

        Page<User> resultAsPage = customerRepository.queryBuilder()
                .select(Select("user.name", "name"), Select("user.age"), Select("name", "surname"), Select("birthdate", "birthdate"))
                .distinct(false)
                .where(Parantesis(Field("id").eq(3), OR, Field("user.id").eq(4), OR, Field("id").eq(5)), OR, Parantesis(Field("id").eq(6), OR, Field("id").eq(4), OR, Field("user.id").eq(5)))
                .orderBy(OrderBy("user.id", Order.ASC))
                .page(1, 2)
                .getResultAsPage(User.class);

        assertEquals(toList(new User(null, user5.getName(), customer5.getName(), user5.getAge(), customer5.getBirthdate(), null, null),
                new User(null, user6.getName(), customer6.getName(), user6.getAge(), customer6.getBirthdate(), null, null)), resultAsPage.getContent());

        assertEquals(4, resultAsPage.getTotalElements());
        assertEquals(2, resultAsPage.getTotalPages());
        assertEquals(Sort.Direction.ASC, resultAsPage.getPageable().getSort().getOrderFor("user.id").getDirection());
    }

    @Test
    void queryBuilderTests() {
        assertEquals(toList(user1, user2, user3, user4, user5, user6, user7, user8),
                userRepository.queryBuilder().getResult());
        assertEquals(toList(user2, user4, user6, user7),
                userRepository.queryBuilder().where(Field("status").eq(User.Status.ACTIVE)).getResult());
        assertEquals(toList(user7, user6, user4, user2),
                userRepository.queryBuilder().where(Field("status").eq(User.Status.ACTIVE)).orderBy(OrderBy("id", Order.DESC)).getResult());
    }

    public static class UserName {
        private String name;

        public UserName() {
        }

        public UserName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    void page() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.setWhere(CriteriaList.of(Criteria.of("id", CriteriaOperator.GREATER_THAN, 3)));
        dynamicQuery.setPageSize(2);
        dynamicQuery.setPageNumber(1);
        Page<User> result = userRepository.findAllAsPage(dynamicQuery);
        assertEquals(toList(user6, user7), result.getContent());
        dynamicQuery.getSelect().add(Pair.of("name", "name"));
        Page<UserName> result2 = userRepository.findAllAsPage(dynamicQuery, UserName.class);
        assertEquals(toList(user6.getName(), user7.getName()), result2.getContent().stream().map(UserName::getName).toList());
    }
}

