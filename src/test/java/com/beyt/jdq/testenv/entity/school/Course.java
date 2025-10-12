package com.beyt.jdq.testenv.entity.school;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "course")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Timestamp startDate;

    private Integer maxStudentCount;

    private Boolean active;

    private String description;

    public Course() {
    }

    public Course(Long id, String name, Timestamp startDate, Integer maxStudentCount, Boolean active, String description) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.maxStudentCount = maxStudentCount;
        this.active = active;
        this.description = description;
    }

    public static Course of(String name, String description) {
        return new Course(null, name, null, null, null, description);
    }

    @JsonIgnore
    @ManyToMany(mappedBy = "courses", fetch = FetchType.LAZY)
    private Set<Student> students;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Integer getMaxStudentCount() {
        return maxStudentCount;
    }

    public void setMaxStudentCount(Integer maxStudentCount) {
        this.maxStudentCount = maxStudentCount;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Student> getStudents() {
        return students;
    }

    public void setStudents(Set<Student> students) {
        this.students = students;
    }

    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", startDate=" + startDate +
                ", maxStudentCount=" + maxStudentCount +
                ", active=" + active +
                ", description='" + description + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course course)) return false;
        return Objects.equals(id, course.id) && Objects.equals(name, course.name) && Objects.equals(startDate, course.startDate) && Objects.equals(maxStudentCount, course.maxStudentCount) && Objects.equals(active, course.active) && Objects.equals(description, course.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, startDate, maxStudentCount, active, description);
    }
}
