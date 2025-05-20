package lagou;

import lombok.Data;
import lombok.SneakyThrows;
import utils.JobUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author loks666
 * 项目链接: <a href="https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 */
@Data
public class LagouConfig {
    /**
     * 搜索关键词列表
     */
    private List<String> keywords;

    /**
     * 城市编码
     */
    private String cityCode;

    /**
     * 薪资范围
     */
    private String salary;

    /**
     * 公司规模
     */
    private List<String> scale;

    /**
     * 工作年限
     */
    private String gj;

    /**
     * 手动配置的岗位黑名单，自动跳过这些岗位
     */
    private List<String> manualBlackJobs;

    /**
     * 手动配置的公司黑名单，自动跳过这些公司
     */
    private List<String> manualBlackCompanies;

    /**
     * 手动配置的招聘者黑名单，自动跳过这些招聘者
     */
    private List<String> manualBlackRecruiters;

    @SneakyThrows
    public static LagouConfig init() {
        LagouConfig config = JobUtils.getConfig(LagouConfig.class);
        // 转换城市编码
        config.setSalary(Objects.equals("不限", config.getSalary()) ? "0" : config.getSalary());
        List<String> scales = config.getScale();
        config.setScale(scales.stream().map(scale -> "不限".equals(scale) ? "0" : scale).collect(Collectors.toList()));
        return config;
    }

}
