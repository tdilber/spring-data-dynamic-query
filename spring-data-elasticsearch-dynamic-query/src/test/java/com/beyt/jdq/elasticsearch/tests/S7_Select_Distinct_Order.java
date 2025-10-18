package com.beyt.jdq.elasticsearch.tests;

import com.beyt.jdq.core.model.Criteria;
import com.beyt.jdq.core.model.CriteriaList;
import com.beyt.jdq.core.model.DynamicQuery;
import com.beyt.jdq.core.model.enums.CriteriaOperator;
import com.beyt.jdq.core.model.enums.Order;
import com.beyt.jdq.elasticsearch.ElasticsearchTestApplication;
import com.beyt.jdq.elasticsearch.entity.Course;
import com.beyt.jdq.elasticsearch.entity.Student;
import com.beyt.jdq.elasticsearch.util.PresentationUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Elasticsearch tests for SELECT, DISTINCT, and ORDER BY operations.
 * 
 * Tests projection capabilities, distinct filtering, ordering, and pagination
 * using Elasticsearch collections.
 */
@SpringBootTest(classes = ElasticsearchTestApplication.class)
@ActiveProfiles("estest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Disabled("TODO: Enable after previous tests pass")
public class S7_Select_Distinct_Order extends BaseElasticsearchJoinTestInstance {

    @Test
    public void selectSameObject() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.getSelect().add(Pair.of("name", "name"));
        dynamicQuery.getSelect().add(Pair.of("description", "description"));
        dynamicQuery.setWhere(CriteriaList.of(Criteria.of("id", CriteriaOperator.GREATER_THAN, 8)));
        PresentationUtil.prettyPrint(dynamicQuery);
        List<Course> courseList = courseRepository.findAll(dynamicQuery);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(Course.of(course9.getName(), course9.getDescription()), Course.of(course10.getName(), course10.getDescription())), courseList);
    }

    public static class CourseName {
        private String name;

        public CourseName() {
        }

        public CourseName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CourseName that)) return false;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }
    }

    public record CourseNameRecord(String name) {
    }

    @Test
    public void selectDifferentObject() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.getSelect().add(Pair.of("name", "name"));
        dynamicQuery.setWhere(CriteriaList.of(Criteria.of("id", CriteriaOperator.GREATER_THAN, 8)));
        PresentationUtil.prettyPrint(dynamicQuery);
        List<CourseName> courseList = courseRepository.findAll(dynamicQuery, CourseName.class);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(new CourseName(course9.getName()), new CourseName(course10.getName())), courseList);

        List<CourseNameRecord> courseListRecord = courseRepository.findAll(dynamicQuery, CourseNameRecord.class);
        PresentationUtil.prettyPrint(courseListRecord);
        assertEquals(List.of(new CourseNameRecord(course9.getName()), new CourseNameRecord(course10.getName())), courseListRecord);
    }

    @Test
    public void selectDifferentObjectDifferent() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.getSelect().add(Pair.of("description", "name"));
        dynamicQuery.setWhere(CriteriaList.of(Criteria.of("id", CriteriaOperator.GREATER_THAN, 8)));
        PresentationUtil.prettyPrint(dynamicQuery);
        List<CourseName> courseList = courseRepository.findAll(dynamicQuery, CourseName.class);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(new CourseName(course9.getDescription()), new CourseName(course10.getDescription())), courseList);

        List<CourseNameRecord> courseListRecord = courseRepository.findAll(dynamicQuery, CourseNameRecord.class);
        PresentationUtil.prettyPrint(courseListRecord);
        assertEquals(List.of(new CourseNameRecord(course9.getDescription()), new CourseNameRecord(course10.getDescription())), courseListRecord);
    }

    @Test
    public void selectDifferentEntityObject() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.getSelect().add(Pair.of("description", "name"));
        dynamicQuery.setWhere(CriteriaList.of(Criteria.of("id", CriteriaOperator.GREATER_THAN, 8)));
        PresentationUtil.prettyPrint(dynamicQuery);
        List<Student> courseList = courseRepository.findAll(dynamicQuery, Student.class);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(Student.of(course9.getDescription()), Student.of(course10.getDescription())), courseList);
    }

    @Test
    public void distinct() {
        DynamicQuery dynamicQuery = new DynamicQuery();
//        dynamicQuery.setDistinct(true);
        dynamicQuery.setWhere(CriteriaList.of(Criteria.of("courses.id", CriteriaOperator.GREATER_THAN, 1), Criteria.of("id", CriteriaOperator.EQUAL, 2)));
        PresentationUtil.prettyPrint(dynamicQuery);
        List<Student> studentList = studentRepository.findAll(dynamicQuery);
        PresentationUtil.prettyPrint(studentList);
        assertEquals(List.of(student2, student2), studentList);
        dynamicQuery.setDistinct(true);
        studentList = studentRepository.findAll(dynamicQuery);
        assertEquals(List.of(student2), studentList);
    }

    @Test
    public void orderBy() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.setOrderBy(List.of(Pair.of("id", Order.DESC)));
        PresentationUtil.prettyPrint(dynamicQuery);
        List<Course> courseList = courseRepository.findAll(dynamicQuery);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(course10, course9, course8, course7, course6, course5, course4, course3, course2, course1), courseList);
    }


    @Test
    public void orderByMulti() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.setOrderBy(List.of(Pair.of("maxStudentCount", Order.DESC), Pair.of("id", Order.DESC)));
        PresentationUtil.prettyPrint(dynamicQuery);
        List<Course> courseList = courseRepository.findAll(dynamicQuery);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(course5, course4, course3, course2, course9, course1, course6, course7, course8, course10), courseList);
    }

    @Test
    public void page() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.setWhere(CriteriaList.of(Criteria.of("id", CriteriaOperator.GREATER_THAN, 3)));
        dynamicQuery.setPageSize(2);
        dynamicQuery.setPageNumber(1);
        System.out.println("Page");
        Page<Course> allAsPage = courseRepository.findAllAsPage(dynamicQuery);
        PresentationUtil.prettyPrint(courseRepository.findAllAsPage(dynamicQuery));
        assertEquals(2, allAsPage.getContent().size());
        assertEquals(List.of(course6, course7), allAsPage.getContent());


        System.out.println("Page With Projection");
        dynamicQuery.getSelect().add(Pair.of("name", "name"));
        Page<CourseName> courseNames = courseRepository.findAllAsPage(dynamicQuery, CourseName.class);
        PresentationUtil.prettyPrint(courseNames);
        assertEquals(2, courseNames.getContent().size());
        assertEquals(List.of(new CourseName(course6.getName()), new CourseName(course7.getName())), courseNames.getContent());

        System.out.println("Page With Projection Record");
        Page<CourseNameRecord> courseNameRecords = courseRepository.findAllAsPage(dynamicQuery, CourseNameRecord.class);
        PresentationUtil.prettyPrint(courseNameRecords);
        assertEquals(2, courseNameRecords.getContent().size());
        assertEquals(List.of(new CourseNameRecord(course6.getName()), new CourseNameRecord(course7.getName())), courseNameRecords.getContent());
    }
}

