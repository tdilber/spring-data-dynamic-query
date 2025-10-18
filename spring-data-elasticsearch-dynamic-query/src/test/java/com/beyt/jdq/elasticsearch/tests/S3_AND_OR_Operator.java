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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Elasticsearch tests for AND and OR operators.
 * Tests combining multiple criteria with AND (default) and OR operations.
 */
@SpringBootTest(classes = ElasticsearchTestApplication.class)
@ActiveProfiles("estest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Disabled("TODO: Enable after previous tests pass")
public class S3_AND_OR_Operator extends BaseElasticsearchTestInstance {

    @Test
    public void and() {
        var criteriaList = CriteriaList.of(
            Criteria.of("name", CriteriaOperator.CONTAIN, "II"),
            Criteria.of("id", CriteriaOperator.GREATER_THAN, 5)
        );
        List<Course> courseList = courseRepository.findAll(criteriaList);
        System.out.println("AND: name contains 'II' AND id > 5: " + courseList);
        
        // Courses 7 (Chemistry II) and 9 (Biology II) have "II" and id > 5
        assertEquals(List.of(course7, course9), courseList);
    }

    @Test
    public void and2() {
        var criteriaList = CriteriaList.of(
            Criteria.of("name", CriteriaOperator.CONTAIN, "II"),
            Criteria.of("id", CriteriaOperator.EQUAL, 7, 8, 9, 10),
            Criteria.of("active", CriteriaOperator.SPECIFIED, false)
        );
        List<Course> courseList = courseRepository.findAll(criteriaList);
        System.out.println("AND2: name contains 'II' AND id IN (7,8,9,10) AND active is null: " + courseList);
        
        // Only course 7 (Chemistry II) meets all criteria: contains "II", id=7, active=null
        assertEquals(List.of(course7), courseList);
    }

    @Test
    public void or() {
        var criteriaList = CriteriaList.of(
            Criteria.of("name", CriteriaOperator.CONTAIN, "II"),
            Criteria.of("id", CriteriaOperator.EQUAL, 7, 8, 9, 10),
            Criteria.of("active", CriteriaOperator.SPECIFIED, false),
            Criteria.OR(),
            Criteria.of("id", CriteriaOperator.EQUAL, 1, 2, 3, 4, 5),
            Criteria.of("id", CriteriaOperator.LESS_THAN, 3)
        );
        List<Course> courseList = courseRepository.findAll(criteriaList);
        System.out.println("OR: (name='II' AND id IN (7-10) AND active=null) OR (id IN (1-5) AND id<3): " + courseList);
        
        // Left side: course7 (matches all three conditions)
        // Right side: courses 1, 2 (id IN (1-5) AND id < 3)
        assertEquals(List.of(course1, course2, course7), courseList);
    }

    @Test
    public void or2() {
        var criteriaList = CriteriaList.of(
            Criteria.of("name", CriteriaOperator.CONTAIN, "II"),
            Criteria.of("id", CriteriaOperator.EQUAL, 7, 8, 9, 10),
            Criteria.of("active", CriteriaOperator.SPECIFIED, false),
            Criteria.OR(),
            Criteria.of("id", CriteriaOperator.EQUAL, 1, 2, 3, 4, 5),
            Criteria.OR(),
            Criteria.of("id", CriteriaOperator.LESS_THAN, 3)
        );
        List<Course> courseList = courseRepository.findAll(criteriaList);
        System.out.println("OR2: (name='II' AND id IN (7-10) AND active=null) OR (id IN (1-5)) OR (id<3): " + courseList);
        
        // First group: course7
        // Second group: courses 1,2,3,4,5
        // Third group: courses 1,2 (but already in second group)
        // Note: Elasticsearch doesn't guarantee order without explicit sorting, so we compare as sets
        var expected = List.of(course1, course2, course3, course4, course5, course7);
        assertEquals(expected.size(), courseList.size());
        assertEquals(expected.stream().sorted((a, b) -> a.getId().compareTo(b.getId())).toList(),
                     courseList.stream().sorted((a, b) -> a.getId().compareTo(b.getId())).toList());
    }

    @Test
    public void complexAnd() {
        var criteriaList = CriteriaList.of(
            Criteria.of("maxStudentCount", CriteriaOperator.GREATER_THAN, 30),
            Criteria.of("maxStudentCount", CriteriaOperator.LESS_THAN, 100),
            Criteria.of("active", CriteriaOperator.SPECIFIED, "true")
        );
        List<Course> courseList = courseRepository.findAll(criteriaList);
        System.out.println("Complex AND: maxStudentCount between 30-100 AND active is not null: " + courseList);
        
        // course1 (50, true), course2 (60, true), course9 (54, true)
        assertEquals(List.of(course1, course2, course9), courseList);
    }

    @Test
    public void simpleOr() {
        var criteriaList = CriteriaList.of(
            Criteria.of("id", CriteriaOperator.EQUAL, 1),
            Criteria.OR(),
            Criteria.of("id", CriteriaOperator.EQUAL, 10)
        );
        List<Course> courseList = courseRepository.findAll(criteriaList);
        System.out.println("Simple OR: id=1 OR id=10: " + courseList);
        
        assertEquals(List.of(course1, course10), courseList);
    }
}

