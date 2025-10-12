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
public class S4_SCOPE_Operator extends BaseTestInstance {
    private @Autowired CourseRepository courseRepository;
    private @Autowired DepartmentRepository departmentRepository;


    //     (A OR B) AND (C OR D)
    @Test
    public void scope() {
        var criteriaList = CriteriaList.of(
                Criteria.of("", CriteriaOperator.PARENTHES,
                        CriteriaList.of(Criteria.of("id", CriteriaOperator.EQUAL, 1),
                                Criteria.OR(),
                                Criteria.of("id", CriteriaOperator.EQUAL, 2))),
                Criteria.of("", CriteriaOperator.PARENTHES,
                        CriteriaList.of(Criteria.of("id", CriteriaOperator.EQUAL, 2),
                                Criteria.OR(),
                                Criteria.of("id", CriteriaOperator.EQUAL, 3)))
        );
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(course2), courseList);
    }


    @Test
    public void scopeInsideScope() {
        var criteriaList = CriteriaList.of(
                Criteria.of("", CriteriaOperator.PARENTHES,
                        CriteriaList.of(Criteria.of("id", CriteriaOperator.EQUAL, 1, 2, 3),
                                Criteria.of("id", CriteriaOperator.NOT_EQUAL, 2),
                        Criteria.OR(),
                        Criteria.of("", CriteriaOperator.PARENTHES,
                                CriteriaList.of(Criteria.of("id", CriteriaOperator.EQUAL, 2),
                                        Criteria.of("id", CriteriaOperator.NOT_EQUAL, 3)))))
        );
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);
        assertEquals(List.of(course1, course2, course3), courseList);
    }
}
