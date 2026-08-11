# -*- coding: utf-8 -*-
"""
导入院级老师数据脚本

为每个学院生成1个院级老师：
- 姓名格式：大数据学院院长 / 大数据学院副院长 / 大数据学院副教授 等
- 用户名格式：bigdata491 / mech502 等（英文）
- 邮箱格式：纯数字@qq.com
- 密码统一为：123456
"""

import os
import random
import string

import bcrypt
import pymysql

from db_config import DB_CONFIG

PASSWORD = os.getenv('DEMO_PASSWORD', '123456')
PASSWORD_HASH = bcrypt.hashpw(PASSWORD.encode('utf-8'), bcrypt.gensalt(rounds=10)).decode('utf-8')

# 16个学院数据（dept_id 从数据库实际值）：(dept_id, 学院名)
COLLEGES = [
    (49, "资源与环境工程学院"),
    (50, "机械工程学院"),
    (51, "电气与信息工程学院"),
    (52, "土木工程学院"),
    (53, "化学工程学院"),
    (54, "食品药品制造工程学院"),
    (55, "矿业工程学院"),
    (56, "经济管理学院"),
    (57, "大数据学院"),
    (58, "航空航天工程学院"),
    (59, "建筑与城市规划学院"),
    (60, "材料化学与冶金工程学院"),
    (61, "交通工程学院"),
    (62, "理学院"),
    (63, "体育学院"),
    (64, "外国语学院"),
]

GENDER_LIST = ["男", "女"]


def gen_phone():
    prefixes = [
        "130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
        "147", "150", "151", "152", "153", "155", "156", "157", "158", "159",
        "166", "170", "171", "172", "173", "175", "176", "177", "178", "180",
        "181", "182", "183", "184", "185", "186", "187", "188", "189",
        "190", "191", "195", "196", "197", "198", "199",
    ]
    return random.choice(prefixes) + ''.join(random.choices(string.digits, k=8))


def gen_id_card(name, gender_char):
    """生成18位身份证号（格式正确，校验位通过）"""
    area_code = "520102"
    birth_year = random.randint(1965, 1985)
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
    """生成纯数字QQ邮箱"""
    return f"{random.randint(10000000, 999999999)}@qq.com"


def gen_username(dept_name, dept_id, counter):
    """生成唯一用户名（英文）"""
    pinyin_map = {
        "资源与环境工程学院": "env",
        "机械工程学院": "mech",
        "电气与信息工程学院": "ee",
        "土木工程学院": "civil",
        "化学工程学院": "chem",
        "食品药品制造工程学院": "food",
        "矿业工程学院": "mining",
        "经济管理学院": "econ",
        "大数据学院": "bigdata",
        "航空航天工程学院": "aero",
        "建筑与城市规划学院": "arch",
        "材料化学与冶金工程学院": "mse",
        "交通工程学院": "trans",
        "理学院": "sci",
        "体育学院": "pe",
        "外国语学院": "fl",
    }
    py = pinyin_map.get(dept_name, dept_name[:2])
    return f"{py}{dept_id}{counter}"


# ============================================================
# 主逻辑
# ============================================================

def main():
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    print("=" * 60)
    print("开始导入院级老师数据...")
    print("=" * 60)

    # 查询 dept_teacher 角色 ID
    cursor.execute("SELECT id FROM sys_role WHERE role_key = 'dept_teacher' LIMIT 1")
    role_row = cursor.fetchone()
    if not role_row:
        print("错误：未找到 'dept_teacher' 角色，请先执行 init.sql")
        return
    role_id = role_row[0]
    print(f"dept_teacher 角色ID = {role_id}")

    teachers_to_insert = []
    used_usernames = set()

    for dept_id, dept_name in COLLEGES:
        # 验证学院是否存在
        cursor.execute("SELECT COUNT(*) FROM sys_dept WHERE id = %s", (dept_id,))
        if cursor.fetchone()[0] == 0:
            print(f"[警告] 学院 ID={dept_id} 不存在，跳过")
            continue

        # 每学院生成 1 个院级老师
        titles = ["院长", "副院长", "副院长", "副教授", "教授"]
        title = random.choice(titles)
        real_name = f"{dept_name}{title}"
        gender = random.choice(GENDER_LIST)
        id_card = gen_id_card(real_name, gender)
        email = gen_email()
        username = gen_username(dept_name, dept_id, 1)
        counter = 2
        while username in used_usernames:
            username = gen_username(dept_name, dept_id, counter)
            counter += 1
        used_usernames.add(username)
        remark = f"dept_teacher_{dept_id}"
        teachers_to_insert.append((
            username, PASSWORD_HASH, real_name, email, gen_phone(),
            gender, id_card, "0", dept_id, remark
        ))
        print(f"  {dept_name} -> {real_name} | {gender} | {email} | {username}")

    # 批量插入
    print(f"\n正在插入 {len(teachers_to_insert)} 条教师数据...")
    insert_sql = """
        INSERT INTO sys_user
            (username, password, real_name, email, phone, gender, id_card, status, dept_id, remark)
        VALUES
            (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    cursor.executemany(insert_sql, teachers_to_insert)

    # 批量绑定角色
    print("正在绑定 dept_teacher 角色...")
    bind_sql = "INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (%s, %s)"
    for username, *_ in teachers_to_insert:
        cursor.execute("SELECT id FROM sys_user WHERE username = %s", (username,))
        row = cursor.fetchone()
        if row:
            cursor.execute(bind_sql, (row[0], role_id))

    conn.commit()

    # 统计
    cursor.execute("SELECT COUNT(*) FROM sys_user WHERE dept_id IS NOT NULL")
    total = cursor.fetchone()[0]

    print()
    print("=" * 60)
    print("导入完成！")
    print(f"  密码统一为：{PASSWORD}")
    print(f"  合计：{total} 位院级老师")
    print("=" * 60)

    cursor.close()
    conn.close()


if __name__ == "__main__":
    main()
