# -*- coding: utf-8 -*-
"""
导入院系和专业数据到数据库
"""

import pymysql
import bcrypt

from db_config import DB_CONFIG

# 院系和专业数据
DEPARTMENTS = [
    ("资源与环境工程学院", "001", 1),
    ("机械工程学院", "002", 2),
    ("电气与信息工程学院", "003", 3),
    ("土木工程学院", "004", 4),
    ("化学工程学院", "005", 5),
    ("食品药品制造工程学院", "006", 6),
    ("矿业工程学院", "007", 7),
    ("经济管理学院", "008", 8),
    ("大数据学院", "009", 9),
    ("航空航天工程学院", "010", 10),
    ("建筑与城市规划学院", "011", 11),
    ("材料化学与冶金工程学院", "012", 12),
    ("交通工程学院", "013", 13),
    ("理学院", "014", 14),
    ("体育学院", "015", 15),
    ("外国语学院", "016", 16),
]

MAJORS = [
    # 一、资源与环境工程学院
    ("资源勘查工程", "00101", "资勘"),
    ("环境工程", "00102", "环工"),
    ("地质工程", "00103", "地质"),
    # 二、机械工程学院
    ("机械设计制造及其自动化", "00201", "机械"),
    ("机械电子工程", "00202", "机电"),
    # 三、电气与信息工程学院
    ("电气工程及其自动化", "00301", "电气"),
    ("自动化", "00302", "自动化"),
    # 四、土木工程学院
    ("土木工程", "00401", "土木"),
    ("水利水电工程", "00402", "水电"),
    ("道路桥梁与渡河工程", "00403", "道桥"),
    # 五、化学工程学院
    ("化学工程与工艺", "00501", "化工"),
    ("新能源科学与工程", "00502", "新能源"),
    ("过程装备与控制工程", "00503", "过控"),
    # 六、食品药品制造工程学院
    ("制药工程", "00601", "制药"),
    ("生物制药", "00602", "生药"),
    ("酿酒工程", "00603", "酿酒"),
    # 七、矿业工程学院
    ("安全工程", "00701", "安全"),
    ("采矿工程", "00702", "采矿"),
    ("智能采矿工程", "00703", "智采"),
    # 八、经济管理学院
    ("工程管理", "00801", "工管"),
    ("工程造价", "00802", "造价"),
    ("投资学", "00803", "投资"),
    # 九、大数据学院
    ("网络工程", "00901", "网工"),
    ("数据科学与大数据技术", "00902", "大数据"),
    ("智能科学与技术", "00903", "智科"),
    ("网络空间安全", "00904", "网安"),
    # 十、航空航天工程学院
    ("飞行器制造工程", "01001", "飞制"),
    ("飞行器动力工程", "01002", "飞动"),
    # 十一、建筑与城市规划学院
    ("人文地理与城乡规划", "01101", "城规"),
    ("环境设计", "01102", "环设"),
    ("建筑学", "01103", "建筑"),
    # 十二、材料化学与冶金工程学院
    ("材料科学与工程", "01201", "材料"),
    ("新能源材料与器件", "01202", "新材"),
    ("焊接技术与工程", "01203", "焊接"),
    # 十三、交通工程学院
    ("交通工程", "01301", "交工"),
    ("交通运输", "01302", "运输"),
    # 十四、理学院
    ("应用统计学", "01401", "统计"),
    ("应用物理学", "01402", "应物"),
    # 十五、体育学院
    ("休闲体育", "01501", "休体"),
    # 十六、外国语学院
    ("英语", "01601", "英语"),
]


def main():
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    print("=" * 50)
    print("开始导入院系和专业数据...")
    print("=" * 50)

    # 清空现有数据（避免重复导入）
    print("\n[1] 清空 sys_major 和 sys_dept 表...")
    cursor.execute("DELETE FROM sys_major")
    cursor.execute("DELETE FROM sys_dept")
    conn.commit()
    print("    清空完成")

    # 插入院系
    print("\n[2] 插入院系数据...")
    dept_ids = {}
    for dept_name, dept_code, sort in DEPARTMENTS:
        cursor.execute(
            "INSERT INTO sys_dept (dept_name, dept_code, sort, status) VALUES (%s, %s, %s, '0')",
            (dept_name, dept_code, sort)
        )
        dept_id = cursor.lastrowid
        dept_ids[dept_code] = dept_id
        print(f"    {dept_name} (id={dept_id})")

    # 插入专业
    print("\n[3] 插入专业数据...")
    for major_name, major_code, short_name in MAJORS:
        # 根据专业代码前3位找到对应院系
        dept_code = major_code[:3]
        dept_id = dept_ids.get(dept_code)
        if dept_id is None:
            print(f"    [警告] {major_name} 未找到对应院系 {dept_code}，跳过")
            continue
        cursor.execute(
            "INSERT INTO sys_major (major_name, major_code, dept_id, is_top_level, short_name, remark) VALUES (%s, %s, %s, '0', %s, '')",
            (major_name, major_code, dept_id, short_name)
        )
        print(f"    {major_name} -> {list(dept_ids.keys())[list(dept_ids.values()).index(dept_id)]}")

    conn.commit()

    # 统计
    cursor.execute("SELECT COUNT(*) FROM sys_dept")
    dept_count = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(*) FROM sys_major")
    major_count = cursor.fetchone()[0]

    print()
    print("=" * 50)
    print("导入完成!")
    print(f"  院系数量: {dept_count}")
    print(f"  专业数量: {major_count}")
    print("=" * 50)

    cursor.close()
    conn.close()


if __name__ == '__main__':
    main()
