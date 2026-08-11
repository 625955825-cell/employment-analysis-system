package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_log", indexes = {
    @Index(name = "idx_log_user_id", columnList = "user_id"),
    @Index(name = "idx_log_type", columnList = "log_type"),
    @Index(name = "idx_log_create_time", columnList = "create_time")
})
public class SysLog extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "log_type", length = 30)
    private String logType;

    @Column(name = "module", length = 100)
    private String module;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "method", length = 20)
    private String method;

    @Column(name = "url", length = 500)
    private String url;

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "params", length = 2000)
    private String params;

    @Column(name = "status", length = 10)
    private String status = "0";

    @Column(name = "error_msg", length = 1000)
    private String errorMsg;

    @Column(name = "cost_time")
    private Long costTime;
}
