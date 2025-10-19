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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Elasticsearch tests for ADDITIONAL FEATURES.
 * 
 * Tests special utility features like:
 * - Partial consumption of results (streaming/batching)
 * - Memory-efficient processing of large datasets
 */
@SpringBootTest(classes = ElasticsearchTestApplication.class)
@ActiveProfiles("estest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class S11_Additional_Features extends BaseElasticsearchJoinTestInstance {

    /**
     * Consume all results in batches
     * Useful for processing large datasets without loading everything into memory
     */
    @Test
    public void consumePartially() {
        List<Course> courses = new ArrayList<>();
        courseRepository.consumePartially((courseList) -> {
            courseList.forEach(course -> {
                System.out.println(course.getName());
            });
            System.out.println("Consumed partially");
            courses.addAll(courseList);
        }, 2);

        assertEquals(List.of(course1, course2, course3, course4, course5, course6, course7, course8, course9, course10), courses);
    }

    /**
     * Consume filtered results in batches
     * Applies criteria before batching
     */
    @Test
    public void consumePartially2() {
        List<Course> courses = new ArrayList<>();
        CriteriaList criteriaList = CriteriaList.of(Criteria.of("id", CriteriaOperator.GREATER_THAN, 5));
        courseRepository.consumePartially(criteriaList, (courseList) -> {
            courseList.forEach(course -> {
                System.out.println(course.getName());
            });
            System.out.println("Consumed partially");
            courses.addAll(courseList);
        }, 2);
        assertEquals(List.of(course6, course7, course8, course9, course10), courses);
    }
}

