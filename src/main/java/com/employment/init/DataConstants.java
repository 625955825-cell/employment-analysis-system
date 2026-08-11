package com.employment.init;

import lombok.Data;

import java.util.*;

/**
 * 数据常量定义
 * 所有静态配置数据集中管理
 */
public class DataConstants {

    // ==================== 学生数量配置 ====================
    public static final int STUDENTS_MIN = 40;
    public static final int STUDENTS_MAX = 45;

    // ==================== 就业率配置（按毕业年份） ====================
    public static final Map<Integer, Double> EMPLOYMENT_RATE;
    static {
        EMPLOYMENT_RATE = new HashMap<>();
        EMPLOYMENT_RATE.put(2018, 0.94);
        EMPLOYMENT_RATE.put(2019, 0.94);
        EMPLOYMENT_RATE.put(2020, 0.94);
        EMPLOYMENT_RATE.put(2021, 0.94);
        EMPLOYMENT_RATE.put(2022, 0.83);
    }

    // ==================== 姓名库 ====================
    public static final List<String> XING = Arrays.asList(
            "王", "李", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴",
            "徐", "孙", "胡", "朱", "高", "林", "何", "郭", "马", "罗",
            "梁", "宋", "郑", "谢", "韩", "唐", "冯", "于", "董", "萧",
            "程", "曹", "袁", "邓", "许", "傅", "沈", "曾", "彭", "吕",
            "苏", "卢", "蒋", "蔡", "贾", "丁", "魏", "薛", "叶", "阎",
            "余", "潘", "杜", "戴", "夏", "钟", "汪", "田", "任", "姜",
            "范", "方", "石", "姚", "谭", "廖", "邹", "熊", "金", "陆",
            "郝", "孔", "白", "崔", "康", "毛", "邱", "秦", "江", "史"
    );

    public static final List<String> MING_MALE = Arrays.asList(
            "浩", "宇", "博", "霖", "俊杰", "子轩", "子涵", "晨曦", "明辉", "伟",
            "强", "杰", "涛", "超", "勇", "鹏", "飞", "龙", "凯", "文",
            "华", "磊", "波", "峰", "刚", "平", "辉", "军", "洋", "健",
            "志", "豪", "睿", "祥", "瑞", "旭", "昊", "然", "彬", "恒",
            "泽", "浩宇", "浩然", "梓轩", "宇航", "志远", "志强", "文博", "子豪", "天宇"
    );

    public static final List<String> MING_FEMALE = Arrays.asList(
            "欣怡", "思雨", "雨萱", "思琪", "欣悦", "雨彤", "思涵", "诗涵", "雅婷", "雅静",
            "雅欣", "雅楠", "欣", "怡", "婷", "琳", "敏", "静", "丽", "艳",
            "娟", "芳", "兰", "洁", "佳", "嘉", "慧", "文静", "诗雨", "雨欣",
            "雨洁", "晓", "颖", "蕾", "燕", "霞", "红", "云", "梅", "莲",
            "倩", "瑶", "丹", "凤", "珍", "翠", "华", "玲", "娜", "秀"
    );

    // ==================== 籍贯/省份 ====================
    public static final List<String> PROVINCES = Arrays.asList(
            "贵州省", "湖北省", "湖南省", "河南省", "广东省", "江西省", "安徽省", "四川省", "其他"
    );

    // 省份加权概率 [省份, 权重]
    public static final List<String[]> PROVINCES_WEIGHTED = Arrays.asList(
            new String[]{"贵州省", "30"},
            new String[]{"湖北省", "22"},
            new String[]{"湖南省", "15"},
            new String[]{"河南省", "10"},
            new String[]{"广东省", "8"},
            new String[]{"江西省", "7"},
            new String[]{"安徽省", "5"},
            new String[]{"四川省", "2"},
            new String[]{"其他", "1"}
    );

    public static final Map<String, List<String>> PROVINCE_CITIES;
    static {
        PROVINCE_CITIES = new LinkedHashMap<>();
        PROVINCE_CITIES.put("贵州省", Arrays.asList("贵阳市", "遵义市", "六盘水市", "安顺市", "毕节市", "铜仁市", "黔西南州", "黔东南州", "黔南州"));
        PROVINCE_CITIES.put("湖北省", Arrays.asList("武汉市", "宜昌市", "襄阳市", "荆州市", "黄石市", "十堰市", "孝感市", "黄冈市", "咸宁市"));
        PROVINCE_CITIES.put("湖南省", Arrays.asList("长沙市", "株洲市", "湘潭市", "衡阳市", "岳阳市", "常德市", "益阳市", "郴州市", "永州市"));
        PROVINCE_CITIES.put("河南省", Arrays.asList("郑州市", "洛阳市", "开封市", "新乡市", "南阳市", "许昌市", "安阳市", "平顶山市", "商丘市"));
        PROVINCE_CITIES.put("广东省", Arrays.asList("广州市", "深圳市", "东莞市", "佛山市", "珠海市", "中山市", "惠州市", "江门市", "湛江市"));
        PROVINCE_CITIES.put("江西省", Arrays.asList("南昌市", "赣州市", "九江市", "上饶市", "宜春市", "吉安市", "抚州市", "景德镇市", "萍乡市"));
        PROVINCE_CITIES.put("安徽省", Arrays.asList("合肥市", "芜湖市", "蚌埠市", "淮南市", "马鞍山市", "安庆市", "阜阳市", "滁州市", "六安市"));
        PROVINCE_CITIES.put("四川省", Arrays.asList("成都市", "绵阳市", "德阳市", "宜宾市", "南充市", "泸州市", "达州市", "乐山市", "内江市"));
        PROVINCE_CITIES.put("其他", Arrays.asList("北京市", "上海市", "天津市", "重庆市", "河北省", "山西省", "辽宁省", "吉林省", "黑龙江省", "江苏省", "浙江省", "福建省", "山东省", "海南省"));
    }

    // ==================== 身份证前6位编码 ====================
    public static final Map<String, List<String>> IDCARD_CODES;
    static {
        IDCARD_CODES = new LinkedHashMap<>();
        IDCARD_CODES.put("贵州省", Arrays.asList(
                "520100", "520101", "520102", "520103", "520104", "520111", "520112", "520113",
                "520121", "520122", "520123", "520181", "520200", "520201", "520203", "520204",
                "520300", "520301", "520302", "520303", "520400", "520500", "520600", "520601", "520602"
        ));
        IDCARD_CODES.put("湖北省", Arrays.asList(
                "420100", "420500", "420600", "421000", "420200", "420300",
                "420900", "421100", "421200", "420100"
        ));
        IDCARD_CODES.put("湖南省", Arrays.asList(
                "430100", "430200", "430300", "430400", "430600",
                "430700", "430900", "431000", "431100"
        ));
        IDCARD_CODES.put("河南省", Arrays.asList(
                "410100", "410300", "410200", "410700", "411300",
                "411000", "410500", "410400", "411400"
        ));
        IDCARD_CODES.put("广东省", Arrays.asList(
                "440100", "440300", "441900", "440600", "440400",
                "442000", "441300", "440700", "440800"
        ));
        IDCARD_CODES.put("江西省", Arrays.asList(
                "360100", "360700", "360400", "361100", "360900",
                "360800", "361000", "360200", "360300"
        ));
        IDCARD_CODES.put("安徽省", Arrays.asList(
                "340100", "340200", "340300", "340400", "340500",
                "340800", "341200", "341100", "341500"
        ));
        IDCARD_CODES.put("四川省", Arrays.asList(
                "510100", "510700", "510600", "511500", "510500",
                "510300", "511700", "511100", "511000"
        ));
        IDCARD_CODES.put("其他", Arrays.asList(
                "110100", "310100", "120100", "500000", "130100",
                "140100", "210100", "220100", "230100", "320100", "330100", "350100", "370100", "460100"
        ));
    }

    // ==================== 民族 ====================
    public static final List<String[]> NATIONS;
    static {
        NATIONS = Arrays.asList(
                new String[]{"汉族", "65"},
                new String[]{"苗族", "8"},
                new String[]{"土家族", "6"},
                new String[]{"布依族", "5"},
                new String[]{"侗族", "4"},
                new String[]{"彝族", "3"},
                new String[]{"仡佬族", "2"},
                new String[]{"水族", "2"},
                new String[]{"瑶族", "1"},
                new String[]{"壮族", "1"},
                new String[]{"毛南族", "1"}
        );
    }

    // ==================== 政治面貌 ====================
    public static final List<String[]> POLITICS;
    static {
        POLITICS = Arrays.asList(
                new String[]{"共青团员", "75"},
                new String[]{"群众", "15"},
                new String[]{"中共党员", "5"},
                new String[]{"中共预备党员", "5"}
        );
    }

    public static final List<String> GENDERS = Arrays.asList("男", "女");

    // ==================== 紧急联系人关系 ====================
    public static final List<String> EMERGENCY_RELATIONS = Arrays.asList(
            "父亲", "母亲", "爷爷", "奶奶", "外公", "外婆", "叔叔", "姑姑"
    );

    public static final List<String> MALE_RELATIONS = Arrays.asList("父亲", "爷爷", "外公", "叔叔");
    public static final List<String> FEMALE_RELATIONS = Arrays.asList("母亲", "奶奶", "外婆", "姑姑");

    // ==================== 住址/宿舍 ====================
    public static final List<String> STREET_SUFFIXES = Arrays.asList(
            "XX路XX号", "XX街XX小区", "XX镇XX村", "XX区XX路XX号"
    );

    public static final List<String> DORMITORY_PATTERNS = Arrays.asList(
            "南苑{}舍{}室", "北苑{}舍{}室", "东苑{}舍{}室", "西苑{}舍{}室",
            "{}号楼{}室", "{}栋{}室", "学生公寓{}号楼{}室"
    );

    // ==================== 专业→行业关键词（用于爬虫搜索）=====================
    public static final Map<String, List<String>> MAJOR_INDUSTRY_KEYWORDS;
    static {
        MAJOR_INDUSTRY_KEYWORDS = new LinkedHashMap<>();
        MAJOR_INDUSTRY_KEYWORDS.put("资源勘查工程", Arrays.asList("地质勘查", "矿产勘查", "地质调查", "岩土工程"));
        MAJOR_INDUSTRY_KEYWORDS.put("环境工程", Arrays.asList("环境工程", "环保工程", "环境影响评价", "污水处理", "环境监测"));
        MAJOR_INDUSTRY_KEYWORDS.put("地质工程", Arrays.asList("地质工程", "工程地质", "岩土工程", "勘察设计", "地质灾害评估"));
        MAJOR_INDUSTRY_KEYWORDS.put("机械设计制造及其自动化", Arrays.asList("机械设计", "机械制造", "工艺工程师", "数控编程", "自动化设备"));
        MAJOR_INDUSTRY_KEYWORDS.put("机械电子工程", Arrays.asList("机电一体化", "PLC", "嵌入式开发", "自动化控制", "电气工程师"));
        MAJOR_INDUSTRY_KEYWORDS.put("电气工程及其自动化", Arrays.asList("电气工程", "电力系统", "PLC", "继电保护", "配电设计"));
        MAJOR_INDUSTRY_KEYWORDS.put("自动化", Arrays.asList("自动化", "DCS", "PLC", "过程控制", "仪表工程师"));
        MAJOR_INDUSTRY_KEYWORDS.put("土木工程", Arrays.asList("土木工程", "建筑施工", "结构设计", "项目经理", "工程监理"));
        MAJOR_INDUSTRY_KEYWORDS.put("水利水电工程", Arrays.asList("水利水电", "水工设计", "大坝工程", "施工工程师"));
        MAJOR_INDUSTRY_KEYWORDS.put("道路桥梁与渡河工程", Arrays.asList("道路桥梁", "桥梁设计", "隧道工程", "交通工程", "轨道交通"));
        MAJOR_INDUSTRY_KEYWORDS.put("化学工程与工艺", Arrays.asList("化工工艺", "化工工程师", "工艺设计", "反应工程师", "化工研发"));
        MAJOR_INDUSTRY_KEYWORDS.put("新能源科学与工程", Arrays.asList("新能源", "光伏发电", "风力发电", "储能工程师", "电气工程"));
        MAJOR_INDUSTRY_KEYWORDS.put("过程装备与控制工程", Arrays.asList("过程装备", "化工设备", "压力容器", "工艺工程师", "装备设计"));
        MAJOR_INDUSTRY_KEYWORDS.put("制药工程", Arrays.asList("制药工程", "药物研发", "工艺工程师", "QA", "药品生产"));
        MAJOR_INDUSTRY_KEYWORDS.put("生物制药", Arrays.asList("生物制药", "药物研发", "细胞培养", "蛋白纯化", "生物工艺"));
        MAJOR_INDUSTRY_KEYWORDS.put("酿酒工程", Arrays.asList("酿酒", "品酒师", "酒类研发", "发酵工程", "质量控制"));
        MAJOR_INDUSTRY_KEYWORDS.put("安全工程", Arrays.asList("安全工程", "安全评价", "安全管理", "EHS", "安全工程师"));
        MAJOR_INDUSTRY_KEYWORDS.put("采矿工程", Arrays.asList("采矿工程", "矿山工程", "安全工程师", "采矿技术"));
        MAJOR_INDUSTRY_KEYWORDS.put("智能采矿工程", Arrays.asList("智慧矿山", "采矿自动化", "智能采矿", "矿山物联网"));
        MAJOR_INDUSTRY_KEYWORDS.put("工程管理", Arrays.asList("工程管理", "项目经理", "施工管理", "造价管理", "监理工程师"));
        MAJOR_INDUSTRY_KEYWORDS.put("工程造价", Arrays.asList("工程造价", "预算员", "造价工程师", "招标代理", "成本控制"));
        MAJOR_INDUSTRY_KEYWORDS.put("投资学", Arrays.asList("投资分析", "金融分析", "证券", "基金", "理财顾问"));
        MAJOR_INDUSTRY_KEYWORDS.put("网络工程", Arrays.asList("网络工程", "网络工程师", "系统运维", "网络安全", "Linux运维"));
        MAJOR_INDUSTRY_KEYWORDS.put("数据科学与大数据技术", Arrays.asList("大数据开发", "数据分析", "数据工程师", "Hadoop", "Spark", "Java开发", "Python开发", "后端开发", "互联网开发", "Web后端", "MySQL", "SpringBoot"));
        MAJOR_INDUSTRY_KEYWORDS.put("智能科学与技术", Arrays.asList("算法工程师", "AI工程师", "机器学习", "深度学习", "NLP"));
        MAJOR_INDUSTRY_KEYWORDS.put("网络空间安全", Arrays.asList("网络安全", "渗透测试", "安全运维", "安全工程师", "等保测评"));
        MAJOR_INDUSTRY_KEYWORDS.put("飞行器制造工程", Arrays.asList("航空制造", "飞行器设计", "工艺工程师", "航空材料", "无人机"));
        MAJOR_INDUSTRY_KEYWORDS.put("飞行器动力工程", Arrays.asList("航空发动机", "燃气轮机", "燃烧工程师", "动力工程师"));
        MAJOR_INDUSTRY_KEYWORDS.put("人文地理与城乡规划", Arrays.asList("城乡规划", "规划设计", "土地规划", "GIS", "方案设计师"));
        MAJOR_INDUSTRY_KEYWORDS.put("环境设计", Arrays.asList("环境设计", "室内设计", "装饰设计", "软装设计", "景观设计"));
        MAJOR_INDUSTRY_KEYWORDS.put("建筑学", Arrays.asList("建筑设计", "建筑设计师", "方案设计", "BIM", "建筑工程师"));
        MAJOR_INDUSTRY_KEYWORDS.put("材料科学与工程", Arrays.asList("材料工程", "金属材料", "材料研发", "材料分析", "冶金工程"));
        MAJOR_INDUSTRY_KEYWORDS.put("新能源材料与器件", Arrays.asList("新能源材料", "电池材料", "光伏材料", "储能材料", "电化学"));
        MAJOR_INDUSTRY_KEYWORDS.put("焊接技术与工程", Arrays.asList("焊接工程", "焊接工艺", "焊接工程师", "无损检测", "焊接设计"));
        MAJOR_INDUSTRY_KEYWORDS.put("交通工程", Arrays.asList("交通工程", "交通规划", "交通设计", "交通工程师", "道路工程"));
        MAJOR_INDUSTRY_KEYWORDS.put("交通运输", Arrays.asList("轨道交通", "地铁运营", "交通运输", "铁路信号", "站务管理"));
        MAJOR_INDUSTRY_KEYWORDS.put("应用统计学", Arrays.asList("统计分析", "数据分析师", "统计建模", "市场调研", "数据挖掘"));
        MAJOR_INDUSTRY_KEYWORDS.put("应用物理学", Arrays.asList("技术支持", "测试工程师", "光学工程", "材料物理", "研发工程师"));
        MAJOR_INDUSTRY_KEYWORDS.put("休闲体育", Arrays.asList("体育教练", "赛事运营", "健身指导", "体育营销", "体育管理"));
        MAJOR_INDUSTRY_KEYWORDS.put("英语", Arrays.asList("英语翻译", "英语教师", "外贸业务员", "跨境电商", "商务英语"));
    }

    // ==================== 专业定制自我介绍（按专业，3-5个模板） ====================
    public static final Map<String, List<String>> PERSONAL_SUMMARIES;
    static {
        PERSONAL_SUMMARIES = new LinkedHashMap<>();
        PERSONAL_SUMMARIES.put("资源勘查工程", Arrays.asList(
                "掌握资源勘查工程基本理论，熟悉地质调查、矿产勘查及储量评价方法，具备野外地质工作能力，参与过多个地质勘查实践项目，对矿产资源勘查有浓厚兴趣。",
                "熟悉MapGIS、AutoCAD等地质软件操作，掌握矿床学及矿产资源评价方法，参与过贵州省某铅锌矿地质调查项目，具备较强的地质分析能力。",
                "掌握地质学基础理论，了解地球化学勘探及遥感地质技术，参与过多个地质实习项目，具备扎实的地质专业基础和实践技能。"
        ));
        PERSONAL_SUMMARIES.put("环境工程", Arrays.asList(
                "掌握环境工程基本理论，熟悉环境影响评价、污水处理及固体废物处理工艺，具备环境监测与工程实践能力，参与过多个环境治理项目。",
                "熟悉环境监测仪器操作及数据处理方法，掌握水污染控制工程基本原理，参与过城市污水处理厂工艺设计项目，具备环境工程实践能力。",
                "掌握环境化学及生态学基础理论，了解大气污染控制及土壤修复技术，参与过工业园区环境影响评价，具备环境综合分析能力。"
        ));
        PERSONAL_SUMMARIES.put("地质工程", Arrays.asList(
                "掌握地质工程基本理论，熟悉工程地质勘察、地质灾害评估及岩土工程分析方法，具备地质调查与岩土测试能力，参与过多个工程地质项目。",
                "熟悉工程地质勘察技术方法，掌握岩土力学及地基处理技术，参与过高速公路边坡稳定性分析项目，具备工程地质实践能力。",
                "掌握地质灾害评估及防治技术，了解岩土工程设计原理，参与过隧道工程地质勘察，具备较强的地质分析能力。"
        ));
        PERSONAL_SUMMARIES.put("机械设计制造及其自动化", Arrays.asList(
                "掌握机械设计制造及自动化基本理论，熟悉机械加工工艺及数控编程，具备机械设计与制造实践能力，参与过多个课程设计和实训项目。",
                "熟悉SolidWorks、AutoCAD等设计软件，掌握机械加工工艺及PLC控制技术，参与过机械设计大赛，具备较强的机械工程实践能力。",
                "掌握机械制造基本原理，了解先进制造技术与工艺，参与过工厂生产实习，对机械制造行业有深入了解。"
        ));
        PERSONAL_SUMMARIES.put("机械电子工程", Arrays.asList(
                "掌握机械电子工程基本理论，熟悉PLC控制、传感器应用及机电一体化系统设计，具备机电产品开发与调试能力，参与过多个嵌入式系统项目。",
                "熟悉电路原理及电子技术，掌握单片机及嵌入式开发技术，参与过智能小车循迹与避障系统设计，具备软硬件结合的实践能力。",
                "掌握电机拖动与控制技术，了解工业控制网络，参与过自动化生产线控制系统设计项目，具备机电系统集成能力。"
        ));
        PERSONAL_SUMMARIES.put("电气工程及其自动化", Arrays.asList(
                "掌握电气工程及其自动化基本理论，熟悉电力系统分析与PLC控制技术，具备电气系统设计与调试能力，参与过多个电气控制实践项目。",
                "熟悉工厂供配电系统设计，掌握继电保护及自动化装置原理，参与过某工厂配电系统设计，具备电气工程实践能力。",
                "掌握PLC编程及变频器应用技术，了解电力电子变换技术，参与过智能配电箱监控系统设计项目。"
        ));
        PERSONAL_SUMMARIES.put("自动化", Arrays.asList(
                "掌握自动化基本理论，熟悉DCS、PLC及过程控制系统的设计与应用，具备工业自动化系统集成能力，参与过多个控制系统实训项目。",
                "熟悉过程控制仪表及调节阀选型，掌握PID参数整定方法，参与过小型过程控制实验装置设计项目。",
                "掌握工业网络及通信技术，了解先进控制算法，参与过DCS控制系统组态与调试实训。"
        ));
        PERSONAL_SUMMARIES.put("土木工程", Arrays.asList(
                "掌握土木工程基本理论，熟悉结构设计、工程施工及项目管理，具备土木工程设计与施工实践能力，参与过多个课程设计和实习项目。",
                "熟悉PKPM及盈建科结构设计软件，掌握混凝土结构设计原理，参与过框架结构教学楼设计项目，具备结构设计实践能力。",
                "掌握土力学与地基基础技术，了解建筑施工技术，参与过混凝土配合比设计及性能测试项目。"
        ));
        PERSONAL_SUMMARIES.put("水利水电工程", Arrays.asList(
                "掌握水利水电工程基本理论，熟悉水工建筑物设计及水利工程施工，具备水利水电工程设计与施工能力，参与过多个水利工程实践项目。",
                "熟悉水力学计算方法，掌握水工建筑物设计原理，参与过某水库除险加固设计方案项目。",
                "掌握水利工程施工技术，了解水利工程测量方法，参与过灌溉渠道设计实训项目。"
        ));
        PERSONAL_SUMMARIES.put("道路桥梁与渡河工程", Arrays.asList(
                "掌握道路桥梁与渡河工程基本理论，熟悉路桥设计及施工技术，具备道路与桥梁工程实践能力，参与过多个交通工程实习项目。",
                "熟悉道路勘测设计流程，掌握路基路面工程设计方法，参与过城市道路交叉口设计项目。",
                "掌握桥梁设计原理及施工技术，了解隧道工程，参与过预应力混凝土简支梁桥设计项目。"
        ));
        PERSONAL_SUMMARIES.put("化学工程与工艺", Arrays.asList(
                "掌握化学工程与工艺基本理论，熟悉化工工艺流程及反应器设计，具备化工生产与工艺开发能力，参与过多个化工实验与实训项目。",
                "熟悉化工单元操作原理，掌握化工分离技术，参与过年产5万吨甲醇合成工艺设计项目。",
                "掌握化学反应工程基础，了解化工热力学，参与过反应精馏分离工艺开发实验项目。"
        ));
        PERSONAL_SUMMARIES.put("新能源科学与工程", Arrays.asList(
                "掌握新能源科学与工程基本理论，熟悉光伏发电、风力发电及储能技术，具备新能源系统设计与应用能力，参与过多个新能源工程实践项目。",
                "熟悉光伏组件原理及系统设计，掌握储能电池管理技术，参与过家用光伏发电系统设计项目。",
                "掌握风力发电机组设计原理，了解智能电网技术，参与过小型风力发电机组设计实训项目。"
        ));
        PERSONAL_SUMMARIES.put("过程装备与控制工程", Arrays.asList(
                "掌握过程装备与控制工程基本理论，熟悉化工设备设计及过程控制技术，具备过程装备开发与系统集成能力，参与过多个化工装备实训项目。",
                "熟悉化工原理及压力容器设计规范，掌握过程控制技术，参与过换热器设计及工艺计算项目。"
        ));
        PERSONAL_SUMMARIES.put("制药工程", Arrays.asList(
                "掌握制药工程基本理论，熟悉药物制剂工艺及GMP规范，具备药品生产与质量控制能力，参与过多个制药工程实训项目。",
                "熟悉药品生产质量管理规范，掌握药物分析技术，参与过口服固体制剂工艺优化项目。",
                "掌握制药工艺学基础，了解药物化学，参与过某药品生产车间设计实训项目。"
        ));
        PERSONAL_SUMMARIES.put("生物制药", Arrays.asList(
                "掌握生物制药基本理论，熟悉生物药物制备及质量分析技术，具备生物药品研发与生产支持能力，参与过多个生物制药实验项目。",
                "熟悉基因工程及细胞培养技术，掌握生物制品检验方法，参与过重组蛋白药物生产工艺设计项目。"
        ));
        PERSONAL_SUMMARIES.put("酿酒工程", Arrays.asList(
                "掌握酿酒工程基本理论，熟悉酿酒微生物学及酒体设计技术，具备酿酒生产与品质控制能力，参与过多个酿酒工程实训项目。",
                "熟悉酿酒发酵工艺，掌握酒类品评技术，参与过酱香型白酒酿造工艺优化项目。"
        ));
        PERSONAL_SUMMARIES.put("安全工程", Arrays.asList(
                "掌握安全工程基本理论，熟悉安全评价及事故预防技术，具备安全检测与风险管控能力，参与过多个安全工程实践项目。",
                "熟悉安全系统工程方法，掌握事故致因理论，参与过某煤矿安全评价报告项目。"
        ));
        PERSONAL_SUMMARIES.put("采矿工程", Arrays.asList(
                "掌握采矿工程基本理论，熟悉矿山开采工艺及安全技术，具备采矿设计与生产管理能力，参与过多个采矿工程实习项目。",
                "熟悉矿山岩体力学，掌握矿井通风设计方法，参与过某地下煤矿开采方案设计项目。"
        ));
        PERSONAL_SUMMARIES.put("智能采矿工程", Arrays.asList(
                "掌握智能采矿工程基本理论，熟悉智能矿山技术及无人开采系统，具备智慧矿山规划与运营能力，参与过多个智能采矿实践项目。",
                "熟悉矿山物联网技术，掌握无人采矿设备控制原理，参与过智能矿山数据平台设计项目。"
        ));
        PERSONAL_SUMMARIES.put("工程管理", Arrays.asList(
                "掌握工程管理基本理论，熟悉工程项目策划及全过程造价管理，具备工程项目组织与协调能力，参与过多个工程项目实训。",
                "熟悉工程项目管理软件操作，掌握施工组织设计方法，参与过某住宅小区施工项目管理策划项目。"
        ));
        PERSONAL_SUMMARIES.put("工程造价", Arrays.asList(
                "掌握工程造价基本理论，熟悉工程量清单编制及造价控制方法，具备工程造价编制与审核能力，参与过多个工程造价实训项目。",
                "熟悉广联达及鲁班造价软件，掌握建筑工程计量与计价方法，参与过某办公楼工程量清单编制项目。"
        ));
        PERSONAL_SUMMARIES.put("投资学", Arrays.asList(
                "掌握投资学基本理论，熟悉金融市场分析及投资组合管理，具备投资分析与风险管理能力，参与过多个金融实训项目。",
                "熟悉证券及基金运作机制，掌握公司金融分析方法，参与过上市公司股票投资价值分析项目。"
        ));
        PERSONAL_SUMMARIES.put("网络工程", Arrays.asList(
                "掌握网络工程基本理论，熟悉网络架构设计及网络安全防护，具备网络系统部署与运维能力，参与过多个网络工程实训项目。",
                "熟悉华为及思科网络设备配置，掌握Linux服务器管理技术，参与过某企业网络架构设计与实施项目。",
                "掌握网络安全防护技术，了解渗透测试方法，参与过校园网安全防护方案设计项目。"
        ));
        PERSONAL_SUMMARIES.put("数据科学与大数据技术", Arrays.asList(
                "掌握数据科学与大数据技术基本理论，熟悉Hadoop、Spark等大数据技术栈，具备数据分析与可视化能力，参与过多个数据处理实践项目。",
                "熟悉Python数据分析生态，掌握Pandas、NumPy及机器学习算法，参与过电商用户行为数据挖掘项目，具备数据驱动的问题解决能力。",
                "了解大数据生态系统（Hadoop/Spark/Hive），掌握数据清洗与ETL流程，参与过学校就业数据分析系统开发项目。"
        ));
        PERSONAL_SUMMARIES.put("智能科学与技术", Arrays.asList(
                "掌握智能科学与技术基本理论，熟悉机器学习及深度学习算法，具备智能系统开发与数据分析能力，参与过多个AI应用实践项目。",
                "熟悉Python及TensorFlow/PyTorch框架，掌握图像识别及NLP技术，参与过图像识别在智能交通中的应用项目。",
                "掌握智能推荐算法，了解数据挖掘方法，参与过自然语言处理情感分析系统开发项目。"
        ));
        PERSONAL_SUMMARIES.put("网络空间安全", Arrays.asList(
                "掌握网络空间安全基本理论，熟悉渗透测试及安全防护技术，具备网络安全评估与运维能力，参与过多个网络安全实践项目。",
                "熟悉Web安全及逆向工程，掌握密码学应用技术，参与过Web应用安全漏洞检测与加固项目。"
        ));
        PERSONAL_SUMMARIES.put("飞行器制造工程", Arrays.asList(
                "掌握飞行器制造工程基本理论，熟悉航空材料及装配工艺，具备航空器设计与制造实践能力，参与过多个航空航天工程实训项目。",
                "熟悉数字化制造技术，掌握飞机装配工艺规划方法，参与过某型无人机机身结构设计项目。"
        ));
        PERSONAL_SUMMARIES.put("飞行器动力工程", Arrays.asList(
                "掌握飞行器动力工程基本理论，熟悉航空发动机原理及测试技术，具备航空动力系统分析能力，参与过多个航发工程实训项目。",
                "熟悉燃气轮机原理，掌握发动机性能仿真技术，参与过航空发动机性能仿真分析项目。"
        ));
        PERSONAL_SUMMARIES.put("人文地理与城乡规划", Arrays.asList(
                "掌握人文地理与城乡规划基本理论，熟悉城市规划设计及GIS应用，具备城乡规划与空间分析能力，参与过多个规划设计实训项目。",
                "熟悉ARCGIS软件操作，掌握国土空间规划方法，参与过某县级市国土空间规划项目。"
        ));
        PERSONAL_SUMMARIES.put("环境设计", Arrays.asList(
                "掌握环境设计基本理论，熟悉室内外装饰设计及材料应用，具备环境艺术设计与项目实践能力，参与过多个环境设计实训项目。",
                "熟悉AutoCAD及3ds Max软件，掌握室内设计原理，参与过商业综合体室内空间设计项目。"
        ));
        PERSONAL_SUMMARIES.put("建筑学", Arrays.asList(
                "掌握建筑学基本理论，熟悉建筑设计及建筑构造技术，具备建筑方案设计与表达能力，参与过多个建筑设计实训项目。",
                "熟悉建筑物理及构造技术，掌握Sketchup及Rhino软件，参与过某文化中心建筑设计项目。"
        ));
        PERSONAL_SUMMARIES.put("材料科学与工程", Arrays.asList(
                "掌握材料科学与工程基本理论，熟悉金属材料及复合材料性能，具备材料分析与加工能力，参与过多个材料工程实训项目。",
                "熟悉材料力学性能测试方法，掌握金相分析技术，参与过某钢材焊接工艺评定项目。"
        ));
        PERSONAL_SUMMARIES.put("新能源材料与器件", Arrays.asList(
                "掌握新能源材料与器件基本理论，熟悉光伏材料及储能器件技术，具备新能源材料研发能力，参与过多个新能源材料实践项目。",
                "熟悉电化学测试方法，掌握太阳能电池材料制备技术，参与过钙钛矿太阳能电池制备与性能优化项目。"
        ));
        PERSONAL_SUMMARIES.put("焊接技术与工程", Arrays.asList(
                "掌握焊接技术与工程基本理论，熟悉各种焊接方法及工艺参数，具备焊接工艺设计与质量检测能力，参与过多个焊接实训项目。",
                "熟悉焊接检验技术，掌握特种焊接工艺，参与过某钢结构焊接工艺评定项目。"
        ));
        PERSONAL_SUMMARIES.put("交通工程", Arrays.asList(
                "掌握交通工程基本理论，熟悉交通规划及道路设计方法，具备交通系统分析与设计能力，参与过多个交通工程实训项目。",
                "熟悉交通仿真软件操作，掌握交通组织设计方法，参与过城市交叉口信号配时优化项目。"
        ));
        PERSONAL_SUMMARIES.put("交通运输", Arrays.asList(
                "掌握交通运输基本理论，熟悉城市轨道交通运营管理，具备交通运营组织与调度能力，参与过多个轨道交通实训项目。",
                "熟悉信号系统及列车运行控制技术，掌握车站机电设备运维方法，参与过地铁车站客流组织方案项目。"
        ));
        PERSONAL_SUMMARIES.put("应用统计学", Arrays.asList(
                "掌握应用统计学基本理论，熟悉统计建模及数据分析方法，具备统计分析与数据挖掘能力，参与过多个统计分析实践项目。",
                "熟悉SPSS及Stata软件操作，掌握多元统计分析方法，参与过大学生就业情况调查数据分析项目。"
        ));
        PERSONAL_SUMMARIES.put("应用物理学", Arrays.asList(
                "掌握应用物理学基本理论，熟悉光电技术及材料物理特性，具备物理实验与技术支持能力，参与过多个应用物理实训项目。",
                "熟悉光学薄膜设计方法，掌握光电传感器特性研究技术，参与过LED光源性能测试与分析项目。"
        ));
        PERSONAL_SUMMARIES.put("休闲体育", Arrays.asList(
                "掌握休闲体育基本理论，熟悉体育项目指导及赛事组织，具备体育教学与活动策划能力，参与过多个体育实践项目。",
                "熟悉运动损伤预防与康复技术，掌握体育教学方法，参与过马拉松赛事组织与管理方案项目。"
        ));
        PERSONAL_SUMMARIES.put("英语", Arrays.asList(
                "掌握英语语言学基本理论，熟悉英汉互译及跨文化交际，具备英语教学与翻译实践能力，参与过多个英语实训项目。",
                "熟悉商务英语及翻译技巧，掌握跨文化交际方法，参与过某企业英文宣传材料翻译项目。"
        ));
    }

    /**
     * 按专业划分的学科竞赛与获奖荣誉
     * Map<专业名, Map<比赛级别, List<String[]{ 比赛名称, 获奖等级 }>>>
     */
    public static final Map<String, Map<String, List<String[]>>> MAJOR_AWARDS;
    static {
        MAJOR_AWARDS = new LinkedHashMap<>();

        putAwards("数据科学与大数据技术", "全国大学生数学建模竞赛",
                Arrays.asList(new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"省二等奖", "三等奖"}, new String[]{"成功参赛奖"}));
        putAwards("数据科学与大数据技术", "中国高校计算机大赛-大数据挑战赛",
                Arrays.asList(new String[]{"二等奖", "三等奖"}, new String[]{"华北赛区二等奖", "三等奖"}, new String[]{"优秀奖"}));
        putAwards("数据科学与大数据技术", "全国大学生智能汽车竞赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"华南赛区一等奖", "二等奖"}, new String[]{"省二等奖", "三等奖"}));

        putAwards("智能科学与技术", "全国大学生智能汽车竞赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"华南赛区一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}));
        putAwards("智能科学与技术", "中国机器人暨RoboCup锦标赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"亚军", "季军"}, new String[]{"一等奖", "二等奖"}));
        putAwards("智能科学与技术", "全国大学生机器人大赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"南部赛区一等奖", "二等奖"}, new String[]{"最佳创意奖"}));

        putAwards("网络工程", "全国大学生网络技术挑战赛",
                Arrays.asList(new String[]{"全国一等奖", "二等奖"}, new String[]{"南区特等奖", "一等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("网络工程", "华为ICT大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省级一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("网络工程", "全国大学生信息安全竞赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));

        putAwards("网络空间安全", "全国大学生信息安全竞赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("网络空间安全", "蓝帽杯全国大学生网络安全技能大赛",
                Arrays.asList(new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}, new String[]{"三等奖", "优秀奖"}));
        putAwards("网络空间安全", "华为杯极客大赛",
                Arrays.asList(new String[]{"特等奖", "一等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));

        putAwards("机械设计制造及其自动化", "全国大学生机械创新设计大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("机械设计制造及其自动化", "全国大学生工程训练综合能力竞赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("机械设计制造及其自动化", "全国大学生机器人大赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"二等奖", "三等奖"}, new String[]{"最佳设计奖"}));

        putAwards("机械电子工程", "全国大学生电子设计竞赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("机械电子工程", "全国大学生智能汽车竞赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"华南赛区一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("机械电子工程", "全国大学生工程训练综合能力竞赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));

        putAwards("电气工程及其自动化", "全国大学生电子设计竞赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("电气工程及其自动化", "西门子杯中国智能制造挑战赛",
                Arrays.asList(new String[]{"国家特等奖", "一等奖"}, new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}));
        putAwards("电气工程及其自动化", "全国大学生智能汽车竞赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"华南赛区一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));

        putAwards("自动化", "全国大学生电子设计竞赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("自动化", "全国大学生过程装备实践与创新大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("自动化", "全国大学生化工安全创意大赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));

        putAwards("土木工程", "全国大学生结构设计竞赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省特等奖", "一等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("土木工程", "全国大学生力学竞赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("土木工程", "全国大学生房地产策划大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"优秀策划奖"}));

        putAwards("化学工程与工艺", "全国大学生化工设计竞赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"华中赛区一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("化学工程与工艺", "全国大学生化学实验创新大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("化学工程与工艺", "全国大学生化工安全创意大赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));

        putAwards("制药工程", "全国大学生药学/中药学实验技能大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("制药工程", "全国大学生生命科学竞赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("制药工程", "全国大学生制药工程设计大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}, new String[]{"最佳创意奖"}));

        putAwards("生物制药", "全国大学生生命科学竞赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("生物制药", "全国大学生药学/中药学实验技能大赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("生物制药", "全国大学生生物学竞赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));

        putAwards("工程造价", "全国大学生算量大赛",
                Arrays.asList(new String[]{"全国冠军", "一等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));
        putAwards("工程造价", "全国高校BIM毕业设计大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));
        putAwards("工程造价", "全国大学生房地产策划大赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"优秀策划奖"}));

        putAwards("工程管理", "全国大学生房地产策划大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("工程管理", "全国大学生BIM大赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"最佳项目管理奖"}));
        putAwards("工程管理", "全国大学生采购模拟大赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));

        putAwards("投资学", "全国大学生金融创新大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("投资学", "全国大学生模拟炒股大赛",
                Arrays.asList(new String[]{"全国十强", "二等奖"}, new String[]{"二等奖", "三等奖"}, new String[]{"最佳收益奖", "最佳策略奖"}));
        putAwards("投资学", "全国大学生保险综合业务大赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));

        putAwards("资源勘查工程", "全国大学生地质技能竞赛",
                Arrays.asList(new String[]{"国家特等奖", "一等奖"}, new String[]{"国家一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("资源勘查工程", "全国大学生地球物理知识竞赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));

        putAwards("环境工程", "全国大学生环境生态科技大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("环境工程", "全国大学生化工安全创意大赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));

        putAwards("地质工程", "全国大学生地质技能竞赛",
                Arrays.asList(new String[]{"国家特等奖", "一等奖"}, new String[]{"国家一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("地质工程", "全国大学生工程地质大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}));

        putAwards("新能源科学与工程", "全国大学生可再生能源科技竞赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("新能源科学与工程", "全国大学生光电设计竞赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));

        putAwards("应用统计学", "全国大学生统计建模大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("应用统计学", "全国大学生数学建模竞赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("应用统计学", "全国大学生市场调查与分析大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));

        putAwards("飞行器制造工程", "中国国际飞行器设计挑战赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("飞行器制造工程", "全国大学生机械创新设计大赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));

        putAwards("交通工程", "全国大学生交通科技大赛",
                Arrays.asList(new String[]{"国家一等奖", "二等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("交通工程", "全国大学生智能汽车竞赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"华南赛区一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));

        putAwards("英语", "外研社杯全国大学生英语演讲/写作/阅读大赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"省一等奖", "二等奖"}, new String[]{"一等奖", "二等奖"}));
        putAwards("英语", "全国大学生翻译大赛",
                Arrays.asList(new String[]{"国家二等奖", "三等奖"}, new String[]{"一等奖", "二等奖"}, new String[]{"二等奖", "三等奖"}));
    }

    // 帮助方法：向 MAJOR_AWARDS 添加条目
    private static void putAwards(String major, String contest, List<String[]> levels) {
        if (!MAJOR_AWARDS.containsKey(major)) {
            MAJOR_AWARDS.put(major, new LinkedHashMap<>());
        }
        MAJOR_AWARDS.get(major).put(contest, levels);
    }

    // ==================== 期望岗位（按专业） ====================
    public static final Map<String, List<String>> EXPECTED_POSITIONS;
    static {
        EXPECTED_POSITIONS = new LinkedHashMap<>();
        EXPECTED_POSITIONS.put("资源勘查工程", Arrays.asList("地质工程师", "勘查技术员", "地质调查员", "矿产评估师"));
        EXPECTED_POSITIONS.put("环境工程", Arrays.asList("环保工程师", "环境影响评价师", "污水处理工程师", "环境监测员"));
        EXPECTED_POSITIONS.put("地质工程", Arrays.asList("地质工程师", "岩土工程师", "工程地质勘察员", "地质灾害评估师"));
        EXPECTED_POSITIONS.put("机械设计制造及其自动化", Arrays.asList("机械设计工程师", "工艺工程师", "数控工程师", "自动化工程师"));
        EXPECTED_POSITIONS.put("机械电子工程", Arrays.asList("机电工程师", "嵌入式工程师", "自动化控制工程师", "技术支持工程师"));
        EXPECTED_POSITIONS.put("电气工程及其自动化", Arrays.asList("电气工程师", "电力系统工程师", "自动化工程师", "PLC工程师"));
        EXPECTED_POSITIONS.put("自动化", Arrays.asList("自动化工程师", "控制工程师", "DCS工程师", "仪表工程师"));
        EXPECTED_POSITIONS.put("土木工程", Arrays.asList("土建工程师", "结构工程师", "施工员", "项目经理"));
        EXPECTED_POSITIONS.put("水利水电工程", Arrays.asList("水利工程师", "水工设计师", "施工工程师", "监理工程师"));
        EXPECTED_POSITIONS.put("道路桥梁与渡河工程", Arrays.asList("路桥工程师", "桥梁设计师", "施工工程师", "交通规划师"));
        EXPECTED_POSITIONS.put("化学工程与工艺", Arrays.asList("化工工程师", "工艺工程师", "研发工程师", "安全工程师"));
        EXPECTED_POSITIONS.put("新能源科学与工程", Arrays.asList("光伏工程师", "风电工程师", "储能工程师", "电气工程师"));
        EXPECTED_POSITIONS.put("过程装备与控制工程", Arrays.asList("装备工程师", "化工设备工程师", "工艺工程师", "技术支持工程师"));
        EXPECTED_POSITIONS.put("制药工程", Arrays.asList("制药工程师", "药品研发工程师", "工艺工程师", "QA工程师"));
        EXPECTED_POSITIONS.put("生物制药", Arrays.asList("生物制药工程师", "研发工程师", "质量工程师", "技术支持工程师"));
        EXPECTED_POSITIONS.put("酿酒工程", Arrays.asList("酿酒工程师", "品酒师", "工艺工程师", "质量控制工程师"));
        EXPECTED_POSITIONS.put("安全工程", Arrays.asList("安全工程师", "安全评价师", "安全主管", "EHS工程师"));
        EXPECTED_POSITIONS.put("采矿工程", Arrays.asList("采矿工程师", "矿山工程师", "安全工程师", "技术员"));
        EXPECTED_POSITIONS.put("智能采矿工程", Arrays.asList("智慧矿山工程师", "采矿工程师", "自动化工程师", "技术员"));
        EXPECTED_POSITIONS.put("工程管理", Arrays.asList("项目管理员", "施工管理员", "监理工程师", "咨询工程师"));
        EXPECTED_POSITIONS.put("工程造价", Arrays.asList("造价工程师", "预算员", "造价员", "招标代理"));
        EXPECTED_POSITIONS.put("投资学", Arrays.asList("投资分析师", "金融分析师", "理财顾问", "客户经理"));
        EXPECTED_POSITIONS.put("网络工程", Arrays.asList("网络工程师", "系统工程师", "运维工程师", "网络安全工程师"));
        EXPECTED_POSITIONS.put("数据科学与大数据技术", Arrays.asList("数据分析师", "大数据开发工程师", "算法工程师", "数据挖掘工程师"));
        EXPECTED_POSITIONS.put("智能科学与技术", Arrays.asList("AI工程师", "算法工程师", "图像算法工程师", "NLP工程师"));
        EXPECTED_POSITIONS.put("网络空间安全", Arrays.asList("网络安全工程师", "渗透测试工程师", "安全运维工程师", "安全分析师"));
        EXPECTED_POSITIONS.put("飞行器制造工程", Arrays.asList("航空制造工程师", "工艺工程师", "结构工程师", "质量工程师"));
        EXPECTED_POSITIONS.put("飞行器动力工程", Arrays.asList("航发工程师", "燃烧工程师", "性能工程师", "测试工程师"));
        EXPECTED_POSITIONS.put("人文地理与城乡规划", Arrays.asList("规划设计师", "城乡规划师", "GIS工程师", "研究员"));
        EXPECTED_POSITIONS.put("环境设计", Arrays.asList("室内设计师", "环境设计师", "软装设计师", "施工图设计师"));
        EXPECTED_POSITIONS.put("建筑学", Arrays.asList("建筑设计师", "建筑方案设计师", "建筑工程师", "BIM工程师"));
        EXPECTED_POSITIONS.put("材料科学与工程", Arrays.asList("材料工程师", "材料研发工程师", "质量工程师", "检测工程师"));
        EXPECTED_POSITIONS.put("新能源材料与器件", Arrays.asList("材料研发工程师", "光伏工程师", "电池工程师", "工艺工程师"));
        EXPECTED_POSITIONS.put("焊接技术与工程", Arrays.asList("焊接工程师", "焊接工艺工程师", "质检工程师", "技术支持工程师"));
        EXPECTED_POSITIONS.put("交通工程", Arrays.asList("交通规划师", "交通工程师", "交通设计师", "交通仿真工程师"));
        EXPECTED_POSITIONS.put("交通运输", Arrays.asList("轨道运营员", "调度员", "信号工程师", "站务员"));
        EXPECTED_POSITIONS.put("应用统计学", Arrays.asList("统计分析师", "数据分析师", "市场分析师", "调查研究员"));
        EXPECTED_POSITIONS.put("应用物理学", Arrays.asList("技术支持工程师", "研发工程师", "测试工程师", "光学工程师"));
        EXPECTED_POSITIONS.put("休闲体育", Arrays.asList("体育教练", "赛事运营", "体育营销", "健身指导员"));
        EXPECTED_POSITIONS.put("英语", Arrays.asList("英语翻译", "英语教师", "外贸业务员", "跨境电商运营"));
    }

    // ==================== 主修课程（按专业） ====================
    public static final Map<String, List<String>> MAJOR_COURSES;
    static {
        MAJOR_COURSES = new LinkedHashMap<>();
        MAJOR_COURSES.put("资源勘查工程", Arrays.asList("矿床学", "矿产勘查学", "地球化学勘探", "地质遥感", "GIS应用", "矿产资源评价", "工程地质学", "构造地质学", "环境地质学", "岩土力学"));
        MAJOR_COURSES.put("环境工程", Arrays.asList("环境化学", "环境监测", "水污染控制工程", "大气污染控制工程", "固体废物处理与处置", "环境影响评价", "环境微生物学", "环境毒理学"));
        MAJOR_COURSES.put("地质工程", Arrays.asList("工程地质学", "岩土力学", "地质灾害评估", "勘察技术方法", "土力学", "地基处理", "地质工程CAD", "基础工程"));
        MAJOR_COURSES.put("机械设计制造及其自动化", Arrays.asList("机械制图", "机械设计", "机械制造技术", "数控技术", "液压与气压传动", "PLC控制技术", "控制工程基础", "机械工程材料"));
        MAJOR_COURSES.put("机械电子工程", Arrays.asList("机械设计基础", "电路原理", "电子技术", "PLC控制", "传感器与检测技术", "机电传动控制", "单片机原理", "电机拖动"));
        MAJOR_COURSES.put("电气工程及其自动化", Arrays.asList("电路原理", "模拟电子技术", "数字电子技术", "电力系统分析", "PLC控制技术", "继电保护", "供配电技术", "电力电子技术"));
        MAJOR_COURSES.put("自动化", Arrays.asList("自动控制原理", "DCS系统", "PLC控制技术", "过程控制", "仪表与传感器", "工业网络技术", "控制系统仿真", "运动控制"));
        MAJOR_COURSES.put("土木工程", Arrays.asList("结构力学", "混凝土结构设计原理", "钢结构设计", "土力学与地基基础", "建筑施工技术", "工程项目管理", "工程力学", "砌体结构"));
        MAJOR_COURSES.put("水利水电工程", Arrays.asList("水力学", "水工建筑物", "水利工程施工", "水电站建筑物", "水利工程测量", "工程项目管理", "工程水文学", "水工钢结构"));
        MAJOR_COURSES.put("道路桥梁与渡河工程", Arrays.asList("道路勘测设计", "路基路面工程", "桥梁工程", "隧道工程", "交通工程", "施工技术", "道路工程材料", "桥梁基础工程"));
        MAJOR_COURSES.put("化学工程与工艺", Arrays.asList("化工原理", "化学反应工程", "化工分离技术", "化工工艺学", "化工热力学", "化学反应器设计", "化工仪表及自动化", "化工安全工程"));
        MAJOR_COURSES.put("新能源科学与工程", Arrays.asList("光伏发电原理", "风力发电技术", "储能技术", "新能源转换与利用", "光伏材料", "电网技术", "电力系统基础", "能源互联网"));
        MAJOR_COURSES.put("过程装备与控制工程", Arrays.asList("化工原理", "过程装备设计", "过程控制技术", "压力容器设计", "流体机械", "PLC应用", "化工设备腐蚀与防护", "过程装备CAD"));
        MAJOR_COURSES.put("制药工程", Arrays.asList("药物化学", "药物制剂学", "制药工艺学", "药品生产质量管理", "药物分析", "制药设备", "生物化学", "药理学基础"));
        MAJOR_COURSES.put("生物制药", Arrays.asList("生物化学", "微生物学", "基因工程药物", "生物制药工艺学", "药物制剂技术", "生物制品检验", "细胞生物学", "免疫学"));
        MAJOR_COURSES.put("酿酒工程", Arrays.asList("酿酒微生物学", "酒类工艺学", "酿酒设备", "酒体设计", "发酵工程", "酒类品评技术", "酿酒化学", "酒类质量控制"));
        MAJOR_COURSES.put("安全工程", Arrays.asList("安全学原理", "安全系统工程", "安全管理学", "安全评价", "矿山安全工程", "安全监测技术", "职业卫生工程", "应急管理"));
        MAJOR_COURSES.put("采矿工程", Arrays.asList("采矿学", "矿山岩体力学", "矿井通风与安全", "采矿工艺", "矿山设计原理", "矿业经济", "矿山机械", "爆破工程"));
        MAJOR_COURSES.put("智能采矿工程", Arrays.asList("采矿学", "智能矿山技术", "矿山物联网", "无人采矿技术", "矿山机械自动化", "矿业大数据", "矿山系统工程", "智能感知技术"));
        MAJOR_COURSES.put("工程管理", Arrays.asList("工程项目管理", "工程经济学", "施工组织设计", "工程合同管理", "工程造价管理", "房地产经营与管理", "建设法规", "工程项目评估"));
        MAJOR_COURSES.put("工程造价", Arrays.asList("工程造价管理", "工程经济学", "工程定额原理", "建筑工程计量与计价", "安装工程计量与计价", "BIM技术应用", "工程合同管理", "造价软件应用"));
        MAJOR_COURSES.put("投资学", Arrays.asList("投资学", "金融市场与工具", "公司金融", "证券投资分析", "投资组合管理", "风险管理", "公司并购", "衍生金融工具"));
        MAJOR_COURSES.put("网络工程", Arrays.asList("计算机网络", "路由交换技术", "网络安全技术", "Linux服务器管理", "网络工程设计与施工", "云计算技术", "SDN技术", "网络程序设计"));
        MAJOR_COURSES.put("数据科学与大数据技术", Arrays.asList("数据结构", "Python程序设计", "Hadoop", "Spark", "Hive", "数据可视化", "机器学习", "数据挖掘", "统计学基础"));
        MAJOR_COURSES.put("智能科学与技术", Arrays.asList("人工智能", "机器学习", "深度学习", "模式识别", "自然语言处理", "数据挖掘", "智能系统", "计算机视觉", "知识图谱"));
        MAJOR_COURSES.put("网络空间安全", Arrays.asList("计算机网络", "网络安全基础", "密码学", "渗透测试", "逆向工程", "网络安全运维", "Web安全", "数据安全"));
        MAJOR_COURSES.put("飞行器制造工程", Arrays.asList("飞行器结构", "飞行器制造工艺", "航空材料", "装配工艺", "数字化制造", "无人机技术", "飞机设计基础", "航空制造装备"));
        MAJOR_COURSES.put("飞行器动力工程", Arrays.asList("航空发动机原理", "燃气轮机原理", "航空发动机测试", "燃烧技术", "发动机设计", "航空发动机控制", "叶轮机械", "发动机故障诊断"));
        MAJOR_COURSES.put("人文地理与城乡规划", Arrays.asList("人文地理学", "城市规划原理", "GIS应用", "城市设计", "景观规划", "土地利用规划", "城市经济学", "规划管理与法规"));
        MAJOR_COURSES.put("环境设计", Arrays.asList("室内设计原理", "建筑装饰材料", "居室设计", "公共空间设计", "设计表现技法", "计算机辅助设计", "人体工程学", "环境照明设计"));
        MAJOR_COURSES.put("建筑学", Arrays.asList("建筑设计原理", "建筑构造", "建筑力学", "中外建筑史", "建筑物理", "计算机辅助建筑设计", "城市规划设计", "建筑结构选型"));
        MAJOR_COURSES.put("材料科学与工程", Arrays.asList("材料科学基础", "材料力学", "材料加工工艺", "金属材料学", "材料分析方法", "材料腐蚀与防护", "复合材料", "材料热处理"));
        MAJOR_COURSES.put("新能源材料与器件", Arrays.asList("新能源材料", "太阳能电池材料", "储能材料", "材料分析与表征", "电化学基础", "器件设计", "材料制备技术", "光伏技术基础"));
        MAJOR_COURSES.put("焊接技术与工程", Arrays.asList("焊接方法与工艺", "焊接结构", "焊接电源", "焊接检验技术", "焊接自动化", "特种焊接技术", "焊接冶金基础", "焊接工艺评定"));
        MAJOR_COURSES.put("交通工程", Arrays.asList("交通工程学", "交通规划", "交通设计", "交通管理与控制", "道路工程", "交通安全工程", "交通仿真", "交通经济与管理"));
        MAJOR_COURSES.put("交通运输", Arrays.asList("城市轨道交通概论", "轨道交通运营管理", "列车运行控制", "信号系统", "车站机电设备", "客运组织", "行车组织", "轨道交通土建工程"));
        MAJOR_COURSES.put("应用统计学", Arrays.asList("概率论与数理统计", "统计软件", "时间序列分析", "多元统计分析", "抽样调查", "数据可视化", "回归分析", "实验设计"));
        MAJOR_COURSES.put("应用物理学", Arrays.asList("光学", "电磁学", "量子力学", "光电检测技术", "材料物理", "物理学实验技术", "半导体物理", "光电子技术"));
        MAJOR_COURSES.put("休闲体育", Arrays.asList("体育概论", "休闲体育项目", "赛事组织与管理", "体育市场营销", "运动损伤与康复", "体育教学法", "体育管理学", "户外运动指导"));
        MAJOR_COURSES.put("英语", Arrays.asList("基础英语", "高级英语", "英汉翻译", "口译技巧", "跨文化交际", "英语教学法", "语言学概论", "商务英语"));
    }

    // ==================== 项目经验池（按专业） ====================
    public static final Map<String, List<String>> PROJECTS;
    static {
        PROJECTS = new LinkedHashMap<>();
        PROJECTS.put("资源勘查工程", Arrays.asList("贵州省某铅锌矿地质勘查", "矿区土壤重金属污染调查", "某水库大坝地质勘察", "岩溶地区隧道工程地质调查", "城市地下管线探测"));
        PROJECTS.put("环境工程", Arrays.asList("城市污水处理厂工艺设计", "某工业园区环境影响评价", "农村生活污水治理方案", "城市空气质量监测分析", "固体废物处理厂设计"));
        PROJECTS.put("地质工程", Arrays.asList("某高速公路边坡稳定性分析", "岩溶地区隧道工程地质勘察", "滑坡灾害危险性评估", "某矿山岩体力学测试", "城市地质信息系统设计"));
        PROJECTS.put("机械设计制造及其自动化", Arrays.asList("机械零件设计及数控加工", "自动化生产线上料系统设计", "工业机器人末端夹具设计", "三维建模及工艺分析", "机电系统综合设计"));
        PROJECTS.put("机械电子工程", Arrays.asList("基于PLC的自动化生产线控制系统设计", "智能小车循迹与避障系统设计", "工业传感器数据采集系统", "基于单片机的温湿度监测系统", "自动化立体仓库控制设计"));
        PROJECTS.put("电气工程及其自动化", Arrays.asList("基于PLC的电机控制系统设计", "某工厂配电系统设计", "智能配电箱监控系统", "工厂变配电所综合自动化设计", "智能照明控制系统"));
        PROJECTS.put("自动化", Arrays.asList("小型过程控制实验装置设计", "DCS控制系统组态与调试", "工业锅炉自动控制系统", "基于PID的温度控制系统设计", "工业机器人搬运系统集成"));
        PROJECTS.put("土木工程", Arrays.asList("某框架结构教学楼设计", "混凝土配合比设计及性能测试", "建筑施工组织设计", "高层建筑结构设计", "土方工程量计算及施工方案"));
        PROJECTS.put("水利水电工程", Arrays.asList("某水库除险加固设计方案", "水电站厂房结构设计", "灌溉渠道设计", "大坝渗流分析", "水闸结构设计"));
        PROJECTS.put("道路桥梁与渡河工程", Arrays.asList("某城市道路交叉口设计", "预应力混凝土简支梁桥设计", "桥梁施工组织设计", "隧道施工方案设计", "挡土墙设计计算"));
        PROJECTS.put("化学工程与工艺", Arrays.asList("年产5万吨甲醇合成工艺设计", "反应精馏分离工艺开发", "化工单元操作实验装置设计", "精馏塔工艺设计", "换热器设计与优化"));
        PROJECTS.put("新能源科学与工程", Arrays.asList("家用光伏发电系统设计", "小型风力发电机组设计", "储能电池管理系统设计", "光伏组件发电效率分析", "智能微电网系统设计"));
        PROJECTS.put("过程装备与控制工程", Arrays.asList("换热器设计及工艺计算", "压力容器壳体设计", "化工管路系统设计", "储罐设计及强度校核", "塔器结构设计"));
        PROJECTS.put("制药工程", Arrays.asList("口服固体制剂工艺优化", "某药品生产车间设计", "药物中间体合成工艺", "中药提取工艺研究", "药品质量分析方法开发"));
        PROJECTS.put("生物制药", Arrays.asList("重组蛋白药物生产工艺设计", "单克隆抗体分离纯化工艺", "疫苗生产质量控制研究", "细胞培养工艺优化", "酶制剂生产工艺开发"));
        PROJECTS.put("酿酒工程", Arrays.asList("酱香型白酒酿造工艺优化", "葡萄酒发酵工艺设计", "酿酒副产品综合利用", "酒类品质检测与分析", "新型果酒酿造工艺研究"));
        PROJECTS.put("安全工程", Arrays.asList("某煤矿安全评价报告", "化工园区多米诺效应分析", "职业病危害因素检测与评价", "企业应急预案编制", "危化品储存安全评估"));
        PROJECTS.put("采矿工程", Arrays.asList("某地下煤矿开采方案设计", "露天矿台阶爆破参数设计", "矿井通风系统优化设计", "矿山充填系统设计", "采矿方法选择与设计"));
        PROJECTS.put("智能采矿工程", Arrays.asList("基于物联网的矿山安全监测系统", "智能矿山数据平台设计", "无人采矿工作面控制系统", "矿山车辆调度系统开发", "矿山通风智能控制设计"));
        PROJECTS.put("工程管理", Arrays.asList("某住宅小区施工项目管理策划", "建设工程项目进度计划编制", "工程项目成本控制分析", "施工安全管理方案设计", "房地产开发项目可行性分析"));
        PROJECTS.put("工程造价", Arrays.asList("某办公楼工程量清单编制", "建设工程造价指标分析", "施工图预算编制", "投标报价策略分析", "工程变更与索赔分析"));
        PROJECTS.put("投资学", Arrays.asList("上市公司股票投资价值分析", "投资组合优化模型构建", "某基金项目可行性分析", "企业估值模型构建", "债券投资风险分析"));
        PROJECTS.put("网络工程", Arrays.asList("某企业网络架构设计与实施", "校园网安全防护方案设计", "云计算平台网络规划", "中小企业信息化建设方案", "网络性能监控与优化"));
        PROJECTS.put("数据科学与大数据技术", Arrays.asList("基于Spark的学生就业数据分析", "电商用户行为数据挖掘", "城市交通流量预测系统", "社交媒体舆情分析系统", "金融风控模型构建"));
        PROJECTS.put("智能科学与技术", Arrays.asList("图像识别在智能交通中的应用", "自然语言处理情感分析系统", "智能推荐算法在求职平台中的实现", "车牌识别系统开发", "智能问答机器人设计"));
        PROJECTS.put("网络空间安全", Arrays.asList("Web应用安全漏洞检测与加固", "企业网络安全防护方案", "渗透测试与安全评估", "数据加密与隐私保护方案", "工控系统安全评估"));
        PROJECTS.put("飞行器制造工程", Arrays.asList("某型无人机机身结构设计", "航空发动机叶片工艺分析", "飞机装配工艺规划", "航空结构复合材料应用分析", "数字化装配工艺设计"));
        PROJECTS.put("飞行器动力工程", Arrays.asList("航空发动机性能仿真分析", "某型燃气轮机燃烧室设计", "发动机试验数据采集与分析", "航空发动机振动分析", "涡轮叶片冷却结构设计"));
        PROJECTS.put("人文地理与城乡规划", Arrays.asList("某县级市国土空间规划", "历史文化街区保护与更新设计", "城市公园景观规划设计", "某乡镇总体规划设计", "城市更新改造规划方案"));
        PROJECTS.put("环境设计", Arrays.asList("商业综合体室内空间设计", "民宿设计方案", "城市家具设计", "办公空间设计方案", "餐饮空间室内设计"));
        PROJECTS.put("建筑学", Arrays.asList("某文化中心建筑设计", "山地建筑设计", "旧建筑改造设计", "住宅小区规划设计", "校园建筑群设计方案"));
        PROJECTS.put("材料科学与工程", Arrays.asList("某钢材焊接工艺评定", "铝合金热处理工艺优化", "建筑材料耐久性研究", "金属材料腐蚀行为分析", "复合材料力学性能测试"));
        PROJECTS.put("新能源材料与器件", Arrays.asList("钙钛矿太阳能电池制备与性能优化", "锂电池正极材料研究", "燃料电池关键材料分析", "超级电容器电极材料制备", "光伏组件可靠性测试"));
        PROJECTS.put("焊接技术与工程", Arrays.asList("某钢结构焊接工艺评定", "铝合金MIG焊接工艺研究", "焊接机器人编程与调试", "管道自动焊接工艺开发", "焊接缺陷分析与质量控制"));
        PROJECTS.put("交通工程", Arrays.asList("城市交叉口信号配时优化", "某区域交通组织设计", "交通影响评价报告", "公交线路优化设计", "停车场交通组织设计"));
        PROJECTS.put("交通运输", Arrays.asList("地铁车站客流组织方案", "列车运行图编制与调整", "城市轨道交通运力评估", "轨道交通应急预案设计", "车站设备调度优化"));
        PROJECTS.put("应用统计学", Arrays.asList("某地区居民收入统计分析", "大学生就业情况调查数据分析", "时间序列预测模型构建", "上市公司财务风险预警模型", "市场调研数据分析报告"));
        PROJECTS.put("应用物理学", Arrays.asList("光学薄膜设计与制备", "LED光源性能测试与分析", "光电传感器特性研究", "光纤通信系统性能分析", "太阳能电池转换效率研究"));
        PROJECTS.put("休闲体育", Arrays.asList("马拉松赛事组织与管理方案", "青少年体育培训课程设计", "社区体育活动策划", "健身房运营管理方案", "体育赛事商业赞助策划"));
        PROJECTS.put("英语", Arrays.asList("某企业英文宣传材料翻译", "跨文化商务沟通案例分析", "旅游景区英语导览系统设计", "商务合同英汉互译实践", "国际会议同声传译模拟"));
    }

    // ==================== 技术栈池（按专业） ====================
    public static final Map<String, List<String>> SKILL_POOLS;
    static {
        SKILL_POOLS = new LinkedHashMap<>();
        SKILL_POOLS.put("资源勘查工程", Arrays.asList("MapGIS", "AutoCAD", "ArcGIS", "QGIS", "ENVI", "Surfer", "3DMine", "Dimine"));
        SKILL_POOLS.put("环境工程", Arrays.asList("AutoCAD", "CADWorx", "MATLAB", "HEC-RAS", "SWMM", "AERMOD", "WQAM", "Python"));
        SKILL_POOLS.put("地质工程", Arrays.asList("AutoCAD", "Plaxis", "MIDAS", "ANSYS", "FLAC3D", "理正软件", "PKPM", "GEO5"));
        SKILL_POOLS.put("机械设计制造及其自动化", Arrays.asList("SolidWorks", "AutoCAD", "CATIA", "Pro/E", "MasterCAM", "UG", "ANSYS", "PLC编程"));
        SKILL_POOLS.put("机械电子工程", Arrays.asList("Altium Designer", "Keil", "Proteus", "MATLAB/Simulink", "PLC", "EPLAN", "电路板设计", "嵌入式C"));
        SKILL_POOLS.put("电气工程及其自动化", Arrays.asList("MATLAB", "AutoCAD Electrical", "PLC", "EPLAN", "ETAP", "PSCAD", "RTDS", "电气仿真"));
        SKILL_POOLS.put("自动化", Arrays.asList("MATLAB/Simulink", "PLC", "DCS", "WinCC", "IFIX", "组态王", "PID参数整定", "OPC通讯"));
        SKILL_POOLS.put("土木工程", Arrays.asList("PKPM", "盈建科", "Midas", "SAP2000", "ANSYS", "Revit", "Navisworks", "BIM"));
        SKILL_POOLS.put("水利水电工程", Arrays.asList("AutoCAD", "PKPM", "Midas", "HEC-RAS", "MIKE", "GIS", "水文计算", "水利工程概算"));
        SKILL_POOLS.put("道路桥梁与渡河工程", Arrays.asList("纬地软件", "鸿业软件", "MIDAS", "桥梁博士", "ANSYS", "SAP2000", "Autocad", "Civil 3D"));
        SKILL_POOLS.put("化学工程与工艺", Arrays.asList("Aspen Plus", "HYSYS", "PRO/II", "MATLAB", "ASPEN", "化工设计软件", "HazardPro", "LIMS"));
        SKILL_POOLS.put("新能源科学与工程", Arrays.asList("PVsyst", " HOMER", "MATLAB", "ETAP", "Comsol", "Python", "Excel建模", "能源仿真"));
        SKILL_POOLS.put("过程装备与控制工程", Arrays.asList("Aspen Plus", "CADWorx", "PV Elite", "ANSYS", "COMSOL", "SW6", "压力容器设计", "GMP规范"));
        SKILL_POOLS.put("制药工程", Arrays.asList("AutoCAD", "GMP", "FDA法规", "药物分析软件", "HPLC", "GC-MS", "工艺流程图", "GXP"));
        SKILL_POOLS.put("生物制药", Arrays.asList("HPLC", "GC-MS", "PCR", "流式细胞仪", "Western Blot", "细胞培养", "蛋白质纯化", "ELISA"));
        SKILL_POOLS.put("酿酒工程", Arrays.asList("气相色谱", "品酒分析", "HPLC", "发酵工艺", "食品安全检测", "QS认证", "CAD", "酿酒工艺设计"));
        SKILL_POOLS.put("安全工程", Arrays.asList("AQ/T9006", "JSA", "HAZOP", "LOPA", "SIL定级", "风险矩阵", "安全评价软件", "应急预案编制"));
        SKILL_POOLS.put("采矿工程", Arrays.asList("Dimine", "3DMine", "Surpac", "FLAC3D", "ANSYS", "CAD", "矿井通风模拟", "采矿CAD"));
        SKILL_POOLS.put("智能采矿工程", Arrays.asList("Python", "MATLAB", "机器视觉", "工业机器人", "PLC", "SCADA", "工业物联网", "数字孪生"));
        SKILL_POOLS.put("工程管理", Arrays.asList("Project", "P6", "Revit", "BIM5D", "斑马进度计划", "广联达", "梦龙", "横道图软件"));
        SKILL_POOLS.put("工程造价", Arrays.asList("广联达", "鲁班软件", "斯维尔", "PKPM", "BIM算量", "Excel", "定额软件", "清单计价软件"));
        SKILL_POOLS.put("投资学", Arrays.asList("Wind终端", "Python", "MATLAB", "R语言", "EViews", "SPSS", "Bloomberg", "量化交易平台"));
        SKILL_POOLS.put("网络工程", Arrays.asList("Wireshark", "Cisco Packet Tracer", "eNSP", "GNS3", "Linux", "Nessus", "WAF", "防火墙配置"));
        SKILL_POOLS.put("数据科学与大数据技术", Arrays.asList("Python", "Spark", "Hadoop", "Hive", "SQL", "Pandas", "NumPy", "Scikit-learn", "TensorFlow", "Pycharm", "Jupyter"));
        SKILL_POOLS.put("智能科学与技术", Arrays.asList("Python", "TensorFlow", "PyTorch", "OpenCV", "Keras", "NLTK", "SpaCy", "Jupyter", "Scikit-learn"));
        SKILL_POOLS.put("网络空间安全", Arrays.asList("Nmap", "Burp Suite", "Metasploit", "Wireshark", "Kali Linux", "SQL注入工具", "逆向分析", "密码破解工具"));
        SKILL_POOLS.put("飞行器制造工程", Arrays.asList("CATIA", "UG", "SolidWorks", "ABAQUS", "ANSYS", "数字化装配", "复合材料设计", "航电系统"));
        SKILL_POOLS.put("飞行器动力工程", Arrays.asList("ANSYS", "Fluent", "GT-Power", "MATLAB", "发动机仿真", "燃烧模拟", "叶轮机械设计", "CFD"));
        SKILL_POOLS.put("人文地理与城乡规划", Arrays.asList("ArcGIS", "AutoCAD", "Photoshop", "GIS空间分析", "GIS可视化", "ENVI", "QGIS", "规划软件"));
        SKILL_POOLS.put("环境设计", Arrays.asList("AutoCAD", "3ds Max", "SketchUp", "V-Ray", "Photoshop", "Lumion", "Enscape", "VRay渲染"));
        SKILL_POOLS.put("建筑学", Arrays.asList("Rhino", "Grasshopper", "Revit", "AutoCAD", "V-Ray", "Lumion", "SketchUp", "BIM"));
        SKILL_POOLS.put("材料科学与工程", Arrays.asList("ANSYS", "JMatPro", "Origin", "金相分析", "热处理工艺", "材料力学测试", "SEM分析", "XRD分析"));
        SKILL_POOLS.put("新能源材料与器件", Arrays.asList("电化学工作站", "SEM", "XRD", "电化学测试", "电池测试系统", "Materials Studio", "Comsol", "涂布工艺"));
        SKILL_POOLS.put("焊接技术与工程", Arrays.asList("焊接工艺评定", "无损检测", "UT", "PT", "MT", "RT", "焊接技能证书", "焊接工艺规程"));
        SKILL_POOLS.put("交通工程", Arrays.asList("TransCAD", "VISSIM", "EMME", "Visum", "MATLAB", "TransModeler", "交通仿真", "CAD"));
        SKILL_POOLS.put("交通运输", Arrays.asList("地铁信号系统", "列车运行控制", "OCC调度系统", "ATS系统", "综合监控", "PLC", "SCADA", "信号基础"));
        SKILL_POOLS.put("应用统计学", Arrays.asList("SPSS", "Stata", "R语言", "EViews", "SAS", "Excel", "Python", "Tableau"));
        SKILL_POOLS.put("应用物理学", Arrays.asList("MATLAB", "Origin", "LabVIEW", "Comsol", "OptiFDTD", "Zemax", "物理学实验", "光电检测"));
        SKILL_POOLS.put("休闲体育", Arrays.asList("体育管理", "赛事运营", "健身指导", "运动康复", "体育营销", "户外拓展", "赛事策划", "体能测试"));
        SKILL_POOLS.put("英语", Arrays.asList("翻译软件", "CAT工具", "Trados", "跨文化交际", "TESOL", "语言测试", "口译技巧", "商务英语"));
    }

    // ==================== 证书池（按专业） ====================
    public static final Map<String, List<String>> CERT_POOLS;
    static {
        CERT_POOLS = new LinkedHashMap<>();
        CERT_POOLS.put("default", Arrays.asList("CET-4证书", "CET-6证书", "计算机二级Java证书", "普通话二级甲等证书"));
        CERT_POOLS.put("资源勘查工程", Arrays.asList("CET-4证书", "CET-6证书", "计算机二级C++证书", "地质工程师基础考试合格证书", "MapGIS认证证书"));
        CERT_POOLS.put("环境工程", Arrays.asList("CET-4证书", "CET-6证书", "计算机二级证书", "环境影响评价工程师基础考试合格证书", "普通话二级甲等证书"));
        CERT_POOLS.put("地质工程", Arrays.asList("CET-4证书", "CET-6证书", "计算机二级证书", "岩土工程师基础考试合格证书", "普通话二级甲等证书"));
        CERT_POOLS.put("机械设计制造及其自动化", Arrays.asList("CET-4证书", "CET-6证书", "CAD工程师认证证书", "计算机二级C语言证书", "数控车工证书"));
        CERT_POOLS.put("机械电子工程", Arrays.asList("CET-4证书", "CET-6证书", "PLC工程师证书", "计算机二级C++证书", "嵌入式系统设计师证书"));
        CERT_POOLS.put("电气工程及其自动化", Arrays.asList("CET-4证书", "CET-6证书", "电工职业资格证书（高级）", "计算机二级C语言证书", "电气工程师证书"));
        CERT_POOLS.put("自动化", Arrays.asList("CET-4证书", "CET-6证书", "PLC工程师证书", "计算机二级证书", "DCS工程师证书"));
        CERT_POOLS.put("土木工程", Arrays.asList("CET-4证书", "CET-6证书", "CAD工程师认证证书", "二级建造师考试合格证书", "BIM一级证书"));
        CERT_POOLS.put("水利水电工程", Arrays.asList("CET-4证书", "CET-6证书", "CAD工程师认证证书", "水利水电工程师基础考试合格证书", "BIM证书"));
        CERT_POOLS.put("道路桥梁与渡河工程", Arrays.asList("CET-4证书", "CET-6证书", "CAD工程师认证证书", "桥梁工程师基础考试合格证书", "BIM证书"));
        CERT_POOLS.put("化学工程与工艺", Arrays.asList("CET-4证书", "CET-6证书", "化学工程师基础考试合格证书", "计算机二级证书", "注册化工工程师基础证书"));
        CERT_POOLS.put("新能源科学与工程", Arrays.asList("CET-4证书", "CET-6证书", "光伏系统工程师证书", "计算机二级证书", "能源管理师证书"));
        CERT_POOLS.put("过程装备与控制工程", Arrays.asList("CET-4证书", "CET-6证书", "压力容器作业证书", "计算机二级证书", "化工机械工程师证书"));
        CERT_POOLS.put("制药工程", Arrays.asList("CET-4证书", "CET-6证书", "执业药师资格证书", "GMP内审员证书", "药品检验工证书"));
        CERT_POOLS.put("生物制药", Arrays.asList("CET-4证书", "CET-6证书", "生物制药工程师证书", "计算机二级证书", "实验动物上岗证"));
        CERT_POOLS.put("酿酒工程", Arrays.asList("CET-4证书", "CET-6证书", "品酒师资格证书", "食品安全管理员证书", "酿酒师资格证书"));
        CERT_POOLS.put("安全工程", Arrays.asList("CET-4证书", "CET-6证书", "注册安全工程师考试合格证书", "计算机二级证书", "安全评价师证书"));
        CERT_POOLS.put("采矿工程", Arrays.asList("CET-4证书", "CET-6证书", "采矿工程师基础考试合格证书", "计算机二级证书", "爆破安全作业证"));
        CERT_POOLS.put("智能采矿工程", Arrays.asList("CET-4证书", "CET-6证书", "智慧矿山工程师证书", "计算机二级证书", "工业机器人操作证书"));
        CERT_POOLS.put("工程管理", Arrays.asList("CET-4证书", "CET-6证书", "二级建造师考试合格证书", "计算机二级证书", "一级建造师考试合格证书"));
        CERT_POOLS.put("工程造价", Arrays.asList("CET-4证书", "CET-6证书", "二级造价工程师考试合格证书", "广联达软件证书", "BIM一级建模师证书"));
        CERT_POOLS.put("投资学", Arrays.asList("CET-4证书", "CET-6证书", "证券从业资格证书", "基金从业资格证书", "期货从业资格证书"));
        CERT_POOLS.put("网络工程", Arrays.asList("CET-4证书", "CET-6证书", "HCIA-Routing & Switching证书", "计算机四级网络工程师证书", "Linux认证证书"));
        CERT_POOLS.put("数据科学与大数据技术", Arrays.asList("CET-4证书", "CET-6证书", "阿里云大数据分析师证书", "计算机二级Python证书", "CDA数据分析师证书"));
        CERT_POOLS.put("智能科学与技术", Arrays.asList("CET-4证书", "CET-6证书", "人工智能工程师证书", "计算机二级Python证书", "深度学习工程师证书"));
        CERT_POOLS.put("网络空间安全", Arrays.asList("CET-4证书", "CET-6证书", "CISP注册信息安全专业人员证书", "计算机四级信息安全工程师证书", "NISP一级证书"));
        CERT_POOLS.put("飞行器制造工程", Arrays.asList("CET-4证书", "CET-6证书", "航空制造工程师证书", "计算机二级C语言证书", "CATIA认证证书"));
        CERT_POOLS.put("飞行器动力工程", Arrays.asList("CET-4证书", "CET-6证书", "航空发动机工程师证书", "计算机二级证书", "ANSYS仿真工程师证书"));
        CERT_POOLS.put("人文地理与城乡规划", Arrays.asList("CET-4证书", "CET-6证书", "注册城乡规划师考试合格证书", "AutoCAD证书", "ArcGIS认证证书"));
        CERT_POOLS.put("环境设计", Arrays.asList("CET-4证书", "CET-6证书", "室内设计师证书", "AutoCAD证书", "3ds Max证书", "普通话二级甲等证书"));
        CERT_POOLS.put("建筑学", Arrays.asList("CET-4证书", "CET-6证书", "一级建筑师基础考试合格证书", "AutoCAD证书", "BIM一级证书"));
        CERT_POOLS.put("材料科学与工程", Arrays.asList("CET-4证书", "CET-6证书", "材料工程师基础考试合格证书", "计算机二级证书", "金相检验员证书"));
        CERT_POOLS.put("新能源材料与器件", Arrays.asList("CET-4证书", "CET-6证书", "光伏系统工程师证书", "计算机二级证书", "电化学工程师证书"));
        CERT_POOLS.put("焊接技术与工程", Arrays.asList("CET-4证书", "CET-6证书", "特种作业操作证（焊接）", "计算机二级证书", "焊接检验师证书"));
        CERT_POOLS.put("交通工程", Arrays.asList("CET-4证书", "CET-6证书", "CAD工程师认证证书", "交通工程师基础考试合格证书", "BIM证书"));
        CERT_POOLS.put("交通运输", Arrays.asList("CET-4证书", "CET-6证书", "城市轨道交通调度员证书", "计算机二级证书", "急救证书"));
        CERT_POOLS.put("应用统计学", Arrays.asList("CET-4证书", "CET-6证书", "统计从业资格证书", "SPSS认证证书", "CDA数据分析师证书"));
        CERT_POOLS.put("应用物理学", Arrays.asList("CET-4证书", "CET-6证书", "计算机二级证书", "教师资格证（物理）", "普通话二级甲等证书"));
        CERT_POOLS.put("休闲体育", Arrays.asList("CET-4证书", "CET-6证书", "社会体育指导员证书", "教师资格证（体育）", "救生员证书"));
        CERT_POOLS.put("英语", Arrays.asList("CET-4证书", "CET-6证书", "TEM-4证书", "教师资格证（英语）", "CATTI翻译资格证（笔译）"));
    }

    // ==================== 自我评价模板 ====================
    public static final List<Object[]> SELF_EVAL_TEMPLATES = Arrays.asList(
            new Object[]{"积极型", Arrays.asList(
                    "本人性格开朗，学习能力强，具备良好的团队协作能力。",
                    "在校期间认真学习专业知识，积极参与实践活动，综合素质较好。",
                    "毕业后希望从事与本专业相关的工作。"
            )},
            new Object[]{"沉稳型", Arrays.asList(
                    "本人踏实稳重，学习态度端正，专业基础扎实。",
                    "在校期间积极参与课程实践和实习，具备一定的工程实践能力。",
                    "期望毕业后从事与专业相关的工作，在岗位上持续成长。"
            )},
            new Object[]{"进取型", Arrays.asList(
                    "本人思维活跃，动手能力强，善于将理论知识应用于实践。",
                    "在校期间认真学习专业知识，积极参加学科竞赛和创新项目。",
                    "期望在专业相关领域发展，为企业创造价值。"
            )},
            new Object[]{"务实型", Arrays.asList(
                    "本人认真负责，吃苦耐劳，具备较强的学习能力和适应能力。",
                    "在校期间认真学习专业知识，积极参与实习实践，积累了一定的专业经验。",
                    "期望毕业后能找到与专业匹配的岗位，脚踏实地，从基层做起。"
            )}
    );

    // ==================== 谈话记录模板 ====================
    @Data
    public static class ConversationTemplate {
        private String type;
        private String content;
        private String result;
        private String nextPlan;

        public ConversationTemplate(String type, String content, String result, String nextPlan) {
            this.type = type;
            this.content = content;
            this.result = result;
            this.nextPlan = nextPlan;
        }
    }

    public static final Map<String, ConversationTemplate> CONVERSATION_TEMPLATES;
    static {
        CONVERSATION_TEMPLATES = new LinkedHashMap<>();
        CONVERSATION_TEMPLATES.put("就业指导", new ConversationTemplate(
                "就业指导",
                "辅导员与学生进行就业意向沟通，了解学生求职进展，指导学生完善简历、梳理求职方向，提醒学生关注学校发布的招聘信息，鼓励学生积极参加招聘会，主动投递简历。",
                "学生表示将认真修改简历，积极投递，争取早日落实就业单位。",
                "继续跟进学生就业情况，预约下次谈话。"
        ));
        CONVERSATION_TEMPLATES.put("心理疏导", new ConversationTemplate(
                "心理疏导",
                "学生反映近期求职压力大，情绪低落，辅导员耐心倾听，帮助学生分析求职受挫原因，调整求职心态，引导学生正确看待就业形势，鼓励其多尝试、多投递，同时提醒学生注意身心健康，合理安排求职与生活。",
                "学生情绪有所缓解，表示将调整心态，积极面对求职挑战。",
                "关注学生心理状态，必要时推荐参加学校心理咨询。"
        ));
        CONVERSATION_TEMPLATES.put("学业辅导", new ConversationTemplate(
                "学业辅导",
                "学生反映学业上遇到困难，影响了求职信心，辅导员了解情况后，针对学生的具体问题提供学习方法指导，帮助学生制定补修计划，鼓励学生在完成学业的同时积极求职。",
                "学生明确了学业和求职的平衡方法，表示将合理安排时间。",
                "跟进学生学业完成情况。"
        ));
        CONVERSATION_TEMPLATES.put("生活关怀", new ConversationTemplate(
                "生活关怀",
                "学生反映家庭经济困难，影响了毕业季求职安排，辅导员了解情况后，向学生介绍学校勤工俭学岗位和就业困难帮扶政策，鼓励学生申请就业补贴，同时帮助学生树立积极向上的生活态度。",
                "学生了解了相关帮扶政策，表示将积极申请，生活态度有所改善。",
                "跟进帮扶政策落实情况。"
        ));
    }
}
