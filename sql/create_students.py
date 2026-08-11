# -*- coding: utf-8 -*-
"""
批量创建学生账号脚本 v3（与数据库表结构完全匹配）

用法：在 PyCharm 中直接运行此脚本即可

功能：
  - 为每个班级创建约 40 名学生（可配置 STUDENTS_PER_CLASS）
  - 每学生创建：sys_user → sys_user_role → student_info → student_resume
  - 已毕业届（2023/2024届）额外创建：
      job_application → interview_invitation → offer_letter → employment_record
    （企业、职位均来自数据库中已录入的真实数据）
  - 每学生账号：用户名 stu_XXXXX / 密码 123456
  - 2025/2026 届的学生仅建档，不生成就业数据（在读状态）

前置要求（按顺序执行）：
  1. 先在系统中添加院系、专业、班级
  2. 先在系统中录入企业（审核通过）和职位（已发布状态）
  3. 再运行本脚本生成学生数据

修复说明（相比旧版本）：
  - student_id 在 sys_user 插入后通过 LAST_INSERT_ID() 获取
  - job_application/interview/offer 全部使用真实 company_id + student_id 关联
  - employment_record 字段与 entity 类完全一致
"""

import pymysql
import bcrypt
import random
import time

from db_config import DB_CONFIG

# =========================== 可配置参数 ===========================
STUDENTS_PER_CLASS = 40    # 每班学生数量
SKIP_CLASSES = 0            # 跳过的班级数量（设为 0 则从头开始）
BATCH_SIZE = 50             # 内存友好的批次大小
RANDOM_SEED = 42            # 随机种子（固定保证每次运行结果一致，设为 None 则每次不同）
# =========================== 可配置参数 ===========================

# -------------------------- 姓名库 --------------------------
XING = ["王", "李", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴",
        "徐", "孙", "胡", "朱", "高", "林", "何", "郭", "马", "罗",
        "梁", "宋", "郑", "谢", "韩", "唐", "冯", "于", "董", "萧",
        "程", "曹", "袁", "邓", "许", "傅", "沈", "曾", "彭", "吕",
        "苏", "卢", "蒋", "蔡", "贾", "丁", "魏", "薛", "叶", "阎",
        "余", "潘", "杜", "戴", "夏", "钟", "汪", "田", "任", "姜",
        "范", "方", "石", "姚", "谭", "廖", "邹", "熊", "金", "陆",
        "郝", "孔", "白", "崔", "康", "毛", "邱", "秦", "江", "史"]

MING_MALE = ["浩", "宇", "博", "霖", "浩然", "俊杰", "子轩", "子涵", "晨曦", "明辉",
             "伟", "强", "杰", "涛", "超", "勇", "鹏", "飞", "龙", "凯",
             "文", "华", "磊", "波", "峰", "刚", "平", "辉", "军", "洋",
             "浩宇", "浩然", "梓轩", "子轩", "宇航", "宇轩", "志远", "志强", "志明"]

MING_FEMALE = ["欣怡", "思雨", "雨萱", "思琪", "欣悦", "雨彤", "思涵", "诗涵", "雅婷",
               "雅静", "雅欣", "雅楠", "欣", "怡", "婷", "琳", "敏",
               "静", "丽", "艳", "娟", "芳", "兰", "洁", "佳", "嘉", "慧",
               "文静", "诗雨", "欣怡", "欣悦", "欣雨", "思雨", "雨欣", "雨洁"]

# -------------------------- 籍贯 --------------------------
PROVINCES_WEIGHTED = [
    ("湖北省", 25), ("湖南省", 17), ("河南省", 15), ("广东省", 13),
    ("江西省", 11), ("安徽省", 9), ("四川省", 4), ("山东省", 2),
    ("江苏省", 1), ("浙江省", 1), ("福建省", 1), ("其他", 1)
]
PROVINCE_CITIES = {
    "湖北省": ["武汉市", "宜昌市", "襄阳市", "荆州市", "黄石市", "十堰市", "孝感市", "黄冈市", "咸宁市"],
    "湖南省": ["长沙市", "株洲市", "湘潭市", "衡阳市", "岳阳市", "常德市", "益阳市", "郴州市", "永州市"],
    "河南省": ["郑州市", "洛阳市", "开封市", "新乡市", "南阳市", "许昌市", "安阳市", "平顶山市", "商丘市"],
    "广东省": ["广州市", "深圳市", "东莞市", "佛山市", "珠海市", "中山市", "惠州市", "江门市", "湛江市"],
    "江西省": ["南昌市", "赣州市", "九江市", "上饶市", "宜春市", "吉安市", "抚州市", "景德镇市", "萍乡市"],
    "安徽省": ["合肥市", "芜湖市", "蚌埠市", "淮南市", "马鞍山市", "安庆市", "阜阳市", "滁州市", "六安市"],
    "四川省": ["成都市", "绵阳市", "德阳市", "宜宾市", "南充市", "泸州市", "达州市", "乐山市", "内江市"],
    "山东省": ["济南市", "青岛市", "烟台市", "潍坊市", "临沂市", "淄博市", "威海市", "济宁市", "泰安市"],
    "江苏省": ["南京市", "苏州市", "无锡市", "常州市", "南通市", "徐州市", "盐城市", "淮安市", "扬州市"],
    "浙江省": ["杭州市", "宁波市", "温州市", "嘉兴市", "湖州市", "绍兴市", "金华市", "衢州市", "台州市"],
    "福建省": ["福州市", "厦门市", "泉州市", "漳州市", "莆田市", "宁德市", "龙岩市", "三明市", "南平市"],
}
NATIONS = ["汉族", "满族", "回族", "蒙古族", "土家族", "苗族", "维吾尔族", "壮族", "布依族", "朝鲜族"]
POLITICS = ["共青团员", "中共预备党员", "中共党员", "群众"]
STUDY_TYPES = ["全日制本科"]

# 专业 → 期望岗位
MAJOR_POSITIONS = {
    "机械设计制造及其自动化": ["机械设计工程师", "机械工艺工程师", "制造工程师", "CAD工程师", "工艺装备工程师"],
    "机械电子工程": ["机械设计工程师", "嵌入式软件工程师", "自动化工程师", "PLC工程师", "机电工程师"],
    "数据科学与大数据技术": ["大数据开发工程师", "数据分析师", "数据挖掘工程师", "ETL工程师", "Hadoop工程师"],
    "网络工程": ["网络工程师", "网络安全工程师", "系统管理员", "网络运维工程师", "云计算工程师"],
    "网络空间安全": ["网络安全工程师", "渗透测试工程师", "安全运维工程师", "等保测评工程师", "安全开发工程师"],
    "电气工程及其自动化": ["电气工程师", "PLC工程师", "电力系统工程师", "继电保护工程师", "电气设计工程师"],
    "自动化": ["自动化工程师", "DCS工程师", "PLC工程师", "控制工程师", "仪表工程师"],
    "土木工程": ["土建工程师", "结构工程师", "施工工程师", "工程造价师", "工程监理工程师"],
    "化学工程与工艺": ["化工工艺工程师", "化工设计工程师", "生产工程师", "安全管理工程师", "研发工程师"],
    "计算机科学与技术": ["Java开发工程师", "前端开发工程师", "后端开发工程师", "全栈工程师", "软件测试工程师"],
    "软件工程": ["Java开发工程师", "前端开发工程师", "后端开发工程师", "全栈工程师", "软件测试工程师"],
    "智能科学与技术": ["算法工程师", "人工智能工程师", "机器学习工程师", "数据分析师", "NLP工程师"],
}
PROJECTS = [
    "基于Spring Boot的校园二手交易平台开发",
    "基于Vue的响应式管理系统前端开发",
    "大数据处理与分析系统设计",
    "机器学习算法在就业数据预测中的应用",
    "智能推荐算法在求职平台中的实现",
    "Web安全漏洞检测工具开发",
    "基于微信小程序的校园服务平台",
    "企业人才管理系统的设计与实现",
    "分布式爬虫系统的架构与实现",
    "深度学习图像识别系统的开发",
]
CERTIFICATES = [
    "全国大学生英语四级证书", "全国大学生英语六级证书",
    "普通话二级甲等证书", "教师资格证（中学数学）",
    "计算机二级Java证书", "计算机二级C++证书",
    "初级会计师证书", "人力资源管理师三级证书",
    "CAD工程师认证证书", "PLC编程工程师证书",
]
SKILLS = [
    "Java / Spring Boot / MyBatis", "JavaScript / Vue / React",
    "Python / Django / Flask", "MySQL / Redis / MongoDB",
    "Hadoop / Spark / Flink", "Linux / Shell / Docker",
    "数据结构与算法", "计算机网络", "操作系统",
    "机器学习 / 深度学习", "大数据技术",
]
EMPLOYMENT_TYPE_WEIGHTS = [
    ("签订劳动合同", 75), ("继续深造", 10),
    ("应征入伍", 3), ("自主创业", 7), ("出国出境", 5),
]


# =========================== 工具函数 ===========================

def weighted_choice(choices):
    total = sum(w for _, w in choices)
    r = random.randint(1, total)
    cur = 0
    for item, w in choices:
        cur += w
        if r <= cur:
            return item
    return choices[-1][0]

def generate_name(gender):
    xing = random.choice(XING)
    ming = random.choice(MING_MALE) if gender == 'male' else random.choice(MING_FEMALE)
    return xing + ming

def generate_phone():
    prefixes = ["130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
                "150", "151", "152", "153", "155", "156", "157", "158", "159",
                "170", "171", "172", "173", "175", "176", "177", "178",
                "180", "181", "182", "183", "184", "185", "186", "187", "188", "189"]
    return random.choice(prefixes) + "".join([str(random.randint(0, 9)) for _ in range(8)])

def generate_student_no(grad_year, class_id, student_seq):
    """学号格式：{毕业年份}{班级ID(4位)}{学生序号(3位)}，如 20182601001"""
    return f"{grad_year}{class_id:04d}{student_seq:03d}"

def generate_id_card(age, is_male):
    birth_year = 2000 + random.randint(age - 2, age + 2)
    birth_month = random.randint(1, 12)
    birth_day = random.randint(1, 28)
    seq = random.randint(100, 999)
    gender_digit = random.randint(0, 8) * 2 + (0 if is_male else 1)
    return (f"{random.randint(4201, 6599)}{birth_year:04d}"
            f"{birth_month:02d}{birth_day:02d}{seq}{gender_digit}0")

def fmt_salary(v):
    return f"{v}元/月"


# =========================== 主程序 ===========================

def main():
    print("=" * 60)
    print("批量创建学生账号 v3（表结构匹配版）")
    print("=" * 60)
    print(f"每班学生数 : {STUDENTS_PER_CLASS}")
    print(f"跳过班级数 : {SKIP_CLASSES}")
    print(f"随机种子   : {RANDOM_SEED}")
    print("=" * 60)

    if RANDOM_SEED is not None:
        random.seed(RANDOM_SEED)

    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # 学生角色
        cursor.execute("SELECT id FROM sys_role WHERE role_key = 'student' LIMIT 1")
        row = cursor.fetchone()
        if not row:
            print("错误：未找到 'student' 角色，请先执行 init.sql")
            return
        student_role_id = row[0]
        print(f"✓ 学生角色 ID = {student_role_id}")

        # 已存在学生数
        cursor.execute("""
            SELECT COUNT(*) FROM sys_user u
            JOIN sys_user_role ur ON u.id = ur.user_id
            WHERE ur.role_id = %s
        """, (student_role_id,))
        existing = cursor.fetchone()[0]
        print(f"✓ 已存在学生: {existing} 个")

        # 真实班级
        cursor.execute("""
            SELECT c.id, c.class_name, c.grade, c.dept_id,
                   d.dept_name, d.dept_code,
                   m.id AS major_id, m.major_name,
                   COALESCE(m.degree_type, '工学学士') AS degree_type
            FROM sys_class c
            JOIN sys_dept d ON c.dept_id = d.id
            JOIN sys_major m ON c.major_id = m.id
            ORDER BY c.dept_id, c.grade, c.id
        """)
        classes = cursor.fetchall()
        print(f"✓ 班级总数: {len(classes)}")
        if not classes:
            print("错误：没有班级数据，请先在系统中添加班级")
            return

        # 真实企业（已审核通过）
        cursor.execute("""
            SELECT id, company_name, industry, scale, province, city
            FROM company_info
            WHERE auth_status = 'approved' AND status = '0'
        """)
        companies = cursor.fetchall()
        print(f"✓ 已审核企业: {len(companies)} 家")

        # 真实职位（已发布）
        cursor.execute("""
            SELECT id, job_name, company_id, company_name, industry,
                   work_city, salary_min, salary_max
            FROM job_position
            WHERE status = 'published' AND is_deleted = '0'
        """)
        jobs = cursor.fetchall()
        print(f"✓ 已发布职位: {len(jobs)} 个")

        print()
        print("开始生成学生数据 ...")
        print()

        total_users = 0
        total_infos = 0
        total_resumes = 0
        total_apps = 0
        total_interviews = 0
        total_offers = 0
        total_employments = 0
        student_seq = existing
        start_time = time.time()
        class_count = 0
        salary_buckets = [5000, 6000, 7000, 8000, 9000, 10000, 12000, 15000, 18000, 20000]

        for cls in classes:
            class_count += 1
            if class_count <= SKIP_CLASSES:
                continue

            (class_id, class_name, grade, dept_id, dept_name, dept_code,
             major_id, major_name, degree_type) = cls
            grade_year = int(grade)
            grad_year = grade_year + 4
            is_graduated = grad_year in (2023, 2024)

            # ---- 批次数据容器 ----
            batch_users = []          # sys_user
            batch_infos = []         # student_info (插入时再加 user_id)
            batch_resumes = []       # student_resume (插入时再加 student_id)
            batch_apps = []          # job_application
            batch_interviews = []     # interview_invitation
            batch_offers = []         # offer_letter
            batch_employments = []    # employment_record

            for s in range(STUDENTS_PER_CLASS):
                student_seq += 1
                gender = random.choice(['male', 'female'])
                is_male = (gender == 'male')
                name = generate_name(gender)
                age = 18 + random.randint(0, 3)
                phone = generate_phone()
                student_no = generate_student_no(grad_year, class_id, s + 1)
                username = f"stu_{student_seq:05d}"

                # sys_user
                password_hash = bcrypt.hashpw(
                    "123456".encode('utf-8'), bcrypt.gensalt(rounds=10)
                ).decode('utf-8')
                batch_users.append((
                    username, password_hash, name, gender,
                    phone, f"{username}@student.edu.cn",
                    generate_id_card(age, is_male),
                    dept_id, major_id,
                    class_name, class_id, student_no, grad_year
                ))

                # student_info
                province = weighted_choice(PROVINCES_WEIGHTED)
                city = random.choice(PROVINCE_CITIES.get(province, ["某市"]))
                by = 2000 + random.randint(age - 2, age + 2)
                bm = random.randint(1, 12)
                bd = random.randint(1, 28)
                batch_infos.append((
                    student_no, name, gender,
                    f"20{by - 2000:02d}-{bm:02d}-{bd:02d}",
                    generate_id_card(age, is_male),
                    random.choice(NATIONS), random.choice(POLITICS),
                    phone, f"{username}@student.edu.cn",
                    province, city, f"{province}{city}某街道某小区",
                    dept_id, dept_name, major_id, major_name,
                    class_name, class_id, grad_year,
                    random.choice(STUDY_TYPES),
                    f"{random.randint(1, 10)}号楼{random.randint(1, 20)}室",
                    f"{generate_name(not is_male)}（{random.choice(['父亲', '母亲', '叔叔', '姑姑'])}）",
                    generate_phone(),
                    "studying" if grad_year > 2024 else "graduated"
                ))

                # student_resume
                major_positions = MAJOR_POSITIONS.get(
                    major_name,
                    ["软件开发工程师", "前端开发工程师", "后端开发工程师", "测试工程师", "运维工程师"]
                )
                expected_position = random.choice(major_positions)
                all_cities = [c for cs in PROVINCE_CITIES.values() for c in cs]
                expected_city = random.choice(all_cities) if all_cities else "武汉市"
                sal_min = random.choice([5000, 6000, 7000, 8000, 9000, 10000])
                sal_max = sal_min + random.randint(2000, 5000)
                enroll_year = grad_year - 4
                gpa = f"{2.5 + random.random() * 1.5:.2f}"
                rank = random.randint(1, 30)

                proj_lines = [
                    f"{enroll_year + 2 + i}-03 至 {enroll_year + 2 + i + 1}-06  |  "
                    f"{random.choice(PROJECTS)}"
                    for i in range(random.randint(1, 2))
                ]
                batch_resumes.append((
                    f"{name}的简历", "1",
                    f"{enroll_year}-09 至 {grad_year}-07  |  {dept_name}  |  {major_name}  |  {class_name}\n"
                    f"GPA: {gpa}/4.0（专业前{rank}%）",
                    "\n\n".join(proj_lines),
                    "",
                    "\n".join(random.sample(CERTIFICATES, min(2, len(CERTIFICATES)))),
                    "、".join(random.sample(SKILLS, min(4, len(SKILLS)))),
                    "本人性格开朗，学习能力强，具备良好的团队协作能力。",
                    sal_min, sal_max,
                    expected_city, expected_position,
                    random.choice(all_cities) if all_cities else "武汉市",
                    degree_type or "工学学士",
                    major_name
                ))

                # ---- 就业数据（仅已毕业届）----
                if is_graduated and jobs:
                    applied_jobs = random.sample(jobs, min(random.randint(1, 3), len(jobs)))

                    for job in applied_jobs:
                        (job_id, job_name, job_company_id, job_company_name,
                         job_industry, job_work_city,
                         job_sal_min, job_sal_max) = job

                        app_status = random.choice(['pending', 'reviewing', 'accepted', 'rejected'])

                        # job_application
                        batch_apps.append((
                            job_id, None, job_company_id, None,  # student_id / resume_id 待补
                            app_status, "0", None, None, None, None
                        ))

                        # interview_invitation + offer_letter（accepted 时）
                        if app_status in ('reviewing', 'accepted'):
                            interview_time = (
                                f"2024-{random.randint(1, 6):02d}-"
                                f"{random.randint(10, 28):02d} "
                                f"{random.randint(9, 17):02d}:00:00"
                            )
                            batch_interviews.append((
                                None, None, job_company_id, job_id,   # application_id / student_id / company_id 待补
                                interview_time,
                                f"{job_work_city or '待定'}{random.choice(['XX大厦A座', 'YY科技园B栋'])}",
                                random.choice(["线上面试", "现场面试"]),
                                name, phone, None, "pending"
                            ))

                            if app_status == 'accepted':
                                sal = random.choice(salary_buckets)
                                prob_sal = int(sal * 0.8)
                                batch_offers.append((
                                    None, None, job_company_id, job_id,   # application_id / student_id 待补
                                    job_name, fmt_salary(sal),
                                    job_work_city or '',
                                    f"2024-{random.randint(7, 9):02d}-01",
                                    "3个月", fmt_salary(prob_sal),
                                    f"2024-{random.randint(6, 7):02d}-{random.randint(20, 28):02d}",
                                    "pending"
                                ))

                                # employment_record（用真实企业/职位）
                                emp_type = weighted_choice(EMPLOYMENT_TYPE_WEIGHTS)
                                work_province = weighted_choice(PROVINCES_WEIGHTED)
                                work_city = random.choice(PROVINCE_CITIES.get(work_province, ["某市"]))

                                if emp_type == "签订劳动合同":
                                    real_company = random.choice(companies) if companies else None
                                    if real_company:
                                        (ec_id, ec_name, ec_industry, ec_scale, ec_prov, ec_city) = real_company
                                    else:
                                        ec_id, ec_name, ec_industry, ec_scale = None, "待定企业", "互联网", "100-499人"
                                    batch_employments.append((
                                        None, emp_type, ec_name, None,
                                        ec_scale, ec_industry,
                                        expected_position, None,
                                        work_city, work_province,
                                        fmt_salary(sal),
                                        "1", f"TP{grad_year}{student_seq:05d}",
                                        f"2024-{random.randint(6, 8):02d}-15",
                                        f"2027-{random.randint(6, 8):02d}-14",
                                        fmt_salary(prob_sal),
                                        "approved", "系统自动审核通过", None, None, None
                                    ))
                                elif emp_type == "继续深造":
                                    batch_employments.append((
                                        None, emp_type, "考研升学", None, "", "教育",
                                        expected_position, None, "武汉市", "湖北省",
                                        "", "0", "", "", "", "", approved_row(),
                                    ))
                                elif emp_type == "应征入伍":
                                    batch_employments.append((
                                        None, emp_type, "应征入伍", None, "", "政府/军队",
                                        "士兵", None, "武汉市", "湖北省",
                                        "", "0", "", "", "", "", approved_row(),
                                    ))
                                elif emp_type == "自主创业":
                                    batch_employments.append((
                                        None, emp_type, "自主创业", None, "", "其他",
                                        expected_position, None, work_city, work_province,
                                        "", "0", "", "", "", "", approved_row(),
                                    ))
                                elif emp_type == "出国出境":
                                    batch_employments.append((
                                        None, emp_type, "出国深造", None, "", "教育",
                                        expected_position, None, "境外", "境外",
                                        "", "0", "", "", "", "", approved_row(),
                                    ))

            # ======================================================
            # 第一阶段：插入 sys_user，获取真实 user_id / student_id
            # ======================================================
            cursor.executemany("""
                INSERT INTO sys_user
                    (username, password, real_name, gender, phone, email, id_card,
                     dept_id, major_id, class_name, class_id, student_no, graduation_year,
                     status, remark)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, '0', '批量导入学生')
            """, batch_users)
            conn.commit()

            cursor.execute("SELECT LAST_INSERT_ID()")
            first_id = cursor.fetchone()[0]
            inserted_ids = list(range(first_id, first_id + STUDENTS_PER_CLASS))

            # sys_user_role
            cursor.executemany(
                "INSERT INTO sys_user_role (user_id, role_id) VALUES (%s, %s)",
                [(uid, student_role_id) for uid in inserted_ids]
            )
            conn.commit()

            # ======================================================
            # 第二阶段：插入 student_info（用 user_id）
            # ======================================================
            infos_with_uid = [(inserted_ids[i],) + info
                               for i, info in enumerate(batch_infos)]
            cursor.executemany("""
                INSERT INTO student_info
                    (user_id, student_no, real_name, gender, birth_date, id_card, nation,
                     politics_status, phone, email, province, city, address,
                     dept_id, dept_name, major_id, major_name,
                     class_name, class_id, graduation_year,
                     study_type, dormitory, emergency_contact, emergency_phone, status)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, infos_with_uid)
            conn.commit()

            # ======================================================
            # 第三阶段：插入 student_resume（用 student_id = user_id）
            # ======================================================
            resumes_with_uid = [(inserted_ids[i],) + resume
                                for i, resume in enumerate(batch_resumes)]
            cursor.executemany("""
                INSERT INTO student_resume
                    (student_id, resume_name, is_default, education_experience,
                     project_experience, work_experience, skill_certificates,
                     self_evaluation, expected_salary_min, expected_salary_max,
                     expected_city, expected_position, expected_industry,
                     degree_type, major_name)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, resumes_with_uid)
            conn.commit()

            # ======================================================
            # 第四阶段：就业数据（需要 job_application 的 id 作为外键）
            # ======================================================

            if batch_apps:
                # 补上 student_id（按循环顺序对应）
                apps_with_sid = [
                    (inserted_ids[i % STUDENTS_PER_CLASS],) + app
                    for i, app in enumerate(batch_apps)
                ]
                # 插入 job_application
                cursor.executemany("""
                    INSERT INTO job_application
                        (student_id, job_id, company_id, resume_id,
                         status, read_status, apply_letter,
                         company_remark, interview_status, offer_status)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """, apps_with_sid)
                conn.commit()

                # 获取刚插入的 application_id（顺序与 batch_apps 一致）
                cursor.execute("SELECT LAST_INSERT_ID()")
                first_app_id = cursor.fetchone()[0]
                inserted_app_ids = list(range(first_app_id, first_app_id + len(batch_apps)))

            if batch_interviews:
                intvs_with_ids = []
                app_idx = 0
                for i, intv in enumerate(batch_interviews):
                    # 找到对应的 app_idx（通过循环顺序）
                    intvs_with_ids.append((
                        inserted_app_ids[app_idx] if batch_apps else None,
                        inserted_ids[i % STUDENTS_PER_CLASS],
                    ) + intv[2:])
                    app_idx += 1

                cursor.executemany("""
                    INSERT INTO interview_invitation
                        (application_id, student_id, company_id, job_id,
                         interview_time, interview_address, interview_type,
                         contact_person, contact_phone, remark, status)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """, intvs_with_ids)
                conn.commit()

            if batch_offers:
                offers_with_ids = []
                app_idx = 0
                for i, offer in enumerate(batch_offers):
                    offers_with_ids.append((
                        inserted_app_ids[app_idx] if batch_apps else None,
                        inserted_ids[i % STUDENTS_PER_CLASS],
                    ) + offer[2:])
                    app_idx += 1

                cursor.executemany("""
                    INSERT INTO offer_letter
                        (application_id, student_id, company_id, job_id,
                         position_name, salary, work_city, start_date,
                         probation_period, probation_salary,
                         response_deadline, status)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """, offers_with_ids)
                conn.commit()

            if batch_employments:
                emps_with_sid = [
                    (inserted_ids[i % STUDENTS_PER_CLASS],) + emp
                    for i, emp in enumerate(batch_employments)
                ]
                cursor.executemany("""
                    INSERT INTO employment_record
                        (student_id, employment_type, company_name, company_code,
                         company_scale, company_industry,
                         position_name, position_category,
                         work_city, work_province, salary,
                         is_three_party_signed, three_party_no,
                         contract_start_date, contract_end_date, probation_salary,
                         audit_status, audit_remark, audit_user_id, audit_time, remark)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """, emps_with_sid)
                conn.commit()

            # 更新班级学生数
            cursor.execute(
                "UPDATE sys_class SET student_count = %s WHERE id = %s",
                (STUDENTS_PER_CLASS, class_id)
            )
            conn.commit()

            # 统计
            total_users += len(batch_users)
            total_infos += len(batch_infos)
            total_resumes += len(batch_resumes)
            total_apps += len(batch_apps)
            total_interviews += len(batch_interviews)
            total_offers += len(batch_offers)
            total_employments += len(batch_employments)

            if class_count % 10 == 0 or class_count == len(classes):
                elapsed = time.time() - start_time
                rate = class_count / elapsed if elapsed > 0 else 0
                remaining = (len(classes) - class_count) / rate if rate > 0 else 0
                print(f"  [{class_count}/{len(classes)}] "
                      f"学生累计 {total_users} 人，预计剩余 {remaining / 60:.1f} 分钟 ...")

        elapsed = time.time() - start_time
        print()
        print("=" * 60)
        print("学生账号创建完成！")
        print(f"  耗时   : {elapsed:.1f} 秒")
        print(f"  班级   : {class_count} 个")
        print(f"  用户账号: {total_users} 个（stu_00001 ~ stu_{student_seq:05d} / 123456）")
        print(f"  学生档案: {total_infos} 条")
        print(f"  学生简历: {total_resumes} 份")
        print(f"  投递记录: {total_apps} 条")
        print(f"  面试邀请: {total_interviews} 条")
        print(f"  Offer   : {total_offers} 条")
        print(f"  就业记录: {total_employments} 条")
        print("=" * 60)

    except pymysql.Error as e:
        print(f"数据库错误：{e}")
        if 'conn' in dir() and conn:
            conn.rollback()
    finally:
        if 'conn' in dir() and conn:
            cursor.close()
            conn.close()


def approved_row():
    """返回就业记录中'审核通过'状态的固定字段"""
    return "approved", "系统自动审核通过", None, None, None


if __name__ == '__main__':
    main()
