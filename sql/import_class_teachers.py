# -*- coding: utf-8 -*-
"""
导入班级老师数据脚本

功能：
  1. 从数据库读取所有班级的真实数据（dept_id, major_id, class_id）
  2. 清理现有的班级老师账号
  3. 为每个班级生成一个班级老师账号
  4. 绑定 class_teacher 角色，回填班级班主任信息

用户名格式：专业英文缩写 + class_id（例：jixie1801）
姓名格式：随机中文姓名，约60%二字名 + 40%单字名，真实好听
密码统一：123456
"""

import os
import random
import string

import bcrypt
import pymysql

from db_config import DB_CONFIG

PASSWORD = os.getenv('DEMO_PASSWORD', '123456')
PASSWORD_HASH = bcrypt.hashpw(PASSWORD.encode('utf-8'), bcrypt.gensalt(rounds=10)).decode('utf-8')

# 专业英文缩写映射（按数据库 sys_major.short_name 实际值）
MAJOR_PY = {
    "资勘": "zikuan",   "环工": "huanggong",  "地质": "dizhi",
    "机械": "jixie",    "机电": "jidian",     "电气": "dianqi",
    "自动化": "zidong",  "土木": "tumu",       "水电": "shuidian",
    "道桥": "daoqiao",  "化工": "huagong",    "新能源": "xinneng",
    "过控": "guokong",  "制药": "zhiyao",    "生药": "shengyao",
    "酿酒": "niangjiu", "安全": "anquan",    "采矿": "caikuang",
    "智采": "zhicai",   "工管": "gongguan",  "造价": "zaojia",
    "投资": "touzi",    "网工": "wanggong",  "大数据": "dashuju",
    "智科": "zhike",    "网安": "wangan",    "飞制": "feizhi",
    "飞动": "feidong",  "城规": "chenggui",  "环设": "huanshe",
    "建筑": "jianzhu",  "材料": "cailiao",   "新材": "xincai",
    "焊接": "hanjie",   "交工": "jiaogong",  "运输": "yunshu",
    "统计": "tongji",   "应物": "yingwu",    "休体": "xiuti",
    "英语": "yingyu",
}

# ============================================================
# 真实姓名生成器
# ============================================================

# 常见姓氏（80个）
SURNAMES = [
    "王", "李", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴",
    "徐", "孙", "胡", "朱", "高", "林", "何", "郭", "马", "罗",
    "梁", "宋", "郑", "谢", "韩", "唐", "冯", "于", "董", "萧",
    "程", "曹", "袁", "邓", "许", "傅", "沈", "曾", "彭", "吕",
    "苏", "卢", "蒋", "蔡", "贾", "丁", "魏", "薛", "叶", "阎",
    "余", "潘", "杜", "戴", "夏", "钟", "汪", "田", "任", "姜",
    "范", "方", "石", "姚", "谭", "廖", "邹", "熊", "金", "陆",
    "郝", "孔", "白", "崔", "康", "毛", "邱", "秦", "江", "史",
]

# 好听的单字名用字（适合老师，优雅、有文化感）
MONO_CHARS = [
    "敏", "静", "丽", "芳", "娜", "婷", "玲", "霞", "秀", "英",
    "华", "慧", "雅", "欣", "蓉", "燕", "梅", "兰", "洁", "丹",
    "君", "怡", "倩", "颖", "媛", "洁", "萍", "雯", "倩", "蓉",
    "君", "琳", "瑶", "瑾", "璇", "琪", "珏", "琳", "珊", "璐",
    "伟", "强", "勇", "磊", "军", "辉", "波", "峰", "超", "杰",
    "涛", "宇", "浩", "晨", "博", "瑞", "昊", "鹏", "飞", "龙",
    "斌", "志", "旭", "阳", "霖", "程", "睿", "翔", "瑜", "豪",
]

# 好听的二字名（真实常用组合，60%概率使用）
DOUBLE_NAMES = [
    # 女名
    "雅婷", "雅静", "雅丽", "雅欣", "雅琴", "雅慧", "雅芳", "雅洁",
    "欣悦", "欣怡", "欣蕾", "欣玲", "欣娜", "欣瑶", "欣燕", "欣洁",
    "丽娜", "丽君", "丽华", "丽萍", "丽梅", "丽娟", "丽霞", "丽红",
    "秀英", "秀兰", "秀珍", "秀梅", "秀娟", "秀玲", "秀云", "秀芳",
    "静文", "静怡", "静雯", "静蕾", "静婷", "静雅", "静洁", "静燕",
    "婷玉", "婷匀", "婷予", "婷月", "婷云", "婷匀", "思婷", "梦婷",
    "晓燕", "晓梅", "晓霞", "晓静", "晓琳", "晓芳", "晓洁", "晓玲",
    "晓娟", "晓红", "晓华", "晓蕾", "晓云", "晓婷", "晓宇", "晓宇",
    "雨婷", "雨燕", "雨洁", "雨欣", "雨霞", "雨静", "雨梅", "雨婷",
    "文静", "文婷", "文燕", "文娟", "文洁", "文琳", "文婷", "文玲",
    "慧敏", "慧君", "慧琳", "慧燕", "慧洁", "慧霞", "慧婷", "慧玲",
    "慧娟", "慧华", "慧英", "慧芳", "慧娜", "慧萍", "慧雯", "慧云",
    "建华", "建平", "建伟", "建成", "建锋", "建波", "建辉", "建强",
    "建华", "建荣", "建新", "建东", "建文", "建明", "建龙", "建国",
    "建军", "建宇", "建超", "建林", "建峰", "建华", "建刚", "建斌",
    # 男名
    "伟明", "伟强", "伟华", "伟东", "伟军", "伟波", "伟峰", "伟平",
    "伟杰", "伟超", "伟斌", "伟林", "伟鹏", "伟龙", "伟刚", "伟忠",
    "志强", "志刚", "志明", "志华", "志伟", "志勇", "志军", "志国",
    "志鹏", "志辉", "志龙", "志远", "志高", "志安", "志诚", "志敏",
    "文博", "文龙", "文强", "文华", "文军", "文辉", "文峰", "文涛",
    "文杰", "文超", "文波", "文东", "文斌", "文宇", "文浩", "文祥",
    "明辉", "明华", "明军", "明波", "明峰", "明强", "明杰", "明超",
    "明东", "明远", "明轩", "明哲", "明瑞", "明亮", "明健", "明星",
    "俊华", "俊强", "俊伟", "俊峰", "俊波", "俊龙", "俊杰", "俊超",
    "俊鹏", "俊飞", "俊宇", "俊豪", "俊凯", "俊熙", "俊泽", "俊贤",
    "海波", "海峰", "海龙", "海涛", "海明", "海华", "海军", "海勇",
    "海超", "海鹏", "海东", "海林", "海峰", "海洲", "海鑫", "海瑞",
    "晓峰", "晓东", "晓军", "晓华", "晓波", "晓龙", "晓雷", "晓刚",
    "晓宇", "晓凯", "晓晨", "晓鹏", "晓辉", "晓杰", "晓超", "晓健",
    "国华", "国强", "国栋", "国峰", "国军", "国平", "国伟", "国辉",
    "国龙", "国勇", "国超", "国良", "国瑞", "国林", "国宾", "国昌",
    "永强", "永伟", "永华", "永峰", "永波", "永军", "永杰", "永超",
    "永鹏", "永龙", "永明", "永刚", "永亮", "永健", "永辉", "永红",
    "斌", "磊", "超", "峰", "涛", "杰", "飞", "龙", "波", "宇",
    "浩", "鹏", "辉", "凯", "晨", "健", "强", "刚", "勇", "华",
    "静", "敏", "霞", "玲", "丽", "芳", "娜", "婷", "燕", "洁",
]

def gen_real_name():
    surname = random.choice(SURNAMES)
    if random.random() < 0.55:
        # 二字名
        return surname + random.choice(DOUBLE_NAMES)
    else:
        # 单字名
        return surname + random.choice(MONO_CHARS)


def gen_phone():
    prefixes = [
        "130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
        "150", "151", "152", "153", "155", "156", "157", "158", "159",
        "180", "181", "182", "183", "184", "185", "186", "187", "188", "189",
        "195", "197", "198", "199",
    ]
    return random.choice(prefixes) + ''.join(random.choices(string.digits, k=8))


def gen_id_card(name, gender_char):
    area_code = "520102"
    birth_year = random.randint(1972, 1992)
    birth_month = random.randint(1, 12)
    birth_day = random.randint(1, 28)
    birth = f"{birth_year}{birth_month:02d}{birth_day:02d}"
    seq = f"{random.randint(0, 9)}{random.randint(0, 9)}{random.randint(0, 9)}"
    if gender_char == "男":
        seq_list = list(seq)
        seq_list[2] = str(random.randint(0, 7) * 2)
        seq = ''.join(seq_list)
    else:
        seq_list = list(seq)
        seq_list[2] = str(random.randint(0, 7) * 2 + 1)
        seq = ''.join(seq_list)
    id17 = area_code + birth + seq
    factors = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
    parity = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']
    total = sum(int(id17[i]) * factors[i] for i in range(17))
    check = parity[total % 11]
    return id17 + check


def gen_email():
    return f"{random.randint(10000000, 999999999)}@qq.com"


def gen_username(short_name, class_id):
    py = MAJOR_PY.get(short_name, short_name[:2])
    return f"{py}{class_id}"


# ============================================================
# 主逻辑
# ============================================================

def main():
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    print("=" * 60)
    print("开始导入班级老师数据...")
    print("=" * 60)

    # Step 1: 查询 class_teacher 角色 ID
    cursor.execute("SELECT id FROM sys_role WHERE role_key = 'class_teacher' LIMIT 1")
    role_row = cursor.fetchone()
    if not role_row:
        print("错误：未找到 'class_teacher' 角色，请先执行 init.sql")
        return
    role_id = role_row[0]
    print(f"class_teacher 角色ID = {role_id}")

    # Step 2: 清理现有的班级老师
    print("\n[Step 1] 清理现有班级老师...")
    cursor.execute("""
        SELECT u.id, u.username FROM sys_user u
        JOIN sys_user_role ur ON u.id = ur.user_id
        JOIN sys_role r ON ur.role_id = r.id
        WHERE r.role_key = 'class_teacher' AND u.class_id IS NOT NULL
    """)
    old_teachers = cursor.fetchall()
    if old_teachers:
        ids = [str(r[0]) for r in old_teachers]
        cursor.execute(f"DELETE FROM sys_user_role WHERE user_id IN ({','.join(ids)})")
        cursor.execute(f"DELETE FROM sys_user WHERE id IN ({','.join(ids)})")
        print(f"  已删除 {len(old_teachers)} 位旧班级老师")
    else:
        print("  无旧数据")

    # Step 3: 从数据库读取班级数据（带专业简称）
    print("\n[Step 2] 读取班级数据...")
    cursor.execute("""
        SELECT c.id, c.class_name, c.major_id, c.dept_id,
               m.short_name, m.major_name, d.dept_name
        FROM sys_class c
        JOIN sys_major m ON c.major_id = m.id
        JOIN sys_dept d ON c.dept_id = d.id
        ORDER BY c.id
    """)
    classes = cursor.fetchall()
    print(f"  共读取 {len(classes)} 个班级")

    # 年级分布
    cursor.execute("SELECT grade, COUNT(*) FROM sys_class GROUP BY grade ORDER BY grade")
    for row in cursor.fetchall():
        print(f"    年级 {row[0]}: {row[1]} 个班")

    # Step 4: 生成老师数据
    print("\n[Step 3] 生成老师信息...")
    teachers_to_insert = []
    used_usernames = set()

    for class_id, class_name, major_id, dept_id, short_name, major_name, dept_name in classes:
        if not short_name or not short_name.strip():
            short_name = class_name[:2]
        short_name = short_name.strip()

        real_name = gen_real_name()
        gender = random.choice(["男", "女"])
        id_card = gen_id_card(real_name, gender)
        email = gen_email()

        username = gen_username(short_name, class_id)
        counter = 1
        while username in used_usernames:
            username = gen_username(short_name, class_id) + str(counter)
            counter += 1
        used_usernames.add(username)

        remark = f"class_teacher_{class_id}"
        teachers_to_insert.append((
            username, PASSWORD_HASH, real_name, email, gen_phone(),
            gender, id_card, "0", dept_id, major_id, class_name, class_id, remark
        ))

    print(f"  生成 {len(teachers_to_insert)} 位老师")

    # Step 5: 批量插入
    print("\n[Step 4] 批量插入老师...")
    insert_sql = """
        INSERT INTO sys_user
            (username, password, real_name, email, phone, gender, id_card,
             status, dept_id, major_id, class_name, class_id, remark)
        VALUES
            (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    cursor.executemany(insert_sql, teachers_to_insert)

    # Step 6: 绑定角色 + 回填班主任
    print("  绑定角色 + 回填班主任...")
    bind_sql = "INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (%s, %s)"
    for username, *_ in teachers_to_insert:
        cursor.execute(
            "SELECT id, class_id, real_name FROM sys_user WHERE username = %s",
            (username,)
        )
        row = cursor.fetchone()
        if row:
            user_id, class_id_val, teacher_name = row[0], row[1], row[2]
            cursor.execute(bind_sql, (user_id, role_id))
            if class_id_val:
                cursor.execute(
                    "UPDATE sys_class SET advisor = %s, advisor_id = %s WHERE id = %s",
                    (teacher_name, user_id, class_id_val)
                )

    conn.commit()

    # Step 7: 统计 + 抽样
    cursor.execute("SELECT COUNT(*) FROM sys_class")
    class_count = cursor.fetchone()[0]
    cursor.execute("""
        SELECT COUNT(*) FROM sys_user u
        JOIN sys_user_role ur ON u.id = ur.user_id
        JOIN sys_role r ON ur.role_id = r.id
        WHERE r.role_key = 'class_teacher' AND u.class_id IS NOT NULL
    """)
    teacher_count = cursor.fetchone()[0]

    cursor.execute("""
        SELECT u.username, u.real_name, u.gender, u.email, c.class_name, d.dept_name
        FROM sys_user u
        JOIN sys_user_role ur ON u.id = ur.user_id
        JOIN sys_role r ON ur.role_id = r.id
        JOIN sys_class c ON u.class_id = c.id
        JOIN sys_dept d ON c.dept_id = d.id
        WHERE r.role_key = 'class_teacher'
        ORDER BY c.id
        LIMIT 20
    """)
    samples = cursor.fetchall()

    print()
    print("=" * 60)
    print("导入完成！")
    print(f"  密码统一为：{PASSWORD}")
    print(f"  班级总数：{class_count} 个")
    print(f"  班级老师总数：{teacher_count} 位")
    print()
    print("  账号样例（前20个）：")
    for i, (username, real_name, gender, email, class_name, dept_name) in enumerate(samples):
        print(f"    {i+1:2d}. {username}  |  {real_name}（{gender}）  |  {email}  |  {dept_name}/{class_name}")
    print("=" * 60)

    cursor.close()
    conn.close()


if __name__ == "__main__":
    main()
