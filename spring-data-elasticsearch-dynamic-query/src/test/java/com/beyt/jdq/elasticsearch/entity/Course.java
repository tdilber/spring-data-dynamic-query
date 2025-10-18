package com.beyt.jdq.elasticsearch.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.Objects;

/**
 * Elasticsearch Course entity for testing.
 * Similar to JPA Course but with Elasticsearch annotations.
 */
@Document(indexName = "courses")
public class Course {
    
    @Id
    private Long id;
    
    @Field(type = FieldType.Keyword)
    private String name;
    
    @Field(type = FieldType.Date)
    private Date startDate;
    
    @Field(type = FieldType.Integer)
    private Integer maxStudentCount;
    
    @Field(type = FieldType.Boolean)
    private Boolean active;
    
    @Field(type = FieldType.Text)
    private String description;

    public Course() {
    }

    public Course(Long id, String name, Date startDate, Integer maxStudentCount, Boolean active, String description) {
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

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
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
        if (!(o instanceof Course)) return false;
        Course course = (Course) o;
        return Objects.equals(id, course.id) && 
               Objects.equals(name, course.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, startDate, maxStudentCount, active, description);
    }
}

