package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "recommend_weight_config")
public class RecommendWeightConfig extends BaseEntity {

    @Column(name = "weight_key", nullable = false, unique = true, length = 50)
    private String weightKey;

    @Column(name = "weight_name", nullable = false, length = 100)
    private String weightName;

    @Column(name = "weight_value", nullable = false)
    private Integer weightValue;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "enabled", nullable = false, length = 5)
    private String enabled = "1";
}
