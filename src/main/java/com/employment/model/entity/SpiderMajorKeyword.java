package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "spider_major_keyword",
    uniqueConstraints = @UniqueConstraint(columnNames = {"major_name", "keyword"}),
    indexes = {
        @Index(name = "idx_major_name", columnList = "major_name")
    }
)
public class SpiderMajorKeyword extends BaseEntity {

    @Column(name = "major_name", nullable = false, length = 100)
    private String majorName;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;
}
