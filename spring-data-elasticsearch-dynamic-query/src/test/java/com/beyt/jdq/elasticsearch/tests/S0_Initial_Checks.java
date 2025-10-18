package com.beyt.jdq.elasticsearch.tests;

import com.beyt.jdq.core.model.Criteria;
import com.beyt.jdq.core.model.CriteriaList;
import com.beyt.jdq.core.model.enums.CriteriaOperator;
import com.beyt.jdq.elasticsearch.ElasticsearchTestApplication;
import com.beyt.jdq.elasticsearch.entity.Course;
import com.beyt.jdq.elasticsearch.util.TestUtil;
import org.apache.commons.collections4.IterableUtils;
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
public class S0_Initial_Checks extends BaseElasticsearchJoinTestInstance {

    @Test
    public void contain() {
        var courseList = IterableUtils.toList(courseRepository.findAll()).stream().sorted(Comparator.comparing(Course::getId)).toList();
        System.out.println("CONTAIN 'Calculus': " + courseList);
        
        assertEquals(10, courseList.size());
        assertEquals(TestUtil.toList(course1,course2,course3,course4,course5,course6,course7,course8,course9,course10), courseList);
    }
}

