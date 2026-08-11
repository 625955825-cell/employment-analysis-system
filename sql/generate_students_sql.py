# -*- coding: utf-8 -*-
"""
批量生成学生账号 SQL 脚本（纯 SQL 输出方案）

功能：
  - 从数据库 sys_class 读取800个班级（5届×41专业×4班）
  - 生成纯 SQL INSERT 语句，写入 batch_students.sql 文件
  - 用户手动通过 MySQL 命令执行生成的 .sql 文件
  - 绕开 Python executemany 的 tuple 字段数错位 bug

前置条件：
  1. import_companies.py（200家企业+585个职位）
  2. import_class_teachers.py（800个班主任账号）
  3. 再运行本脚本：python generate_students_sql.py
  4. 最后执行：mysql -u root -p employment_db < batch_students.sql
"""

import pymysql
import random
import time
import os
from typing import List, Tuple, Dict

from db_config import DB_CONFIG

# =========================== 可配置参数 ===========================
RANDOM_SEED = 42
STUDENTS_MIN = 40
STUDENTS_MAX = 45
# =========================== 可配置参数 ===========================

# =========================== 姓名库 ===========================
XING = [
    "王", "李", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴",
    "徐", "孙", "胡", "朱", "高", "林", "何", "郭", "马", "罗",
    "梁", "宋", "郑", "谢", "韩", "唐", "冯", "于", "董", "萧",
    "程", "曹", "袁", "邓", "许", "傅", "沈", "曾", "彭", "吕",
    "苏", "卢", "蒋", "蔡", "贾", "丁", "魏", "薛", "叶", "阎",
    "余", "潘", "杜", "戴", "夏", "钟", "汪", "田", "任", "姜",
    "范", "方", "石", "姚", "谭", "廖", "邹", "熊", "金", "陆",
    "郝", "孔", "白", "崔", "康", "毛", "邱", "秦", "江", "史",
]

MING_MALE = [
    "浩", "宇", "博", "霖", "俊杰", "子轩", "子涵", "晨曦", "明辉", "伟",
    "强", "杰", "涛", "超", "勇", "鹏", "飞", "龙", "凯", "文",
    "华", "磊", "波", "峰", "刚", "平", "辉", "军", "洋", "健",
    "志", "豪", "睿", "祥", "瑞", "旭", "昊", "然", "彬", "恒",
    "泽", "浩宇", "浩然", "梓轩", "宇航", "志远", "志强", "文博", "子豪", "天宇",
]

MING_FEMALE = [
    "欣怡", "思雨", "雨萱", "思琪", "欣悦", "雨彤", "思涵", "诗涵", "雅婷", "雅静",
    "雅欣", "雅楠", "欣", "怡", "婷", "琳", "敏", "静", "丽", "艳",
    "娟", "芳", "兰", "洁", "佳", "嘉", "慧", "文静", "诗雨", "雨欣",
    "雨洁", "晓", "颖", "蕾", "燕", "霞", "红", "云", "梅", "莲",
    "倩", "瑶", "丹", "凤", "珍", "翠", "华", "玲", "娜", "秀",
]

# =========================== 籍贯 ===========================
PROVINCES_WEIGHTED = [
    ("贵州省", 30), ("湖北省", 22), ("湖南省", 15), ("河南省", 10),
    ("广东省", 8), ("江西省", 5), ("安徽省", 4),
    ("四川省", 3), ("其他", 3),
]

PROVINCE_CITIES: Dict[str, List[str]] = {
    "贵州省": ["贵阳市", "遵义市", "六盘水市", "安顺市", "毕节市", "铜仁市", "黔西南州", "黔东南州", "黔南州"],
    "湖北省": ["武汉市", "宜昌市", "襄阳市", "荆州市", "黄石市", "十堰市", "孝感市", "黄冈市", "咸宁市"],
    "湖南省": ["长沙市", "株洲市", "湘潭市", "衡阳市", "岳阳市", "常德市", "益阳市", "郴州市", "永州市"],
    "河南省": ["郑州市", "洛阳市", "开封市", "新乡市", "南阳市", "许昌市", "安阳市", "平顶山市", "商丘市"],
    "广东省": ["广州市", "深圳市", "东莞市", "佛山市", "珠海市", "中山市", "惠州市", "江门市", "湛江市"],
    "江西省": ["南昌市", "赣州市", "九江市", "上饶市", "宜春市", "吉安市", "抚州市", "景德镇市", "萍乡市"],
    "安徽省": ["合肥市", "芜湖市", "蚌埠市", "淮南市", "马鞍山市", "安庆市", "阜阳市", "滁州市", "六安市"],
    "四川省": ["成都市", "绵阳市", "德阳市", "宜宾市", "南充市", "泸州市", "达州市", "乐山市", "内江市"],
    "其他": ["北京市", "上海市", "天津市", "重庆市", "河北省", "山西省", "辽宁省",
             "吉林省", "黑龙江省", "江苏省", "浙江省", "福建省", "山东省", "海南省"],
}

# =========================== 民族 ===========================
NATION_WEIGHTED = [
    ("汉族", 65),
    ("苗族", 8), ("土家族", 6), ("布依族", 5), ("侗族", 4),
    ("彝族", 3), ("仡佬族", 2.5), ("水族", 2),
    ("瑶族", 1.5), ("壮族", 1.5), ("毛南族", 0.5),
]

MINORITY_OTHER = ["湖南省", "云南省", "广西壮族自治区", "四川省"]

# =========================== 政治面貌 ===========================
POLITICS_WEIGHTED = [
    ("共青团员", 72), ("中共党员", 12), ("中共预备党员", 3),
    ("群众", 10), ("入党积极分子", 3),
]

STREET_SUFFIXES = ["XX路XX号", "XX街XX小区", "XX镇XX村", "XX区XX路XX号"]
DORMITORY_PATTERNS = [
    "{:02d}号楼{:03d}室", "{:02d}栋{:03d}室",
    "{:02d}号楼{:d}层{:03d}室", "{:02d}公寓{:03d}室"
]
EMERGENCY_RELATIONS = ["父亲", "母亲", "爷爷", "奶奶", "外公", "外婆", "叔叔", "姑姑"]

# =========================== 就业 ===========================
EMPLOYMENT_RATE = {2018: 0.94, 2019: 0.94, 2020: 0.94, 2021: 0.94, 2022: 0.83}
EMPLOYMENT_TYPE_WEIGHTED = [
    ("签订三方协议", 52), ("继续深造", 12), ("签订劳动合同", 20),
    ("自由职业", 7), ("出国出境", 2), ("应征入伍", 1)
]

# =========================== 专业 → 行业关键词 ===========================
MAJOR_INDUSTRY_KEYWORDS: Dict[str, List[str]] = {
    "资源勘查工程": ["矿产", "地质勘查", "矿业"],
    "环境工程": ["环保", "环境"],
    "地质工程": ["地质", "岩土"],
    "机械设计制造及其自动化": ["机械", "制造", "自动化"],
    "机械电子工程": ["机电", "自动化", "电子"],
    "电气工程及其自动化": ["电气", "电力", "自动化"],
    "自动化": ["自动化", "控制"],
    "土木工程": ["建筑", "土木", "工程"],
    "水利水电工程": ["水利", "水电"],
    "道路桥梁与渡河工程": ["桥梁", "道路", "交通"],
    "化学工程与工艺": ["化工", "化学"],
    "新能源科学与工程": ["新能源", "光伏", "风电"],
    "过程装备与控制工程": ["化工装备", "压力容器"],
    "制药工程": ["制药", "医药"],
    "生物制药": ["生物制药", "医药"],
    "酿酒工程": ["酿酒", "食品"],
    "安全工程": ["安全", "应急"],
    "采矿工程": ["采矿", "矿业"],
    "智能采矿工程": ["采矿", "智能矿山"],
    "工程管理": ["工程管理", "建筑"],
    "工程造价": ["造价", "建筑"],
    "投资学": ["金融", "投资"],
    "网络工程": ["网络", "通信"],
    "数据科学与大数据技术": ["数据", "IT", "互联网"],
    "智能科学与技术": ["人工智能", "IT"],
    "网络空间安全": ["网络安全", "信息安全"],
    "飞行器制造工程": ["航空", "航天"],
    "飞行器动力工程": ["航空发动机", "航天"],
    "人文地理与城乡规划": ["规划", "地理"],
    "环境设计": ["设计", "环境"],
    "建筑学": ["建筑", "设计"],
    "材料科学与工程": ["材料", "金属"],
    "新能源材料与器件": ["新能源材料", "电池"],
    "焊接技术与工程": ["焊接", "制造"],
    "交通工程": ["交通", "道路"],
    "交通运输": ["交通", "运输"],
    "应用统计学": ["统计", "数据分析"],
    "应用物理学": ["物理", "光电"],
    "休闲体育": ["体育", "健身"],
    "英语": ["英语", "翻译", "教育"],
}

# =========================== 项目经验池（按专业） ===========================
PROJECTS: Dict[str, List[str]] = {
    "资源勘查工程": ["贵州省某铅锌矿地质勘查", "矿区土壤重金属污染调查", "某水库大坝地质勘察", "岩溶地区隧道工程地质调查", "城市地下管线探测"],
    "环境工程": ["城市污水处理厂工艺设计", "某工业园区环境影响评价", "农村生活污水治理方案", "城市空气质量监测分析", "固体废物处理厂设计"],
    "地质工程": ["某高速公路边坡稳定性分析", "岩溶地区隧道工程地质勘察", "滑坡灾害危险性评估", "某矿山岩体力学测试", "城市地质信息系统设计"],
    "机械设计制造及其自动化": ["机械零件设计及数控加工", "自动化生产线上料系统设计", "工业机器人末端夹具设计", "三维建模及工艺分析", "机电系统综合设计"],
    "机械电子工程": ["基于PLC的自动化生产线控制系统设计", "智能小车循迹与避障系统设计", "工业传感器数据采集系统", "基于单片机的温湿度监测系统", "自动化立体仓库控制设计"],
    "电气工程及其自动化": ["基于PLC的电机控制系统设计", "某工厂配电系统设计", "智能配电箱监控系统", "工厂变配电所综合自动化设计", "智能照明控制系统"],
    "自动化": ["小型过程控制实验装置设计", "DCS控制系统组态与调试", "工业锅炉自动控制系统", "基于PID的温度控制系统设计", "工业机器人搬运系统集成"],
    "土木工程": ["某框架结构教学楼设计", "混凝土配合比设计及性能测试", "建筑施工组织设计", "高层建筑结构设计", "土方工程量计算及施工方案"],
    "水利水电工程": ["某水库除险加固设计方案", "水电站厂房结构设计", "灌溉渠道设计", "大坝渗流分析", "水闸结构设计"],
    "道路桥梁与渡河工程": ["某城市道路交叉口设计", "预应力混凝土简支梁桥设计", "桥梁施工组织设计", "隧道施工方案设计", "挡土墙设计计算"],
    "化学工程与工艺": ["年产5万吨甲醇合成工艺设计", "反应精馏分离工艺开发", "化工单元操作实验装置设计", "精馏塔工艺设计", "换热器设计与优化"],
    "新能源科学与工程": ["家用光伏发电系统设计", "小型风力发电机组设计", "储能电池管理系统设计", "光伏组件发电效率分析", "智能微电网系统设计"],
    "过程装备与控制工程": ["换热器设计及工艺计算", "压力容器壳体设计", "化工管路系统设计", "储罐设计及强度校核", "塔器结构设计"],
    "制药工程": ["口服固体制剂工艺优化", "某药品生产车间设计", "药物中间体合成工艺", "中药提取工艺研究", "药品质量分析方法开发"],
    "生物制药": ["重组蛋白药物生产工艺设计", "单克隆抗体分离纯化工艺", "疫苗生产质量控制研究", "细胞培养工艺优化", "酶制剂生产工艺开发"],
    "酿酒工程": ["酱香型白酒酿造工艺优化", "葡萄酒发酵工艺设计", "酿酒副产品综合利用", "酒类品质检测与分析", "新型果酒酿造工艺研究"],
    "安全工程": ["某煤矿安全评价报告", "化工园区多米诺效应分析", "职业病危害因素检测与评价", "企业应急预案编制", "危化品储存安全评估"],
    "采矿工程": ["某地下煤矿开采方案设计", "露天矿台阶爆破参数设计", "矿井通风系统优化设计", "矿山充填系统设计", "采矿方法选择与设计"],
    "智能采矿工程": ["基于物联网的矿山安全监测系统", "智能矿山数据平台设计", "无人采矿工作面控制系统", "矿山车辆调度系统开发", "矿山通风智能控制设计"],
    "工程管理": ["某住宅小区施工项目管理策划", "建设工程项目进度计划编制", "工程项目成本控制分析", "施工安全管理方案设计", "房地产开发项目可行性分析"],
    "工程造价": ["某办公楼工程量清单编制", "建设工程造价指标分析", "施工图预算编制", "投标报价策略分析", "工程变更与索赔分析"],
    "投资学": ["上市公司股票投资价值分析", "投资组合优化模型构建", "某基金项目可行性分析", "企业估值模型构建", "债券投资风险分析"],
    "网络工程": ["某企业网络架构设计与实施", "校园网安全防护方案", "云计算平台网络规划", "中小企业信息化建设方案", "网络性能监控与优化"],
    "数据科学与大数据技术": ["基于Spark的学生就业数据分析", "电商用户行为数据挖掘", "城市交通流量预测系统", "社交媒体舆情分析系统", "金融风控模型构建"],
    "智能科学与技术": ["图像识别在智能交通中的应用", "自然语言处理情感分析系统", "智能推荐算法在求职平台中的实现", "车牌识别系统开发", "智能问答机器人设计"],
    "网络空间安全": ["Web应用安全漏洞检测与加固", "企业网络安全防护方案", "渗透测试与安全评估", "数据加密与隐私保护方案", "工控系统安全评估"],
    "飞行器制造工程": ["某型无人机机身结构设计", "航空发动机叶片工艺分析", "飞机装配工艺规划", "航空结构复合材料应用分析", "数字化装配工艺设计"],
    "飞行器动力工程": ["航空发动机性能仿真分析", "某型燃气轮机燃烧室设计", "发动机试验数据采集与分析", "航空发动机振动分析", "涡轮叶片冷却结构设计"],
    "人文地理与城乡规划": ["某县级市国土空间规划", "历史文化街区保护与更新设计", "城市公园景观规划设计", "某乡镇总体规划设计", "城市更新改造规划方案"],
    "环境设计": ["商业综合体室内空间设计", "民宿设计方案", "城市家具设计", "办公空间设计方案", "餐饮空间室内设计"],
    "建筑学": ["某文化中心建筑设计", "山地建筑设计", "旧建筑改造设计", "住宅小区规划设计", "校园建筑群设计方案"],
    "材料科学与工程": ["某钢材焊接工艺评定", "铝合金热处理工艺优化", "建筑材料耐久性研究", "金属材料腐蚀行为分析", "复合材料力学性能测试"],
    "新能源材料与器件": ["钙钛矿太阳能电池制备与性能优化", "锂电池正极材料研究", "燃料电池关键材料分析", "超级电容器电极材料制备", "光伏组件可靠性测试"],
    "焊接技术与工程": ["某钢结构焊接工艺评定", "铝合金MIG焊接工艺研究", "焊接机器人编程与调试", "管道自动焊接工艺开发", "焊接缺陷分析与质量控制"],
    "交通工程": ["城市交叉口信号配时优化", "某区域交通组织设计", "交通影响评价报告", "公交线路优化设计", "停车场交通组织设计"],
    "交通运输": ["地铁车站客流组织方案", "列车运行图编制与调整", "城市轨道交通运力评估", "轨道交通应急预案设计", "车站设备调度优化"],
    "应用统计学": ["某地区居民收入统计分析", "大学生就业情况调查数据分析", "时间序列预测模型构建", "上市公司财务风险预警模型", "市场调研数据分析报告"],
    "应用物理学": ["光学薄膜设计与制备", "LED光源性能测试与分析", "光电传感器特性研究", "光纤通信系统性能分析", "太阳能电池转换效率研究"],
    "休闲体育": ["马拉松赛事组织与管理方案", "青少年体育培训课程设计", "社区体育活动策划", "健身房运营管理方案", "体育赛事商业赞助策划"],
    "英语": ["某企业英文宣传材料翻译", "跨文化商务沟通案例分析", "旅游景区英语导览系统设计", "商务合同英汉互译实践", "国际会议同声传译模拟"],
}

# =========================== 技术栈池（按专业） ===========================
SKILL_POOLS: Dict[str, List[str]] = {
    "资源勘查工程": ["MapGIS", "AutoCAD", "ArcGIS", "QGIS", "ENVI", "Surfer", "3DMine", "Dimine"],
    "环境工程": ["AutoCAD", "CADWorx", "MATLAB", "HEC-RAS", "SWMM", "AERMOD", "WQAM", "Python"],
    "地质工程": ["AutoCAD", "Plaxis", "MIDAS", "ANSYS", "FLAC3D", "理正软件", "PKPM", "GEO5"],
    "机械设计制造及其自动化": ["SolidWorks", "AutoCAD", "CATIA", "Pro/E", "MasterCAM", "UG", "ANSYS", "PLC编程"],
    "机械电子工程": ["Altium Designer", "Keil", "Proteus", "MATLAB/Simulink", "PLC", "EPLAN", "电路板设计", "嵌入式C"],
    "电气工程及其自动化": ["MATLAB", "AutoCAD Electrical", "PLC", "EPLAN", "ETAP", "PSCAD", "RTDS", "电气仿真"],
    "自动化": ["MATLAB/Simulink", "PLC", "DCS", "WinCC", "IFIX", "组态王", "PID参数整定", "OPC通讯"],
    "土木工程": ["PKPM", "盈建科", "Midas", "SAP2000", "ANSYS", "Revit", "Navisworks", "BIM"],
    "水利水电工程": ["AutoCAD", "PKPM", "Midas", "HEC-RAS", "MIKE", "GIS", "水文计算", "水利工程概算"],
    "道路桥梁与渡河工程": ["纬地软件", "鸿业软件", "MIDAS", "桥梁博士", "ANSYS", "SAP2000", "Autocad", "Civil 3D"],
    "化学工程与工艺": ["Aspen Plus", "HYSYS", "PRO/II", "MATLAB", "ASPEN", "化工设计软件", "HazardPro", "LIMS"],
    "新能源科学与工程": ["PVsyst", "HOMER", "MATLAB", "ETAP", "Comsol", "Python", "Excel建模", "能源仿真"],
    "过程装备与控制工程": ["Aspen Plus", "CADWorx", "PV Elite", "ANSYS", "COMSOL", "SW6", "压力容器设计", "GMP规范"],
    "制药工程": ["AutoCAD", "GMP", "FDA法规", "药物分析软件", "HPLC", "GC-MS", "工艺流程图", "GXP"],
    "生物制药": ["HPLC", "GC-MS", "PCR", "流式细胞仪", "Western Blot", "细胞培养", "蛋白质纯化", "ELISA"],
    "酿酒工程": ["气相色谱", "品酒分析", "HPLC", "发酵工艺", "食品安全检测", "QS认证", "CAD", "酿酒工艺设计"],
    "安全工程": ["AQ/T9006", "JSA", "HAZOP", "LOPA", "SIL定级", "风险矩阵", "安全评价软件", "应急预案编制"],
    "采矿工程": ["Dimine", "3DMine", "Surpac", "FLAC3D", "ANSYS", "CAD", "矿井通风模拟", "采矿CAD"],
    "智能采矿工程": ["Python", "MATLAB", "机器视觉", "工业机器人", "PLC", "SCADA", "工业物联网", "数字孪生"],
    "工程管理": ["Project", "P6", "Revit", "BIM5D", "斑马进度计划", "广联达", "梦龙", "横道图软件"],
    "工程造价": ["广联达", "鲁班软件", "斯维尔", "PKPM", "BIM算量", "Excel", "定额软件", "清单计价软件"],
    "投资学": ["Wind终端", "Python", "MATLAB", "R语言", "EViews", "SPSS", "Bloomberg", "量化交易平台"],
    "网络工程": ["Wireshark", "Cisco Packet Tracer", "eNSP", "GNS3", "Linux", "Nessus", "WAF", "防火墙配置"],
    "数据科学与大数据技术": ["Python", "Spark", "Hadoop", "Hive", "SQL", "Pandas", "NumPy", "Scikit-learn", "TensorFlow", "Pycharm", "Jupyter"],
    "智能科学与技术": ["Python", "TensorFlow", "PyTorch", "OpenCV", "Keras", "NLTK", "SpaCy", "Jupyter", "Scikit-learn"],
    "网络空间安全": ["Nmap", "Burp Suite", "Metasploit", "Wireshark", "Kali Linux", "SQL注入工具", "逆向分析", "密码破解工具"],
    "飞行器制造工程": ["CATIA", "UG", "SolidWorks", "ABAQUS", "ANSYS", "数字化装配", "复合材料设计", "航电系统"],
    "飞行器动力工程": ["ANSYS", "Fluent", "GT-Power", "MATLAB", "发动机仿真", "燃烧模拟", "叶轮机械设计", "CFD"],
    "人文地理与城乡规划": ["ArcGIS", "AutoCAD", "Photoshop", "GIS空间分析", "GIS可视化", "ENVI", "QGIS", "规划软件"],
    "环境设计": ["AutoCAD", "3ds Max", "SketchUp", "V-Ray", "Photoshop", "Lumion", "Enscape", "VRay渲染"],
    "建筑学": ["Rhino", "Grasshopper", "Revit", "AutoCAD", "V-Ray", "Lumion", "SketchUp", "BIM"],
    "材料科学与工程": ["ANSYS", "JMatPro", "Origin", "金相分析", "热处理工艺", "材料力学测试", "SEM分析", "XRD分析"],
    "新能源材料与器件": ["电化学工作站", "SEM", "XRD", "电化学测试", "电池测试系统", "Materials Studio", "Comsol", "涂布工艺"],
    "焊接技术与工程": ["焊接工艺评定", "无损检测", "UT", "PT", "MT", "RT", "焊接技能证书", "焊接工艺规程"],
    "交通工程": ["TransCAD", "VISSIM", "EMME", "Visum", "MATLAB", "TransModeler", "交通仿真", "CAD"],
    "交通运输": ["地铁信号系统", "列车运行控制", "OCC调度系统", "ATS系统", "综合监控", "PLC", "SCADA", "信号基础"],
    "应用统计学": ["SPSS", "Stata", "R语言", "EViews", "SAS", "Excel", "Python", "Tableau"],
    "应用物理学": ["MATLAB", "Origin", "LabVIEW", "Comsol", "OptiFDTD", "Zemax", "物理学实验", "光电检测"],
    "休闲体育": ["体育管理", "赛事运营", "健身指导", "运动康复", "体育营销", "户外拓展", "赛事策划", "体能测试"],
    "英语": ["翻译软件", "CAT工具", "Trados", "跨文化交际", "TESOL", "语言测试", "口译技巧", "商务英语"],
}

# =========================== 证书池（按专业） ===========================
CERT_POOLS: Dict[str, List[str]] = {
    "default": ["CET-4证书", "CET-6证书", "计算机二级Java证书", "普通话二级甲等证书"],
    "资源勘查工程": ["CET-4证书", "CET-6证书", "计算机二级C++证书", "地质工程师基础考试合格证书", "MapGIS认证证书"],
    "环境工程": ["CET-4证书", "CET-6证书", "计算机二级证书", "环境影响评价工程师基础考试合格证书", "普通话二级甲等证书"],
    "地质工程": ["CET-4证书", "CET-6证书", "计算机二级证书", "岩土工程师基础考试合格证书", "普通话二级甲等证书"],
    "机械设计制造及其自动化": ["CET-4证书", "CET-6证书", "CAD工程师认证证书", "计算机二级C语言证书", "数控车工证书"],
    "机械电子工程": ["CET-4证书", "CET-6证书", "PLC工程师证书", "计算机二级C++证书", "嵌入式系统设计师证书"],
    "电气工程及其自动化": ["CET-4证书", "CET-6证书", "电工职业资格证书（高级）", "计算机二级C语言证书", "电气工程师证书"],
    "自动化": ["CET-4证书", "CET-6证书", "PLC工程师证书", "计算机二级证书", "DCS工程师证书"],
    "土木工程": ["CET-4证书", "CET-6证书", "CAD工程师认证证书", "二级建造师考试合格证书", "BIM一级证书"],
    "水利水电工程": ["CET-4证书", "CET-6证书", "CAD工程师认证证书", "水利水电工程师基础考试合格证书", "BIM证书"],
    "道路桥梁与渡河工程": ["CET-4证书", "CET-6证书", "CAD工程师认证证书", "桥梁工程师基础考试合格证书", "BIM证书"],
    "化学工程与工艺": ["CET-4证书", "CET-6证书", "计算机二级证书", "化学工程师基础考试合格证书", "普通话二级甲等证书"],
    "新能源科学与工程": ["CET-4证书", "CET-6证书", "光伏工程师证书", "计算机二级Python证书", "电工职业资格证书"],
    "过程装备与控制工程": ["CET-4证书", "CET-6证书", "计算机二级证书", "压力容器作业证书", "普通话二级甲等证书"],
    "制药工程": ["CET-4证书", "CET-6证书", "计算机二级证书", "GMP内审员证书", "普通话二级甲等证书"],
    "生物制药": ["CET-4证书", "CET-6证书", "HPLC操作证书", "计算机二级证书", "细胞培养技术证书"],
    "酿酒工程": ["CET-4证书", "CET-6证书", "品酒师证书", "计算机二级证书", "食品安全员证书"],
    "安全工程": ["CET-4证书", "CET-6证书", "安全员证书", "计算机二级证书", "应急救援员证书"],
    "采矿工程": ["CET-4证书", "CET-6证书", "计算机二级证书", "采矿工程师基础考试合格证书", "通风安全员证书"],
    "智能采矿工程": ["CET-4证书", "CET-6证书", "Python程序员证书", "计算机二级证书", "物联网应用证书"],
    "工程管理": ["CET-4证书", "CET-6证书", "计算机二级证书", "二级建造师考试合格证书", "BIM一级证书"],
    "工程造价": ["CET-4证书", "CET-6证书", "计算机二级证书", "一级造价工程师基础考试合格证书", "广联达技能证书"],
    "投资学": ["CET-4证书", "CET-6证书", "计算机二级证书", "证券从业资格证", "基金从业资格证"],
    "网络工程": ["CET-4证书", "CET-6证书", "HCIA证书", "计算机二级证书", "网络工程师证书"],
    "数据科学与大数据技术": ["CET-4证书", "CET-6证书", "计算机二级Python证书", "数据分析证书", "CDA数据分析师证书"],
    "智能科学与技术": ["CET-4证书", "CET-6证书", "计算机二级Python证书", "人工智能工程师证书", "TensorFlow开发者证书"],
    "网络空间安全": ["CET-4证书", "CET-6证书", "计算机二级C语言证书", "CISP证书", "NISP证书"],
    "飞行器制造工程": ["CET-4证书", "CET-6证书", "计算机二级证书", "适航证书", "CATIA证书"],
    "飞行器动力工程": ["CET-4证书", "CET-6证书", "计算机二级证书", "航空发动机基础证书", "ANSYS证书"],
    "人文地理与城乡规划": ["CET-4证书", "CET-6证书", "计算机二级证书", "城乡规划师基础证书", "ArcGIS证书"],
    "环境设计": ["CET-4证书", "CET-6证书", "计算机二级证书", "室内设计师证书", "AutoCAD证书"],
    "建筑学": ["CET-4证书", "CET-6证书", "计算机二级证书", "建筑学基础证书", "Revit证书"],
    "材料科学与工程": ["CET-4证书", "CET-6证书", "计算机二级证书", "材料工程师基础证书", "金相检验证书"],
    "新能源材料与器件": ["CET-4证书", "CET-6证书", "计算机二级证书", "电池工程师证书", "电化学测试证书"],
    "焊接技术与工程": ["CET-4证书", "CET-6证书", "计算机二级证书", "焊接工程师证书", "无损检测证书"],
    "交通工程": ["CET-4证书", "CET-6证书", "计算机二级证书", "交通工程师基础证书", "TransCAD证书"],
    "交通运输": ["CET-4证书", "CET-6证书", "计算机二级证书", "轨道交通信号证书", "城市轨道交通客运员证书"],
    "应用统计学": ["CET-4证书", "CET-6证书", "计算机二级证书", "SPSS证书", "统计师基础考试合格证书"],
    "应用物理学": ["CET-4证书", "CET-6证书", "计算机二级证书", "物理实验技能证书", "MATLAB证书"],
    "休闲体育": ["CET-4证书", "CET-6证书", "社会体育指导员证书", "计算机二级证书", "普通话二级甲等证书"],
    "英语": ["CET-4证书", "CET-6证书", "TEM-4证书", "计算机二级证书", "教师资格证（英语）"],
}

# =========================== 自我评价模板 ===========================
SELF_EVAL_TEMPLATES = [
    ("积极型", [
        "本人性格开朗，学习能力强，具备良好的团队协作能力。",
        "在校期间认真学习专业知识，积极参与实践活动，综合素质较好。",
        "毕业后希望从事与本专业相关的工作。",
    ]),
    ("沉稳型", [
        "本人踏实稳重，学习态度端正，专业基础扎实。",
        "在校期间积极参与课程实践和实习，具备一定的工程实践能力。",
        "期望毕业后从事与专业相关的工作，在岗位上持续成长。",
    ]),
    ("进取型", [
        "本人思维活跃，动手能力强，善于将理论知识应用于实践。",
        "在校期间认真学习专业知识，积极参加学科竞赛和创新项目。",
        "期望在专业相关领域发展，为企业创造价值。",
    ]),
    ("务实型", [
        "本人认真负责，吃苦耐劳，具备较强的学习能力和适应能力。",
        "在校期间认真学习专业知识，积极参与实习实践，积累了一定的专业经验。",
        "期望毕业后能找到与专业匹配的岗位，脚踏实地，从基层做起。",
    ]),
]


# =========================== 工具函数 ===========================

def weighted_choice(choices: List[Tuple[str, float]]) -> str:
    total = sum(w for _, w in choices)
    r = random.uniform(0, total)
    cur = 0.0
    for item, w in choices:
        cur += w
        if r <= cur:
            return item
    return choices[-1][0]


def weighted_choice_int(choices: List[Tuple[str, int]]) -> str:
    total = sum(w for _, w in choices)
    r = random.random() * total
    cur = 0
    for item, w in choices:
        cur += w
        if r < cur:
            return item
    return choices[-1][0]


def pick_n_random(population: List, n: int) -> List:
    return random.sample(population, min(n, len(population)))


def generate_name(gender: str) -> str:
    surname = random.choice(XING)
    ming = random.choice(MING_MALE if gender == "男" else MING_FEMALE)
    return surname + ming


def generate_phone() -> str:
    prefixes = [
        "130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
        "150", "151", "152", "153", "155", "156", "157", "158", "159",
        "170", "171", "172", "173", "175", "176", "177", "178",
        "180", "181", "182", "183", "184", "185", "186", "187", "188", "189",
        "198", "199",
    ]
    return random.choice(prefixes) + "".join([str(random.randint(0, 9)) for _ in range(8)])


def generate_student_no(grad_year: int, class_id: int, student_seq: int) -> str:
    """学号格式：{毕业年份}{班级ID(4位)}{学生序号(3位)}，如 20182601001"""
    return f"{grad_year}{class_id:04d}{student_seq:03d}"


def generate_id_card(grad_year: int, gender: str) -> str:
    guizhou_codes = [
        "520100", "520101", "520102", "520103", "520104", "520111", "520112", "520113",
        "520121", "520122", "520123", "520181", "520200", "520201", "520203", "520204",
        "520300", "520301", "520302", "520303", "520400", "520500", "520600", "520601", "520602",
    ]
    birth_year = random.randint(grad_year - 24, grad_year - 20)
    birth_month = random.randint(1, 12)
    birth_day = random.randint(1, 28)
    seq = random.randint(1, 999)
    gender_code = seq * 2 + (0 if gender == "男" else 1)
    return (f"{random.choice(guizhou_codes)}{birth_year:04d}{birth_month:02d}"
            f"{birth_day:02d}{seq:03d}{gender_code % 10}")


def generate_province_city() -> Tuple[str, str]:
    province = weighted_choice(PROVINCES_WEIGHTED)
    cities = PROVINCE_CITIES.get(province, ["某市"])
    return province, random.choice(cities)


def generate_conversation_content(conv_type: str) -> Tuple[str, str, str]:
    if conv_type == "就业指导":
        content = (
            "辅导员与学生进行就业意向沟通，了解学生求职进展，"
            "指导学生完善简历、梳理求职方向，提醒学生关注学校发布的招聘信息，"
            "鼓励学生积极参加招聘会，主动投递简历。"
        )
        result = "学生表示将认真修改简历，积极投递，争取早日落实就业单位。"
        next_plan = "继续跟进学生就业情况，预约下次谈话。"
    elif conv_type == "心理疏导":
        content = (
            "学生反映近期求职压力大，情绪低落，辅导员耐心倾听，"
            "帮助学生分析求职受挫原因，调整求职心态，"
            "引导学生正确看待就业形势，鼓励其多尝试、多投递，"
            "同时提醒学生注意身心健康，合理安排求职与生活。"
        )
        result = "学生情绪有所缓解，表示将调整心态，积极面对求职挑战。"
        next_plan = "关注学生心理状态，必要时推荐参加学校心理咨询。"
    elif conv_type == "学业辅导":
        content = (
            "学生反映学业上遇到困难，影响了求职信心，辅导员了解情况后，"
            "针对学生的具体问题提供学习方法指导，"
            "帮助学生制定补修计划，鼓励学生在完成学业的同时积极求职。"
        )
        result = "学生明确了学业和求职的平衡方法，表示将合理安排时间。"
        next_plan = "跟进学生学业完成情况。"
    elif conv_type == "生活关怀":
        content = (
            "学生反映家庭经济困难，影响了毕业季求职安排，辅导员了解情况后，"
            "向学生介绍学校勤工俭学岗位和就业困难帮扶政策，"
            "鼓励学生申请就业补贴，同时帮助学生树立积极向上的生活态度。"
        )
        result = "学生了解了相关帮扶政策，表示将积极申请，生活态度有所改善。"
        next_plan = "跟进帮扶政策落实情况。"
    else:
        content = "辅导员与学生进行日常交流，了解学生近期学习生活情况，关注学生心理健康，引导学生做好毕业规划。"
        result = "学生状态良好，对未来有初步规划。"
        next_plan = "持续关注学生动态。"
    return content, result, next_plan


def generate_conversation_time(grad_year: int, index: int, total: int) -> str:
    start_year = grad_year - 1
    months_before_grad = 12
    month_step = months_before_grad / max(total - 1, 1)
    target_month = int(7 + index * month_step)
    month = ((target_month - 1) % 12) + 1
    year_offset = (target_month - 1) // 12
    year = start_year + year_offset
    day = random.randint(10, 25)
    hour = random.randint(9, 17)
    return f"{year}-{month:02d}-{day:02d} {hour:02d}:00:00"


# =========================== SQL 辅助 ===========================

def sql_val(v) -> str:
    """将 Python 值转换为可安全嵌入 SQL 的字符串（不依赖 executemany）。"""
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "1" if v else "0"
    if isinstance(v, int):
        return str(v)
    if isinstance(v, float):
        return str(v)
    s = str(v)
    # 简单转义：单引号 doubled，\b 替换
    s = s.replace("\\", "\\\\").replace("'", "''").replace("\r", "\\r").replace("\n", "\\n").replace("\b", "\\b")
    return f"'{s}'"


def sql_insert(table: str, fields: List[str], rows: List[tuple]) -> List[str]:
    """生成多条 INSERT INTO table (f1,f2...) VALUES (v1,v2...), (...) ... 语句。"""
    if not rows:
        return []
    field_str = ", ".join(fields)
    stmts = []
    # 每500行一个语句，避免 SQL 太大
    BATCH = 500
    for i in range(0, len(rows), BATCH):
        batch = rows[i:i + BATCH]
        vals_list = [", ".join(sql_val(v) for v in row) for row in batch]
        stmts.append(f"INSERT INTO {table} ({field_str}) VALUES\n    ({vals_list[0]})")
        for v in vals_list[1:]:
            stmts[-1] += f",\n    ({v})"
    return stmts


# =========================== 主程序 ===========================

def main():
    random.seed(RANDOM_SEED)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    sql_out_path = os.path.join(script_dir, "batch_students.sql")

    print("============================================================")
    print("批量生成学生账号 SQL 脚本（纯 SQL 输出方案）")
    print("============================================================")
    print(f"随机种子   : {RANDOM_SEED}")
    print(f"每班人数   : {STUDENTS_MIN}-{STUDENTS_MAX}人")
    print(f"就业类型   : 签三方52%/考研升学12%/签劳动合同20%/灵活就业7%/出国2%/入伍1%")
    print(f"民族规则   : 少数民族99%贵州省籍")
    print("============================================================")

    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    try:
        # =========================== 读取角色 ===========================
        cursor.execute("SELECT id FROM sys_role WHERE role_key = 'student' LIMIT 1")
        row = cursor.fetchone()
        if not row:
            print("错误：未找到 'student' 角色，请先执行 init.sql")
            return
        student_role_id = row[0]
        print(f"✓ 学生角色 ID = {student_role_id}")

        # =========================== 清理旧数据 ===========================
        cursor.execute("DELETE FROM sys_user WHERE remark = '批量导入学生'")
        deleted = cursor.rowcount
        conn.commit()
        print(f"✓ 清理旧数据：删除 {deleted} 个账号")

        # =========================== 读取班级 ===========================
        cursor.execute("""
            SELECT c.id, c.class_name, c.grade, c.dept_id,
                   c.advisor_id, d.dept_name,
                   m.id AS major_id, m.major_name
            FROM sys_class c
            JOIN sys_dept d ON c.dept_id = d.id
            JOIN sys_major m ON c.major_id = m.id
            ORDER BY c.dept_id, c.grade, c.id
        """)
        classes = cursor.fetchall()
        print(f"✓ 班级总数: {len(classes)}")
        if not classes:
            print("错误：没有班级数据")
            return

        # =========================== 读取企业 ===========================
        cursor.execute("""
            SELECT id, company_name, industry, scale, province, city
            FROM company_info
            WHERE auth_status = 'approved' AND status = '0'
        """)
        companies = cursor.fetchall()
        print(f"✓ 已审核企业: {len(companies)} 家")

        # =========================== 读取企业统一社会信用代码 ===========================
        cursor.execute("SELECT id, company_code FROM company_info WHERE company_code IS NOT NULL AND company_code != ''")
        company_code_map = {row[0]: row[1] for row in cursor.fetchall()}
        print(f"✓ 企业统一社会信用代码: {len(company_code_map)} 条")

        # =========================== 读取职位（按专业分类） ===========================
        cursor.execute("""
            SELECT id, job_name, company_id, salary_min, salary_max, work_city, company_scale, company_industry
            FROM job_position
            WHERE status = 'published'
        """)
        positions = cursor.fetchall()
        print(f"✓ 已发布职位: {len(positions)} 个")

        # 职位按行业关键词分组
        industry_jobs: Dict[str, List] = {}
        for pos in positions:
            keywords = []
            for major_name_kw, kw_list in MAJOR_INDUSTRY_KEYWORDS.items():
                for kw in kw_list:
                    if kw in (pos[2] or ""):  # company_industry
                        keywords.append(kw)
            key = keywords[0] if keywords else "其他"
            industry_jobs.setdefault(key, []).append(pos)
        print(f"✓ 行业关键词分组: {len(industry_jobs)} 个行业")

        print("\n开始生成学生数据 ...")

        # =========================== 收集所有 SQL 语句 ===========================
        all_sql_stmts = []

        total_users = 0
        total_classes = len(classes)

        for class_idx, cls in enumerate(classes):
            class_id, class_name, grade_str, dept_id, advisor_id, dept_name, major_id, major_name = cls
            try:
                grad_year = int(grade_str)
            except (ValueError, TypeError):
                grad_year = 2022

            stu_count = random.randint(STUDENTS_MIN, STUDENTS_MAX)
            class_count = class_idx + 1

            # ---- 数据容器 ----
            users = []       # (username, password_hash, real_name, gender, phone, email, id_card, dept_id, major_id, class_name, class_id, student_no, grad_year, status, remark)
            infos = []      # (student_no, real_name, gender, birth_date, id_card, nation, politics_status, phone, email, province, city, address, dept_id, dept_name, major_id, major_name, class_name, class_id, grad_year, study_type, dormitory, emergency_contact, emergency_phone, status)
            user_roles = []  # (user_id, role_id)
            resumes = []     # (student_id, resume_name, is_default, personal_summary, education_experience, project_experience, work_experience, skill_certificates, awards_honors, self_evaluation, expected_salary_min, expected_salary_max, expected_city, expected_position, expected_industry, file_path)
            employments = []  # (student_id, emp_type, company_name, company_code, company_scale, company_industry, position_name, position_category, work_city, work_province, salary, is_three_party_signed, three_party_no, contract_start_date, contract_end_date, probation_salary, audit_status, audit_remark, audit_user_id, audit_time, remark)
            attachments = []  # (employment_id, attachment_type, attachment_name, file_path, file_size, upload_status)
            agreements = []  # (student_id, company_id, agreement_no, student_sign_time, company_sign_time, school_sign_time, file_path, status)
            conversations = []  # (teacher_id, student_id, conversation_time, conversation_type, conversation_place, topic, content, result, next_plan, attachment_path)
            applications = []  # (student_id, job_id, company_id, resume_id, status, read_status, apply_letter, company_remark, interview_status, offer_status)
            interviews = []   # (application_id, student_id, company_id, job_id, interview_time, interview_address, interview_type, contact_person, contact_phone, remark, status)
            offers = []       # (application_id, student_id, company_id, job_id, position_name, salary, work_city, start_date, probation_period, probation_salary, response_deadline, status)

            # 先收集所有学生信息，user_id 暂时用 -1 占位，最后再替换
            user_id_map: Dict[int, int] = {}  # local_idx -> user_id（插入后填充）

            for stu_seq in range(1, stu_count + 1):
                student_no = generate_student_no(grad_year, class_id, stu_seq)
                username = f"stu{grad_year}{class_id:04d}{stu_seq:03d}"
                gender = random.choice(["男", "女"])
                name = generate_name(gender)
                phone = generate_phone()

                # sys_user
                id_card = generate_id_card(grad_year, gender)
                users.append((
                    username, "$2b$10$placeholder", name, gender,
                    phone, f"{username}@student.edu.cn", id_card,
                    dept_id, major_id, class_name, class_id, student_no, grad_year,
                    "0", "批量导入学生"
                ))

                # student_info
                province, city = generate_province_city()
                nation = weighted_choice(NATION_WEIGHTED)
                if nation != "汉族":
                    if random.random() < 0.99:
                        province = "贵州省"
                        city = random.choice(PROVINCE_CITIES.get("贵州省", ["贵阳市"]))
                    else:
                        province = random.choice(MINORITY_OTHER)
                        city = random.choice(PROVINCE_CITIES.get(province, ["某市"]))

                politics = weighted_choice(POLITICS_WEIGHTED)
                birth_year = random.randint(grad_year - 24, grad_year - 20)
                birth_month = random.randint(1, 12)
                birth_day = random.randint(1, 28)
                birth_date = f"{birth_year}-{birth_month:02d}-{birth_day:02d}"
                street = random.choice(STREET_SUFFIXES)
                address = f"{province}{city}{street}"
                pattern = random.choice(DORMITORY_PATTERNS)
                dormitory = pattern.format(random.randint(1, 20), random.randint(1, 400))
                rel = random.choice(EMERGENCY_RELATIONS)
                eg = "男" if rel in ["爷爷", "外公", "叔叔"] else "女"
                emergency_contact = f"{rel} {generate_name(eg)}"
                emergency_phone = generate_phone()

                infos.append((
                    student_no, name, gender, birth_date,
                    id_card, nation, politics,
                    phone, f"{username}@student.edu.cn",
                    province, city, address,
                    dept_id, dept_name, major_id, major_name,
                    class_name, class_id, grad_year,
                    "统招", dormitory, emergency_contact, emergency_phone,
                    "graduated"
                ))

                # =========================== 就业决策 ===========================
                will_be_employed = random.random() < EMPLOYMENT_RATE.get(grad_year, 0.83)

                if will_be_employed:
                    emp_type = weighted_choice_int(EMPLOYMENT_TYPE_WEIGHTED)
                    local_idx = stu_seq - 1  # 0-based local index

                    # ---- 简历生成（多样化）----
                    personal_summarys = [
                        "本人系统学习了本专业核心课程，具备扎实的理论基础和实践能力。",
                        "在校期间积极参与专业实践和课程设计，具备独立解决问题的能力。",
                        "通过多门专业课程的学习和实践项目，掌握了本专业的基本技能和方法。",
                        "本人学习能力较强，能够快速适应新环境，具备团队协作精神。",
                    ]
                    personal_summary = random.choice(personal_summarys)

                    enroll_year = grad_year - 4
                    gpa = round(random.uniform(2.5, 4.0), 2)
                    rank = max(1, min(30, int((4.0 - gpa) * 10 + random.randint(1, 10))))
                    rand_eng = random.random()
                    if rand_eng < 0.3:
                        cet_line = ""
                    elif rand_eng < 0.8:
                        cet_line = f"英语：CET-4 {random.randint(400, 500)}分"
                    else:
                        cet_line = f"英语：CET-4 {random.randint(400, 500)}分 / CET-6 {random.randint(400, 500)}分"
                    courses_pool = []
                    for k in MAJOR_INDUSTRY_KEYWORDS:
                        if k in PROJECTS:
                            courses_pool = [
                                "专业核心课程A", "专业核心课程B", "专业基础课程", "专业选修课程",
                                "专业实践课程", "专业设计课程", "毕业设计", "生产实习",
                            ]
                            break
                    courses_line = "、".join(pick_n_random(courses_pool, random.randint(8, min(10, len(courses_pool)))))
                    edu_exp_parts = [
                        f"{enroll_year}-09 至 {grad_year}-07  |  {dept_name}  |  {major_name}  |  {class_name}",
                        f"GPA: {gpa}/4.0（专业前{rank}%）",
                    ]
                    if cet_line:
                        edu_exp_parts.append(cet_line)
                    edu_exp_parts.append(f"主修课程：{courses_line}")
                    edu_exp = "\n".join(edu_exp_parts)

                    # 项目经验
                    proj_pool = PROJECTS.get(major_name, ["专业实践项目"])
                    chosen_projs = pick_n_random(proj_pool, min(2, len(proj_pool)))
                    skill_pool = SKILL_POOLS.get(major_name, ["CAD", "Office"])
                    proj_year_base = random.randint(enroll_year + 2, grad_year - 1)
                    projects = []
                    for pi, proj_name in enumerate(chosen_projs):
                        pyear = proj_year_base + pi
                        proj_tech = ", ".join(pick_n_random(skill_pool, random.randint(2, min(3, len(skill_pool)))))
                        proj_desc_opt = [
                            "进行了系统需求分析与方案设计，完成了主要功能模块的开发与测试。",
                            "完成了数据分析与建模工作，建立了预测模型并进行了验证。",
                            "进行了工艺设计与参数优化，解决了关键技术问题，达到了预期目标。",
                            "完成了整体方案设计与实现，进行了性能测试与评估。",
                            "进行了理论研究与实验验证，积累了实践经验。",
                        ]
                        proj_resp_opt = [
                            "负责项目整体规划与实施，完成了主要模块的设计与实现。",
                            "担任项目技术骨干，主要负责技术方案设计与核心代码编写。",
                            "参与项目全过程，负责实验数据采集与分析整理工作。",
                        ]
                        projects.append(
                            f"{pyear}-03 至 {pyear + 1}-06  |  {proj_name}\n"
                            f"  项目描述：{random.choice(proj_desc_opt)}\n"
                            f"  技术栈：{proj_tech}\n"
                            f"  {random.choice(proj_resp_opt)}"
                        )

                    # 证书
                    cert_pool = CERT_POOLS.get(major_name, CERT_POOLS["default"])
                    certs = pick_n_random(cert_pool, min(2, len(cert_pool)))

                    # 自我评价
                    eval_style, eval_lines = random.choice(SELF_EVAL_TEMPLATES)
                    self_eval = "\n".join(eval_lines)

                    sal_min = random.choice([5000, 5500, 6000, 6500, 7000, 7500, 8000, 9000, 10000])
                    sal_max = sal_min + random.randint(2000, 5000)
                    all_cities = [c for cs in PROVINCE_CITIES.values() for c in cs]
                    exp_city = random.choice(all_cities) if all_cities else "贵阳市"
                    keywords = MAJOR_INDUSTRY_KEYWORDS.get(major_name, ["技术"])
                    exp_industry = keywords[0]
                    exp_position = random.choice(["技术工程师", "研发工程师", "设计工程师",
                                                   "工艺工程师", "分析工程师", "管理培训生"])

                    resumes.append((
                        -1,  # student_id 占位
                        f"{name}的简历", "1",
                        personal_summary, edu_exp,
                        "\n\n".join(projects) if projects else "",
                        "",
                        "\n".join(certs), "",
                        self_eval,
                        sal_min, sal_max,
                        exp_city, exp_position, exp_industry,
                        ""
                    ))

                    # ---- 投递链路 ----
                    if emp_type == "继续深造":
                        deep_study_city = random.choice([
                            ("北京市", "北京市"), ("上海市", "上海市"),
                            ("南京市", "江苏省"), ("杭州市", "浙江省"),
                            ("武汉市", "湖北省"), ("西安市", "陕西省"),
                            ("成都市", "四川省"), ("长沙市", "湖南省"),
                            ("广州市", "广东省"), ("厦门市", "福建省"),
                            ("昆明市", "云南省"),
                        ])
                        employments.append((
                            -1, emp_type, "考研升学", "", "", "教育",
                            "升学", "",
                            deep_study_city[0], deep_study_city[1],
                            "", "0", "", "", "", "",
                            "approved", "", None, None, None, ""
                        ))

                    elif emp_type == "应征入伍":
                        employments.append((
                            -1, emp_type, "应征入伍", "", "", "政府/军队",
                            "士兵", "",
                            "待定", "待定",
                            "", "0", "", "", "", "",
                            "approved", "", None, None, None, ""
                        ))

                    elif emp_type == "出国出境":
                        employments.append((
                            -1, emp_type, "出国深造", "", "", "教育",
                            "留学生", "",
                            "境外", "境外",
                            "", "0", "", "", "", "",
                            "approved", "", None, None, None, ""
                        ))

                    elif emp_type in ("签订三方协议", "签订劳动合同", "自由职业"):
                        keywords = MAJOR_INDUSTRY_KEYWORDS.get(major_name, ["技术"])
                        exp_industry_for_job = keywords[0]
                        available_jobs = industry_jobs.get(exp_industry_for_job, positions)
                        if not available_jobs:
                            available_jobs = positions

                        applied_jobs = random.sample(available_jobs, min(random.randint(3, 4), len(available_jobs)))

                        for j_idx, job in enumerate(applied_jobs):
                            job_id, job_name, job_company_id, job_sal_min, job_sal_max, job_city, job_scale, job_industry = job
                            job_code = company_code_map.get(job_company_id, "")
                            job_company_name = ""
                            for comp in companies:
                                if comp[0] == job_company_id:
                                    job_company_name = comp[1]
                                    break

                            # 面试结果
                            interview_result = random.choices(
                                ["待定", "通过", "未通过"],
                                weights=[20, 65, 15]
                            )[0]

                            # offer结果（最后一个申请）
                            if j_idx == len(applied_jobs) - 1:
                                if interview_result == "通过":
                                    offer_status = "accepted"
                                else:
                                    offer_status = "declined"
                            else:
                                offer_status = "withdrawn"

                            sal = random.randint(max(4000, job_sal_min or 4000), min(12000, (job_sal_max or 8000) + 2000))
                            prob_sal = int(sal * 0.8)

                            applications.append((
                                -1, job_id, job_company_id, None,
                                "completed", "1", "",
                                "", interview_result.lower().replace("通过", "passed").replace("未通过", "failed"),
                                offer_status
                            ))

                            if interview_result == "通过":
                                start_month = random.randint(6, 8)
                                start_day = random.randint(10, 25)
                                resp_day = random.randint(1, 10)
                                offers.append((
                                    -1, -1, job_company_id, job_id, job_name,
                                    f"{sal}元/月",
                                    job_city or "某市",
                                    f"{grad_year}-{start_month:02d}-{start_day:02d}",
                                    "3个月",
                                    f"{prob_sal}元/月",
                                    f"{grad_year}-05-{resp_day:02d}",
                                    offer_status
                                ))

                                work_city = job_city or "某市"
                                work_province = "贵州省"
                                for prov, cities in PROVINCE_CITIES.items():
                                    if work_city in cities:
                                        work_province = prov
                                        break

                                if emp_type == "签订三方协议":
                                    three_party_no = f"TP{grad_year}{total_users + stu_seq:06d}"
                                    employments.append((
                                        -1, emp_type,
                                        job_company_name, job_code,
                                        job_scale or "", job_industry or "",
                                        job_name, "",
                                        work_city, work_province,
                                        f"{sal}元/月",
                                        "1", three_party_no,
                                        f"{grad_year}-{random.randint(6, 8):02d}-15",
                                        f"{grad_year + 3}-{random.randint(6, 8):02d}-14",
                                        f"{prob_sal}元/月",
                                        "pending", "", None, None, None, ""
                                    ))
                                    agreements.append((
                                        -1, job_company_id,
                                        three_party_no,
                                        f"{grad_year - 1}-{random.randint(10, 12):02d}-{random.randint(10, 28):02d}",
                                        f"{grad_year - 1}-{random.randint(11, 12):02d}-{random.randint(10, 28):02d}",
                                        f"{grad_year}-06-15",
                                        "", "completed"
                                    ))
                                else:
                                    employments.append((
                                        -1, emp_type,
                                        job_company_name, job_code,
                                        job_scale or "", job_industry or "",
                                        job_name, "",
                                        work_city, work_province,
                                        f"{sal}元/月",
                                        "0", "", "", "", "",
                                        "pending", "", None, None, None, ""
                                    ))
                                break  # 找到工作了，结束

                else:
                    # ---- 未就业学生 ----
                    has_resume = random.random() < 0.5
                    if has_resume:
                        enroll_year = grad_year - 4
                        resumes.append((
                            -1,
                            f"{name}的简历", "1",
                            "本人性格开朗，乐观向上，积极进取。",
                            f"{enroll_year}-09 至 {grad_year}-07  |  {dept_name}  |  {major_name}  |  {class_name}",
                            "", "", "", "",
                            "希望找到一份工作。",
                            None, None, "", "", "", ""
                        ))

                    # 谈心谈话
                    num_convs = random.randint(2, 4)
                    conv_types_w = [
                        ("就业指导", 40), ("心理疏导", 25),
                        ("学业辅导", 15), ("生活关怀", 15), ("其他", 5)
                    ]
                    for ci in range(num_convs):
                        conv_type = weighted_choice(conv_types_w)
                        content, result, next_plan = generate_conversation_content(conv_type)
                        conv_time = generate_conversation_time(grad_year, ci, num_convs)
                        conv_place = random.choice([
                            "辅导员办公室", "线上沟通", "学生宿舍", "学院会议室", "电话访谈"
                        ])
                        topic_map = {
                            "就业指导": "就业意向沟通与指导",
                            "心理疏导": "求职心态调整",
                            "学业辅导": "学业困难帮扶",
                            "生活关怀": "生活困难关怀",
                            "其他": "日常谈心交流",
                        }
                        conversations.append((
                            advisor_id or 1, -1,
                            conv_time, conv_type, conv_place,
                            topic_map[conv_type],
                            content, result, next_plan, ""
                        ))

            # =========================== 生成 SQL ===========================
            # sys_user
            sys_user_fields = [
                "username", "password", "real_name", "gender", "phone", "email", "id_card",
                "dept_id", "major_id", "class_name", "class_id", "student_no", "graduation_year",
                "status", "remark"
            ]
            all_sql_stmts.extend(sql_insert("sys_user", sys_user_fields, users))
            total_users += stu_count

            # sys_user_role
            # user_id 在第一条 INSERT 后递增，所以是连续的
            first_user_id_query = f"(SELECT MAX(id) - {stu_count} + 1 FROM sys_user)"
            for i in range(stu_count):
                all_sql_stmts.append(
                    f"INSERT INTO sys_user_role (user_id, role_id) "
                    f"SELECT MAX(id) - {stu_count} + {i + 1}, {student_role_id} FROM sys_user;"
                )

            # student_info
            info_fields = [
                "user_id", "student_no", "real_name", "gender", "birth_date", "id_card", "nation",
                "politics_status", "phone", "email", "province", "city", "address",
                "dept_id", "dept_name", "major_id", "major_name",
                "class_name", "class_id", "graduation_year",
                "study_type", "dormitory", "emergency_contact", "emergency_phone", "status"
            ]
            # 需要用子查询从 sys_user 拿 user_id（按 student_no 匹配）
            for i, info_row in enumerate(infos):
                student_no = info_row[0]
                uid_sub = f"(SELECT id FROM sys_user WHERE student_no = {sql_val(student_no)} LIMIT 1)"
                vals = [uid_sub] + list(info_row)
                all_sql_stmts.append(
                    f"INSERT INTO student_info ({', '.join(info_fields)}) VALUES\n    ({', '.join(sql_val(v) for v in vals)});"
                )

            # student_resume
            if resumes:
                resume_fields = [
                    "student_id", "resume_name", "is_default", "personal_summary",
                    "education_experience", "project_experience", "work_experience",
                    "skill_certificates", "awards_honors", "self_evaluation",
                    "expected_salary_min", "expected_salary_max",
                    "expected_city", "expected_position", "expected_industry", "file_path"
                ]
                for res in resumes:
                    student_no = infos[res[0]][0]  # res[0] is local idx
                    uid_sub = f"(SELECT id FROM sys_user WHERE student_no = {sql_val(student_no)} LIMIT 1)"
                    vals = [uid_sub] + list(res[1:])
                    all_sql_stmts.append(
                        f"INSERT INTO student_resume ({', '.join(resume_fields)}) VALUES\n    ({', '.join(sql_val(v) for v in vals)});"
                    )

            # employment_record（需要先有 student_id）
            if employments:
                emp_fields = [
                    "student_id", "employment_type", "company_name", "company_code",
                    "company_scale", "company_industry", "position_name", "position_category",
                    "work_city", "work_province", "salary",
                    "is_three_party_signed", "three_party_no",
                    "contract_start_date", "contract_end_date", "probation_salary",
                    "audit_status", "audit_remark", "audit_user_id", "audit_time", "remark"
                ]
                for emp in employments:
                    student_no = infos[emp[0]][0]
                    uid_sub = f"(SELECT id FROM sys_user WHERE student_no = {sql_val(student_no)} LIMIT 1)"
                    vals = [uid_sub] + list(emp[1:])
                    all_sql_stmts.append(
                        f"INSERT INTO employment_record ({', '.join(emp_fields)}) VALUES\n    ({', '.join(sql_val(v) for v in vals)});"
                    )

            # tripartite_agreement
            if agreements:
                agr_fields = [
                    "student_id", "company_id", "agreement_no",
                    "student_sign_time", "company_sign_time", "school_sign_time",
                    "file_path", "status"
                ]
                for agr in agreements:
                    student_no = infos[agr[0]][0]
                    uid_sub = f"(SELECT id FROM sys_user WHERE student_no = {sql_val(student_no)} LIMIT 1)"
                    vals = [uid_sub] + list(agr[1:])
                    all_sql_stmts.append(
                        f"INSERT INTO tripartite_agreement ({', '.join(agr_fields)}) VALUES\n    ({', '.join(sql_val(v) for v in vals)});"
                    )

            # conversation_record
            if conversations:
                conv_fields = [
                    "teacher_id", "student_id", "conversation_time", "conversation_type",
                    "conversation_place", "topic", "content", "result", "next_plan", "attachment_path"
                ]
                for conv in conversations:
                    student_no = infos[conv[1]][0]
                    uid_sub = f"(SELECT id FROM sys_user WHERE student_no = {sql_val(student_no)} LIMIT 1)"
                    vals = [conv[0], uid_sub] + list(conv[2:])
                    all_sql_stmts.append(
                        f"INSERT INTO conversation_record ({', '.join(conv_fields)}) VALUES\n    ({', '.join(sql_val(v) for v in vals)});"
                    )

            # 进度提示
            if (class_idx + 1) % 100 == 0:
                print(f"  已处理 {class_idx + 1}/{total_classes} 个班级 ...")

        # =========================== 写入 SQL 文件 ===========================
        print(f"\n共生成 {len(all_sql_stmts)} 条 SQL 语句，写入文件 ...")

        header = [
            "-- =========================================================",
            "-- 批量学生账号 SQL（由 generate_students_sql.py 自动生成）",
            f"-- 生成时间: {time.strftime('%Y-%m-%d %H:%M:%S')}",
            f"-- 学生总数: ~{total_users} 人",
            "-- =========================================================",
            "SET NAMES utf8mb4;",
            "SET FOREIGN_KEY_CHECKS = 0;",
            "",
            "-- 清理旧数据",
            "DELETE FROM sys_user WHERE remark = '批量导入学生';",
            "",
            "-- ========== 学生主数据 ==========",
            "",
        ]

        footer = [
            "",
            "-- ========== 更新 sys_class.student_count ==========",
            "UPDATE sys_class c SET student_count = (",
            "    SELECT COUNT(*) FROM sys_user WHERE class_id = c.id",
            ") WHERE EXISTS (SELECT 1 FROM sys_user WHERE class_id = c.id);",
            "",
            "-- ========== 更新 student_info.user_id（以防有遗漏）==========",
            "UPDATE student_info si SET user_id = (",
            "    SELECT id FROM sys_user WHERE student_no = si.student_no LIMIT 1",
            ") WHERE si.user_id IS NULL OR si.user_id = 0;",
            "",
            "SET FOREIGN_KEY_CHECKS = 1;",
            "",
            f"-- 生成完成：{time.strftime('%Y-%m-%d %H:%M:%S')}",
        ]

        with open(sql_out_path, "w", encoding="utf-8") as f:
            f.write("\n".join(header))
            f.write("\n")
            for stmt in all_sql_stmts:
                f.write(stmt)
                f.write(";\n\n")

            # 写入三方协议 student_id 更新（从 employment_record 反查）
            f.write("-- ========== 更新三方协议的 student_id ==========\n")
            f.write("UPDATE tripartite_agreement ta SET student_id = (\n")
            f.write("    SELECT er.student_id FROM employment_record er\n")
            f.write("    WHERE er.company_name = (SELECT company_name FROM company_info WHERE id = ta.company_id LIMIT 1)\n")
            f.write("    AND er.student_id IS NOT NULL LIMIT 1\n")
            f.write(") WHERE ta.student_id IS NULL OR ta.student_id = 0;\n\n")

            f.write("\n".join(footer))

        print(f"✓ SQL 文件已生成：{sql_out_path}")
        print(f"✓ 请执行以下命令导入数据：")
        print(f"  mysql -u root -p employment_db < \"{sql_out_path}\"")
        print(f"\n共处理 {total_classes} 个班级，~{total_users} 名学生")

    finally:
        cursor.close()
        conn.close()


if __name__ == "__main__":
    main()
