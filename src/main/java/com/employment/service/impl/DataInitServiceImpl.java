package com.employment.service.impl;

import com.employment.common.Constants;
import com.employment.exception.BusinessException;
import com.employment.model.entity.*;
import com.employment.repository.*;
import com.employment.service.DataInitService;
import com.employment.service.NotificationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataInitServiceImpl implements DataInitService {

    private final SysRoleRepository roleRepository;
    private final SysDeptRepository deptRepository;
    private final SysMajorRepository majorRepository;
    private final SysClassRepository sysClassRepository;
    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final StudentResumeRepository studentResumeRepository;
    private final CompanyInfoRepository companyInfoRepository;
    private final JobPositionRepository jobPositionRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobFavoriteRepository jobFavoriteRepository;
    private final InterviewInvitationRepository interviewInvitationRepository;
    private final OfferLetterRepository offerLetterRepository;
    private final EmploymentRecordRepository employmentRecordRepository;
    private final NotificationRepository notificationRepository;
    private final SysLogRepository sysLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    /** 标记文件路径：项目根目录 */
    private static final String MARKER_FILE = "./data_init_done.json";
    private static final String MARKER_VERSION = "2.0";
    private static final long SEED = 42L;
    private static final Random RANDOM = new Random(SEED);

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // =========================================================
    //  九学院定义：名称、代码、专业列表
    // =========================================================
    private static final String[][] DEPARTMENTS = {
        {"机械工程学院",   "JIXIE",  "机械设计制造及其自动化|机械设计制造及其自动化（中英合办）|机械电子工程|过程装备与控制工程|智能制造工程"},
        {"人工智能与电气工程学院", "AIRONG", "电气工程及其自动化|自动化|智能科学与技术|机器人工程"},
        {"大数据学院",     "DASHUJU", "数据科学与大数据技术|网络工程|网络空间安全|信息管理与信息系统"},
        {"土木工程学院",   "TUMU",    "土木工程|水利水电工程|道路桥梁与渡河工程|测绘工程"},
        {"资源与环境工程学院", "ZIYUAN", "资源勘查工程|环境工程|地质工程|环境科学"},
        {"矿业工程学院",   "KUANGYE", "采矿工程|安全工程|矿物加工工程|智能采矿工程"},
        {"航空航天工程学院","HANGKONG","飞行器制造工程|飞行器动力工程|航空航天工程|无人驾驶航空器系统工程"},
        {"材料科学与冶金工程学院","CAILIAO","材料科学与工程|新能源材料与器件|冶金工程|焊接技术与工程"},
        {"化学工程学院",   "HUAXUE",  "化学工程与工艺|应用化学|生物工程|制药工程"},
    };

    // =========================================================
    //  各学院 → 行业映射（与前端行业枚举对齐）
    // =========================================================
    private static final String[][] DEPT_INDUSTRY_MAP = {
        {"JIXIE",     "机械/装备"},      // 机械工程学院
        {"AIRONG",    "机械/装备"},      // 人工智能与电气工程学院
        {"DASHUJU",   "互联网"},        // 大数据学院
        {"TUMU",      "建筑/房地产"},   // 土木工程学院
        {"ZIYUAN",    "矿业/能源"},     // 资源与环境工程学院
        {"KUANGYE",   "矿业/能源"},     // 矿业工程学院
        {"HANGKONG",  "航空航天"},      // 航空航天工程学院
        {"CAILIAO",   "化工/材料"},     // 材料科学与冶金工程学院
        {"HUAXUE",    "化工/材料"},     // 化学工程学院
    };

    // =========================================================
    //  各专业 → 期望岗位池（与专业高度相关）
    // =========================================================
    private static final String[][] MAJOR_POSITION_MAP = {
        // 机械类
        {"机械设计制造及其自动化",      "机械设计工程师|机械工艺工程师|制造工程师|CAD工程师|工艺装备工程师|机电一体化工程师"},
        {"机械设计制造及其自动化（中英合办）","机械设计工程师|机械工艺工程师|制造工程师|CAD工程师|工艺装备工程师"},
        {"机械电子工程",                "机械设计工程师|嵌入式软件工程师|自动化工程师|PLC工程师|机电工程师"},
        {"过程装备与控制工程",          "工艺工程师|设备工程师|化工机械工程师|压力容器设计工程师"},
        {"智能制造工程",                "智能制造工程师|工业软件工程师|数字化制造工程师|MES工程师|机器人集成工程师"},
        // 电气/自动化类
        {"电气工程及其自动化",          "电气工程师|PLC工程师|电力系统工程师|继电保护工程师|电气设计工程师"},
        {"自动化",                      "自动化工程师|DCS工程师|PLC工程师|控制工程师|仪表工程师"},
        {"智能科学与技术",              "算法工程师|人工智能工程师|机器学习工程师|数据分析师|NLP工程师"},
        {"机器人工程",                  "机器人工程师|嵌入式软件工程师|运动控制工程师|机器人算法工程师|工业机器人工程师"},
        // 大数据/计算机类
        {"数据科学与大数据技术",        "大数据开发工程师|数据分析师|数据挖掘工程师|ETL工程师|Hadoop工程师|Spark工程师"},
        {"网络工程",                    "网络工程师|网络安全工程师|系统管理员|网络运维工程师|云计算工程师"},
        {"网络空间安全",                "网络安全工程师|渗透测试工程师|安全运维工程师|等保测评工程师|安全开发工程师"},
        {"信息管理与信息系统",          "ERP实施工程师|信息系统管理员|产品经理|需求分析师|项目经理"},
        // 土木类
        {"土木工程",                    "土建工程师|结构工程师|施工工程师|工程造价师|工程监理工程师|BIM工程师"},
        {"水利水电工程",                "水利工程师|水工结构工程师|施工工程师|水文工程师|监理工程师"},
        {"道路桥梁与渡河工程",          "道路桥梁工程师|桥梁设计工程师|施工工程师|检测工程师|造价工程师"},
        {"测绘工程",                    "测绘工程师|GIS工程师|遥感工程师|测量工程师|地理信息工程师"},
        // 资源与环境类
        {"资源勘查工程",                "地质勘查工程师|资源评价工程师|勘探工程师|矿山地质工程师"},
        {"环境工程",                    "环保工程师|水处理工程师|废气处理工程师|环境影响评价工程师|EHS工程师"},
        {"地质工程",                    "地质工程师|岩土工程师|勘察工程师|地质灾害评估工程师"},
        {"环境科学",                    "环境工程师|环境影响评价工程师|生态修复工程师|环境监测工程师"},
        // 矿业类
        {"采矿工程",                    "采矿工程师|矿山设计工程师|矿井工程师|安全工程师|爆破工程师"},
        {"安全工程",                    "安全工程师|安全管理工程师|矿山安全工程师|安全评价工程师|EHS工程师"},
        {"矿物加工工程",                "选矿工程师|工艺工程师|矿山设计工程师|矿物加工技术员"},
        {"智能采矿工程",                "智能采矿工程师|采矿自动化工程师|矿山物联网工程师|智慧矿山工程师"},
        // 航空航天类
        {"飞行器制造工程",              "航空工艺工程师|飞机制造工程师|装配工程师|航空材料工程师|质量工程师"},
        {"飞行器动力工程",              "航空发动机工程师|动力系统工程师|燃气轮机工程师|强度工程师"},
        {"航空航天工程",                "航天工程师|总体设计工程师|系统工程师|卫星工程师|火箭工程师"},
        {"无人驾驶航空器系统工程",       "无人机工程师|飞控算法工程师|嵌入式软件工程师|任务规划工程师|通信链路工程师"},
        // 材料/冶金类
        {"材料科学与工程",              "材料工程师|材料研发工程师|工艺工程师|检测工程师|材料分析工程师"},
        {"新能源材料与器件",            "新能源材料工程师|储能工程师|电池研发工程师|材料工程师|光伏工程师"},
        {"冶金工程",                    "冶金工程师|炼钢工程师|铸造工程师|金属材料工程师|工艺工程师"},
        {"焊接技术与工程",              "焊接工程师|焊接工艺工程师|无损检测工程师|结构工程师|质量工程师"},
        // 化工类
        {"化学工程与工艺",              "化工工艺工程师|化工设计工程师|生产工程师|安全管理工程师|研发工程师"},
        {"应用化学",                    "应用化学工程师|化学分析师|研发工程师|质量控制工程师|配方工程师"},
        {"生物工程",                    "生物工程师|发酵工程师|工艺工程师|药品研发工程师|质量工程师"},
        {"制药工程",                    "制药工程师|药品研发工程师|质量工程师|工艺工程师|药品注册工程师"},
    };

    // =========================================================
    //  籍贯：省份 → 主要城市列表
    // =========================================================
    private static final String[][] PROVINCE_CITIES = {
        {"湖北省", "武汉市","宜昌市","襄阳市","荆州市","黄石市","十堰市","孝感市","黄冈市","咸宁市","随州市","恩施市","鄂州市","荆门市","仙桃市","潜江市","天门市"},
        {"湖南省", "长沙市","株洲市","湘潭市","衡阳市","岳阳市","常德市","益阳市","郴州市","永州市","邵阳市","怀化市","娄底市","张家界市","湘西土家族苗族自治州"},
        {"河南省", "郑州市","洛阳市","开封市","新乡市","南阳市","许昌市","安阳市","平顶山市","商丘市","周口市","信阳市","驻马店市","焦作市","濮阳市","三门峡市"},
        {"广东省", "广州市","深圳市","东莞市","佛山市","珠海市","中山市","惠州市","江门市","湛江市","茂名市","肇庆市","汕头市","韶关市","清远市","梅州市","汕尾市","河源市","阳江市","潮州市","揭阳市","云浮市"},
        {"浙江省", "杭州市","宁波市","温州市","嘉兴市","湖州市","绍兴市","金华市","衢州市","舟山市","台州市","丽水市"},
        {"江苏省", "南京市","苏州市","无锡市","常州市","南通市","徐州市","盐城市","淮安市","泰州市","镇江市","扬州市","连云港市","宿迁市","镇江市"},
        {"四川省", "成都市","绵阳市","德阳市","宜宾市","南充市","泸州市","达州市","乐山市","内江市","自贡市","遂宁市","广安市","眉山市","资阳市","广元市","雅安市","巴中市","攀枝花市","眉山市"},
        {"山东省", "济南市","青岛市","烟台市","潍坊市","临沂市","淄博市","威海市","济宁市","泰安市","德州市","聊城市","滨州市","菏泽市","枣庄市","日照市"},
        {"安徽省", "合肥市","芜湖市","蚌埠市","淮南市","马鞍山市","淮北市","铜陵市","安庆市","黄山市","滁州市","阜阳市","宿州市","六安市","池州市","宣城市"},
        {"江西省", "南昌市","九江市","赣州市","上饶市","宜春市","吉安市","抚州市","景德镇市","萍乡市","新余市","鹰潭市"},
        {"北京市", "北京市"},
        {"上海市", "上海市"},
        {"重庆市", "重庆市"},
        {"陕西省", "西安市","宝鸡市","咸阳市","铜川市","渭南市","延安市","榆林市","汉中市","安康市","商洛市"},
        {"福建省", "福州市","厦门市","泉州市","漳州市","莆田市","宁德市","龙岩市","三明市","南平市"},
        {"广西壮族自治区", "南宁市","柳州市","桂林市","梧州市","北海市","贵港市","玉林市","百色市","河池市","钦州市","防城港市","贺州市","来宾市","崇左市"},
        {"贵州省", "贵阳市","遵义市","六盘水市","安顺市","毕节市","铜仁市","黔西南布依族苗族自治州","黔东南苗族侗族自治州","黔南布依族苗族自治州"},
        {"云南省", "昆明市","曲靖市","玉溪市","保山市","昭通市","丽江市","普洱市","临沧市","楚雄彝族自治州","红河哈尼族彝族自治州","文山壮族苗族自治州","西双版纳傣族自治州","大理白族自治州","德宏傣族景颇族自治州","怒江傈僳族自治州","迪庆藏族自治州"},
        {"海南省", "海口市","三亚市","三沙市","儋州市"},
        {"河北省", "石家庄市","唐山市","保定市","邯郸市","秦皇岛市","沧州市","邢台市","廊坊市","衡水市","张家口市","承德市"},
        {"山西省", "太原市","大同市","阳泉市","长治市","晋城市","朔州市","晋中市","运城市","忻州市","临汾市","吕梁市"},
        {"辽宁省", "沈阳市","大连市","鞍山市","抚顺市","本溪市","丹东市","锦州市","营口市","辽阳市","盘锦市","铁岭市","朝阳市","葫芦岛市"},
        {"吉林省", "长春市","吉林市","四平市","辽源市","通化市","白山市","松原市","白城市","延边朝鲜族自治州"},
        {"黑龙江省", "哈尔滨市","齐齐哈尔市","鸡西市","鹤岗市","双鸭山市","大庆市","伊春市","佳木斯市","七台河市","牡丹江市","黑河市","绥化市"},
        {"内蒙古自治区", "呼和浩特市","包头市","乌海市","赤峰市","通辽市","鄂尔多斯市","呼伦贝尔市","巴彦淖尔市","乌兰察布市","兴安盟","锡林郭勒盟","阿拉善盟"},
        {"新疆维吾尔自治区", "乌鲁木齐市","克拉玛依市","吐鲁番市","哈密市","阿克苏市","喀什市","和田市","昌吉回族自治州","博尔塔拉蒙古自治州","巴音郭楞蒙古自治州","伊犁哈萨克自治州"},
        {"甘肃省", "兰州市","嘉峪关市","金昌市","白银市","天水市","武威市","张掖市","平凉市","酒泉市","庆阳市","定西市","陇南市","临夏回族自治州","甘南藏族自治州"},
        {"宁夏回族自治区", "银川市","石嘴山市","吴忠市","固原市","中卫市"},
        {"青海省", "西宁市","海东市","海北藏族自治州","黄南藏族自治州","海南藏族自治州","果洛藏族自治州","玉树藏族自治州","海西蒙古族藏族自治州"},
        {"西藏自治区", "拉萨市","日喀则市","昌都市","林芝市","山南市","那曲市","阿里地区"},
        {"天津市", "天津市"},
        {"香港特别行政区", "香港"},
        {"澳门特别行政区", "澳门"},
        {"台湾省", "台北市","新北市","桃园市","台中市","台南市","高雄市"}
    };

    // 籍贯省份权重（本校为湖北高校，本省及邻省学生较多）
    private String randomBirthProvince() {
        int r = RANDOM.nextInt(100);
        if (r < 25) return "湖北省";      // 本省 ~25%
        else if (r < 42) return "湖南省"; // 邻省 ~17%
        else if (r < 57) return "河南省"; // 邻省 ~15%
        else if (r < 70) return "广东省"; // 发达省份 ~13%
        else if (r < 81) return "江西省"; // 邻省 ~11%
        else if (r < 90) return "安徽省"; // 邻省 ~9%
        else return "其他";
    }

    private String randomBirthCity(String province) {
        if ("其他".equals(province)) {
            String[] others = {"山东省","四川省","浙江省","江苏省","福建省","广西壮族自治区","贵州省","云南省","河北省","山西省","陕西省"};
            return others[RANDOM.nextInt(others.length)] + randomCitySuffix();
        }
        for (String[] pc : PROVINCE_CITIES) {
            if (pc[0].equals(province)) {
                return pc[1 + RANDOM.nextInt(Math.min(pc.length - 1, 4))];
            }
        }
        return province + "某市";
    }

    private String randomCitySuffix() {
        String[] cities = {"市"};
        return cities[RANDOM.nextInt(cities.length)];
    }

    // =========================================================
    //  就业城市（按行业分层，覆盖全国主流城市）
    // =========================================================
    private static final String[] CITIES_IT = {   // IT/互联网/通信
        "北京市","上海市","深圳市","广州市","杭州市","南京市","成都市","武汉市","西安市",
        "苏州市","东莞市","长沙市","郑州市","天津市","合肥市","厦门市","重庆市","佛山市",
        "宁波市","青岛市","济南市","大连市","沈阳市","南昌市","福州市","珠海市","中山市",
        "惠州市","江门市","无锡市","常州市","南通市","温州市","嘉兴市","绍兴市","金华市",
        "太原市","石家庄市","哈尔滨市","长春市","昆明市","贵阳市","兰州市","乌鲁木齐市",
        "呼和浩特市","包头市","南宁市","柳州市","桂林市","海口市","三亚市","绵阳市","德阳市"
    };
    private static final String[] CITIES_TRADITIONAL = { // 传统行业（机械/化工/建筑/矿业/航空航天）
        "北京市","上海市","深圳市","广州市","成都市","武汉市","西安市","长沙市","郑州市",
        "天津市","重庆市","杭州市","南京市","苏州市","合肥市","沈阳市","大连市","青岛市",
        "济南市","烟台市","福州市","厦门市","南昌市","贵阳市","昆明市","兰州市","乌鲁木齐市",
        "呼和浩特市","包头市","柳州市","哈尔滨市","长春市","吉林市","大庆市","鞍山市","锦州市",
        "石家庄市","唐山市","保定市","邯郸市","秦皇岛市","沧州市","廊坊市","太原市","大同市",
        "长治市","晋城市","无锡市","常州市","南通市","徐州市","扬州市","盐城市","连云港市",
        "宁波市","温州市","嘉兴市","绍兴市","金华市","湖州市","台州市","舟山市","芜湖市",
        "蚌埠市","淮南市","马鞍山市","安庆市","阜阳市","滁州市","六安市","赣州市","九江市",
        "上饶市","宜春市","吉安市","潍坊市","临沂市","淄博市","威海市","济宁市","泰安市",
        "德州市","聊城市","滨州市","菏泽市","绵阳市","德阳市","宜宾市","南充市","泸州市",
        "达州市","乐山市","内江市","自贡市","遂宁市","广安市","绵阳市","遵义市","六盘水市",
        "安顺市","毕节市","曲靖市","玉溪市","保山市","昭通市","丽江市","拉萨市","日喀则市",
        "兰州市","嘉峪关市","金昌市","白银市","天水市","武威市","银川市","石嘴山市","西宁市",
        "海东市","鄂尔多斯市","南宁市","桂林市","梧州市","北海市","贵港市","玉林市","海口市",
        "三亚市","儋州市","东莞市","珠海市","中山市","惠州市","江门市","湛江市","茂名市","肇庆市"
    };

    private String randomWorkCityForIndustry(String industry) {
        String[] pool;
        if (industry.contains("互联网") || industry.contains("通信") || industry.contains("电子") || industry.contains("软件")) {
            pool = CITIES_IT;
        } else {
            pool = CITIES_TRADITIONAL;
        }
        return pool[RANDOM.nextInt(pool.length)];
    }

    private String mapCityToProvince(String city) {
        if (city == null || city.isEmpty()) return "湖北省";
        for (String[] pc : PROVINCE_CITIES) {
            for (int i = 1; i < pc.length; i++) {
                if (pc[i].equals(city)) {
                    return pc[0];
                }
            }
        }
        return "湖北省";
    }

    // =========================================================
    //  姓氏池 & 名字池（随机组合生成真实姓名）
    // =========================================================
    private static final String[] XING = {
        "王","李","张","刘","陈","杨","赵","黄","周","吴",
        "徐","孙","胡","朱","高","林","何","郭","马","罗",
        "梁","宋","郑","谢","韩","唐","冯","于","董","萧",
        "程","曹","袁","邓","许","傅","沈","曾","彭","吕",
        "苏","卢","蒋","蔡","贾","丁","魏","薛","叶","阎",
        "余","潘","杜","戴","夏","钟","汪","田","任","姜",
        "范","方","石","姚","谭","廖","邹","熊","金","陆",
        "郝","孔","白","崔","康","毛","邱","秦","江","史",
        "顾","侯","邵","孟","龙","万","段","雷","钱","汤",
    };
    private static final String[] MING_MAS = {
        "伟","强","磊","洋","勇","艳","杰","涛","超","军",
        "波","鹏","飞","霞","萍","辉","静","敏","丽","华",
        "刚","平","辉","玲","桂英","建军","志强","秀英","秀兰","建华",
        "建华","志明","志伟","志强","志刚","志鹏","志辉","志勇","志超","志军",
        "志国","志民","志远","志刚","志红","志芳","志英","志华","志平","志国"
    };
    private static final String[] MING_FEM = {
        "霞","丽","敏","静","玲","艳","芳","华","萍","红",
        "英","梅","兰","菊","娟","婷","燕","凤","芹","丹",
        "秀英","秀兰","秀珍","凤英","丽华","丽娟","丽萍","丽红","丽芳","丽英",
        "建华","志明","志伟","志强","志刚","志鹏","志辉","志勇","志超","志军",
        "志国","志民","志远","志刚","志红","志芳","志英","志华","志平","志国"
    };

    // 性别对应的名字池
    private String randomName(boolean isMale) {
        String xing = XING[RANDOM.nextInt(XING.length)];
        String[] ming = isMale ? MING_MAS : MING_FEM;
        return xing + ming[RANDOM.nextInt(ming.length)];
    }

    // =========================================================
    //  身份证前6位（湖北武汉，可根据需要调整）
    // =========================================================
    private static final String IDCARD_PREFIX = "4201";

    private String generateIdCard(int age, boolean isMale) {
        int year = LocalDateTime.now().getYear() - age;
        int month = RANDOM.nextInt(12) + 1;
        int day = RANDOM.nextInt(28) + 1;
        int seq = RANDOM.nextInt(5000);
        int sexCode = isMale ? (seq % 2 == 0 ? 0 : 1) : (seq % 2 == 0 ? 0 : 1);
        String yearStr = String.format("%04d", year);
        String monthStr = String.format("%02d", month);
        String dayStr = String.format("%02d", day);
        String seqStr = String.format("%04d", seq);
        String body = IDCARD_PREFIX + yearStr + monthStr + dayStr + seqStr + sexCode;
        char check = calcCheckCode(body);
        return body + check;
    }

    private char calcCheckCode(String body17) {
        int[] weight = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checkCodes = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (body17.charAt(i) - '0') * weight[i];
        }
        return checkCodes[sum % 11];
    }

    // =========================================================
    //  手机号前缀（各省市真实号段）
    // =========================================================
    private static final String[] PHONE_PREFIXES = {
        "130","131","132","133","134","135","136","137","138","139",
        "150","151","152","153","155","156","157","158","159",
        "170","171","172","173","175","176","177","178",
        "180","181","182","183","184","185","186","187","188","189",
    };

    private String generatePhone() {
        return PHONE_PREFIXES[RANDOM.nextInt(PHONE_PREFIXES.length)]
             + String.format("%08d", RANDOM.nextInt(100000000));
    }

    // =========================================================
    //  学号生成：2023届→2023010001，2024届→2024010001，...
    // =========================================================
    private int studentSeq = 0;
    private synchronized String nextStudentNo(int graduationYear) {
        return String.format("%d%06d", graduationYear, ++studentSeq);
    }

    // =========================================================
    //  简历内容生成
    // =========================================================
    private static final String[] CERTIFICATES = {
        "全国大学生英语四级证书","全国大学生英语六级证书","计算机二级证书",
        "软件设计师（初级）","软件设计师（中级）","数据库系统工程师",
        "网络工程师","信息系统项目管理师","PMP项目管理专业人士资格认证"
    };
    private static final String[] AWARDS = {
        "校级一等奖学金","校级二等奖学金","三好学生","优秀学生干部",
        "全国大学生数学建模竞赛省二等奖","中国大学生计算机设计大赛省三等奖",
        "蓝桥杯全国软件大赛省一等奖","ACM-ICPC亚洲区域赛铜奖"
    };
    private static final String[] PROJECTS = {
        "基于Spring Boot的校园二手交易平台：采用Vue3+Spring Boot前后端分离架构，实现了商品发布、浏览、搜索、购买、评价等功能，使用Redis实现缓存，MySQL存储数据。负责后端接口设计与开发。",
        "基于Vue3的在线协同编辑文档系统：使用WebSocket实现实时同步，支持多人同时编辑同一文档，采用Quill富文本编辑器，整合了权限管理与版本历史功能。负责前端页面开发与状态管理。",
        "机器学习课程项目-房价预测模型：使用Python scikit-learn库，基于Kaggle房价数据集，构建了线性回归、随机森林、XGBoost等模型进行房价预测，R²达到0.88。负责数据清洗与模型训练。",
        "Android校园导览APP：基于Android原生开发，集成了百度地图SDK，实现了校园地图展示、建筑物查询、最优路径导航、语音讲解等功能。负责地图模块与定位功能开发。",
        "基于Spring Cloud的微服务电商系统：拆分为用户服务、商品服务、订单服务、支付服务等模块，使用Nacos注册中心，Sentinel流量控制，Seata分布式事务。负责订单服务开发与系统集成。",
        "数据可视化大屏系统：基于ECharts和Vue3，实时展示企业销售数据、用户增长趋势、运营指标等，支持自定义拖拽布局与多数据源接入。负责前端交互与图表配置。"
    };
    private static final String[] SELF_EVALS = {
        "本人性格开朗，责任心强，具备良好的团队协作能力和沟通能力。在校期间学习成绩优异，多次获得奖学金，积极参与各类学科竞赛和实践活动。",
        "热爱编程，具有扎实的计算机基础知识，擅长Java开发。具有良好的学习能力和问题分析能力，能够快速掌握新技术并应用到实际项目中。",
        "具备扎实的数学基础和逻辑思维能力，对算法有浓厚兴趣。做事认真细致，有较强的抗压能力和自我驱动能力，能够独立完成项目开发任务。",
        "对前端技术有深入研究，熟悉Vue3、React等主流框架。具有UI设计能力，注重用户体验，能够独立完成从前端页面开发到交互优化的全流程工作。"
    };

    private String randomItem(String[] arr) {
        return arr[RANDOM.nextInt(arr.length)];
    }

    // 根据学院代码获取行业
    private String getIndustryByDeptCode(String deptCode) {
        for (String[] m : DEPT_INDUSTRY_MAP) {
            if (m[0].equals(deptCode)) return m[1];
        }
        return "互联网";
    }

    // 根据专业名称获取匹配的职位池
    private String getPositionsByMajorName(String majorName) {
        for (String[] m : MAJOR_POSITION_MAP) {
            if (m[0].equals(majorName)) return m[1];
        }
        return "技术支持工程师|售前工程师|测试工程师|运维工程师|项目助理";
    }

    private String randomMajorPosition(String majorName) {
        String pool = getPositionsByMajorName(majorName);
        String[] arr = pool.split("\\|");
        return arr[RANDOM.nextInt(arr.length)];
    }

    private String getPositionCategory(String majorName) {
        String[] techCats = {"机械设计制造及其自动化","机械电子工程","过程装备与控制工程","智能制造工程",
                "电气工程及其自动化","自动化","智能科学与技术","机器人工程",
                "数据科学与大数据技术","网络工程","网络空间安全","信息管理与信息系统",
                "土木工程","水利水电工程","道路桥梁与渡河工程","测绘工程",
                "资源勘查工程","环境工程","地质工程","环境科学",
                "采矿工程","安全工程","矿物加工工程","智能采矿工程",
                "飞行器制造工程","飞行器动力工程","航空航天工程","无人驾驶航空器系统工程",
                "材料科学与工程","新能源材料与器件","冶金工程","焊接技术与工程",
                "化学工程与工艺","应用化学","生物工程","制药工程"};
        for (String m : techCats) {
            if (m.equals(majorName)) return "技术";
        }
        return "技术";
    }

    private int getSalaryForIndustry(String industry) {
        if (industry.contains("互联网") || industry.contains("通信") || industry.contains("电子")) {
            return 8000 + RANDOM.nextInt(15) * 500;  // IT类 8k~15.5k
        } else if (industry.contains("金融")) {
            return 7000 + RANDOM.nextInt(15) * 500;  // 金融 7k~14k
        } else if (industry.contains("建筑") || industry.contains("房地产")) {
            return 5000 + RANDOM.nextInt(12) * 500;  // 建筑 5k~10.5k
        } else if (industry.contains("矿业") || industry.contains("能源") || industry.contains("化工") || industry.contains("材料")) {
            return 5000 + RANDOM.nextInt(12) * 500;  // 传统工业 5k~10.5k
        } else if (industry.contains("航空航天")) {
            return 7000 + RANDOM.nextInt(12) * 500;  // 航空航天 7k~12.5k
        } else if (industry.contains("机械") || industry.contains("装备")) {
            return 5000 + RANDOM.nextInt(12) * 500;  // 机械 5k~10.5k
        }
        return 6000 + RANDOM.nextInt(12) * 500;  // 默认 6k~11.5k
    }

    // 根据专业获取行业（简历用）
    private String getIndustryByMajorName(String majorName) {
        for (String[] m : MAJOR_POSITION_MAP) {
            if (m[0].equals(majorName)) {
                String positions = m[1];
                for (String[] di : DEPT_INDUSTRY_MAP) {
                    for (int i = 1; i < di.length; i++) {
                        if (positions.contains(di[i])) return di[0].equals("DASHUJU") ? "互联网" : di[1];
                    }
                }
            }
        }
        return "互联网";
    }

    // =========================================================
    //  就业类型分布（已毕业届）—— 全部使用中文，与前端枚举一致
    // =========================================================
    // 分布：签订劳动合同~62%，三方协议~15%，继续深造~8%，自主创业~4%，出国出境~2%，应征入伍~2%，暂未就业~5%，自由职业~1%，其他~1%
    private String randomEmploymentType() {
        int r = RANDOM.nextInt(100);
        if (r < 62) return "签订劳动合同";
        if (r < 77) return "签订三方协议";
        if (r < 85) return "继续深造";
        if (r < 89) return "自主创业";
        if (r < 91) return "出国出境";
        if (r < 93) return "应征入伍";
        if (r < 98) return "暂未就业";
        if (r < 99) return "自由职业";
        return "其他";
    }

    // =========================================================
    //  主流程入口（由 DataInitRunner 调用）
    // =========================================================
    @Override
    public void initRoles() { initRolesInternal(true); }
    @Override
    public void initDepartments() { /* 由 initAllInternal 统一驱动 */ }
    @Override
    public void initMajors() { /* 由 initAllInternal 统一驱动 */ }
    @Override
    public void initAdminUser() { /* 由 initAllInternal 统一驱动 */ }
    @Override
    public void initTestUsers() { /* 由 initAllInternal 统一驱动 */ }
    @Override
    public void initCompaniesAndJobs() { /* 由 initAllInternal 统一驱动 */ }
    @Override
    public void initClasses() { /* 由 initAllInternal 统一驱动 */ }

    /**
     * 一次性完整初始化（由 DataInitRunner 调用）
     */
    @Transactional
    public void initAllInternal() {
        if (isAlreadyInitialized()) {
            log.info("========== 检测到标记文件，已跳过完整数据初始化 ==========");
            return;
        }

        long t0 = System.currentTimeMillis();
        log.info("========== 开始完整数据初始化 ==========");

        // 第一阶段：基础数据
        initRolesInternal(true);
        initDepartmentsInternal();
        initMajorsInternal();
        initAdminUserInternal();
        initDeptTeachersInternal();  // 9个院级老师
        initClassesInternal();
        initClassTeachersInternal(); // 405个班主任

        // 第二阶段：大量学生
        initAllStudents();           // 12150学生 + 简历 + 投递/收藏

        // 第三阶段：企业数据
        initCompaniesAndJobsInternal();

        // 第四阶段：已毕业届的就业数据
        initEmploymentRecordsForGraduates();

        // 标记完成
        writeMarker();
        long cost = System.currentTimeMillis() - t0;
        log.info("========== 完整数据初始化完成！共耗时 {} ms ==========", cost);
    }

    // =========================================================
    //  阶段一：基础数据
    // =========================================================
    private void initRolesInternal(boolean checkExist) {
        if (checkExist && roleRepository.count() > 0) {
            log.info("角色已存在，跳过");
            return;
        }
        List<SysRole> roles = Arrays.asList(
            createRole("学生", Constants.ROLE_STUDENT, 1),
            createRole("班级老师", Constants.ROLE_CLASS_TEACHER, 2),
            createRole("院级老师", Constants.ROLE_DEPT_TEACHER, 3),
            createRole("校级管理员", Constants.ROLE_ADMIN, 4),
            createRole("用人单位", Constants.ROLE_COMPANY, 5),
            createRole("数据分析工程师", Constants.ROLE_DATA_ANALYST, 6)
        );
        roleRepository.saveAll(roles);
        log.info("角色初始化完成: 6条");
    }

    private SysRole createRole(String name, String key, int sort) {
        SysRole r = new SysRole();
        r.setRoleName(name);
        r.setRoleKey(key);
        r.setRoleSort(sort);
        r.setStatus(Constants.STATUS_NORMAL);
        r.setRemark("系统自动初始化");
        return r;
    }

    private void initDepartmentsInternal() {
        if (deptRepository.count() > 0) {
            log.info("院系已存在，跳过");
            return;
        }
        List<SysDept> depts = new ArrayList<>();
        int sort = 1;
        for (String[] d : DEPARTMENTS) {
            SysDept dept = new SysDept();
            dept.setDeptName(d[0]);
            dept.setDeptCode(d[1]);
            dept.setParentId(0L);
            dept.setIsTopLevel("1");
            dept.setSort(sort++);
            dept.setStatus(Constants.STATUS_NORMAL);
            dept.setRemark("系统自动初始化");
            depts.add(dept);
        }
        deptRepository.saveAll(depts);
        log.info("院系初始化完成: {}条", depts.size());
    }

    private void initMajorsInternal() {
        if (majorRepository.count() > 0) {
            log.info("专业已存在，跳过");
            return;
        }
        List<SysMajor> majors = new ArrayList<>();
        int sort = 1;
        for (String[] d : DEPARTMENTS) {
            Long deptId = deptRepository.findByDeptCode(d[1]).map(SysDept::getId).orElse(null);
            if (deptId == null) continue;
            String[] majorNames = d[2].split("\\|");
            for (String rawName : majorNames) {
                boolean isTop = rawName.contains("（");
                String name = isTop ? rawName.replaceAll("（[^）]*）", "").trim() : rawName.trim();
                if (name.isEmpty()) continue;
                SysMajor m = new SysMajor();
                m.setMajorName(name);
                m.setMajorCode("M" + String.format("%03d", sort++));
                m.setDeptId(deptId);
                m.setDegreeType("本科");
                m.setIsTopLevel(isTop ? "1" : "0");
                m.setRemark("系统自动初始化");
                majors.add(m);
            }
        }
        majorRepository.saveAll(majors);
        log.info("专业初始化完成: {}条", majors.size());
    }

    private void initAdminUserInternal() {
        SysRole adminRole = roleRepository.findByRoleKey(Constants.ROLE_ADMIN)
                .orElseThrow(() -> new BusinessException("管理员角色不存在"));
        if (!userRepository.findByUsername("admin").isPresent()) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setRealName("校级管理员");
            admin.setEmail("admin@employment.edu.cn");
            admin.setPhone("13800138000");
            admin.setStatus(Constants.STATUS_NORMAL);
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setRemark("系统自动初始化管理员");
            admin = userRepository.save(admin);
            SysUserRole ur = new SysUserRole();
            ur.setUserId(admin.getId());
            ur.setRoleId(adminRole.getId());
            userRoleRepository.save(ur);
            log.info("管理员账号: admin / 123456");
        }

        // 数据分析师账号
        SysRole dataRole = roleRepository.findByRoleKey(Constants.ROLE_DATA_ANALYST)
                .orElseThrow(() -> new BusinessException("数据分析师角色不存在"));
        if (!userRepository.findByUsername("datastaff").isPresent()) {
            SysUser dataUser = new SysUser();
            dataUser.setUsername("datastaff");
            dataUser.setRealName("数据分析师");
            dataUser.setEmail("datastaff@employment.edu.cn");
            dataUser.setPhone("13800138010");
            dataUser.setStatus(Constants.STATUS_NORMAL);
            dataUser.setPassword(passwordEncoder.encode("123456"));
            dataUser.setRemark("系统自动初始化");
            dataUser = userRepository.save(dataUser);
            SysUserRole ur = new SysUserRole();
            ur.setUserId(dataUser.getId());
            ur.setRoleId(dataRole.getId());
            userRoleRepository.save(ur);
            log.info("数据分析师账号: datastaff / 123456");
        }
    }

    // =========================================================
    //  阶段二：9个院级老师
    // =========================================================
    private void initDeptTeachersInternal() {
        SysRole teacherRole = roleRepository.findByRoleKey(Constants.ROLE_DEPT_TEACHER)
                .orElseThrow(() -> new BusinessException("院级老师角色不存在"));
        if (userRepository.findByUsername("deptteacher_1").isPresent()) {
            log.info("院级老师账号已存在，跳过");
            return;
        }
        List<SysUser> teachers = new ArrayList<>();
        for (int i = 0; i < DEPARTMENTS.length; i++) {
            String[] d = DEPARTMENTS[i];
            SysDept dept = deptRepository.findByDeptCode(d[1]).orElse(null);
            if (dept == null) continue;
            boolean isMale = RANDOM.nextBoolean();
            String name = randomName(isMale);
            String username = "deptteacher_" + (i + 1);
            SysUser u = new SysUser();
            u.setUsername(username);
            u.setRealName(dept.getDeptName().replace("学院", "") + "教学副院长");
            u.setEmail(username + "@edu.cn");
            u.setPhone(generatePhone());
            u.setGender(isMale ? "male" : "female");
            u.setDeptId(dept.getId());
            u.setStatus(Constants.STATUS_NORMAL);
            u.setPassword(passwordEncoder.encode("123456"));
            u.setRemark("系统自动初始化院级老师");
            u = userRepository.save(u);
            SysUserRole ur = new SysUserRole();
            ur.setUserId(u.getId());
            ur.setRoleId(teacherRole.getId());
            userRoleRepository.save(ur);
            teachers.add(u);
        }
        log.info("院级老师初始化完成: {}条 (deptteacher_1 ~ deptteacher_9 / 123456)", teachers.size());
    }

    // =========================================================
    //  阶段三：405个班 + 405个班主任
    // =========================================================
    private void initClassesInternal() {
        if (sysClassRepository.count() > 0) {
            log.info("班级已存在，跳过");
            return;
        }
        SysRole teacherRole = roleRepository.findByRoleKey(Constants.ROLE_CLASS_TEACHER)
                .orElseThrow(() -> new BusinessException("班级老师角色不存在"));
        List<SysDept> allDepts = deptRepository.findAll();
        Map<Long, List<SysMajor>> deptIdToMajors = new HashMap<>();
        for (SysDept d : allDepts) {
            deptIdToMajors.put(d.getId(), majorRepository.findByDeptId(d.getId()));
        }
        String[] GRADES = {"2020","2021","2022","2023","2024"};
        String[] CLASS_SUFFIXES = {"1班","2班","3班"};
        List<SysClass> allClasses = new ArrayList<>();
        int idx = 0;
        for (SysDept dept : allDepts) {
            List<SysMajor> majors = deptIdToMajors.get(dept.getId());
            if (majors == null || majors.isEmpty()) continue;
            for (SysMajor major : majors) {
                for (String grade : GRADES) {
                    for (String suffix : CLASS_SUFFIXES) {
                        String prefix = major.getMajorName().length() >= 2
                                ? major.getMajorName().substring(0, 2) : major.getMajorName();
                        SysClass c = new SysClass();
                        c.setClassName(prefix + grade.substring(2) + "-" + suffix);
                        c.setMajorId(major.getId());
                        c.setDeptId(dept.getId());
                        c.setGrade(grade);
                        c.setStudentCount(30);
                        c.setStatus("0");
                        c.setRemark("系统自动初始化");
                        allClasses.add(c);
                        idx++;
                    }
                }
            }
        }
        sysClassRepository.saveAll(allClasses);
        log.info("班级初始化完成: {}条 (9学院×3专业×5年级×3班=405)", idx);
    }

    private void initClassTeachersInternal() {
        SysRole teacherRole = roleRepository.findByRoleKey(Constants.ROLE_CLASS_TEACHER)
                .orElseThrow(() -> new BusinessException("班级老师角色不存在"));
        List<SysClass> allClasses = sysClassRepository.findAll();
        int count = 0;
        for (int i = 0; i < allClasses.size(); i++) {
            SysClass cls = allClasses.get(i);
            if (cls.getAdvisorId() != null) continue; // 已分配过
            String username = "classteacher_" + String.format("%03d", i + 1);
            boolean isMale = RANDOM.nextBoolean();
            SysUser u = new SysUser();
            u.setUsername(username);
            u.setRealName(randomName(isMale));
            u.setEmail(username + "@edu.cn");
            u.setPhone(generatePhone());
            u.setGender(isMale ? "male" : "female");
            u.setDeptId(cls.getDeptId());
            u.setMajorId(cls.getMajorId());
            u.setClassId(cls.getId());
            u.setClassName(cls.getClassName());
            u.setStatus(Constants.STATUS_NORMAL);
            u.setPassword(passwordEncoder.encode("123456"));
            u.setRemark("系统自动初始化班主任");
            u = userRepository.save(u);
            SysUserRole ur = new SysUserRole();
            ur.setUserId(u.getId());
            ur.setRoleId(teacherRole.getId());
            userRoleRepository.save(ur);
            cls.setAdvisor(u.getRealName());
            cls.setAdvisorId(u.getId());
            sysClassRepository.save(cls);
            count++;
        }
        log.info("班主任初始化完成: {}条 (classteacher_001 ~ 405 / 123456)", count);
    }

    // =========================================================
    //  阶段四：12150个学生（每班30人）
    // =========================================================
    private void initAllStudents() {
        long existing = studentInfoRepository.count();
        if (existing > 0) {
            log.info("学生数据已存在 ({}条)，跳过", existing);
            return;
        }

        List<SysClass> allClasses = sysClassRepository.findAll();
        log.info("开始生成 {} 个班级的学生数据 ...", allClasses.size());

        int totalStudents = 0;
        int totalResumes = 0;

        // 收集所有已发布的职位（用于投递）
        List<JobPosition> allJobs = jobPositionRepository.findAll().stream()
                .filter(j -> "published".equals(j.getStatus()))
                .collect(Collectors.toList());

        int classIndex = 0;
        for (SysClass cls : allClasses) {
            classIndex++;
            int gradeYear = Integer.parseInt(cls.getGrade());
            int gradYear = gradeYear + 4; // 入学年份+4=毕业年份
            String gradeLabel = String.valueOf(gradeYear);

            List<SysUser> batchUsers = new ArrayList<>();
            List<StudentInfo> batchInfos = new ArrayList<>();
            List<StudentResume> batchResumes = new ArrayList<>();

            for (int s = 0; s < 30; s++) {
                boolean isMale = RANDOM.nextBoolean();
                String name = randomName(isMale);
                int age = 18 + RANDOM.nextInt(4);

                SysUser stu = new SysUser();
                String username = "stu_" + String.format("%05d", ++studentSeq);
                stu.setUsername(username);
                stu.setRealName(name);
                stu.setGender(isMale ? "male" : "female");
                stu.setPhone(generatePhone());
                stu.setEmail(username + "@student.edu.cn");
                stu.setIdCard(generateIdCard(age, isMale));
                stu.setDeptId(cls.getDeptId());
                stu.setMajorId(cls.getMajorId());
                stu.setClassId(cls.getId());
                stu.setClassName(cls.getClassName());
                stu.setStudentNo(nextStudentNo(gradYear));
                stu.setGraduationYear(gradYear);
                stu.setStatus(Constants.STATUS_NORMAL);
                stu.setPassword(passwordEncoder.encode("123456"));
                stu.setRemark("系统自动初始化学生");
                batchUsers.add(stu);

                // student_info
                StudentInfo info = new StudentInfo();
                info.setRealName(name);
                info.setGender(isMale ? "male" : "female");
                info.setBirthDate("20" + IDCARD_PREFIX.substring(2, 4) + "-" + String.format("%02d", RANDOM.nextInt(12) + 1) + "-" + String.format("%02d", RANDOM.nextInt(28) + 1));
                info.setIdCard(generateIdCard(age, isMale));
                info.setNation(randomItem(new String[]{"汉族","满族","回族","蒙古族","土家族","苗族","维吾尔族","壮族"}));
                info.setPoliticsStatus(randomItem(new String[]{"共青团员","中共预备党员","中共党员","群众"}));
                info.setPhone(stu.getPhone());
                info.setEmail(stu.getEmail());
                info.setProvince(randomBirthProvince());
                info.setCity(randomBirthCity(info.getProvince()));
                info.setAddress(info.getProvince() + info.getCity() + "某街道某小区");
                info.setDeptId(cls.getDeptId());
                info.setMajorId(cls.getMajorId());
                info.setClassId(cls.getId());
                info.setClassName(cls.getClassName());
                info.setGraduationYear(gradYear);
                info.setStudentNo(stu.getStudentNo());
                info.setStudyType("全日制本科");
                info.setDormitory("学生公寓" + (RANDOM.nextInt(10) + 1) + "栋" + (RANDOM.nextInt(4) + 1) + "楼" + (RANDOM.nextInt(20) + 1) + "室");
                info.setEmergencyContact(randomName(!isMale) + "（" + randomItem(new String[]{"父亲","母亲","叔叔","姑姑","舅舅","姨"}) + "）");
                info.setEmergencyPhone(generatePhone());
                info.setStatus("studying");
                batchInfos.add(info);

                // student_resume（期望城市/职位/行业由 build() 内部根据专业匹配生成）
                StudentResume resume = new ResumeBuilder()
                        .name(name).gender(isMale ? "male" : "female")
                        .phone(stu.getPhone()).email(stu.getEmail())
                        .graduationYear(gradYear)
                        .majorName(majorRepository.findById(cls.getMajorId()).map(SysMajor::getMajorName).orElse(""))
                        .deptName(deptRepository.findById(cls.getDeptId()).map(SysDept::getDeptName).orElse(""))
                        .className(cls.getClassName())
                        .build();
                batchResumes.add(resume);
            }

            // 批量保存用户
            List<SysUser> savedUsers = userRepository.saveAll(batchUsers);
            SysRole studentRole = roleRepository.findByRoleKey(Constants.ROLE_STUDENT)
                    .orElseThrow(() -> new BusinessException("学生角色不存在"));
            List<SysUserRole> urs = new ArrayList<>();
            for (SysUser u : savedUsers) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(u.getId());
                ur.setRoleId(studentRole.getId());
                urs.add(ur);
            }
            userRoleRepository.saveAll(urs);

            // 填充 userId 并保存 student_info
            for (int i = 0; i < savedUsers.size(); i++) {
                batchInfos.get(i).setUserId(savedUsers.get(i).getId());
                batchResumes.get(i).setStudentId(savedUsers.get(i).getId());
            }
            studentInfoRepository.saveAll(batchInfos);
            studentResumeRepository.saveAll(batchResumes);

            totalStudents += savedUsers.size();
            totalResumes += batchResumes.size();

            // 已毕业届（2023/2024届）：生成投递记录
            if (gradYear <= 2024) {
                generateApplicationsForClass(savedUsers, batchResumes, allJobs, gradYear);
            }

            if (classIndex % 50 == 0) {
                log.info("  已处理 {}/{} 个班级，学生累计 {} 人 ...", classIndex, allClasses.size(), totalStudents);
            }
        }

        log.info("学生数据初始化完成: {}人, {}份简历", totalStudents, totalResumes);
    }

    // 简历构建器
    private class ResumeBuilder {
        private final StudentResume r = new StudentResume();
        private String name, gender, phone, email, expectedCity, expectedPosition;
        private int graduationYear;
        private String majorName, deptName, className;

        ResumeBuilder name(String v) { this.name = v; this.r.setResumeName(v + "的简历"); return this; }
        ResumeBuilder gender(String v) { this.gender = v; return this; }
        ResumeBuilder phone(String v) { this.phone = v; return this; }
        ResumeBuilder email(String v) { this.email = v; return this; }
        ResumeBuilder expectedCity(String v) { this.expectedCity = v; return this; }
        ResumeBuilder expectedPosition(String v) { this.expectedPosition = v; return this; }
        ResumeBuilder graduationYear(int v) { this.graduationYear = v; return this; }
        ResumeBuilder majorName(String v) { this.majorName = v; return this; }
        ResumeBuilder deptName(String v) { this.deptName = v; return this; }
        ResumeBuilder className(String v) { this.className = v; return this; }

        StudentResume build() {
            r.setIsDefault("1");

            // 专业匹配的职位和行业
            String matchedPosition = randomMajorPosition(majorName);
            String deptCode = deptRepository.findByDeptName(deptName)
                    .map(SysDept::getDeptCode).orElse("DASHUJU");
            String deptIndustry = getIndustryByDeptCode(deptCode);
            r.setExpectedPosition(matchedPosition);
            r.setExpectedCity(randomWorkCityForIndustry(deptIndustry));
            r.setExpectedIndustry(deptIndustry);
            r.setExpectedSalaryMin(5000 + RANDOM.nextInt(10) * 500);
            r.setExpectedSalaryMax(r.getExpectedSalaryMin() + 3000 + RANDOM.nextInt(8) * 500);

            // 教育经历
            int enrollYear = graduationYear - 4;
            r.setEducationExperience(
                enrollYear + "-09 至 " + graduationYear + "-07  |  " + deptName + "  |  " + majorName + "  |  " + className + "\n" +
                "GPA: " + String.format("%.2f", 2.5 + RANDOM.nextDouble() * 1.5) + "/4.0（专业前" + (RANDOM.nextInt(30) + 1) + "%）"
            );

            // 项目经历（1-3个）
            int projCount = 1 + RANDOM.nextInt(3);
            StringBuilder projSb = new StringBuilder();
            for (int i = 0; i < projCount; i++) {
                projSb.append(enrollYear + 2 + i).append("-03 至 ").append(enrollYear + 2 + i + 1).append("-06  |  ").append(randomItem(PROJECTS));
                if (i < projCount - 1) projSb.append("\n\n");
            }
            r.setProjectExperience(projSb.toString());

            // 技能证书
            int certCount = 1 + RANDOM.nextInt(3);
            StringBuilder certSb = new StringBuilder();
            for (int i = 0; i < certCount; i++) {
                if (i > 0) certSb.append("\n");
                certSb.append("• ").append(randomItem(CERTIFICATES));
            }
            r.setSkillCertificates(certSb.toString());

            // 获奖情况
            int awardCount = RANDOM.nextInt(4);
            StringBuilder awardSb = new StringBuilder();
            for (int i = 0; i < awardCount; i++) {
                if (i > 0) awardSb.append("\n");
                awardSb.append("• ").append(enrollYear + RANDOM.nextInt(3)).append("学年 ").append(randomItem(AWARDS));
            }
            if (awardCount == 0) r.setAwardsHonors("无");
            else r.setAwardsHonors(awardSb.toString());

            r.setSelfEvaluation(randomItem(SELF_EVALS));
            return r;
        }
    }

    // =========================================================
    //  阶段五：已毕业届的投递记录
    // =========================================================
    private void generateApplicationsForClass(List<SysUser> students, List<StudentResume> resumes,
                                               List<JobPosition> allJobs, int gradYear) {
        if (allJobs.isEmpty() || students.isEmpty()) return;
        List<JobApplication> applications = new ArrayList<>();
        List<JobFavorite> favorites = new ArrayList<>();
        List<Notification> notifications = new ArrayList<>();

        for (int i = 0; i < students.size(); i++) {
            SysUser stu = students.get(i);
            StudentResume resume = resumes.get(i);

            // 每个学生随机投递 0~5 个职位
            int applyCount = RANDOM.nextInt(6);
            Set<Long> appliedJobs = new HashSet<>();
            for (int j = 0; j < applyCount; j++) {
                JobPosition job = allJobs.get(RANDOM.nextInt(allJobs.size()));
                if (appliedJobs.contains(job.getId())) continue;
                appliedJobs.add(job.getId());

                JobApplication app = new JobApplication();
                app.setJobId(job.getId());
                app.setStudentId(stu.getId());
                app.setCompanyId(job.getCompanyId());
                app.setResumeId(resume.getId());

                // 模拟不同状态
                int statusRoll = RANDOM.nextInt(100);
                String status;
                if (statusRoll < 60) {
                    status = "reviewed";        // 已查看
                } else if (statusRoll < 80) {
                    status = "interview";        // 进入面试
                    // 生成面试邀请
                    InterviewInvitation inv = new InterviewInvitation();
                    inv.setApplicationId(0L); // 先占位，后面替换
                    inv.setStudentId(stu.getId());
                    inv.setCompanyId(job.getCompanyId());
                    inv.setJobId(job.getId());
                    inv.setInterviewTime(LocalDateTime.now().minusMonths(gradYear - 2023).minusDays(RANDOM.nextInt(90)).format(DT_FMT));
                    inv.setInterviewAddress(job.getWorkAddress());
                    inv.setInterviewType(randomItem(new String[]{"线上面试","线下面试","电话面试"}));
                    inv.setContactPerson("HR");
                    inv.setContactPhone(generatePhone());
                    inv.setStatus("accepted");
                    InterviewInvitation savedInv = interviewInvitationRepository.save(inv);
                    app.setInterviewStatus("scheduled");
                    // 30%概率生成offer
                    if (RANDOM.nextInt(100) < 30) {
                        OfferLetter offer = new OfferLetter();
                        offer.setApplicationId(savedInv.getApplicationId());
                        offer.setStudentId(stu.getId());
                        offer.setCompanyId(job.getCompanyId());
                        offer.setJobId(job.getId());
                        offer.setPositionName(job.getJobName());
                        offer.setSalary(job.getSalaryMin() + "-" + job.getSalaryMax() + "元/月");
                        offer.setWorkCity(job.getWorkCity());
                        offer.setStartDate(gradYear + "-07-01");
                        offer.setProbationPeriod("3个月");
                        offer.setProbationSalary((int)(job.getSalaryMin() * 0.8) + "元/月");
                        offer.setStatus("accepted");
                        offerLetterRepository.save(offer);
                        app.setOfferStatus("received");
                        status = "offer";
                    }
                } else if (statusRoll < 95) {
                    status = "rejected";          // 被拒
                    app.setCompanyRemark("与岗位要求不太匹配，感谢投递");
                } else {
                    status = "offer";             // 直接给offer
                    app.setOfferStatus("received");
                    OfferLetter offer = new OfferLetter();
                    offer.setApplicationId(0L);
                    offer.setStudentId(stu.getId());
                    offer.setCompanyId(job.getCompanyId());
                    offer.setJobId(job.getId());
                    offer.setPositionName(job.getJobName());
                    offer.setSalary(job.getSalaryMin() + "-" + job.getSalaryMax() + "元/月");
                    offer.setWorkCity(job.getWorkCity());
                    offer.setStartDate(gradYear + "-07-01");
                    offer.setStatus("accepted");
                    offerLetterRepository.save(offer);
                }
                app.setStatus(status);
                app.setReadStatus("1");
                applications.add(app);

                // 通知学生投递被查看
                if ("reviewed".equals(status) || "interview".equals(status)) {
                    Notification n = new Notification();
                    n.setUserId(stu.getId());
                    n.setTitle("简历被查看");
                    n.setContent("您的简历已被 " + job.getCompanyName() + " 查看，岗位：" + job.getJobName());
                    n.setType("application");
                    n.setCategory("投递动态");
                    n.setIsRead("0");
                    notifications.add(n);
                }
                if ("interview".equals(status)) {
                    Notification n = new Notification();
                    n.setUserId(stu.getId());
                    n.setTitle("面试邀请");
                    n.setContent(job.getCompanyName() + " 邀请您参加" + job.getJobName() + "岗位的面试，请注意查收");
                    n.setType("interview");
                    n.setCategory("投递动态");
                    n.setIsRead("0");
                    notifications.add(n);
                }
            }

            // 收藏 0~3 个职位
            int favCount = RANDOM.nextInt(4);
            for (int j = 0; j < favCount; j++) {
                JobPosition job = allJobs.get(RANDOM.nextInt(allJobs.size()));
                JobFavorite fav = new JobFavorite();
                fav.setJobId(job.getId());
                fav.setStudentId(stu.getId());
                favorites.add(fav);
            }
        }

        if (!applications.isEmpty())   jobApplicationRepository.saveAll(applications);
        if (!favorites.isEmpty())      jobFavoriteRepository.saveAll(favorites);
        if (!notifications.isEmpty()) notificationRepository.saveAll(notifications);
    }

    // =========================================================
    //  阶段六：已毕业届的就业登记（2023/2024届）
    // =========================================================
    private void initEmploymentRecordsForGraduates() {
        long existing = employmentRecordRepository.count();
        if (existing > 0) {
            log.info("就业登记数据已存在 ({}条)，跳过", existing);
            return;
        }
        log.info("开始生成已毕业届（2023/2024届）就业登记数据 ...");
        List<SysUser> allUsers = userRepository.findAll();
        List<SysUser> graduatedUsers = allUsers.stream()
                .filter(u -> u.getGraduationYear() != null
                          && (u.getGraduationYear() == 2023 || u.getGraduationYear() == 2024))
                .collect(Collectors.toList());
        log.info("已毕业届学生共 {} 人", graduatedUsers.size());

        List<CompanyInfo> companies = companyInfoRepository.findAll();
        if (companies.isEmpty()) {
            log.warn("没有企业数据，跳过就业登记生成");
            return;
        }

        // 按行业分类公司
        Map<String, List<CompanyInfo>> companiesByIndustry = new HashMap<>();
        for (CompanyInfo c : companies) {
            companiesByIndustry.computeIfAbsent(c.getIndustry(), k -> new ArrayList<>()).add(c);
        }

        List<EmploymentRecord> records = new ArrayList<>();
        for (SysUser stu : graduatedUsers) {
            String empType = randomEmploymentType();
            EmploymentRecord rec = new EmploymentRecord();
            rec.setStudentId(stu.getId());
            rec.setEmploymentType(empType);
            rec.setAuditStatus("approved");
            rec.setAuditTime(LocalDateTime.of(
                    stu.getGraduationYear(), 6, 15, 10, 0).format(DT_FMT));

            // 获取学生所在学院代码，推断行业
            Long deptId = stu.getDeptId();
            String deptCode = deptRepository.findById(deptId)
                    .map(SysDept::getDeptCode).orElse("DASHUJU");
            String targetIndustry = getIndustryByDeptCode(deptCode);

            // 获取学生专业名称，用于匹配职位
            String majorName = majorRepository.findById(stu.getMajorId() != null ? stu.getMajorId() : 0L)
                    .map(SysMajor::getMajorName).orElse("");

            if ("签订劳动合同".equals(empType) || "签订三方协议".equals(empType)) {
                // 根据行业选择公司
                List<CompanyInfo> industryCompanies = companiesByIndustry.getOrDefault(targetIndustry, companies);
                if (industryCompanies.isEmpty()) industryCompanies = companies;
                CompanyInfo comp = industryCompanies.get(RANDOM.nextInt(industryCompanies.size()));
                rec.setCompanyName(comp.getCompanyName());
                rec.setCompanyCode(comp.getCompanyCode());
                rec.setCompanyScale(comp.getScale());
                rec.setCompanyIndustry(comp.getIndustry());
                rec.setPositionName(randomMajorPosition(majorName));
                rec.setPositionCategory(getPositionCategory(majorName));
                rec.setWorkCity(comp.getCity());
                rec.setWorkProvince(comp.getProvince());
                int salary = getSalaryForIndustry(comp.getIndustry());
                rec.setSalary(salary + "元/月");
                boolean isSigned = "签订三方协议".equals(empType);
                rec.setIsThreePartySigned(isSigned ? "1" : "0");
                if (isSigned) {
                    rec.setThreePartyNo("TPA-" + stu.getGraduationYear() + "-" + String.format("%06d", RANDOM.nextInt(999999)));
                    rec.setContractStartDate(stu.getGraduationYear() + "-07-01");
                    rec.setContractEndDate(stu.getGraduationYear() + 3 + "-06-30");
                    rec.setProbationSalary((int)(salary * 0.8) + "元/月");
                }
                rec.setRemark("系统自动生成的模拟就业数据");
            } else if ("继续深造".equals(empType)) {
                rec.setCompanyName("——升学——");
                rec.setPositionName(randomItem(new String[]{"国内考研","出国深造","保送研究生","第二学位","定向培养"}));
                rec.setWorkCity("未知");
                rec.setWorkProvince("湖北省"); // 升学学生留在湖北省内继续深造比例最高
                rec.setSalary("待定");
                rec.setRemark("系统自动生成的模拟升学数据");
            } else if ("自主创业".equals(empType)) {
                rec.setCompanyName("——自主创业——");
                rec.setPositionName("创业（" + targetIndustry + "相关领域）");
                rec.setCompanyIndustry(targetIndustry);
                rec.setWorkCity(randomWorkCityForIndustry(targetIndustry));
                rec.setWorkProvince(mapCityToProvince(rec.getWorkCity())); // 从城市反推省份
                rec.setSalary("待定（创业）");
                rec.setRemark("系统自动生成的模拟创业数据");
            } else if ("出国出境".equals(empType)) {
                rec.setCompanyName("——出国出境——");
                rec.setPositionName("出国深造/工作");
                rec.setWorkCity("境外");
                rec.setWorkProvince("境外"); // 境外统一标记
                rec.setSalary("待定");
                rec.setRemark("系统自动生成的模拟出国数据");
            } else if ("应征入伍".equals(empType)) {
                rec.setCompanyName("——应征入伍——");
                rec.setPositionName("义务兵/直招士官");
                rec.setWorkCity("部队驻地");
                rec.setWorkProvince("湖北省"); // 湖北省高校学生应征入伍以本省为主
                rec.setSalary("军队薪资标准");
                rec.setRemark("系统自动生成的模拟入伍数据");
            } else if ("暂未就业".equals(empType)) {
                rec.setCompanyName("——暂未就业——");
                rec.setPositionName("求职中");
                rec.setWorkCity("未知");
                rec.setWorkProvince("湖北省"); // 未就业学生求职以本省为主
                rec.setSalary("待定");
                rec.setRemark("系统自动生成的模拟待就业数据");
            } else {
                rec.setCompanyName("——其他——");
                rec.setPositionName("其他就业形式");
                rec.setWorkCity("未知");
                rec.setWorkProvince("湖北省"); // 归入本省统计
                rec.setSalary("待定");
                rec.setRemark("系统自动生成的其他就业数据");
            }
            records.add(rec);
        }
        employmentRecordRepository.saveAll(records);
        log.info("就业登记初始化完成: {}条", records.size());
    }

    // =========================================================
    //  阶段七：企业 & 职位
    // =========================================================
    private void initCompaniesAndJobsInternal() {
        if (companyInfoRepository.count() > 0) {
            log.info("企业数据已存在，跳过");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMonths(3);

        List<CompanyInfo> companies = Arrays.asList(
            mkCompany("字节跳动",        "91100100MA6KWG9J0E", "张一鸣",  "HR部门",        "18800001001", "hr@bytedance.com",  "北京", "北京市",    "海淀区",     "互联网",         "1000人以上", "民营企业", "www.bytedance.com",  "全球领先的互联网科技公司，旗下产品包括今日头条、抖音、TikTok等。", now),
            mkCompany("华为技术有限公司","91440300100089316E", "胡厚崑",  "校园招聘组",    "18800001002", "campus@huawei.com",  "广东", "深圳市",    "龙岗区",     "互联网",         "1000人以上", "民营企业", "www.huawei.com",      "全球领先的ICT基础设施和智能终端提供商，服务全球30亿用户。", now),
            mkCompany("阿里巴巴集团",    "91330000MA2H4XXM0W", "张勇",    "招聘团队",      "18800001003", "campus@alibaba.com", "浙江", "杭州市",    "余杭区",     "互联网",         "1000人以上", "民营企业", "www.alibaba.com",     "全球最大的电子商务平台之一，业务涵盖电商、云计算、数字媒体等。", now),
            mkCompany("腾讯科技",        "91440300770885628T", "马化腾",  "HR招聘",        "18800001004", "talent@tencent.com", "广东", "深圳市",    "南山区",     "互联网",         "1000人以上", "民营企业", "www.tencent.com",    "中国最大的综合性互联网服务企业之一，产品涵盖社交、游戏、金融科技等。", now),
            mkCompany("小米集团",        "91110108551385082Q", "雷军",    "HR校招组",      "18800001005", "campus@xiaomi.com",  "北京", "北京市",    "海淀区",     "互联网",         "1000人以上", "民营企业", "www.xiaomi.com",     "以手机、智能硬件和IoT平台为核心的互联网公司。", now),
            mkCompany("大疆创新",        "91440300553897623C", "汪滔",    "人才招聘",      "18800001006", "djihr@dji.com",      "广东", "深圳市",    "南山区",     "航空航天",       "1000-4999人","民营企业","www.dji.com",         "全球无人机与航拍科技领导者，市场份额超过70%。", now),
            mkCompany("海康威视",        "91330000796699228P", "胡扬忠",  "校招HR",        "18800001007", "campus@hikvision.com","浙江","杭州市",    "滨江区",     "电子/半导体",    "1000人以上", "民营企业", "www.hikvision.com",  "以视频为核心的物联网解决方案和数据服务提供商。", now),
            mkCompany("中兴通讯",        "91440300192264518E", "李自学",  "招聘中心",      "18800001008", "campus@zte.com.cn",  "广东", "深圳市",    "南山区",     "互联网",         "1000人以上", "国有企业", "www.zte.com.cn",     "全球领先的通信设备制造商和解决方案提供商。", now),
            mkCompany("三一重工",        "91430000790327729R", "梁稳根",  "人力资源部",    "18800001009", "campus@sany.com.cn", "湖南", "长沙市",    "长沙县",     "机械/装备",      "1000人以上", "民营企业", "www.sany.com.cn",    "全球工程机械制造商50强，中国最大的混凝土机械制造商。", now),
            mkCompany("中国中车",        "91110000100001936J", "刘华龙",  "校园招聘组",    "18800001010", "campus@crrc.com.cn", "北京", "北京市",    "丰台区",     "机械/装备",      "1000人以上", "国有企业", "www.crrc.com.cn",    "全球规模最大的轨道交通装备制造商，产品覆盖轨道交通全产业链。", now),
            mkCompany("比亚迪股份",      "91440300192334855A", "王传福",  "HR校招中心",    "18800001011", "campus@byd.com",     "广东", "深圳市",    "坪山区",     "机械/装备",      "1000人以上", "民营企业", "www.byd.com",        "全球新能源汽车领导者，业务涵盖汽车、轨道交通、新能源和电子四大领域。", now),
            mkCompany("京东方",         "91110302700417405N", "陈炎顺",  "人才招聘",      "18800001012", "campus@boe.com.cn",  "北京", "北京市",    "亦庄经济开发区","电子/半导体","1000人以上","民营企业","www.boe.com.cn",     "全球半导体显示产业龙头企业，显示屏出货量全球第一。", now),
            mkCompany("中国建筑",        "91110000100001945T", "郑学选",  "校园招聘",      "18800001013", "campus@cscec.com.cn","北京", "北京市",    "朝阳区",     "建筑/房地产",    "1000人以上", "国有企业", "www.cscec.com.cn",  "全球最大的投资建设集团，位列世界500强前列。", now),
            mkCompany("紫金矿业",        "91350000158142167J", "邹来昌",  "人力资源",      "18800001014", "campus@zijinmining.com","福建","龙岩市",   "上杭县",     "矿业/能源",      "1000人以上", "民营企业", "www.zijinmining.com","中国最大的黄金生产企业之一，全球金属矿业企业重要成员。", now),
            mkCompany("万华化学",        "91370000163520081K", "寇光武",  "校招HR",        "18800001015", "campus@whchem.com",  "山东", "烟台市",    "开发区",     "化工/材料",      "1000-4999人","民营企业","www.whchem.com",     "全球聚氨酯龙头企业，MDI产能居全球首位。", now),
            mkCompany("中国航空工业",    "91110000100001988F", "谭瑞松",  "人力资源部",    "18800001016", "campus@avic.com.cn", "北京", "北京市",    "朝阳区",     "航空航天",       "1000人以上", "国有企业", "www.avic.com.cn",    "特大型国有企业集团，是我国军民用航空器和防务装备的研制生产基地。", now),
            mkCompany("美的集团",        "91440606190352347A", "方洪波",  "校园招聘",      "18800001017", "campus@midea.com",   "广东", "佛山市",    "顺德区",     "机械/装备",      "1000人以上", "民营企业", "www.midea.com",     "全球领先的家电与暖通空调系统企业，业务遍及200多个国家和地区。", now),
            mkCompany("国家电网",        "9111000010000093XF", "辛保安",  "招聘中心",      "18800001018", "campus@sgcc.com.cn", "北京", "北京市",    "西城区",     "矿业/能源",      "1000人以上", "国有企业", "www.sgcc.com.cn",    "全球最大的公用事业企业，世界500强第二位。", now),
            mkCompany("中国石化",        "91110000100001955U", "马永生",  "毕业生招聘",    "18800001019", "campus@sinopec.com","北京", "北京市",    "朝阳区",     "化工/材料",      "1000人以上", "国有企业", "www.sinopec.com",   "中国最大的一体化能源化工公司之一，世界500强前列。", now),
            mkCompany("网易（杭州）",    "91330100717575806P", "丁磊",    "校招HR",        "18800001020", "campus163@corp.netease.com","浙江","杭州市","滨江区","互联网","1000-4999人","民营企业","www.163.com","中国领先的互联网技术公司，以游戏、教育、音乐、电商为核心。", now),
            mkCompany("中国中铁",        "91110000100003022E", "陈云",    "人力资源部",    "18800001021", "campus@crecg.com",   "北京", "北京市",    "海淀区",     "建筑/房地产",    "1000人以上", "国有企业", "www.crecg.com",     "全球大型建筑工程承包商，世界500强前列，主营铁路、公路、房地产等。", now),
            mkCompany("中国铁建",        "91110000100003845X", "汪建平",  "校园招聘组",    "18800001022", "campus@crcc.cn",     "北京", "北京市",    "海淀区",     "建筑/房地产",    "1000人以上", "国有企业", "www.crcc.cn",       "全球最具实力的特大型综合建设集团之一，主营工程承包、房地产开发等。", now),
            mkCompany("国家能源集团",    "91110000100001889M", "刘国跃",  "招聘中心",      "18800001023", "campus@ceic.com",    "北京", "北京市",    "东城区",     "矿业/能源",      "1000人以上", "国有企业", "www.ceic.com",       "全球最大的煤炭生产公司、世界一流能源集团。", now),
            mkCompany("中国中煤能源",    "91110000100000929W", "王树东",  "人力资源",      "18800001024", "campus@chinacoal.com","北京","北京市",    "朝阳区",     "矿业/能源",      "100-499人", "国有企业", "www.chinacoal.com","中国煤炭行业的大型骨干企业。", now),
            mkCompany("宝山钢铁",        "9131000063119964X2", "邹继新",  "校园招聘",      "18800001025", "campus@baosteel.com","上海","上海市",    "宝山区",     "化工/材料",      "1000人以上", "国有企业", "www.baosteel.com",  "中国最大、最现代化的钢铁联合企业。", now),
            mkCompany("中国铝业",        "91110000100003889A", "刘建平",  "招聘组",        "18800001026", "campus@chalco.com.cn","北京","北京市",   "海淀区",     "化工/材料",      "1000人以上", "国有企业", "www.chalco.com.cn",  "中国铝行业龙头企业，全球铝业重要成员。", now),
            mkCompany("中国化工集团",    "91110000100001938J", "李凡荣",  "人才招聘",      "18800001027", "campus@chemchina.com","北京","北京市",   "海淀区",     "化工/材料",      "1000人以上", "国有企业", "www.chemchina.com",  "中国最大的化工企业，全球化工行业重要成员。", now),
            mkCompany("中国核电",        "91110000100001941N", "卢铁忠",  "校园招聘",      "18800001028", "campus@cnnc.com.cn", "北京", "北京市",    "西城区",     "矿业/能源",      "100-499人", "国有企业", "www.cnnc.com.cn",   "中国核电发展的主力军，全球核电建设重要力量。", now),
            mkCompany("航天科工集团",    "91110000100001948P", "刘石泉",  "人力资源部",    "18800001029", "campus@casic.com.cn","北京","北京市",    "海淀区",     "航空航天",       "1000人以上", "国有企业", "www.casic.com.cn",  "中国航天事业的主力军之一，主营航天防务、信息技术等。", now),
            mkCompany("中国商飞",        "91310000681001857N", "贺东风",  "人才引进",      "18800001030", "campus@comac.cc",   "上海", "上海市",    "浦东新区",   "航空航天",       "500-999人","国有企业","www.comac.cc",       "中国商用飞机有限责任公司，ARJ21、C919等国产大飞机研制单位。", now)
        );

        List<CompanyInfo> savedCompanies = companyInfoRepository.saveAll(companies);
        log.info("企业初始化完成: {}条", savedCompanies.size());

        // 每个企业发布 2~4 个职位
        List<JobPosition> allJobs = new ArrayList<>();
        String[][] JOB_TEMPLATES = {
            {"后端开发工程师",   "后端开发", "本科及以上", "Java / Spring Boot / MySQL / Redis", "15000", "25000", "12薪"},
            {"前端开发工程师",   "前端开发", "本科及以上", "Vue3 / React / JavaScript / CSS",      "13000", "22000", "12薪"},
            {"算法工程师",       "人工智能", "硕士及以上", "Python / 机器学习 / 深度学习 / TensorFlow","25000","40000","14薪"},
            {"测试开发工程师",   "软件测试", "本科及以上", "Python / Selenium / JUnit / 接口测试","10000","18000","12薪"},
            {"运维工程师",       "运维",     "本科及以上", "Linux / Docker / K8s / Shell",        "12000","20000","12薪"},
            {"数据分析师",       "数据分析", "本科及以上", "Python / SQL / Pandas / Tableau",    "12000","20000","13薪"},
            {"嵌入式软件工程师", "嵌入式",   "本科及以上", "C / C++ / ARM / RTOS / 嵌入式Linux","14000","25000","12薪"},
            {"机械设计工程师",   "机械设计", "本科及以上", "SolidWorks / AutoCAD / ANSYS",        "8000","15000","12薪"},
            {"电气工程师",       "电气",     "本科及以上", "PLC / CAD / 电气控制 / 变频器",       "8000","15000","12薪"},
            {"材料研发工程师",   "材料研发", "硕士及以上", "材料科学 / 金相分析 / 制备工艺",       "12000","22000","12薪"},
        };

        for (CompanyInfo comp : savedCompanies) {
            int jobCount = 2 + RANDOM.nextInt(3);
            for (int j = 0; j < jobCount; j++) {
                String[] tmpl = JOB_TEMPLATES[RANDOM.nextInt(JOB_TEMPLATES.length)];
                JobPosition job = new JobPosition();
                job.setCompanyId(comp.getId());
                job.setCompanyName(comp.getCompanyName());
                job.setJobName(tmpl[0]);
                job.setJobCategory(tmpl[1]);
                job.setJobType("全职");
                job.setWorkCity(comp.getCity());
                job.setWorkAddress(comp.getAddress());
                job.setSalaryMin(Integer.parseInt(tmpl[4].replace("K","")));
                job.setSalaryMax(Integer.parseInt(tmpl[5].replace("K","")));
                job.setSalaryMonths(tmpl[6]);
                job.setEducationRequired(tmpl[2]);
                job.setExperienceRequired("不限");
                job.setSkillRequired(tmpl[3]);
                job.setRecruitNumber(2 + RANDOM.nextInt(8));
                job.setRequirement("1. 计算机/软件相关专业本科及以上学历\n2. 熟悉相关技术栈\n3. 有良好的沟通能力和团队协作精神\n4. 有项目经验或实习经验者优先");
                job.setResponsibility("1. 负责项目核心模块设计与开发\n2. 参与技术方案设计与评审\n3. 解决开发过程中的技术难题\n4. 撰写技术文档");
                job.setBenefits("六险一金 | 弹性工作 | 免费三餐 | 租房补贴 | 带薪年假 | 节日福利 | 年度体检 | 团建活动 | 股票期权");
                job.setIsRemote("0");
                job.setIsHighSalary(j == 0 ? "1" : "0");
                job.setStatus("published");
                job.setPublishTime(now.minusDays(RANDOM.nextInt(60) + 1).format(DT_FMT));
                job.setDeadline(deadline.format(D_FMT));
                job.setViewCount(50 + RANDOM.nextInt(500));
                job.setApplyCount(5 + RANDOM.nextInt(50));
                job.setIsDeleted("0");
                allJobs.add(job);
            }
        }
        jobPositionRepository.saveAll(allJobs);
        log.info("职位初始化完成: {}条", allJobs.size());
    }

    private CompanyInfo mkCompany(String name, String creditCode, String legalPerson,
                                  String contact, String phone, String email,
                                  String province, String city, String district,
                                  String industry, String scale, String nature,
                                  String website, String intro, LocalDateTime now) {
        CompanyInfo c = new CompanyInfo();
        c.setCompanyName(name);
        c.setUnifiedCreditCode(creditCode);
        c.setLegalPerson(legalPerson);
        c.setContactPerson(contact);
        c.setContactPhone(phone);
        c.setContactEmail(email);
        c.setProvince(province);
        c.setCity(city);
        c.setDistrict(district);
        c.setAddress(province + city + district + name + "总部");
        c.setIndustry(industry);
        c.setScale(scale);
        c.setNature(nature);
        c.setWebsite(website);
        c.setIntroduction(intro);
        c.setAuthStatus("approved");
        c.setStatus("1");
        return c;
    }

    // =========================================================
    //  标记文件读写
    // =========================================================
    private boolean isAlreadyInitialized() {
        Path marker = Paths.get(MARKER_FILE);
        if (!Files.exists(marker)) return false;
        try (BufferedReader reader = Files.newBufferedReader(marker, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[512];
            int len;
            while ((len = reader.read(buf)) != -1) sb.append(buf, 0, len);
            return sb.toString().contains("\"version\":\"" + MARKER_VERSION + "\"");
        } catch (IOException e) {
            log.warn("读取标记文件失败，将重新初始化: {}", e.getMessage());
            return false;
        }
    }

    private void writeMarker() {
        String json = "{\n  \"version\": \"" + MARKER_VERSION + "\",\n  " +
                      "\"initTime\": \"" + LocalDateTime.now().format(DT_FMT) + "\",\n  " +
                      "\"description\": \"高校就业分析系统完整数据初始化标记文件\"\n}";
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(MARKER_FILE), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(json);
            log.info("标记文件已写入: {}", Paths.get(MARKER_FILE).toAbsolutePath());
        } catch (IOException e) {
            log.error("写入标记文件失败: {}", e.getMessage());
        }
    }

    // =========================================================
    //  接口方法（保留兼容性）
    // =========================================================
    @Override
    public List<Map<String, Object>> getDeptTree() {
        return deptRepository.findByStatus(Constants.STATUS_NORMAL).stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("deptName", d.getDeptName());
            m.put("isTopLevel", d.getIsTopLevel());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getMajorsByDeptId(Long deptId) {
        return majorRepository.findByDeptId(deptId).stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("majorName", m.getMajorName());
            map.put("majorCode", m.getMajorCode());
            map.put("isTopLevel", m.getIsTopLevel());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> autoInitAll() {
        initAllInternal();
        Map<String, Object> r = new HashMap<>();
        r.put("deptCount", deptRepository.count());
        r.put("majorCount", majorRepository.count());
        r.put("classCount", sysClassRepository.count());
        r.put("studentCount", studentInfoRepository.count());
        r.put("companyCount", companyInfoRepository.count());
        r.put("jobCount", jobPositionRepository.count());
        r.put("message", "初始化完成（标记文件已生成，后续启动将自动跳过）");
        return r;
    }

    @Override
    @Transactional
    public Map<String, Object> resetAndInitAll() {
        log.info("===== 开始强制重置 =====");
        notificationRepository.deleteAll();
        offerLetterRepository.deleteAll();
        interviewInvitationRepository.deleteAll();
        employmentRecordRepository.deleteAll();
        jobApplicationRepository.deleteAll();
        jobFavoriteRepository.deleteAll();
        studentResumeRepository.deleteAll();
        studentInfoRepository.deleteAll();
        // 清理非管理员、非数据分析师的用户
        userRoleRepository.deleteAll();
        userRepository.findAll().forEach(u -> {
            if (!"admin".equals(u.getUsername()) && !"datastaff".equals(u.getUsername())) {
                userRepository.delete(u);
            }
        });
        sysClassRepository.deleteAll();
        majorRepository.deleteAll();
        deptRepository.deleteAll();
        companyInfoRepository.deleteAll();
        jobPositionRepository.deleteAll();
        // 删除标记文件
        try { Files.deleteIfExists(Paths.get(MARKER_FILE)); } catch (IOException ignored) {}
        log.info("===== 强制重置完成，开始重新初始化 =====");
        initAllInternal();
        Map<String, Object> r = new HashMap<>();
        r.put("deptCount", deptRepository.count());
        r.put("majorCount", majorRepository.count());
        r.put("classCount", sysClassRepository.count());
        r.put("studentCount", studentInfoRepository.count());
        r.put("companyCount", companyInfoRepository.count());
        r.put("jobCount", jobPositionRepository.count());
        r.put("message", "强制重置并重新初始化完成");
        return r;
    }
}
