package com.beyt.jdq.resolver;

import com.beyt.jdq.BaseTestInstance;
import com.beyt.jdq.TestApplication;
import com.beyt.jdq.TestUtil;
import com.beyt.jdq.testenv.entity.Customer;
import com.beyt.jdq.testenv.entity.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static com.beyt.jdq.TestUtil.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest(classes = TestApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArgumentResolversTests extends BaseTestInstance {

    @Autowired
    private MockMvc mockMvc;

    private static final String CUSTOMER_CRITERIA_API_URL = "/test-api/customer";
    private static final String USER_CRITERIA_API_URL = "/test-api/user";
    private static final String CUSTOMER_SEARCH_LIST_API_URL = "/test-api/customer/as-list";
    private static final String CUSTOMER_SEARCH_PAGE_API_URL = "/test-api/customer/as-page";
    private static final String USER_SEARCH_LIST_API_URL = "/test-api/user/as-list";
    private static final String USER_SEARCH_PAGE_API_URL = "/test-api/user/as-page";

    @Test
    public void argumentCriteriaListTests() throws Exception {
       /* CONTAIN
                DOES_NOT_CONTAIN
        END_WITH
                START_WITH
        SPECIFIED
                EQUAL
        NOT_EQUAL
                GREATER_THAN
        GREATER_THAN_OR_EQUAL
                LESS_THAN
        LESS_THAN_OR_EQUAL
                OR
        PARENTHES */
        // toList(customer1, customer2, customer3, customer4, customer5, customer6, customer7, customer8)
        // toList(user1, user2, user3, user4, user5, user6, user7, user8)
        checkRequestSingleUserEntity(toList(user1), USER_CRITERIA_API_URL,
                "key0=id&operation0=EQUAL&values0=1", User[].class);
        checkRequestSingleUserEntity(toList(user2, user4, user6, user7), USER_CRITERIA_API_URL,
                "key0=status&operation0=EQUAL&values0=ACTIVE", User[].class);
        checkRequestSingleUserEntity(toList(user2, user5, user6, user8), USER_CRITERIA_API_URL,
                "key0=type&operation0=EQUAL&values0=ADMIN", User[].class);
        checkRequestSingleUserEntity(toList(user2, user6), USER_CRITERIA_API_URL,
                "key0=type&operation0=EQUAL&values0=ADMIN&key1=status&operation1=EQUAL&values1=ACTIVE", User[].class);
        checkRequestSingleUserEntity(toList(user1, user2, user3, user4, user6, user7, user8), USER_CRITERIA_API_URL,
                "key0=id&operation0=NOT_EQUAL&values0=5", User[].class);
        checkRequestSingleUserEntity(toList(user5), USER_CRITERIA_API_URL,
                "key0=name&operation0=CONTAIN&values0=5", User[].class);
        checkRequestSingleUserEntity(toList(user1, user2, user3, user4, user6, user7, user8), USER_CRITERIA_API_URL,
                "key0=name&operation0=DOES_NOT_CONTAIN&values0=5", User[].class);
        checkRequestSingleUserEntity(toList(user5), USER_CRITERIA_API_URL,
                "key0=name&operation0=END_WITH&values0=5", User[].class);
        checkRequestSingleUserEntity(toList(user1, user2, user3, user4, user5, user6, user7, user8), USER_CRITERIA_API_URL,
                "key0=name&operation0=START_WITH&values0=Name", User[].class);
        checkRequestSingleUserEntity(toList(user2, user3, user4, user5, user6, user7, user8), USER_CRITERIA_API_URL,
                "key0=id&operation0=GREATER_THAN&values0=1", User[].class);
        checkRequestSingleUserEntity(toList(user5, user6, user7, user8), USER_CRITERIA_API_URL,
                "key0=id&operation0=GREATER_THAN_OR_EQUAL&values0=5", User[].class);
        checkRequestSingleUserEntity(toList(user1, user2), USER_CRITERIA_API_URL,
                "key0=id&operation0=LESS_THAN&values0=3", User[].class);
        checkRequestSingleUserEntity(toList(user1, user2, user3), USER_CRITERIA_API_URL,
                "key0=id&operation0=LESS_THAN_OR_EQUAL&values0=3", User[].class);
        checkRequestSingleUserEntity(toList(user1, user2), USER_CRITERIA_API_URL,
                "key0=id&operation0=EQUAL&values0=1" +
                        "&key1=&operation1=OR&values1=" +
                        "&key2=id&operation2=EQUAL&values2=2", User[].class);
        checkShouldNotBeFound(USER_CRITERIA_API_URL,
                "key0=id&operation0=EQUAL&values0=0");

        // Join Tests
        checkRequestSingleUserEntity(toList(customer1, customer2, customer3, customer4, customer5, customer6, customer7, customer8), CUSTOMER_CRITERIA_API_URL,
                "key0=user.surname&operation0=CONTAIN&values0=Surname", Customer[].class);
    }


    @Test
    public void argumentSearchQueryTests() throws Exception {
        checkRequestSingleUserEntity(toList(User.builder().name(user7.getName()).surname(user7.getSurname()).birthdate(user7.getBirthdate()).build(),
                User.builder().name(user6.getName()).surname(user6.getSurname()).birthdate(user6.getBirthdate()).build()), USER_SEARCH_LIST_API_URL,
                "select0=name&select1=surname&select2=birthdate&" +
                        "orderBy0=name&orderByDirection0=desc&" +
                        "page=0&" +
                        "pageSize=2&" +
                        "key0=id&operation0=NOT_EQUAL&values0=8", User[].class);
    }

    private <T> void checkRequestSingleUserEntity(List<T> userList, String apiUrl, String filter, Class<T[]> clazz) throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(apiUrl + "?" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(userList, TestUtil.getResultListValue(mvcResult.getResponse().getContentAsString(), clazz));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void checkShouldNotBeFound(String apiUrl, String filter) throws Exception {
        mockMvc.perform(get(apiUrl + "?" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

}
