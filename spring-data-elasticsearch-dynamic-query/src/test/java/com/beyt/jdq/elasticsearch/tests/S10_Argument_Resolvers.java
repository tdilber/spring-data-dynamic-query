package com.beyt.jdq.elasticsearch.tests;

import com.beyt.jdq.elasticsearch.ElasticsearchTestApplication;
import com.beyt.jdq.elasticsearch.entity.Course;
import com.beyt.jdq.elasticsearch.util.PresentationUtil;
import com.beyt.jdq.elasticsearch.util.TestUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Elasticsearch tests for ARGUMENT RESOLVERS functionality.
 * 
 * Tests automatic conversion of HTTP query parameters to:
 * - CriteriaList: Simple criteria-based filtering
 * - DynamicQuery: Complex queries with select, where, orderBy, pagination
 * 
 * Current implementation status:
 * ✓ MongoTestController created with REST endpoints
 * ✓ Argument resolvers should work the same way as JPA (CriteriaListArgumentResolver, DynamicQueryArgumentResolver)
 * ✓ Elasticsearch repositories support findAll(CriteriaList) and findAll(DynamicQuery)
 * ✓ Test data setup complete
 * 
 * These tests verify that:
 * - Query parameters are correctly parsed into Criteria objects
 * - Different CriteriaOperator types work correctly (CONTAIN, EQUAL, GREATER_THAN, etc.)
 * - DynamicQuery projection and pagination work with Elasticsearch
 * - Nested field filtering works (e.g., roles.roleAuthorizations.authorization.menuIcon)
 * 
 * Query parameter format:
 * - key{N}=fieldName - the field to filter on
 * - operation{N}=OPERATOR - the comparison operator
 * - values{N}=value - the value to compare against
 * - select{N}=fieldName - field to select in projection
 * - selectAs{N}=alias - alias for selected field
 * - orderBy{N}=fieldName - field to order by
 * - orderByDirection{N}=asc|desc - order direction
 * - page=N - page number
 * - pageSize=N - page size
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = ElasticsearchTestApplication.class)
@ActiveProfiles("estest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class S10_Argument_Resolvers extends BaseElasticsearchJoinTestInstance {

    @Autowired
    private MockMvc mockMvc;

    private static final String COURSE_SEARCH_LIST_API_URL = "/elasticsearch-test-api/course/as-list";
    private static final String COURSE_CRITERIA_API_URL = "/elasticsearch-test-api/course";

    /**
     * Test CriteriaList argument resolver with various operators
     * 
     * Verifies that query parameters are correctly converted to CriteriaList
     * and that Elasticsearch queries are executed properly for different operator types.
     */
    @Test
    public void argumentCriteriaListTests() throws Exception {
        // Test data: List.of(course1, course2, course3, course4, course5, course6, course7, course8, course9, course10)
        
        // CONTAIN operator - find courses with "Calculus" in name
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=name&operation0=CONTAIN&values0=Calculus", Course[].class, List.of(course2, course3));
        
        // DOES_NOT_CONTAIN operator - find courses without "I" in name (all courses have "I" in name)
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=name&operation0=DOES_NOT_CONTAIN&values0=I", Course[].class, List.of());
        
        // END_WITH operator - find courses ending with "Science"
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=name&operation0=END_WITH&values0=Science", Course[].class, List.of(course1));
        
        // START_WITH operator - find courses starting with "Physics"
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=name&operation0=START_WITH&values0=Physics", Course[].class, List.of(course4, course5));
        
        // SPECIFIED operator (true) - find courses where active field is not null and true
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=active&operation0=SPECIFIED&values0=true", Course[].class, List.of(course1, course2, course8, course9, course10));
        
        // SPECIFIED operator (false) - find courses where active field is null
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=active&operation0=SPECIFIED&values0=false", Course[].class, List.of(course3, course4, course5, course6, course7));
        
        // EQUAL operator - exact string match
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=name&operation0=EQUAL&values0=Calculus I", Course[].class, List.of(course2));
        
        // EQUAL operator with date - find courses with specific start date
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=startDate&operation0=EQUAL&values0=2015-06-18", Course[].class, List.of());
        
        // EQUAL operator with number
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=maxStudentCount&operation0=EQUAL&values0=54", Course[].class, List.of(course9));
        
        // NOT_EQUAL operator
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=name&operation0=NOT_EQUAL&values0=Introduction to Computer Science", Course[].class, List.of(course2, course3, course4, course5, course6, course7, course8, course9, course10));
        
        // GREATER_THAN operator with number
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=id&operation0=GREATER_THAN&values0=5", Course[].class, List.of(course6, course7, course8, course9, course10));
        
        // GREATER_THAN operator with date
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=startDate&operation0=GREATER_THAN&values0=2015-06-18", Course[].class, List.of(course1, course2, course3, course4, course5, course6, course7, course10));
        
        // GREATER_THAN_OR_EQUAL operator
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=id&operation0=GREATER_THAN_OR_EQUAL&values0=8", Course[].class, List.of(course8, course9, course10));
        
        // GREATER_THAN_OR_EQUAL operator with date
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=startDate&operation0=GREATER_THAN_OR_EQUAL&values0=2019-06-18", Course[].class, List.of(course5, course6, course7, course10));
        
        // LESS_THAN operator
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=maxStudentCount&operation0=LESS_THAN&values0=40", Course[].class, List.of(course7, course8, course10));
        
        // LESS_THAN_OR_EQUAL operator
        printRequestedResultAndAssert(COURSE_CRITERIA_API_URL,
                "key0=maxStudentCount&operation0=LESS_THAN_OR_EQUAL&values0=40", Course[].class, List.of(course6, course7, course8, course10));
    }

    /**
     * Test DynamicQuery argument resolver with complex query
     * 
     * Verifies that:
     * - Field selection with aliases works (select/selectAs parameters)
     * - Nested field filtering works
     * - OrderBy works with nested fields
     * - Pagination works
     * 
     * This test uses deep nested paths:
     * AdminUser -> roles[] -> roleAuthorizations[] -> authorization -> {id, name, menuIcon}
     */
    @Test
    public void argumentSearchQueryTests() throws Exception {
        printRequestedResultAndAssert(COURSE_SEARCH_LIST_API_URL,
                "select0=id&select1=username&select2=roles.id&select3=roles.name&select4=roles.roleAuthorizations.authorization.id&select5=roles.roleAuthorizations.authorization.name&select6=roles.roleAuthorizations.authorization.menuIcon&" +
                "selectAs0=adminId&selectAs1=adminUsername&selectAs2=roleId&selectAs3=roleName&selectAs4=authorizationId&selectAs5=authorizationName&selectAs6=menuIcon&" +
                        "orderBy0=roles.id&orderByDirection0=desc&" +
                        "page=1&" +
                        "pageSize=2&" +
                        "key0=roles.roleAuthorizations.authorization.menuIcon&operation0=START_WITH&values0=icon", 
                S9_Query_Builder.AuthorizationSummary[].class, 
                List.of(
                    new S9_Query_Builder.AuthorizationSummary(3L, "admin3", 3L, "role3", 3L, "auth3", "icon3"), 
                    new S9_Query_Builder.AuthorizationSummary(2L, "admin2", 2L, "role2", 2L, "auth2", "icon2")
                ));
    }

    /**
     * Helper method to execute HTTP request and assert result
     */
    private <T> void printRequestedResultAndAssert(String apiUrl, String filter, Class<T[]> clazz, Object result) throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(apiUrl + "?" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        PresentationUtil.prettyPrint(TestUtil.getResultListValue(mvcResult.getResponse().getContentAsString(), clazz));

        assertEquals(result, TestUtil.getResultListValue(mvcResult.getResponse().getContentAsString(), clazz));
    }
}

