# 高校就业数据分析综合平台

一个基于 Spring Boot + Vue 3 的高校就业数据分析系统，支持学生求职管理、教师就业审核、数据分析统计、岗位采集与智能推荐等功能。项目同时包含可选的 FastAPI TF-IDF 推荐服务。

> 本仓库中的账号和业务数据均用于本地演示。生产环境请使用独立数据库、强密码和随机生成的 JWT 密钥。

---

## 项目架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3)                          │
│           http://localhost:3000                              │
└──────────────────────────┬────────────────────────────────────┘
                           │ HTTP / REST
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                    后端 (Spring Boot)                        │
│              http://localhost:8080                           │
│                                                              │
│   ┌──────────────────┐    ┌─────────────────────────────┐   │
│   │   MySQL 数据库     │    │     Redis 缓存 / Session    │   │
│   │  employment_db    │    │        localhost:6379       │   │
│   └──────────────────┘    └─────────────────────────────┘   │
└──────────────────────────┬────────────────────────────────────┘
                           │ HTTP (POST /similarity)
                           ▼
┌──────────────────────────────────────────────────────────────┐
│              Python 推荐服务 (FastAPI)                        │
│              http://localhost:8000                           │
│   (TF-IDF 文本相似度推荐算法，仅在使用该算法时需要启动)        │
└──────────────────────────────────────────────────────────────┘
```

---

## 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端运行必需 |
| Maven | 3.6+ | 编译后端 |
| Node.js | 18+ | 前端运行必需 |
| MySQL | 8.0+ | 数据库，需提前创建空库 `employment_db` |
| Redis | 6.0+ | 缓存和会话，需启动 |
| Python | 3.9+ | 仅 TF-IDF 推荐算法需要 |
| pip | 最新版 | 安装 Python 依赖 |

---

## 启动步骤

### 1. 初始化数据库

```sql
-- 登录 MySQL，创建空数据库
CREATE DATABASE employment_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

数据库表结构可由 JPA 自动创建（`application.yml` 中配置了 `ddl-auto: update`），也可按需执行 `sql/init.sql` 初始化。

启动前至少设置数据库密码和 JWT 密钥。PowerShell 示例：

```powershell
$env:DB_PASSWORD = "你的 MySQL 密码"
$env:JWT_SECRET = "长度足够的随机密钥"
```

可用环境变量：`DB_URL`、`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`JWT_SECRET`、`JWT_EXPIRATION`、`UPLOAD_PATH`、`UPLOAD_BASE_URL`。`sql/` 下的 Python 数据脚本会复用 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME` 和 `DB_PASSWORD`。

### 2. 启动 Redis

```bash
# Windows
redis-server

# Linux / Mac
redis-server --daemonize yes
```

### 3. 启动后端（Spring Boot）

在 IDEA 中直接运行主类，或在项目根目录执行：

```bash
mvn spring-boot:run
```

也可以先编译再运行：

```bash
./mvnw clean package -DskipTests
java -jar target/employment-analysis-system-1.0.0.jar
```

后端启动后访问：**http://localhost:8080**

> **注意**：仓库不保存数据库密码。请通过 `DB_PASSWORD` 环境变量传入本机 MySQL 密码。

### 4. 启动前端（Vue）

```bash
cd employment_analysis_system_ui
npm install
npm run dev
```

前端启动后访问：**http://localhost:3000**

### 5. 启动 Python 推荐服务（可选）

仅在使用「文本相似度（TF-IDF）」推荐算法时才需要启动。如果只用默认的「规则打分」算法，可以跳过此步骤。

```bash
cd recommend_service
pip install -r requirements.txt
uvicorn main:app --port 8000
```

> Python 服务启动后，后端会在需要时自动调用。确保端口 `8000` 未被占用。

---

## 默认测试账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | 123456 | 系统管理 |
| 数据分析员 | analyst | 123456 | 就业率分析、爬虫管理 |
| 辅导员 | teacher1 | 123456 | 审核学生就业材料 |
| 院系老师 | dept_teacher1 | 123456 | 院系级数据统计 |
| 学生 | student1 | 123456 | 求职投递、查看推荐 |

> 演示账号由 `sql/create_admin.py`、`sql/create_datastaff.py`、`sql/import_teachers.py` 等脚本生成。首次登录后请立即修改密码，生产环境不要使用这些默认账号。

---

## 功能模块说明

| 模块 | 路径 | 角色 | 说明 |
|------|------|------|------|
| 学生求职 | /student/* | student | 简历管理、职位搜索、投递记录 |
| 就业去向 | /student/employment | student | 就业信息登记 |
| 数据查看申请 | /student/data-permission | student | 申请查看他届就业数据 |
| 职位推荐 | /student/recommendation | student | 智能推荐岗位 |
| 老师审核 | /teacher/* | teacher | 材料审核、协议管理 |
| 就业统计 | /teacher/statistics | teacher | 本班就业率 |
| 数据审批 | /teacher/data-approval | teacher | 审批学生数据查看申请 |
| 就业率分析 | /data/employment-rate | employment_staff | 全校/各院系/各班级统计 |
| 爬虫管理 | /data/spider | employment_staff | 职位数据采集 |
| 推荐算法 | /data/recommendation | employment_staff | 算法参数配置与评估 |

---

## 常见问题

### 1. 前端启动报错 ` Cannot find module`
```bash
cd employment_analysis_system_ui
npm install
```

### 2. 后端启动报错 `Communications link failure`
确认 MySQL 服务已启动，且 `application.yml` 中的 `url`、`username`、`password` 与本地一致。

### 3. 登录提示 `Redis 连接异常`
确认 Redis 服务已启动，默认端口 `6379`，无密码。

### 4. 推荐算法无法使用
确保 Python 服务已启动在 `localhost:8000`，且 `recommend_service/requirements.txt` 中的依赖已安装。

### 5. 地图（ECharts 中国地图）无法显示
地图依赖 `/china.json` 文件，由前端自行从 `/china.json` 加载。确保前端项目根目录（`public/` 或通过代理）存在该文件，或检查网络加载情况。

---

## 许可证

本项目采用 [MIT License](LICENSE) 开源。
