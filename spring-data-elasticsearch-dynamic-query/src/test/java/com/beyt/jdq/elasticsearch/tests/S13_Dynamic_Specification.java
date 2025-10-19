package com.beyt.jdq.elasticsearch.tests;

import com.beyt.jdq.core.helper.QueryHelper;
import com.beyt.jdq.core.model.enums.JoinType;
import com.beyt.jdq.elasticsearch.ElasticsearchTestApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Elasticsearch tests for DynamicSpecification utility methods.
 * Tests field name extraction and join path parsing.
 */
@SpringBootTest(classes = ElasticsearchTestApplication.class)
@ActiveProfiles("estest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class S13_Dynamic_Specification extends BaseElasticsearchTestInstance {

    @Test
    void getFieldName() {
        Assertions.assertEquals("name", QueryHelper.getFieldName("asd.asd<asd>name"));
    }

    @Test
    void getFieldJoins() {
        List<Pair<String, JoinType>> fieldJoins = new ArrayList<>();
        fieldJoins.add(Pair.of("asd2fg", JoinType.INNER));
        fieldJoins.add(Pair.of("asdhj", JoinType.LEFT));
        fieldJoins.add(Pair.of("asdasda", JoinType.RIGHT));

        assertEquals(fieldJoins, QueryHelper.getFieldJoins("asd2fg.asdhj<asdasda>name"));
    }
}

