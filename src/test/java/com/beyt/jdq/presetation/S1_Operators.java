package com.beyt.jdq.presetation;

import com.beyt.jdq.BaseTestInstance;
import com.beyt.jdq.TestApplication;
import com.beyt.jdq.dto.Criteria;
import com.beyt.jdq.dto.CriteriaList;
import com.beyt.jdq.dto.DynamicQuery;
import com.beyt.jdq.dto.enums.CriteriaOperator;
import com.beyt.jdq.testenv.entity.User;
import com.beyt.jdq.testenv.entity.school.Course;
import com.beyt.jdq.testenv.repository.CourseRepository;
import com.beyt.jdq.testenv.repository.DepartmentRepository;
import com.beyt.jdq.util.PresentationUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(classes = TestApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class S1_Operators extends BaseTestInstance {
    private @Autowired CourseRepository courseRepository;
    private @Autowired DepartmentRepository departmentRepository;


//CONTAIN
//DOES_NOT_CONTAIN
//END_WITH
//START_WITH
//SPECIFIED
//EQUAL
//NOT_EQUAL
//GREATER_THAN
//GREATER_THAN_OR_EQUAL
//LESS_THAN
//LESS_THAN_OR_EQUAL


    @PostConstruct
    public void postConstructTest() {
        assertDoesNotThrow(() -> courseRepository.findAll(CriteriaList.of(Criteria.of("id", CriteriaOperator.GREATER_THAN, 0))));
    }

    @Test
    public void contain() {
        var criteriaList = CriteriaList.of(Criteria.of("name", CriteriaOperator.CONTAIN, "Calculus"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);

        assertEquals(new ArrayList<>(List.of(course2, course3)), courseList);
    }

    @Test
    public void doesNotContain() {
        var criteriaList = CriteriaList.of(Criteria.of("name", CriteriaOperator.DOES_NOT_CONTAIN, "I"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(), courseList);
    }

    @Test
    public void endWith() {
        var criteriaList = CriteriaList.of(Criteria.of("name", CriteriaOperator.END_WITH, "Science"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(course1), courseList);
    }

    @Test
    public void startWith() {
        var criteriaList = CriteriaList.of(Criteria.of("name", CriteriaOperator.START_WITH, "Physics"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);
       assertEquals(List.of(course4, course5), courseList);
    }

    @Test
    public void specifiedTrue() {
        var criteriaList = CriteriaList.of(Criteria.of("active", CriteriaOperator.SPECIFIED, "true"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(course1, course2, course8, course9, course10), courseList);
    }



    @Test
    public void specifiedFalse() {
        var criteriaList = CriteriaList.of(Criteria.of("active", CriteriaOperator.SPECIFIED, "false"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(course3, course4, course5, course6, course7), courseList);
    }

    @Test
    public void equal() {
        var criteriaList = CriteriaList.of(Criteria.of("name", CriteriaOperator.EQUAL, "Calculus I"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(course2), courseList);
    }

    @SneakyThrows
    @Test
    public void equalDate() {
        var criteriaList = CriteriaList.of(Criteria.of("startDate", CriteriaOperator.EQUAL, "2015-06-18"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(), courseList);
    }

    @SneakyThrows
    @Test
    public void equalInteger() {
        var criteriaList = CriteriaList.of(Criteria.of("maxStudentCount", CriteriaOperator.EQUAL, 54));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);//9
        assertEquals(List.of(course9), courseList);
    }

    @Test
    public void notEqual() {
        var criteriaList = CriteriaList.of(Criteria.of("name", CriteriaOperator.NOT_EQUAL, "Introduction to Computer Science"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList); //2,3,4,5,6,7,8,9,10
        assertEquals(List.of(course2, course3, course4, course5, course6, course7, course8, course9, course10), courseList);
    }

    @Test
    public void greaterThan() {
        var criteriaList = CriteriaList.of(Criteria.of("id", CriteriaOperator.GREATER_THAN, 5));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(course6, course7, course8, course9, course10), courseList);
    }

    @Test
    public void greaterThanDate() {
        var criteriaList = CriteriaList.of(Criteria.of("startDate", CriteriaOperator.GREATER_THAN, "2015-06-18"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList); //1,2,3,4,5,6,7,10
        assertEquals(List.of(course1, course2, course3, course4, course5, course6, course7, course10), courseList);
    }

    @Test
    public void greaterThanOrEqual() {
        var criteriaList = CriteriaList.of(Criteria.of("id", CriteriaOperator.GREATER_THAN_OR_EQUAL, 8));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);//8,9,10
        assertEquals(List.of(course8, course9, course10), courseList);
    }

    @Test
    public void greaterThanOrEqualDate() {
        var criteriaList = CriteriaList.of(Criteria.of("startDate", CriteriaOperator.GREATER_THAN_OR_EQUAL, "2019-06-18"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);//5.6.7.10
        assertEquals(List.of(course5, course6, course7, course10), courseList);
    }

    @Test
    public void lessThan() {
        var criteriaList = CriteriaList.of(Criteria.of(Course.Fields.maxStudentCount, CriteriaOperator.LESS_THAN, 40));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(course7, course8, course10), courseList);
    }

    @Test
    public void lessThanOrEqual() {
        var criteriaList = CriteriaList.of(Criteria.of(Course.Fields.maxStudentCount, CriteriaOperator.LESS_THAN_OR_EQUAL, 40));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList); // 6,7,8,10
        assertEquals(List.of(course6, course7, course8, course10), courseList);
    }
}
