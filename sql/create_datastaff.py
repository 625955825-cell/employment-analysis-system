# -*- coding: utf-8 -*-
"""
创建数据分析员账号脚本

用法：在 PyCharm 中直接运行此脚本即可
密码统一为：123456
"""

import os

import bcrypt
import pymysql

from db_config import DB_CONFIG

# 数据分析员信息
STAFF_USERNAME = 'datastaff'
STAFF_PASSWORD = os.getenv('DEMO_PASSWORD', '123456')
STAFF_PASSWORD_HASH = bcrypt.hashpw(STAFF_PASSWORD.encode('utf-8'), bcrypt.gensalt(rounds=10)).decode('utf-8')
STAFF_REAL_NAME = '数据分析员'
STAFF_EMAIL = 'datastaff@edu.cn'
STAFF_PHONE = '13800000006'
STAFF_DEPT_ID = 1  # 计算机学院


def main():
    print("=" * 50)
    print("开始创建数据分析员账号...")
    print("=" * 50)

    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # 查询 employment_staff 角色 ID
        cursor.execute("SELECT id FROM sys_role WHERE role_key = 'employment_staff' LIMIT 1")
        role_row = cursor.fetchone()
        if not role_row:
            print("错误：未找到 'employment_staff' 角色，请先执行 init.sql")
            return
        role_id = role_row[0]
        print(f"✓ 找到数据分析员角色，role_id = {role_id}")

        # 查询账号是否已存在
        cursor.execute("SELECT id FROM sys_user WHERE username = %s", (STAFF_USERNAME,))
        existing_user = cursor.fetchone()

        if existing_user:
            print(f"账号 '{STAFF_USERNAME}' 已存在，先更新密码哈希")
            cursor.execute("""
                UPDATE sys_user
                SET password = %s, real_name = %s, email = %s, phone = %s, dept_id = %s, status = '0', remark = '数据分析工程师'
                WHERE username = %s
            """, (STAFF_PASSWORD_HASH, STAFF_REAL_NAME, STAFF_EMAIL, STAFF_PHONE, STAFF_DEPT_ID, STAFF_USERNAME))
            print(f"✓ 密码已更新")
        else:
            # 插入账号
            sql_user = """
                INSERT INTO sys_user
                    (username, password, real_name, email, phone, dept_id, status, remark)
                VALUES
                    (%s, %s, %s, %s, %s, %s, '0', '数据分析工程师')
            """
            cursor.execute(sql_user, (
                STAFF_USERNAME,
                STAFF_PASSWORD_HASH,
                STAFF_REAL_NAME,
                STAFF_EMAIL,
                STAFF_PHONE,
                STAFF_DEPT_ID
            ))
            print(f"✓ 数据分析员账号 '{STAFF_USERNAME}' 创建成功")

        # 查询账号 ID
        cursor.execute("SELECT id FROM sys_user WHERE username = %s", (STAFF_USERNAME,))
        user_id = cursor.fetchone()[0]

        # 绑定角色
        cursor.execute("""
            INSERT IGNORE INTO sys_user_role (user_id, role_id)
            VALUES (%s, %s)
        """, (user_id, role_id))
        print(f"✓ 角色 'employment_staff' 绑定成功")

        conn.commit()
        print()
        print("=" * 50)
        print("数据分析员账号创建完成！")
        print(f"  用户名：{STAFF_USERNAME}")
        print(f"  密码：{STAFF_PASSWORD}")
        print(f"  姓名：{STAFF_REAL_NAME}")
        print("=" * 50)

    except pymysql.Error as e:
        print(f"数据库错误：{e}")
    finally:
        if 'conn' in dir() and conn:
            cursor.close()
            conn.close()


if __name__ == '__main__':
    main()
