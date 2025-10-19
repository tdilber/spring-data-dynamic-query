package com.beyt.jdq.elasticsearch.entity;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Objects;

/**
 * Simplified Student information for storage in Department.
 * Avoids circular reference by not including department field.
 */
public class StudentInfo {
    
    @Field(type = FieldType.Long)
    private Long id;
    
    @Field(type = FieldType.Text)
    private String name;

    public StudentInfo() {
    }

    public StudentInfo(Long id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public static StudentInfo from(Student student) {
        if (student == null) {
            return null;
        }
        return new StudentInfo(student.getId(), student.getName());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentInfo)) return false;
        StudentInfo that = (StudentInfo) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "StudentInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}

