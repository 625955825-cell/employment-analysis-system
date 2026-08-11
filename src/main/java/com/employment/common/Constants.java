package com.employment.common;

public class Constants {
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_CLASS_TEACHER = "class_teacher";
    public static final String ROLE_DEPT_TEACHER = "dept_teacher";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_COMPANY = "company";
    public static final String ROLE_DATA_ANALYST = "employment_staff";

    public static final String STATUS_NORMAL = "0";
    public static final String STATUS_DISABLED = "1";

    public static final String AUDIT_PENDING = "pending";
    public static final String AUDIT_APPROVED = "approved";
    public static final String AUDIT_REJECTED = "rejected";

    public static final String EMPLOYMENT_TYPE_EMPLOYED = "签订劳动合同";
    public static final String EMPLOYMENT_TYPE_THREE_PARTY = "签订三方协议";
    public static final String EMPLOYMENT_TYPE_SELF_EMPLOYED = "自主创业";
    public static final String EMPLOYMENT_TYPE_ABROAD = "出国出境";
    public static final String EMPLOYMENT_TYPE_MILITARY = "应征入伍";
    public static final String EMPLOYMENT_TYPE_UNEMPLOYED = "暂未就业";
    public static final String EMPLOYMENT_TYPE_GRADUATE_SCHOOL = "继续深造";
    public static final String EMPLOYMENT_TYPE_FREELANCE = "自由职业";
    public static final String EMPLOYMENT_TYPE_OTHER = "其他";

    // 日志类型
    public static final String LOG_TYPE_LOGIN = "login";       // 登录日志
    public static final String LOG_TYPE_LOGOUT = "logout";     // 登出日志
    public static final String LOG_TYPE_OPERATION = "operation"; // 操作日志
}
