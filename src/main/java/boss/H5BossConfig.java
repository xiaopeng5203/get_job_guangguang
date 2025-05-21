package boss;

import lombok.Data;
import lombok.SneakyThrows;
import utils.JobUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author loks666
 * 项目链接: <a href="https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 */
@Data
public class H5BossConfig {
    /**
     * 开发者模式，默认为false即可
     */
    private Boolean debugger;

    /**
     * 用于打招呼的语句
     */
    private String sayHi;


    /**
     * 搜索关键词列表
     */
    private List<String> keywords;

    /**
     * 城市编码
     */
    private List<String> cityCode;

    /**
     * 自定义城市编码映射
     */
    private Map<String, String> customCityCode;


    /**
     * 工作经验要求
     */
    private List<String> experience;


    /**
     * 薪资范围
     */
    private String salary;

    /**
     * 学历要求列表
     */
    private List<String> degree;

    List<String> scale;

    /**
     * 公司行业列表
     */
    private List<String> industry;

    private Boolean keyFilter;

    /**
     * 投递去重的过期天数
     */
    private Integer deliverExpireDays;

    /**
     * 通用黑名单项，支持 name 和 days
     */
    @lombok.Data
    public static class BlackItem {
        private String name;
        private Integer days; // null 表示永久
        public BlackItem() {}
        public BlackItem(String name, Integer days) {
            this.name = name;
            this.days = days;
        }
        public boolean isExpired(Long addTime) {
            if (days == null || addTime == null) return false;
            long now = System.currentTimeMillis();
            return now - addTime > days * 24L * 3600 * 1000;
        }
    }

    /**
     * 手动配置的岗位黑名单，自动跳过这些岗位，支持字符串和对象两种方式
     */
    private List<Object> manualBlackJobs;
    /**
     * 手动配置的公司黑名单，自动跳过这些公司，支持字符串和对象两种方式
     */
    private List<Object> manualBlackCompanies;
    /**
     * 手动配置的招聘者黑名单，自动跳过这些招聘者，支持字符串和对象两种方式
     */
    private List<Object> manualBlackRecruiters;

    /**
     * 是否发送图片简历
     */
    private Boolean sendImgResume;

    private List<String> deadStatus;

    /**
     * 下一次检查的间隔时间（分钟）
     */
    private Integer nextIntervalMinutes;

    /**
     * 求职类型
     */
    private String jobType;

    /**
     * 公司融资阶段列表
     */
    private List<String> stage;

    /**
     * 期望薪资，单位为K，第一个数字为最低薪资，第二个数字为最高薪资，只填一个数字默认为最低薪水
     */
    private List<Integer> expectedSalary;

    /**
     * 每投递一个岗位，等待几秒
     */
    private Integer waitTime;

    /**
     * 开启AI检测与自动生成打招呼语
     */
    private Boolean enableAI;

    /**
     * 是否过滤不活跃HR,该选项会过滤半年前活跃的HR
     */
    private Boolean filterDeadHR;

    /**
     * 是否启用本地二次过滤，true为只投递严格匹配配置的岗位，false为允许平台推荐岗位
     */
    private Boolean strictLocalFilter = false;

    @SneakyThrows
    public static H5BossConfig init() {
        H5BossConfig config = JobUtils.getConfig(H5BossConfig.class);

        // 转换薪资范围
        config.setSalary(H5BossEnum.Salary.forValue(config.getSalary()).getCode());

        // 处理城市编码
        List<String> convertedCityCodes = config.getCityCode().stream()
                .map(city -> {
                    // 优先从自定义映射中获取
                    if (config.getCustomCityCode() != null && config.getCustomCityCode().containsKey(city)) {
                        return config.getCustomCityCode().get(city);
                    }
                    // 否则从枚举中获取
                    return H5BossEnum.CityCode.forValue(city).getCode();
                })
                .collect(Collectors.toList());
        config.setCityCode(convertedCityCodes);

        // 转换工作经验要求
        config.setExperience(config.getExperience().stream().map(value -> H5BossEnum.Experience.forValue(value).getCode()).collect(Collectors.toList()));
        // 转换学历要求
        config.setDegree(config.getDegree().stream().map(value -> H5BossEnum.Degree.forValue(value).getCode()).collect(Collectors.toList()));
        // 转换公司规模
        config.setScale(config.getScale().stream().map(value -> H5BossEnum.Scale.forValue(value).getCode()).collect(Collectors.toList()));

        return config;
    }

    /**
     * 统一获取岗位黑名单项列表
     */
    public List<BlackItem> getManualBlackJobItemList() {
        return parseBlackItemList(manualBlackJobs);
    }
    /**
     * 统一获取公司黑名单项列表
     */
    public List<BlackItem> getManualBlackCompanyItemList() {
        return parseBlackItemList(manualBlackCompanies);
    }
    /**
     * 统一获取招聘者黑名单项列表
     */
    public List<BlackItem> getManualBlackRecruiterItemList() {
        return parseBlackItemList(manualBlackRecruiters);
    }

    /**
     * 解析黑名单配置，兼容字符串和对象
     */
    private List<BlackItem> parseBlackItemList(List<Object> list) {
        if (list == null) return java.util.Collections.emptyList();
        return list.stream().map(obj -> {
            if (obj instanceof String) {
                return new BlackItem((String) obj, null);
            } else if (obj instanceof java.util.Map) {
                Map map = (Map) obj;
                String name = String.valueOf(map.get("name"));
                Integer days = map.get("days") == null ? null : Integer.valueOf(map.get("days").toString());
                return new BlackItem(name, days);
            } else {
                return null;
            }
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }

}
