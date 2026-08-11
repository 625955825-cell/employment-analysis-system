# -*- coding: utf-8 -*-
"""
创建校级管理员账号脚本

用法：在 PyCharm 中直接运行此脚本即可
密码统一为：123456
"""

import os

import bcrypt
import pymysql

from db_config import DB_CONFIG

# 管理员信息
ADMIN_PASSWORD = os.getenv('DEMO_PASSWORD', '123456')
ADMIN_USERNAME = 'admin'
ADMIN_REAL_NAME = '张建国'
ADMIN_EMAIL = 'admin@edu.cn'
ADMIN_PHONE = '13800000001'
ADMIN_DEPT_ID = 1  # 计算机学院


def main():
    print("=" * 50)
    print("开始创建校级管理员账号...")
    print("=" * 50)

    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # 查询 admin 角色 ID
        cursor.execute("SELECT id FROM sys_role WHERE role_key = 'admin' LIMIT 1")
        role_row = cursor.fetchone()
        if not role_row:
            print("错误：未找到 'admin' 角色，请先执行 init.sql")
            return
        role_id = role_row[0]
        print(f"找到 admin 角色，role_id = {role_id}")

        # 动态生成 BCrypt 哈希
        admin_hash = bcrypt.hashpw(ADMIN_PASSWORD.encode('utf-8'), bcrypt.gensalt(rounds=10)).decode('utf-8')
        print(f"生成密码哈希: {admin_hash}")

        # 查询 admin 用户是否已存在
        cursor.execute("SELECT id FROM sys_user WHERE username = %s", (ADMIN_USERNAME,))
        existing_user = cursor.fetchone()

        if existing_user:
            # 已存在则更新密码哈希（确保与当前 bcrypt 版本一致）
            cursor.execute(
                "UPDATE sys_user SET password = %s, real_name = %s WHERE username = %s",
                (admin_hash, ADMIN_REAL_NAME, ADMIN_USERNAME)
            )
            print(f"账号 '{ADMIN_USERNAME}' 已存在，已更新密码哈希")
        else:
            # 插入 admin 用户
            sql_user = """
                INSERT INTO sys_user
                    (username, password, real_name, email, phone, dept_id, status, remark)
                VALUES
                    (%s, %s, %s, %s, %s, %s, '0', '校级管理员')
            """
            cursor.execute(sql_user, (
                ADMIN_USERNAME,
                admin_hash,
                ADMIN_REAL_NAME,
                ADMIN_EMAIL,
                ADMIN_PHONE,
                ADMIN_DEPT_ID
            ))
            print(f"管理员账号 '{ADMIN_USERNAME}' 创建成功")

        # 查询 admin 用户 ID
        cursor.execute("SELECT id FROM sys_user WHERE username = %s", (ADMIN_USERNAME,))
        user_id = cursor.fetchone()[0]

        # 绑定 admin 角色
        cursor.execute("""
            INSERT IGNORE INTO sys_user_role (user_id, role_id)
            VALUES (%s, %s)
        """, (user_id, role_id))
        print(f"✓ 角色 'admin' 绑定成功")

        conn.commit()
        print()
        print("=" * 50)
        print("管理员账号创建完成！")
        print(f"  用户名：{ADMIN_USERNAME}")
        print(f"  密码：123456")
        print(f"  姓名：{ADMIN_REAL_NAME}")
        print("=" * 50)

    except pymysql.Error as e:
        print(f"数据库错误：{e}")
    finally:
        if 'conn' in dir() and conn:
            cursor.close()
            conn.close()


if __name__ == '__main__':
    main()
