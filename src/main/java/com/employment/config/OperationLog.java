package com.employment.config;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** 操作模块，如"注册码管理"、"就业登记" */
    String module() default "";

    /** 操作描述，如"生成注册码"、"删除院系" */
    String content() default "";
}
