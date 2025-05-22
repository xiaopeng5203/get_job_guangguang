package boss;

import lombok.Data;
import lombok.SneakyThrows;
import utils.JobUtils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author loks666
 * 项目链接: <a href="https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 */
@Data
public class BossConfig {
    /**
     * 用于打招呼的语句
     */
    private String sayHi;

    /**
     * 开发者模式
     */
    private Boolean debugger;

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
     * 行业列表
     */
    private List<String> industry;

    /**
     * 工作经验要求
     */
    private List<String> experience;

    /**
     * 工作类型
     */
    private String jobType;

    /**
     * 薪资范围
     */
    private Object salary;

    /**
     * 学历要求列表
     */
    private List<String> degree;

    /**
     * 公司规模列表
     */
    private List<String> scale;

    /**
     * 公司融资阶段列表
     */
    private List<String> stage;

    /**
     * 是否开放AI检测
     */
    private Boolean enableAI;

    /**
     * 是否过滤不活跃hr
     */
    private Boolean filterDeadHR;

    /**
     * 是否发送图片简历
     */
    private Boolean sendImgResume;

    /**
     * 目标薪资
     */
    private List<Integer> expectedSalary;

    /**
     * 等待时间
     */
    private String waitTime;

    private List<String> deadStatus;

    /**
     * 投递去重的过期天数
     */
    private Integer deliverExpireDays;

    /**
     * 黑名单项，支持字符串和对象两种方式
     */
    public static class BlackItem {
        public String name;
        public Integer days;
        public BlackItem() {}
        public BlackItem(String name, Integer days) {
            this.name = name;
            this.days = days;
        }
        // getter/setter
    }

    /**
     * 手动配置的公司黑名单，自动与data.json合并
     */
    @JsonDeserialize(using = BlackItemDeserializer.class)
    private List<BlackItem> manualBlackCompanies;

    /**
     * 手动配置的岗位黑名单，自动跳过这些岗位
     */
    @JsonDeserialize(using = BlackItemDeserializer.class)
    private List<BlackItem> manualBlackJobs;

    /**
     * 手动配置的招聘者黑名单，自动跳过这些招聘者
     */
    @JsonDeserialize(using = BlackItemDeserializer.class)
    private List<BlackItem> manualBlackRecruiters;

    /**
     * 手动配置的公司+招聘者黑名单，自动跳过这些公司下该招聘者发布的岗位
     */
    private List<Object> manualBlackCompanyRecruiters;

    /**
     * 是否使用关键词匹配岗位m名称，岗位名称不包含关键字就过滤
     *
     */
    private Boolean keyFilter;

    private Boolean recommendJobs;

    private Boolean h5Jobs;

    /**
     * cookie文件名
     */
    private String cookie;

    /**
     * 是否允许重复投递（同公司同招聘者同岗位）
     */
    private Boolean allowRepeatApply;
    public Boolean getAllowRepeatApply() { return allowRepeatApply; }
    public void setAllowRepeatApply(Boolean allowRepeatApply) { this.allowRepeatApply = allowRepeatApply; }

    private List<String> recommendTabPriority;
    public List<String> getRecommendTabPriority() { return recommendTabPriority; }
    public void setRecommendTabPriority(List<String> recommendTabPriority) { this.recommendTabPriority = recommendTabPriority; }

    public Object getSalary() {
        return salary;
    }
    public void setSalary(Object salary) {
        this.salary = salary;
    }

    @SneakyThrows
    public static BossConfig init() {
        BossConfig config = JobUtils.getConfig(BossConfig.class);

        // 转换工作类型
        config.setJobType(BossEnum.JobType.forValue(config.getJobType()).getCode());
        // 转换薪资范围，兼容数组和字符串
        Object salaryObj = config.getSalary();
        if (salaryObj instanceof List) {
            List<String> salaryList = (List<String>) salaryObj;
            List<String> codeList = salaryList.stream().map(s -> BossEnum.Salary.forValue(s).getCode()).collect(Collectors.toList());
            config.setSalary(codeList);
        } else if (salaryObj instanceof String) {
            config.setSalary(BossEnum.Salary.forValue((String) salaryObj).getCode());
        }
        // 转换城市编码
//        config.setCityCode(config.getCityCode().stream().map(value -> BossEnum.CityCode.forValue(value).getCode()).collect(Collectors.toList()));
        List<String> convertedCityCodes = config.getCityCode().stream()
                .map(city -> {
                    // 优先从自定义映射中获取
                    if (config.getCustomCityCode() != null && config.getCustomCityCode().containsKey(city)) {
                        return config.getCustomCityCode().get(city);
                    }
                    // 否则从枚举中获取
                    return BossEnum.CityCode.forValue(city).getCode();
                })
                .collect(Collectors.toList());
        config.setCityCode(convertedCityCodes);
        // 转换工作经验要求
        config.setExperience(config.getExperience().stream().map(value -> BossEnum.Experience.forValue(value).getCode()).collect(Collectors.toList()));
        // 转换学历要求
        config.setDegree(config.getDegree().stream().map(value -> BossEnum.Degree.forValue(value).getCode()).collect(Collectors.toList()));
        // 转换公司规模
        config.setScale(config.getScale().stream().map(value -> BossEnum.Scale.forValue(value).getCode()).collect(Collectors.toList()));
        // 转换公司融资阶段
        config.setStage(config.getStage().stream().map(value -> BossEnum.Financing.forValue(value).getCode()).collect(Collectors.toList()));
        // 转换行业
        config.setIndustry(config.getIndustry().stream().map(value -> BossEnum.Industry.forValue(value).getCode()).collect(Collectors.toList()));
        return config;
    }

}
