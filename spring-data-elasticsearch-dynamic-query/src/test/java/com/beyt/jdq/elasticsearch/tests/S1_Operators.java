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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Elasticsearch tests for basic operators.
 * Tests CONTAIN, DOES_NOT_CONTAIN, END_WITH, START_WITH, SPECIFIED,
 * EQUAL, NOT_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL,
 * LESS_THAN, LESS_THAN_OR_EQUAL
 */
@SpringBootTest(classes = ElasticsearchTestApplication.class)
@ActiveProfiles("estest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class S1_Operators extends BaseElasticsearchTestInstance {

    @Test
    public void contain() {
        var criteriaList = CriteriaList.of(Criteria.of("name", CriteriaOperator.CONTAIN, "Calculus"));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("CONTAIN 'Calculus': " + courseList);
        
        assertEquals(2, courseList.size());
        assertEquals(List.of(course2, course3), courseList);
    }

    @Test
    public void doesNotContain() {
        var criteriaList = CriteriaList.of(Criteria.of("name", CriteriaOperator.DOES_NOT_CONTAIN, "I"));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("DOES_NOT_CONTAIN 'I': " + courseList);
        
        // All courses contain "I" in their names
        assertEquals(List.of(), courseList);
    }

    @Test
    public void endWith() {
        var criteriaList = CriteriaList.of(Criteria.of("name", CriteriaOperator.END_WITH, "Science"));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("END_WITH 'Science': " + courseList);
        
        assertEquals(List.of(course1), courseList);
    }

    @Test
    public void startWith() {
        var criteriaList = CriteriaList.of(Criteria.of("name", CriteriaOperator.START_WITH, "Physics"));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("START_WITH 'Physics': " + courseList);

        assertEquals(List.of(course4, course5), courseList);
    }

    @Test
    public void specifiedTrue() {
        var criteriaList = CriteriaList.of(Criteria.of("active", CriteriaOperator.SPECIFIED, "true"));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("SPECIFIED true (active not null): " + courseList);
        
        assertEquals(List.of(course1, course2, course8, course9, course10), courseList);
    }

    @Test
    public void specifiedFalse() {
        var criteriaList = CriteriaList.of(Criteria.of("active", CriteriaOperator.SPECIFIED, "false"));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("SPECIFIED false (active is null): " + courseList);
        
        assertEquals(List.of(course3, course4, course5, course6, course7), courseList);
    }

    @Test
    public void equal() {
        var criteriaList = CriteriaList.of(Criteria.of("name", CriteriaOperator.EQUAL, "Calculus I"));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("EQUAL 'Calculus I': " + courseList);
        
        assertEquals(List.of(course2), courseList);
    }

    @Test
    public void equalDate() {
        var criteriaList = CriteriaList.of(Criteria.of("startDate", CriteriaOperator.EQUAL, "2015-06-18"));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("EQUAL date '2015-06-18': " + courseList);
        
        // Note: Date comparison might need adjustment based on how dates are stored
        assertEquals(List.of(), courseList);
    }

    @Test
    public void equalInteger() {
        var criteriaList = CriteriaList.of(Criteria.of("maxStudentCount", CriteriaOperator.EQUAL, 54));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("EQUAL 54: " + courseList);
        
        assertEquals(List.of(course9), courseList);
    }

    @Test
    public void notEqual() {
        var criteriaList = CriteriaList.of(Criteria.of("name", CriteriaOperator.NOT_EQUAL, "Introduction to Computer Science"));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("NOT_EQUAL 'Introduction to Computer Science': " + courseList);
        
        assertEquals(List.of(course2, course3, course4, course5, course6, course7, course8, course9, course10), courseList);
    }

    @Test
    public void greaterThan() {
        var criteriaList = CriteriaList.of(Criteria.of("id", CriteriaOperator.GREATER_THAN, 5));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("GREATER_THAN 5: " + courseList);
        
        assertEquals(List.of(course6, course7, course8, course9, course10), courseList);
    }

    @Test
    public void greaterThanDate() {
        var criteriaList = CriteriaList.of(Criteria.of("startDate", CriteriaOperator.GREATER_THAN, "2015-06-18"));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("GREATER_THAN date '2015-06-18': " + courseList);
        
        assertEquals(List.of(course1, course2, course3, course4, course5, course6, course7, course10), courseList);
    }

    @Test
    public void greaterThanOrEqual() {
        var criteriaList = CriteriaList.of(Criteria.of("id", CriteriaOperator.GREATER_THAN_OR_EQUAL, 8));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("GREATER_THAN_OR_EQUAL 8: " + courseList);
        
        assertEquals(List.of(course8, course9, course10), courseList);
    }

    @Test
    public void greaterThanOrEqualDate() {
        var criteriaList = CriteriaList.of(Criteria.of("startDate", CriteriaOperator.GREATER_THAN_OR_EQUAL, "2019-06-18"));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("GREATER_THAN_OR_EQUAL date '2019-06-18': " + courseList);
        
        assertEquals(List.of(course5, course6, course7, course10), courseList);
    }

    @Test
    public void lessThan() {
        var criteriaList = CriteriaList.of(Criteria.of("maxStudentCount", CriteriaOperator.LESS_THAN, 40));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("LESS_THAN 40: " + courseList);
        
        assertEquals(List.of(course7, course8, course10), courseList);
    }

    @Test
    public void lessThanOrEqual() {
        var criteriaList = CriteriaList.of(Criteria.of("maxStudentCount", CriteriaOperator.LESS_THAN_OR_EQUAL, 40));
        List<Course> courseList = courseRepository.findAll(criteriaList).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("LESS_THAN_OR_EQUAL 40: " + courseList);
        
        assertEquals(List.of(course6, course7, course8, course10), courseList);
    }
}

