package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_major")
public class SysMajor extends BaseEntity {

    @Column(name = "major_name", nullable = false, length = 100)
    private String majorName;

    @Column(name = "major_code", unique = true, length = 50)
    private String majorCode;

    @Column(name = "dept_id", nullable = false)
    private Long deptId;

    @Column(name = "degree_type", length = 20)
    private String degreeType;

    @Column(name = "is_top_level", length = 10)
    private String isTopLevel;

    @Column(name = "short_name", length = 50)
    private String shortName;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "recommend_enabled", length = 10)
    private String recommendEnabled = "0";

    /**
     * 模型训练状态: untrained=未训练, trained=已训练
     */
    @Column(name = "model_trained", length = 20)
    private String modelTrained = "untrained";

    /**
     * 最后训练时间
     */
    @Column(name = "last_model_trained_time")
    private LocalDateTime lastModelTrainedTime;

    @Transient
    private String deptName;
}
