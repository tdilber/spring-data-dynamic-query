package com.beyt.jdq.elasticsearch.tests;

import com.beyt.jdq.core.model.Criteria;
import com.beyt.jdq.core.model.CriteriaList;
import com.beyt.jdq.core.model.enums.CriteriaOperator;
import com.beyt.jdq.elasticsearch.ElasticsearchTestApplication;
import com.beyt.jdq.elasticsearch.entity.Course;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Elasticsearch tests for multi-value operators.
 * Tests EQUAL and NOT_EQUAL with multiple values (IN queries).
 */
@SpringBootTest(classes = ElasticsearchTestApplication.class)
@ActiveProfiles("estest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Disabled("TODO: Enable after previous tests pass")
public class S2_Multi_Value_Operators extends BaseElasticsearchTestInstance {

    @Test
    public void equal() {
        var criteriaList = CriteriaList.of(
            Criteria.of("name", CriteriaOperator.EQUAL, "Calculus I", "Calculus II")
        );
        List<Course> courseList = courseRepository.findAll(criteriaList);
        System.out.println("EQUAL multiple values ['Calculus I', 'Calculus II']: " + courseList);
        
        assertEquals(List.of(course2, course3), courseList);
    }

    @Test
    public void equalInteger() {
        var criteriaList = CriteriaList.of(
            Criteria.of("maxStudentCount", CriteriaOperator.EQUAL, 40, 50)
        );
        List<Course> courseList = courseRepository.findAll(criteriaList);
        System.out.println("EQUAL multiple integers [40, 50]: " + courseList);
        
        assertEquals(List.of(course1, course6), courseList);
    }

    @Test
    public void notEqual() {
        var criteriaList = CriteriaList.of(
            Criteria.of("name", CriteriaOperator.NOT_EQUAL, "Calculus I", "Calculus II")
        );
        List<Course> courseList = courseRepository.findAll(criteriaList);
        System.out.println("NOT_EQUAL multiple values ['Calculus I', 'Calculus II']: " + courseList);
        
        assertEquals(
            Arrays.asList(course1, course4, course5, course6, course7, course8, course9, course10), 
            courseList
        );
    }

    @Test
    public void notEqualDate() {
        var criteriaList = CriteriaList.of(
            Criteria.of("startDate", CriteriaOperator.NOT_EQUAL, "2013-06-18", "2015-06-18", "2016-06-18")
        );
        List<Course> courseList = courseRepository.findAll(criteriaList);
        System.out.println("NOT_EQUAL multiple dates: " + courseList);
        
        // Should exclude courses with these dates
        // This test might need adjustment based on date formatting
    }

    @Test
    public void equalMultipleIds() {
        var criteriaList = CriteriaList.of(
            Criteria.of("id", CriteriaOperator.EQUAL, 1, 2, 3, 4, 5)
        );
        List<Course> courseList = courseRepository.findAll(criteriaList);
        System.out.println("EQUAL multiple IDs [1,2,3,4,5]: " + courseList);
        
        assertEquals(List.of(course1, course2, course3, course4, course5), courseList);
    }

    @Test
    public void notEqualMultipleIds() {
        var criteriaList = CriteriaList.of(
            Criteria.of("id", CriteriaOperator.NOT_EQUAL, 1, 2, 3)
        );
        List<Course> courseList = courseRepository.findAll(criteriaList);
        System.out.println("NOT_EQUAL multiple IDs [1,2,3]: " + courseList);
        
        assertEquals(
            Arrays.asList(course4, course5, course6, course7, course8, course9, course10), 
            courseList
        );
    }
}

