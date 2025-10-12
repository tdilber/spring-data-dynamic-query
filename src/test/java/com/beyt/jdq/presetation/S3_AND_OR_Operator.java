package com.beyt.jdq.presetation;

import com.beyt.jdq.BaseTestInstance;
import com.beyt.jdq.TestApplication;
import com.beyt.jdq.dto.Criteria;
import com.beyt.jdq.dto.CriteriaList;
import com.beyt.jdq.dto.enums.CriteriaOperator;
import com.beyt.jdq.testenv.entity.school.Course;
import com.beyt.jdq.testenv.repository.CourseRepository;
import com.beyt.jdq.testenv.repository.DepartmentRepository;
import com.beyt.jdq.util.PresentationUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = TestApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class S3_AND_OR_Operator extends BaseTestInstance {
    private @Autowired CourseRepository courseRepository;
    private @Autowired DepartmentRepository departmentRepository;


    @Test
    public void and() {
        var criteriaList = CriteriaList.of(
                Criteria.of("name", CriteriaOperator.CONTAIN, "II"),
                Criteria.of("id", CriteriaOperator.GREATER_THAN, 5));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList); // 7 , 9
        assertEquals(List.of(course7, course9), courseList);
    }


    @Test
    public void and2() {
        var criteriaList = CriteriaList.of(
                Criteria.of("name", CriteriaOperator.CONTAIN, "II"),
                Criteria.of("id", CriteriaOperator.EQUAL, 7, 8, 9, 10),
                Criteria.of("active", CriteriaOperator.SPECIFIED, false));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList); // 7
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
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);//1,2,7
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
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);//1,2,3,4,5,7
        assertEquals(List.of(course1, course2, course3, course4, course5, course7), courseList);
    }
}
