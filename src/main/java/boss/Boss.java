package boss;

import ai.AiConfig;
import ai.AiFilter;
import ai.AiService;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import utils.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static boss.BossElementLocators.*;
import static utils.Bot.sendMessageByTime;
import static utils.Constant.CHROME_DRIVER;
import static utils.JobUtils.formatDuration;

/**
 * @author loks666
 * 项目链接: <a href=
 * "https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 * Boss直聘自动投递
 */
public class Boss {
    private static final Logger log = LoggerFactory.getLogger(Boss.class);
    static String homeUrl = "https://www.zhipin.com";
    static String baseUrl = "https://www.zhipin.com/web/geek/job?";
    static Set<String> blackCompanies;
    static Set<String> blackRecruiters;
    static Set<String> blackJobs;
    static List<Job> resultList = new ArrayList<>();
    static String dataPath = ProjectRootResolver.rootPath + "/src/main/java/boss/data.json";
    static String cookiePath = ProjectRootResolver.rootPath + "/src/main/java/boss/cookie.json";
    static Date startDate;
    public static BossConfig config = BossConfig.init();
    static H5BossConfig h5Config = H5BossConfig.init();
    // 默认推荐岗位集合
    static List<Job> recommendJobs = new ArrayList<>();

    static String blacklistTimePath = ProjectRootResolver.rootPath + "/src/main/resources/blacklist_time.json";
    static Map<String, Map<String, Object>> blacklistTimeData = new HashMap<>();

    static void loadBlacklistTime() {
        File file = new File(blacklistTimePath);
        if (!file.exists()) {
            blacklistTimeData.put("companies", new HashMap<>());
            blacklistTimeData.put("jobs", new HashMap<>());
            blacklistTimeData.put("recruiters", new HashMap<>());
            saveBlacklistTime();
            return;
        }
        try {
            String json = new String(Files.readAllBytes(Paths.get(blacklistTimePath)), StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(json);
            for (String type : Arrays.asList("companies", "jobs", "recruiters")) {
                Map<String, Object> map = new HashMap<>();
                if (obj.has(type)) {
                    JSONObject sub = obj.getJSONObject(type);
                    for (String key : sub.keySet()) {
                        map.put(key, sub.getJSONObject(key).toMap());
                    }
                }
                blacklistTimeData.put(type, map);
            }
        } catch (Exception e) {
            log.warn("加载blacklist_time.json失败: {}", e.getMessage());
        }
    }

    static void saveBlacklistTime() {
        try {
            JSONObject obj = new JSONObject();
            for (String type : Arrays.asList("companies", "jobs", "recruiters")) {
                Map<String, Object> map = blacklistTimeData.getOrDefault(type, new HashMap<>());
                JSONObject sub = new JSONObject();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    sub.put(entry.getKey(), entry.getValue());
                }
                obj.put(type, sub);
            }
            Files.write(Paths.get(blacklistTimePath), obj.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("保存blacklist_time.json失败: {}", e.getMessage());
        }
    }

    // === 新增：黑名单项结构 ===
    public static class BlackItem {
        public String name;
        public Integer days; // 有效天数，null为永久
        public Long addTime; // 加入时间戳，null为永久
        public BlackItem(String name, Integer days, Long addTime) {
            this.name = name;
            this.days = days;
            this.addTime = addTime;
        }
        public BlackItem(String name) {
            this(name, null, null);
        }
        public boolean isExpired() {
            if (days == null || addTime == null) return false;
            long now = System.currentTimeMillis();
            return (now - addTime) > days * 24 * 60 * 60 * 1000L;
        }
        public long remainDays() {
            if (days == null || addTime == null) return Long.MAX_VALUE;
            long now = System.currentTimeMillis();
            long remain = days * 24 * 60 * 60 * 1000L - (now - addTime);
            return remain > 0 ? remain / (24 * 60 * 60 * 1000L) : 0;
        }
    }
    // === 新增：黑名单加载与持久化 ===
    static List<BlackItem> blackCompanyItems = new ArrayList<>();
    static List<BlackItem> blackJobItems = new ArrayList<>();
    static List<BlackItem> blackRecruiterItems = new ArrayList<>();

    // === 新增：公司+招聘者黑名单项结构 ===
    public static class BlackCompanyRecruiterItem {
        public String company;
        public String recruiter;
        public Integer days; // 有效天数，null为永久
        public Long addTime; // 加入时间戳，null为永久
        public BlackCompanyRecruiterItem(String company, String recruiter, Integer days, Long addTime) {
            this.company = company;
            this.recruiter = recruiter;
            this.days = days;
            this.addTime = addTime;
        }
        public BlackCompanyRecruiterItem(String company, String recruiter) {
            this(company, recruiter, null, null);
        }
        public boolean isExpired() {
            if (days == null || addTime == null) return false;
            long now = System.currentTimeMillis();
            return (now - addTime) > days * 24 * 60 * 60 * 1000L;
        }
        public long remainDays() {
            if (days == null || addTime == null) return Long.MAX_VALUE;
            long now = System.currentTimeMillis();
            long remain = days * 24 * 60 * 60 * 1000L - (now - addTime);
            return remain > 0 ? remain / (24 * 60 * 60 * 1000L) : 0;
        }
    }
    static List<BlackCompanyRecruiterItem> blackCompanyRecruiterItems = new ArrayList<>();

    static void loadBlackItems() {
        loadBlacklistTime();
        List<?> companies = config.getManualBlackCompanies();
        List<?> jobs = config.getManualBlackJobs();
        List<?> recruiters = config.getManualBlackRecruiters();
        List<?> companyRecruiters = config.getManualBlackCompanyRecruiters();
        blackCompanyItems = parseBlackListWithTime(companies, "companies");
        blackJobItems = parseBlackListWithTime(jobs, "jobs");
        blackRecruiterItems = parseBlackListWithTime(recruiters, "recruiters");
        blackCompanyRecruiterItems = parseBlackCompanyRecruiterListWithTime(companyRecruiters, "companyRecruiters");
    }

    static List<BlackItem> parseBlackListWithTime(List<?> list, String type) {
        List<BlackItem> result = new ArrayList<>();
        if (list == null) return result;
        Map<String, Object> timeMap = blacklistTimeData.getOrDefault(type, new HashMap<>());
        for (Object o : list) {
            if (o instanceof String) {
                String name = (String) o;
                Long addTime = null;
                Integer days = null;
                if (timeMap.containsKey(name)) {
                    Map t = (Map) timeMap.get(name);
                    addTime = t.get("addTime") == null ? null : Long.parseLong(t.get("addTime").toString());
                    days = t.get("days") == null ? null : Integer.parseInt(t.get("days").toString());
                }
                result.add(new BlackItem(name, days, addTime));
            } else if (o instanceof Map) {
                Map map = (Map) o;
                String name = (String) map.get("name");
                Integer days = map.get("days") == null ? null : Integer.parseInt(map.get("days").toString());
                Long addTime = null;
                if (timeMap.containsKey(name)) {
                    Map t = (Map) timeMap.get(name);
                    addTime = t.get("addTime") == null ? null : Long.parseLong(t.get("addTime").toString());
                }
                result.add(new BlackItem(name, days, addTime));
            }
        }
        return result;
    }

    static List<BlackCompanyRecruiterItem> parseBlackCompanyRecruiterListWithTime(List<?> list, String type) {
        List<BlackCompanyRecruiterItem> result = new ArrayList<>();
        if (list == null) return result;
        Map<String, Object> timeMap = blacklistTimeData.getOrDefault(type, new HashMap<>());
        for (Object o : list) {
            if (o instanceof String) {
                String s = (String) o;
                String[] arr = s.split("\\|");
                if (arr.length == 2) {
                    String company = arr[0];
                    String recruiter = arr[1];
                    Long addTime = null;
                    Integer days = null;
                    String key = company + "|" + recruiter;
                    if (timeMap.containsKey(key)) {
                        Map t = (Map) timeMap.get(key);
                        addTime = t.get("addTime") == null ? null : Long.parseLong(t.get("addTime").toString());
                        days = t.get("days") == null ? null : Integer.parseInt(t.get("days").toString());
                    }
                    result.add(new BlackCompanyRecruiterItem(company, recruiter, days, addTime));
                }
            } else if (o instanceof Map) {
                Map map = (Map) o;
                String company = (String) map.get("company");
                String recruiter = (String) map.get("recruiter");
                Integer days = map.get("days") == null ? null : Integer.parseInt(map.get("days").toString());
                Long addTime = null;
                String key = company + "|" + recruiter;
                if (timeMap.containsKey(key)) {
                    Map t = (Map) timeMap.get(key);
                    addTime = t.get("addTime") == null ? null : Long.parseLong(t.get("addTime").toString());
                }
                result.add(new BlackCompanyRecruiterItem(company, recruiter, days, addTime));
            }
        }
        return result;
    }

    static {
        try {
            // 检查dataPath文件是否存在，不存在则创建
            File dataFile = new File(dataPath);
            if (!dataFile.exists()) {
                // 确保父目录存在
                if (!dataFile.getParentFile().exists()) {
                    dataFile.getParentFile().mkdirs();
                }
                // 创建文件并写入初始JSON结构
                Map<String, Set<String>> initialData = new HashMap<>();
                initialData.put("blackCompanies", new HashSet<>());
                initialData.put("blackRecruiters", new HashSet<>());
                initialData.put("blackJobs", new HashSet<>());
                String initialJson = customJsonFormat(initialData);
                Files.write(Paths.get(dataPath), initialJson.getBytes());
                log.info("创建数据文件: {}", dataPath);
            }

            // 检查cookiePath文件是否存在，不存在则创建
            File cookieFile = new File(cookiePath);
            if (!cookieFile.exists()) {
                // 确保父目录存在
                if (!cookieFile.getParentFile().exists()) {
                    cookieFile.getParentFile().mkdirs();
                }
                // 创建空的cookie文件
                Files.write(Paths.get(cookiePath), "[]".getBytes());
                log.info("创建cookie文件: {}", cookiePath);
            }
        } catch (IOException e) {
            log.error("创建文件时发生异常: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        loadData(dataPath);
        PlaywrightUtil.init();
        startDate = new Date();
        login();
        if (config.getH5Jobs()) {
            h5Config.getCityCode().forEach(Boss::postH5JobByCityByPlaywright);
        }
        if (recommendJobs.isEmpty() && config.getRecommendJobs()) {
            getRecommendJobs();
            // 处理推荐职位
            int recommendResult = processRecommendJobs();
        }
        config.getCityCode().forEach(Boss::postJobByCityByPlaywright);
        log.info(resultList.isEmpty() ? "未发起新的聊天..." : "新发起聊天公司如下:\n{}",
                resultList.stream().map(Object::toString).collect(Collectors.joining("\n")));
        if (!config.getDebugger()) {
            printResult();
        }
    }

    private static void printResult() {
        String message = String.format("\nBoss投递完成，共发起%d个聊天，用时%s", resultList.size(),
                formatDuration(startDate, new Date()));
        log.info(message);
        sendMessageByTime(message);
        saveData(dataPath);
        resultList.clear();
        if (!config.getDebugger()) {
            PlaywrightUtil.close();
        }
    }

    /**
     * 推荐岗位
     */
    private static void getRecommendJobs() {
        Page page = PlaywrightUtil.getPageObject();
        PlaywrightUtil.loadCookies(cookiePath);
        page.navigate("https://www.zhipin.com/web/geek/jobs");

        // 等待页面加载
        page.waitForLoadState();

        try {
            // 等待元素出现，最多等待10秒
            page.waitForSelector("a.expect-item", new Page.WaitForSelectorOptions().setTimeout(10000));

            // 获取a标签且class是expect-item的元素
            ElementHandle activeElement = page.querySelector("a.expect-item");

            if (activeElement != null) {
                log.info("找到'expect-item'元素，准备点击");
                // 点击该元素
                activeElement.click();
                // 点击后等待页面响应
                page.waitForLoadState();
                log.info("已点击'expect-item'元素");


                if (isJobsPresent()) {
                    // 尝试滚动页面加载更多数据
                    try {
                        // 获取岗位列表并下拉加载更多
                        log.info("开始获取推荐岗位信息...");

                        // 记录下拉前后的岗位数量
                        int previousJobCount = 0;
                        int currentJobCount = 0;
                        int unchangedCount = 0;

                        while (unchangedCount < 2) {
                            // 获取所有岗位卡片
                            List<ElementHandle> jobCards = page.querySelectorAll(JOB_LIST_SELECTOR);
                            currentJobCount = jobCards.size();

                            log.info("当前已加载岗位数量:{} ", currentJobCount);

                            // 判断是否有新增岗位
                            if (currentJobCount > previousJobCount) {
                                previousJobCount = currentJobCount;
                                unchangedCount = 0;

                                // 滚动到页面底部加载更多
                                PlaywrightUtil.evaluate("window.scrollTo(0, document.body.scrollHeight)");
                                log.info("下拉页面加载更多...");

                                // 等待新内容加载
                                page.waitForTimeout(2000);
                            } else {
                                unchangedCount++;
                                if (unchangedCount < 2) {
                                    System.out.println("下拉后岗位数量未增加，再次尝试...");
                                    // 再次尝试滚动
                                    page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
                                    page.waitForTimeout(2000);
                                } else {
                                    break;
                                }
                            }
                        }

                        log.info("已获取所有可加载推荐岗位，共计: " + currentJobCount + " 个");


                        // 使用page.locator方法获取所有匹配的元素
                        Locator jobLocators = BossElementFinder.getPlaywrightLocator(page, BossElementLocators.JOB_CARD_BOX);
                        // 获取元素总数
                        int count = jobLocators.count();

                        List<Job> jobs = new ArrayList<>();
                        // 遍历所有找到的job卡片
                        for (int i = 0; i < count; i++) {
                            try {
                                Locator jobCard = jobLocators.nth(i);
                                String jobName = jobCard.locator(BossElementLocators.JOB_NAME).textContent();
                                if (blackJobs.stream().anyMatch(jobName::contains)) {
                                    // 排除黑名单岗位
                                    continue;
                                }
                                String companyName = jobCard.locator(BossElementLocators.COMPANY_NAME).textContent();
                                if (blackCompanies.stream().anyMatch(companyName::contains)) {
                                    // 排除黑名单公司
                                    continue;
                                }


                                Job job = new Job();
                                job.setHref(jobCard.locator(BossElementLocators.JOB_NAME).getAttribute("href"));
                                job.setCompanyName(companyName);
                                job.setJobName(jobName);
                                job.setJobArea(jobCard.locator(BossElementLocators.JOB_AREA).textContent());
                                // 获取标签列表
                                Locator tagElements = jobCard.locator(BossElementLocators.TAG_LIST);
                                int tagCount = tagElements.count();
                                StringBuilder tag = new StringBuilder();
                                for (int j = 0; j < tagCount; j++) {
                                    tag.append(tagElements.nth(j).textContent()).append("·");
                                }
                                if (tag.length() > 0) {
                                    job.setCompanyTag(tag.substring(0, tag.length() - 1));
                                } else {
                                    job.setCompanyTag("");
                                }

                                recommendJobs.add(job);
                            } catch (Exception e) {
                                log.debug("处理岗位卡片失败: {}", e.getMessage());
                            }
                        }

                    } catch (Exception e) {
                        log.error("滚动加载数据异常: {}", e.getMessage());
                    }
                }
            } else {
                log.error("未找到class为'expect-item'的a标签元素");
            }
        } catch (Exception e) {
            log.error("寻找或点击'expect-item'元素时出错: {}", e.getMessage());
        }

    }


    private static void postJobByCityByPlaywright(String cityCode) {
        String searchUrl = getSearchUrl(cityCode);
        for (String keyword : config.getKeywords()) {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = searchUrl + "&query=" + encodedKeyword;
            log.info("查询岗位链接:{}", url);

            Page page = PlaywrightUtil.getPageObject().context().newPage();
            PlaywrightUtil.loadCookies(cookiePath);
            try {
                page.navigate(url, new Page.NavigateOptions().setTimeout(60000));
                // 记录下拉前后的岗位数量
                int previousJobCount = 0;
                int currentJobCount = 0;
                int unchangedCount = 0;

                if (isJobsPresent()) {
                    // 尝试滚动页面加载更多数据
                    try {
                        // 获取岗位列表并下拉加载更多
                        log.info("开始获取岗位信息...");

                        while (unchangedCount < 2) {
                            // 获取所有岗位卡片
                            List<ElementHandle> jobCards = page.querySelectorAll(JOB_LIST_SELECTOR);
                            currentJobCount = jobCards.size();

                            log.info("当前已加载岗位数量:{} ", currentJobCount);

                            // 判断是否有新增岗位
                            if (currentJobCount > previousJobCount) {
                                previousJobCount = currentJobCount;
                                unchangedCount = 0;

                                // 滚动到页面底部加载更多
                                PlaywrightUtil.evaluate("window.scrollTo(0, document.body.scrollHeight)");
                                log.info("下拉页面加载更多...");

                                // 等待新内容加载
                                page.waitForTimeout(2000);
                            } else {
                                unchangedCount++;
                                if (unchangedCount < 2) {
                                    System.out.println("下拉后岗位数量未增加，再次尝试...");
                                    // 再次尝试滚动
                                    page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
                                    page.waitForTimeout(2000);
                                } else {
                                    break;
                                }
                            }
                        }

                        log.info("已获取所有可加载岗位，共计: " + currentJobCount + " 个");

                        log.info("继续滚动加载更多岗位");
                    } catch (Exception e) {
                        log.error("滚动加载数据异常: {}", e.getMessage());
                        break;
                    }
                }

                resumeSubmission(keyword, page);
            } catch (Exception e) {
                log.error("页面跳转超时: {}，url: {}", e.getMessage(), url);
                continue;
            } finally {
                if (page != null) {
                    page.close();
                }
            }
            try {
                Thread.sleep(3000); // 每次投递后休息3秒
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("线程休眠被中断: {}", ie.getMessage());
            }
        }
    }

    private static boolean isJobsPresent() {
        try {
            // 判断页面是否存在岗位的元素
            PlaywrightUtil.waitForElement(JOB_LIST_CONTAINER);
            return true;
        } catch (Exception e) {
            log.error("加载岗位区块失败:{}", e.getMessage());
            return false;
        }
    }

    private static String getSearchUrl(String cityCode) {
        return baseUrl + JobUtils.appendParam("city", cityCode) +
                JobUtils.appendParam("jobType", config.getJobType()) +
                JobUtils.appendParam("salary", config.getSalary()) +
                JobUtils.appendListParam("experience", config.getExperience()) +
                JobUtils.appendListParam("degree", config.getDegree()) +
                JobUtils.appendListParam("scale", config.getScale()) +
                JobUtils.appendListParam("industry", config.getIndustry()) +
                JobUtils.appendListParam("stage", config.getStage());
    }

    private static void saveData(String path) {
        try {
            updateListData();
            Map<String, Set<String>> data = new HashMap<>();
            data.put("blackCompanies", blackCompanies);
            data.put("blackRecruiters", blackRecruiters);
            data.put("blackJobs", blackJobs);
            String json = customJsonFormat(data);
            Files.write(Paths.get(path), json.getBytes());
        } catch (IOException e) {
            log.error("保存【{}】数据失败！", path);
        }
    }

    private static void updateListData() {
        com.microsoft.playwright.Page page = PlaywrightUtil.getPageObject();
        page.navigate("https://www.zhipin.com/web/geek/chat");
        PlaywrightUtil.sleep(3);

        boolean shouldBreak = false;
        while (!shouldBreak) {
            try {
                Locator bottomElement = page.locator(FINISHED_TEXT);
                if (bottomElement.isVisible() && "没有更多了".equals(bottomElement.textContent())) {
                    shouldBreak = true;
                }
            } catch (Exception ignore) {
            }

            Locator items = page.locator(CHAT_LIST_ITEM);
            int itemCount = items.count();

            for (int i = 0; i < itemCount; i++) {
                try {
                    Locator companyElements = page.locator(COMPANY_NAME_IN_CHAT);
                    Locator messageElements = page.locator(LAST_MESSAGE);

                    String companyName = null;
                    String message = null;
                    int retryCount = 0;

                    while (retryCount < 2) {
                        try {
                            if (i < companyElements.count() && i < messageElements.count()) {
                                companyName = companyElements.nth(i).textContent();
                                message = messageElements.nth(i).textContent();
                                break; // 成功获取文本，跳出循环
                            } else {
                                log.info("元素索引超出范围");
                                break;
                            }
                        } catch (Exception e) {
                            retryCount++;
                            if (retryCount >= 2) {
                                log.info("尝试获取元素文本2次失败，放弃本次获取");
                                break;
                            }
                            log.info("页面元素已变更，正在重试第{}次获取元素文本...", retryCount);
                            // 等待短暂时间后重试
                            PlaywrightUtil.sleep(1);
                        }
                    }

                    // 只有在成功获取文本的情况下才继续处理
                    if (companyName != null && message != null) {
                        boolean match = message.contains("不") || message.contains("感谢") || message.contains("但")
                                || message.contains("遗憾") || message.contains("需要本") || message.contains("对不");
                        boolean nomatch = message.contains("不是") || message.contains("不生");
                        if (match && !nomatch) {
                            log.info("黑名单公司：【{}】，信息：【{}】", companyName, message);
                            if (blackCompanies.stream().anyMatch(companyName::contains)) {
                                continue;
                            }
                            companyName = companyName.replaceAll("\\.{3}", "");
                            if (companyName.matches(".*(\\p{IsHan}{2,}|[a-zA-Z]{4,}).*")) {
                                blackCompanies.add(companyName);
                            }
                            // 新增：自动将公司+招聘者组合加入黑名单
                            String recruiterName = null;
                            try {
                                recruiterName = messageElements.nth(i).textContent(); // 这里假设能获取到招聘者名
                            } catch (Exception e) {}
                            if (recruiterName != null && !recruiterName.isEmpty()) {
                                addToBlackCompanyRecruiterBlacklist(companyName, recruiterName, 7); // 默认7天，可根据配置调整
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("寻找黑名单公司异常...", e);
                }
            }

            try {
                // 尝试找到加载更多的元素
                Locator loadMoreElement = page.locator(SCROLL_LOAD_MORE);
                if (loadMoreElement.isVisible()) {
                    // 滚动到加载更多元素
                    loadMoreElement.scrollIntoViewIfNeeded();
                    PlaywrightUtil.sleep(1);
                } else {
                    // 如果找不到特定元素，尝试滚动到页面底部
                    page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
                    PlaywrightUtil.sleep(1);
                }
            } catch (Exception e) {
                log.info("没找到滚动条...");
                break;
            }
        }
        log.info("黑名单公司数量：{}", blackCompanies.size());
    }

    private static String customJsonFormat(Map<String, Set<String>> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (Map.Entry<String, Set<String>> entry : data.entrySet()) {
            sb.append("    \"").append(entry.getKey()).append("\": [\n");
            sb.append(entry.getValue().stream().map(s -> "        \"" + s + "\"").collect(Collectors.joining(",\n")));

            sb.append("\n    ],\n");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append("\n}");
        return sb.toString();
    }

    private static void loadData(String path) {
        try {
            String json = new String(Files.readAllBytes(Paths.get(path)));
            parseJson(json);
        } catch (IOException e) {
            log.error("读取【{}】数据失败！", path);
        }
    }

    private static void parseJson(String json) {
        JSONObject jsonObject = new JSONObject(json);
        blackCompanies = jsonObject.getJSONArray("blackCompanies").toList().stream().map(Object::toString)
                .collect(Collectors.toSet());
        blackRecruiters = jsonObject.getJSONArray("blackRecruiters").toList().stream().map(Object::toString)
                .collect(Collectors.toSet());
        blackJobs = jsonObject.getJSONArray("blackJobs").toList().stream().map(Object::toString)
                .collect(Collectors.toSet());
    }


    @SneakyThrows
    private static Integer h5ResumeSubmission(String keyword, Page page) {
        // 查找所有job卡片元素
        // 获取元素总数
        List<ElementHandle> jobCards = page.querySelectorAll("ul li.item");
        List<Job> jobs = new ArrayList<>();
        for (ElementHandle jobCard : jobCards) {
            // 获取完整HTML
            String outerHtml = jobCard.evaluate("el => el.outerHTML").toString();
            // 获取招聘者信息
            ElementHandle recruiterElement = jobCard.querySelector("div.recruiter div.name");
            String recruiterText = recruiterElement.textContent();

            String salary = jobCard.querySelector("div.title span.salary").textContent();
            String jobHref = jobCard.querySelector("a").getAttribute("href");

            if (isInBlackList(blackRecruiterItems, recruiterText, "招聘者", jobHref)) {
                continue;
            }
            String jobName = jobCard.querySelector("div.title span.title-text").textContent();
            if (isInBlackList(blackJobItems, jobName, "岗位", jobName) || !isTargetJob(keyword, jobName)) {
                continue;
            }
            String companyName = jobCard.querySelector("div.name span.company").textContent();
            if (isInBlackList(blackCompanyItems, companyName, "公司", jobName)) {
                continue;
            }
            if (isSalaryNotExpected(salary)) {
                // 过滤薪资
                log.info("已过滤:【{}】公司【{}】岗位薪资【{}】不符合投递要求", companyName, jobName, salary);
                continue;
            }

            if (config.getKeyFilter()) {
                // 修改为：只要岗位名称包含任意一个关键词就通过
                boolean matchAnyKeyword = false;
                for (String k : config.getKeywords()) {
                    if (jobName.toLowerCase().contains(k.toLowerCase())) {
                        matchAnyKeyword = true;
                        break;
                    }
                }
                if (!matchAnyKeyword) {
                    log.info("已过滤：岗位【{}】名称不包含任意关键字{}", jobName, config.getKeywords());
                    continue;
                }
            }

            Job job = new Job();
            // 获取职位链接
            job.setHref(jobHref);
            // 获取职位名称
            job.setJobName(jobName);
            // 获取工作地点
            job.setJobArea(jobCard.querySelector("div.name span.workplace").textContent());
            // 获取薪资
            job.setSalary(salary);
            // 获取标签
            List<ElementHandle> tagElements = jobCard.querySelectorAll("div.labels span");
            StringBuilder tag = new StringBuilder();
            for (ElementHandle tagElement : tagElements) {
                tag.append(tagElement.textContent()).append("·");
            }
            if (tag.length() > 0) {
                job.setCompanyTag(tag.substring(0, tag.length() - 1));
            } else {
                job.setCompanyTag("");
            }
            // 获取公司名称
            job.setCompanyName(companyName);
            // 设置招聘者信息
            job.setRecruiter(recruiterText);
            jobs.add(job);
        }

        // 处理每个职位详情
        int result = processJobListDetails(jobs, keyword, page);
        if (result < 0) {
            return result;
        }

        return resultList.size();
    }


    @SneakyThrows
    private static Integer resumeSubmission(String keyword, Page page) {
        // 查找所有job卡片元素
        // 使用page.locator方法获取所有匹配的元素
        Locator jobLocators = BossElementFinder.getPlaywrightLocator(page, BossElementLocators.JOB_CARD_BOX);
        // 获取元素总数
        int count = jobLocators.count();

        List<Job> jobs = new ArrayList<>();
        // 遍历所有找到的job卡片
        for (int i = 0; i < count; i++) {
            try {
                Locator jobCard = jobLocators.nth(i);
                String jobName = jobCard.locator(BossElementLocators.JOB_NAME).textContent();
                String companyName = jobCard.locator(BossElementLocators.COMPANY_NAME).textContent();
                String jobArea = jobCard.locator(BossElementLocators.JOB_AREA).textContent();


                Job job = new Job();
                job.setHref(jobCard.locator(BossElementLocators.JOB_NAME).getAttribute("href"));
                job.setCompanyName(companyName);
                job.setJobName(jobName);
                job.setJobArea(jobArea);
                // 获取标签列表
                Locator tagElements = jobCard.locator(BossElementLocators.TAG_LIST);
                int tagCount = tagElements.count();
                StringBuilder tag = new StringBuilder();
                for (int j = 0; j < tagCount; j++) {
                    tag.append(tagElements.nth(j).textContent()).append("·");
                }
                if (tag.length() > 0) {
                    job.setCompanyTag(tag.substring(0, tag.length() - 1));
                } else {
                    job.setCompanyTag("");
                }


                if (isInBlackList(blackJobItems, jobName, "岗位", jobName) || !isTargetJob(keyword, jobName)) {
                    continue;
                }


                if (isInBlackList(blackCompanyItems, companyName, "公司", jobName)) {
                    continue;
                }

                if (config.getKeyFilter()) {
                    // 修改为：只要岗位名称包含任意一个关键词就通过
                    boolean matchAnyKeyword = false;
                    for (String k : config.getKeywords()) {
                        if (jobName.toLowerCase().contains(k.toLowerCase())) {
                            matchAnyKeyword = true;
                            break;
                        }
                    }
                    if (!matchAnyKeyword) {
                        log.info("已过滤：岗位【{}】名称不包含任意关键字{}", jobName, config.getKeywords());
                        continue;
                    }
                }

                jobs.add(job);
            } catch (Exception e) {
                log.debug("处理岗位卡片失败: {}", e.getMessage());
            }
        }

        // 处理每个职位详情
        int result = processJobListDetails(jobs, keyword, page);
        if (result < 0) {
            return result;
        }

        return resultList.size();
    }

    /**
     * 处理职位列表
     *
     * @param jobs    职位列表
     * @param keyword 搜索关键词
     * @return 处理结果，负数表示出错
     */
    @SneakyThrows
    private static int processJobListDetails(List<Job> jobs, String keyword, Page page) {
        List<String> keywords = config.getKeywords(); // 获取配置中的关键词列表

        for (Job job : jobs) {
            // 使用Playwright在新标签页中打开链接
            Page jobPage = page.context().newPage();
            try {
                jobPage.navigate(homeUrl + job.getHref());
                // 等待聊天按钮出现或其他判断页面加载成功的元素
                Locator chatButton = jobPage.locator(BossElementLocators.CHAT_BUTTON);
                 // 增加一个备用等待，例如等待页面主体内容加载
                jobPage.waitForLoadState();

                if (!chatButton.nth(0).isVisible(new Locator.IsVisibleOptions().setTimeout(5000))) {
                    Locator errorElement = jobPage.locator(BossElementLocators.ERROR_CONTENT);
                    if (errorElement.isVisible() && errorElement.textContent().contains("异常访问")) {
                        log.warn("加载岗位详情页【{}】异常访问", job.getJobName());
                        jobPage.close();
                        return -2; // 返回特定错误码表示异常访问
                    } else {
                         log.warn("加载岗位详情页【{}】超时或未找到沟通按钮", job.getJobName());
                         jobPage.close();
                         continue; // 跳过当前岗位，继续下一个
                    }
                }
            } catch (Exception e) {
                if (config.getDebugger()) {
                    e.printStackTrace();
                }
                log.error("加载岗位详情页【{}】失败: {}", job.getJobName(), e.getMessage());
                jobPage.close();
                continue;
            }

            // 过滤不活跃HR
            if (isDeadHR(jobPage)) {
                jobPage.close();
                log.info("已过滤【{}】公司【{}】岗位，该HR不活跃", job.getCompanyName(), job.getJobName());
                PlaywrightUtil.sleep(1);
                continue;
            }

            // 获取职位描述和职责
            String jobDescriptionAndResponsibility = "";
            try {
                Locator jdElement = jobPage.locator(BossElementLocators.JOB_DESCRIPTION);
                 if (jdElement.isVisible()) {
                     jobDescriptionAndResponsibility = jdElement.textContent();
                }
            } catch (Exception e) {
                 log.info("获取职位描述失败:{}", e.getMessage());
            }
            // 更新Job对象的jobKeywordTag，包含描述和职责
            job.setJobKeywordTag(jobDescriptionAndResponsibility);

            // 修改后的关键词匹配逻辑，检查岗位名称、描述或职责是否包含任一关键词
            boolean containsKeyword = false;
            if (keywords != null && !keywords.isEmpty()) {
                String lowerCaseJobName = job.getJobName().toLowerCase();
                String lowerCaseJobDescription = jobDescriptionAndResponsibility.toLowerCase();

                    for (String keywordItem : keywords) {
                    String lowerCaseKeywordItem = keywordItem.toLowerCase();
                    // 只要岗位名称、描述或职责中包含关键词之一，就视为匹配
                    if (lowerCaseJobName.contains(lowerCaseKeywordItem) || 
                        lowerCaseJobDescription.contains(lowerCaseKeywordItem)) {
                            containsKeyword = true;
                            break;
                    }
                }
            }

            // 如果不包含任何关键字，则跳过此职位
            if (!keywords.isEmpty() && !containsKeyword) {
                log.info("已过滤:【{}】公司【{}】岗位，名称或描述不包含任何关键字", job.getCompanyName(), job.getJobName());
                jobPage.close();
                continue;
            }

            // 处理职位详情页 (薪资过滤和黑名单公司过滤等)
            int result = processJobDetail(jobPage, job, keyword);
            if (result < 0) {
                jobPage.close();
                return result;
            }

            // 关闭页面
                        jobPage.close();

            if (config.getDebugger()) {
                break;
            }
        }
        return 0;
    }

    /**
     * 处理单个职位详情页 - 共同处理流程
     *
     * @param jobPage 职位详情页面
     * @param job     职位信息
     * @param keyword 搜索关键词（可能为空）
     * @return 处理结果，负数表示出错或跳过，0表示成功处理
     */
    @SneakyThrows
    private static int processJobDetail(com.microsoft.playwright.Page jobPage, Job job, String keyword) {
        // 获取薪资
        try {
            Locator salaryElement = jobPage.locator(BossElementLocators.JOB_DETAIL_SALARY);
            if (salaryElement.isVisible()) {
                String salaryText = salaryElement.textContent();
                job.setSalary(salaryText);
                if (isSalaryNotExpected(salaryText)) {
                    // 过滤薪资
                    log.info("已过滤:【{}】公司【{}】岗位薪资【{}】不符合投递要求", job.getCompanyName(), job.getJobName(), salaryText);
                    // jobPage.close(); // 在调用 processJobDetail 的地方关闭页面
                    return 0; // 返回0表示已处理（跳过）
                }
            }
        } catch (Exception ignore) {
            log.info("获取岗位薪资失败:{}", ignore.getMessage());
             // 出错时，可以根据需要决定是否跳过。这里选择继续，让其他过滤逻辑判断
        }

        // 获取招聘人员信息
        try {
            Locator recruiterElement = jobPage.locator(BossElementLocators.RECRUITER_INFO);
            String recruiterName = "未知";
            String recruiterTitle = "";
            if (recruiterElement.isVisible()) {
                String recruiterRaw = recruiterElement.textContent().replaceAll("\r|\n", "").trim();
                // 尝试用正则提取真实姓名和岗位
                // 例如"张三 HRBP"或"穗彩科技·HRBP"
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("([\u4e00-\u9fa5]{2,4})[·\s]*([A-Za-z0-9\u4e00-\u9fa5]+)?").matcher(recruiterRaw);
                if (m.find()) {
                    recruiterName = m.group(1);
                    recruiterTitle = m.group(2) != null ? m.group(2) : "";
                } else {
                    recruiterTitle = recruiterRaw;
                }
            }
            String recruiterField = recruiterName + " | " + job.getCompanyName() + " | " + recruiterTitle;
            job.setRecruiter(recruiterField);
            if (isInBlackCompanyRecruiterList(blackCompanyRecruiterItems, job.getCompanyName(), recruiterName, job.getJobName())) {
                log.info("已过滤:【{}】公司【{}】岗位，招聘人员【{}】在公司+招聘者黑名单中", job.getCompanyName(), job.getJobName(), recruiterName);
                return 0;
            }
            if (isInBlackList(blackRecruiterItems, recruiterName, "招聘者", job.getJobName())) {
                log.info("已过滤:【{}】公司【{}】岗位，招聘人员【{}】在黑名单中", job.getCompanyName(), job.getJobName(), recruiterName);
                return 0;
            }
        } catch (Exception ignore) {
            log.info("获取招聘人员信息失败:{}", ignore.getMessage());
        }

        // 检查黑名单公司 (这个检查也可以放在processJobListDetails中更早执行)
         if (isInBlackList(blackCompanyItems, job.getCompanyName(), "公司", job.getJobName())) {
             log.info("已过滤:【{}】公司【{}】岗位，公司在黑名单中", job.getCompanyName(), job.getJobName());
             // jobPage.close(); // 在调用 processJobDetail 的地方关闭页面
             return 0; // 返回0表示已处理（跳过）
         }

         // 检查黑名单岗位名称 (这个检查也可以放在processJobListDetails中更早执行)
         if (isInBlackList(blackJobItems, job.getJobName(), "岗位", job.getJobName())) {
             log.info("已过滤:【{}】公司【{}】岗位名称在黑名单中", job.getCompanyName(), job.getJobName());
             // jobPage.close(); // 在调用 processJobDetail 的地方关闭页面
             return 0; // 返回0表示已处理（跳过）
        }

        // 模拟用户浏览行为
        jobPage.evaluate("window.scrollBy(0, 300)");
        PlaywrightUtil.sleep(1);
        jobPage.evaluate("window.scrollBy(0, 300)");
        PlaywrightUtil.sleep(1);
        jobPage.evaluate("window.scrollTo(0, 0)");
        PlaywrightUtil.sleep(1);

        Locator chatBtn = jobPage.locator(BossElementLocators.CHAT_BUTTON);
        chatBtn = chatBtn.nth(0);
        boolean debug = config.getDebugger();

        // 每次点击沟通前都休眠5秒 减少调用频率
        PlaywrightUtil.sleep(5);

        if (chatBtn.isVisible() && "立即沟通".equals(chatBtn.textContent().replaceAll("\\s+", ""))) {
            String waitTimeConfig = config.getWaitTime(); // 避免变量名冲突
            int sleepTime = 10; // 默认等待10秒

            if (waitTimeConfig != null) {
                try {
                    sleepTime = Integer.parseInt(waitTimeConfig);
                } catch (NumberFormatException e) {
                    log.error("等待时间转换异常！！", e);
                }
            }

            PlaywrightUtil.sleep(sleepTime);

            AiFilter filterResult = null;
            if (config.getEnableAI() && keyword != null) {
                // AI检测岗位是否匹配
                // 注意：这里依赖于 jobDescriptionAndResponsibility 已经被正确设置到 job.jobKeywordTag 中
                filterResult = checkJob(keyword, job.getJobName(), job.getJobKeywordTag());
            }

             // 如果开启了AI过滤且AI认为不匹配，则跳过
             if (config.getEnableAI() && filterResult != null && !filterResult.isMatch()) {
                 log.info("已过滤:【{}】公司【{}】岗位，AI认为不匹配", job.getCompanyName(), job.getJobName());
                 // jobPage.close(); // 在调用 processJobDetail 的地方关闭页面
                 return 0; // 返回0表示已处理（跳过）
            }

            chatBtn.click();

            if (isLimit()) {
                PlaywrightUtil.sleep(1);
                // jobPage.close(); // 在调用 processJobDetail 的地方关闭页面
                return -1; // 返回-1表示达到上限
            }

            // 沟通对话框
            try {
                // 不知道是什么情况下可能出现的弹框，执行关闭处理
                try {
                    Locator dialogTitle = jobPage.locator(BossElementLocators.DIALOG_TITLE);
                    if (dialogTitle.nth(0).isVisible()) {
                        Locator closeBtn = jobPage.locator(BossElementLocators.DIALOG_CLOSE);
                        if (closeBtn.nth(0).isVisible()) {
                            closeBtn.nth(0).click();
                            // 再次尝试点击沟通按钮，因为弹窗可能阻止了第一次点击
                            chatBtn.nth(0).click();
                        }
                    }
                } catch (Exception ignore) {
                    // 忽略关闭弹窗时的异常
                }

                // 对话文本录入框
                Locator input = jobPage.locator(BossElementLocators.CHAT_INPUT);
                input = input.nth(0);

                // 使用 new Locator.IsVisibleOptions().setTimeout(10000) 似乎存在某些情况返回不可见，但是在else种确又可以返回true
                try {
                    input.waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(10000));
                    log.info("✅ input 可见了");
                } catch (PlaywrightException e) {
                    log.info("❌ 10秒内 input 没变可见");
                    log.info("实际状态: " + input.isVisible());
                }

                // 再次检查 input 是否可见，确保可以输入文本
                if (input.isVisible(new Locator.IsVisibleOptions().setTimeout(5000))) { // 再次等待5秒确保可见
                    input.click();
                    // 检查是否是"不匹配"的弹窗，如果是则跳过
                    Locator dialogElement = jobPage.locator(BossElementLocators.DIALOG_CONTAINER);
                    dialogElement = dialogElement.nth(0);
                     if (dialogElement.isVisible() && dialogElement.textContent().contains("不匹配")) {
                         log.info("已过滤:【{}】公司【{}】岗位，出现不匹配弹窗", job.getCompanyName(), job.getJobName());
                         // jobPage.close(); // 在调用 processJobDetail 的地方关闭页面
                         return 0; // 返回0表示已处理（跳过）
                     }

                    // 根据AI过滤结果或默认打招呼语填充输入框
                    String sayHiText = config.getSayHi().replaceAll("\\r|\\n", "");
                     if (config.getEnableAI() && filterResult != null && filterResult.getResult() != null && !filterResult.getResult().contains("false")) {
                         // 假设 AiFilter 的 getResult() 方法返回AI生成的打招呼语或表示匹配的状态
                         // 这里直接使用 AI 返回的非 false 结果作为打招呼语
                         sayHiText = filterResult.getResult();
                     } else if (config.getEnableAI() && filterResult != null && filterResult.getResult() != null && filterResult.getResult().contains("false")) {
                         // 如果AI明确返回false，则使用默认打招呼语（或者根据需要跳过）
                         log.info("AI建议不投递该岗位【{}】-【{}】", job.getCompanyName(), job.getJobName());
                         // 如果AI建议不投递，这里选择跳过
                        return 0;
                    }
                    input.fill(sayHiText);

                    Locator sendBtn = jobPage.locator(BossElementLocators.SEND_BUTTON);
                    sendBtn = sendBtn.nth(0);
                    if (sendBtn.isVisible(new Locator.IsVisibleOptions().setTimeout(5000))) {
                        if (!debug) {
                            // 点击发送打招呼内容
                            sendBtn.click();
                        }
                        PlaywrightUtil.sleep(5);

                        String recruiter = job.getRecruiter();
                        String company = job.getCompanyName();
                        String position = job.getJobName() + " " + job.getSalary() + " " + job.getJobArea();

                        // 发送简历图片
                        Boolean imgResume = false;
                        if (config.getSendImgResume()) {
                            try {
                                // 从类路径加载 resume.jpg
                                URL resourceUrl = Boss.class.getResource("/resume.jpg");
                                if (resourceUrl != null) {
                                    File imageFile = new File(resourceUrl.toURI());
                                    // 使用Playwright上传文件
                                    Locator fileInput = jobPage.locator(BossElementLocators.IMAGE_UPLOAD);
                                    if (fileInput.isVisible()) {
                                        fileInput.setInputFiles(new java.nio.file.Path[]{java.nio.file.Paths.get(imageFile.getPath())});
                                        // 等待发送按钮并点击
                                        Locator imageSendBtn = jobPage.locator(".image-uploader-btn");
                                        if (imageSendBtn.isVisible(new Locator.IsVisibleOptions().setTimeout(2000))) {
                                            // 发送简历图片
                                            if (!debug) {
                                                imageSendBtn.click();
                                                imgResume = true;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.error("获取简历图片路径失败: {}", e.getMessage());
                            }
                        }

                        PlaywrightUtil.sleep(2);
                        log.info("正在投递【{}】公司，【{}】职位，招聘官:【{}】{}", company, position, recruiter,
                                imgResume ? "发送图片简历成功！" : "");
                        // 投递成功，添加到结果列表
                        resultList.add(job);

                    } else {
                        log.info("没有定位到对话框回车按钮");
                    }
                } else {
                    log.info("对话框输入框不可见，无法输入文本");
                }
            } catch (Exception e) {
                log.error("处理沟通对话框时出错: {}", e.getMessage());
            }
        } else {
            log.info("已过滤:【{}】公司【{}】岗位，无法立即沟通或已沟通", job.getCompanyName(), job.getJobName());
        }

        // === 新增：写入岗位详情到文档 ===
        try {
            String dirPath = ProjectRootResolver.rootPath + "/src/main/resources/job_details";
            java.nio.file.Path dir = java.nio.file.Paths.get(dirPath);
            if (!java.nio.file.Files.exists(dir)) {
                java.nio.file.Files.createDirectories(dir);
            }
            String filePath = dirPath + "/job_details.md";
            StringBuilder sb = new StringBuilder();
            sb.append("## 岗位名称：").append(job.getJobName()).append("\n");
            sb.append("- 公司名称：").append(job.getCompanyName()).append("\n");
            sb.append("- 工作地点：").append(job.getJobArea()).append("\n");
            sb.append("- 详细地址：").append(job.getDetailAddress() == null ? "" : job.getDetailAddress()).append("\n");
            sb.append("- 薪资：").append(job.getSalary() == null ? "" : job.getSalary()).append("\n");
            sb.append("- 招聘者：").append(job.getRecruiter() == null ? "" : job.getRecruiter()).append("\n");
            sb.append("- 职位描述/职责/要求：\n");
            // 分行处理岗位职责/要求
            String rawDesc = job.getJobKeywordTag() == null ? "" : job.getJobKeywordTag();
            String[] lines = rawDesc.split("[；;。\n]");
            int idx = 1;
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    // 保留原有序号，否则自动加序号
                    if (!line.matches("^\\d+[、.].*")) {
                        sb.append(idx).append("、");
                        idx++;
                    }
                    sb.append(line).append("\n");
                }
            }
            sb.append("- 抓取时间：").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).append("\n");
            sb.append("---\n\n");
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.warn("写入岗位详情文档失败: {}", e.getMessage());
        }
        // === 新增结束 ===

        return 0;
    }

    /**
     * 处理推荐职位列表
     *
     * @return 处理结果，负数表示出错
     */
    @SneakyThrows
    private static int processRecommendJobs() {
        List<String> keywords = config.getKeywords();

        for (Job job : recommendJobs) {
            // 使用Playwright在新标签页中打开链接
            Page jobPage = PlaywrightUtil.getPageObject().context().newPage();
            try {
                jobPage.navigate(homeUrl + job.getHref());
                // 等待聊天按钮出现
                Locator chatButton = jobPage.locator(BossElementLocators.CHAT_BUTTON);
                // 增加一个备用等待，例如等待页面主体内容加载
                 jobPage.waitForLoadState();

                if (!chatButton.nth(0).isVisible(new Locator.IsVisibleOptions().setTimeout(5000))) {
                    Locator errorElement = jobPage.locator(BossElementLocators.ERROR_CONTENT);
                    if (errorElement.isVisible() && errorElement.textContent().contains("异常访问")) {
                         log.warn("加载推荐岗位详情页【{}】异常访问", job.getJobName());
                        jobPage.close();
                        return -2; // 返回特定错误码表示异常访问
                    }
                    else {
                         log.warn("加载推荐岗位详情页【{}】超时或未找到沟通按钮", job.getJobName());
                         jobPage.close();
                         continue; // 跳过当前岗位，继续下一个
                    }
                }
        } catch (Exception e) {
                if (config.getDebugger()) {
                    e.printStackTrace();
                }
                log.error("加载推荐岗位详情页【{}】失败: {}", job.getJobName(), e.getMessage());
                jobPage.close();
                continue;
            }

            // 过滤不活跃HR
            if (isDeadHR(jobPage)) {
                jobPage.close();
                log.info("已过滤【{}】公司【{}】推荐岗位，该HR不活跃", job.getCompanyName(), job.getJobName());
                PlaywrightUtil.sleep(1);
                continue;
            }

            // 尝试获取完整的职位描述和职责
            String jobDescriptionAndResponsibility = "";
            try {
                Locator jdElement = jobPage.locator(BossElementLocators.JOB_DESCRIPTION);
                 if (jdElement.isVisible()) {
                     jobDescriptionAndResponsibility = jdElement.textContent();
                 }
        } catch (Exception e) {
                 log.info("获取推荐职位描述失败:{}", e.getMessage());
    }

            // 如果获取到了完整描述，就使用它；否则尝试获取标签
            if(isValidString(jobDescriptionAndResponsibility)){
                job.setJobKeywordTag(jobDescriptionAndResponsibility);
            } else {
                // 获取职位描述标签
                String jobKeywordTag = "";
                try {
                    Locator tagElements = jobPage.locator(JOB_KEYWORD_LIST);
                    int tagCount = tagElements.count();
                    StringBuilder tag = new StringBuilder();
                    for (int j = 0; j < tagCount; j++) {
                        tag.append(tagElements.nth(j).textContent()).append("·");
        }
                    if(tag.length() > 0){
                        jobKeywordTag = tag.substring(0, tag.length() - 1);
                    }
                } catch (Exception e) {
                    log.info("获取推荐职位描述标签失败:{}", e.getMessage());
    }

                if (isValidString(jobKeywordTag)){
                    job.setJobKeywordTag(jobKeywordTag);
                } else {
                    job.setJobKeywordTag("");
                }
            }

            // 修改后的关键词匹配逻辑，检查岗位名称、描述或职责是否包含任一关键词
            boolean containsKeyword = false;
            if (keywords != null && !keywords.isEmpty()) {
                String lowerCaseJobName = job.getJobName().toLowerCase();
                String lowerCaseJobDescription = job.getJobKeywordTag().toLowerCase();

                for (String keywordItem : keywords) {
                    String lowerCaseKeywordItem = keywordItem.toLowerCase();
                    // 只要岗位名称、描述或职责中包含关键词之一，就视为匹配
                    if (lowerCaseJobName.contains(lowerCaseKeywordItem) || 
                        lowerCaseJobDescription.contains(lowerCaseKeywordItem)) {
                        containsKeyword = true;
                        break;
    }
                }
            }

            // 如果不包含任何关键字，则跳过此职位
            if (!keywords.isEmpty() && !containsKeyword) {
                log.info("已过滤:【{}】公司【{}】推荐岗位，名称或描述不包含任何关键字", job.getCompanyName(), job.getJobName());
                jobPage.close();
                continue;
            }

            // 处理职位详情页 (薪资过滤和黑名单公司过滤等)
            int result = processJobDetail(jobPage, job, null);
            if (result < 0) {
                jobPage.close();
                return result;
        }
            
            // 关闭页面
            jobPage.close();

            if (config.getDebugger()) {
                break;
            }
        }
        return 0;
    }

    // 保留 isValidString 方法
    public static boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty();
    }

    // 保留 isDeadHR 方法
    private static boolean isDeadHR(Page page) {
        if (!config.getFilterDeadHR()) {
            return false;
        }
        try {
            // 尝试获取 HR 的活跃时间
            Locator activeTimeElement = page.locator(HR_ACTIVE_TIME);
            activeTimeElement = activeTimeElement.nth(0);
            String outerHtml = activeTimeElement.first().evaluate("el => el.outerHTML").toString();


            if (activeTimeElement.isVisible(new Locator.IsVisibleOptions().setTimeout(5000))) {
                String activeTimeText = activeTimeElement.textContent();
                log.info("{}：{}", getCompanyAndHR(page).replaceAll("\\s+", ""), activeTimeText);
                // 如果 HR 活跃状态符合预期，则返回 true
                return containsDeadStatus(activeTimeText, config.getDeadStatus());
            }
        } catch (Exception e) {
            log.info("没有找到【{}】的活跃状态, 默认此岗位将会投递...", getCompanyAndHR(page).replaceAll("\\s+", ""));
        }
        return false;
    }

    public static boolean containsDeadStatus(String activeTimeText, List<String> deadStatus) {
        for (String status : deadStatus) {
            if (activeTimeText.contains(status)) {
                return true;// 一旦找到包含的值，立即返回 true
            }
        }
        return false;// 如果没有找到，返回 false
    }

    private static String getCompanyAndHR(Page page) {
        try {
            Locator element = page.locator(RECRUITER_INFO);
            element = element.nth(0);
            if (element.isVisible(new Locator.IsVisibleOptions().setTimeout(2000))) {
                return element.textContent().replaceAll("\n", "");
            }
        } catch (Exception e) {
            log.debug("获取公司和HR信息失败: {}", e.getMessage());
        }
        return "未知公司和HR";
    }

    private static AiFilter checkJob(String keyword, String jobName, String jd) {
        AiConfig aiConfig = AiConfig.init();
        String requestMessage = String.format(aiConfig.getPrompt(), aiConfig.getIntroduce(), keyword, jobName, jd,
                config.getSayHi());
        String result = AiService.sendRequest(requestMessage);
        
        // 检查AI服务返回是否包含错误信息
        if (result.startsWith("AI服务未配置") || result.startsWith("AI请求失败") || result.startsWith("AI请求异常") || result.startsWith("AI请求超时")) {
            log.warn("AI服务调用失败: {}", result);
            // 如果AI服务不可用，返回一个默认的匹配结果，确保投递流程能继续
            return new AiFilter(true);
        }
        
        // 正常处理AI返回结果
        return result.contains("false") ? new AiFilter(false) : new AiFilter(true, result);
    }

    private static boolean isTargetJob(String keyword, String jobName) {
        boolean keywordIsAI = false;
        for (String target : new String[]{"大模型", "AI"}) {
            if (keyword.contains(target)) {
                keywordIsAI = true;
                break;
            }
        }

        boolean jobIsDesign = false;
        for (String designOrVision : new String[]{"设计", "视觉", "产品", "运营"}) {
            if (jobName.contains(designOrVision)) {
                jobIsDesign = true;
                break;
            }
        }

        boolean jobIsAI = false;
        for (String target : new String[]{"AI", "人工智能", "大模型", "生成"}) {
            if (jobName.contains(target)) {
                jobIsAI = true;
                break;
            }
        }

        if (keywordIsAI) {
            if (jobIsDesign) {
                return false;
            } else if (!jobIsAI) {
                return true;
            }
        }
        return true;
    }

    private static Integer[] parseSalaryRange(String salaryText) {
        try {
            return Arrays.stream(salaryText.split("-")).map(s -> s.replaceAll("[^0-9]", "")) // 去除非数字字符
                    .map(Integer::parseInt) // 转换为Integer
                    .toArray(Integer[]::new); // 转换为Integer数组
        } catch (Exception e) {
            log.error("薪资解析异常！{}", e.getMessage(), e);
        }
        return null;
    }

    private static boolean isLimit() {
        try {
            PlaywrightUtil.sleep(1);
            com.microsoft.playwright.Page page = PlaywrightUtil.getPageObject();
            Locator dialogElement = page.locator(DIALOG_CON);
            if (dialogElement.isVisible(new Locator.IsVisibleOptions().setTimeout(2000))) {
                String text = dialogElement.textContent();
                return text.contains("已达上限");
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @SneakyThrows
    private static void login() {
        log.info("打开Boss直聘网站中...");

        // 使用Playwright打开网站
        Page page = PlaywrightUtil.getPageObject();
        page.navigate(homeUrl);
        Page h5Page = PlaywrightUtil.getPageObject(PlaywrightUtil.DeviceType.MOBILE);
        if (!ObjectUtils.isEmpty(h5Page)) {
            h5Page.navigate(homeUrl);
        }


        // 检查并加载Cookie
        if (isCookieValid(cookiePath)) {
            PlaywrightUtil.loadCookies(cookiePath);
            page.reload();
            if (!ObjectUtils.isEmpty(h5Page)) {
                h5Page.reload();
            }
            PlaywrightUtil.sleep(2);
        }

        // 检查是否需要登录
        if (isLoginRequired()) {
            log.error("cookie失效，尝试扫码登录...");
            scanLogin();
        }
    }

    // 检查cookie是否有效的方法，替换SeleniumUtil的实现
    private static boolean isCookieValid(String cookiePath) {
        try {
            String cookieContent = new String(Files.readAllBytes(Paths.get(cookiePath)));
            return cookieContent != null && !cookieContent.equals("[]") && cookieContent.contains("name");
        } catch (Exception e) {
            log.error("读取cookie文件失败: {}", e.getMessage());
            return false;
        }
    }

    private static boolean isLoginRequired() {
        try {
            Page page = PlaywrightUtil.getPageObject();

            // 检查是否有登录按钮
            Locator loginButton = page.locator(BossElementLocators.LOGIN_BTNS);
            if (loginButton.isVisible() && loginButton.textContent().contains("登录")) {
                return true;
            }

            // 检查是否有错误页面
            try {
                Locator pageHeader = page.locator(BossElementLocators.PAGE_HEADER);
                if (pageHeader.isVisible()) {
                    Locator errorPageLogin = page.locator(BossElementLocators.ERROR_PAGE_LOGIN);
                    if (errorPageLogin.isVisible()) {
                        errorPageLogin.click();
                        return true;
                    }
                }
            } catch (Exception ex) {
                log.info("没有出现403访问异常");
            }

            log.info("cookie有效，已登录...");
            return false;
        } catch (Exception e) {
            log.error("检查登录状态出错: {}", e.getMessage());
            return true; // 遇到错误，默认需要登录
        }
    }

    @SneakyThrows
    private static void scanLogin() {
        // 使用Playwright进行登录操作
        Page page = PlaywrightUtil.getPageObject();
        // 访问登录页面
        page.navigate(homeUrl + "/web/user/?ka=header-login");
        PlaywrightUtil.sleep(3);

        // 1. 如果已经登录，则直接返回
        try {
            Locator loginBtn = page.locator(BossElementLocators.LOGIN_BTN);
            if (loginBtn.isVisible() && !loginBtn.textContent().equals("登录")) {
                log.info("已经登录，直接开始投递...");
                return;
            }
        } catch (Exception ignored) {
        }

        log.info("等待登录...");

        // 2. 定位二维码登录的切换按钮
        Locator scanButton = page.locator(BossElementLocators.LOGIN_SCAN_SWITCH);
        boolean scanButtonVisible = scanButton.isVisible(new Locator.IsVisibleOptions().setTimeout(30000));
        if (!scanButtonVisible) {
            log.error("未找到二维码登录按钮，登录失败");
            return;
        }

        // 3. 登录逻辑
        boolean login = false;

        // 4. 记录开始时间，用于判断10分钟超时
        long startTime = System.currentTimeMillis();
        final long TIMEOUT = 10 * 60 * 1000; // 10分钟

        // 5. 用于监听用户是否在控制台回车
        Scanner scanner = new Scanner(System.in);

        while (!login) {
            // 如果已经超过10分钟，退出程序
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= TIMEOUT) {
                log.error("超过10分钟未完成登录，程序退出...");
                System.exit(1);
            }

            try {
                // 尝试点击二维码按钮并等待页面出现已登录的元素
                scanButton.click();
                // 等待登录成功标志
                boolean loginSuccess = page.locator(BossElementLocators.LOGIN_SUCCESS_HEADER)
                        .isVisible(new Locator.IsVisibleOptions().setTimeout(2000));

                // 如果找到登录成功元素，说明登录成功
                if (loginSuccess) {
                    login = true;
                    log.info("登录成功！保存cookie...");
                } else {
                    // 登录失败
                    log.error("登录失败，等待用户操作或者 2 秒后重试...");

                    // 每次登录失败后，等待2秒，同时检查用户是否按了回车
                    boolean userInput = waitForUserInputOrTimeout(scanner);
                    if (userInput) {
                        log.info("检测到用户输入，继续尝试登录...");
                    }
                }
            } catch (Exception e) {
                // scanButton.click() 可能已经登录成功，没有这个扫码登录按钮
                boolean loginSuccess = page.locator(BossElementLocators.LOGIN_SUCCESS_HEADER)
                        .isVisible(new Locator.IsVisibleOptions().setTimeout(2000));
                if (loginSuccess) {
                    login = true;
                    log.info("登录成功！保存cookie...");
                }
            }
        }

        // 登录成功后，保存Cookie
        PlaywrightUtil.saveCookies(cookiePath);
    }

    /**
     * 在指定的毫秒数内等待用户输入回车；若在等待时间内用户按回车则返回 true，否则返回 false。
     *
     * @param scanner 用于读取控制台输入
     * @return 用户是否在指定时间内按回车
     */
    private static boolean waitForUserInputOrTimeout(Scanner scanner) {
        long end = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < end) {
            try {
                // 判断输入流中是否有可用字节
                if (System.in.available() > 0) {
                    // 读取一行（用户输入）
                    scanner.nextLine();
                    return true;
                }
            } catch (IOException e) {
                // 读取输入流异常，直接忽略
            }

            // 小睡一下，避免 CPU 空转
            SeleniumUtil.sleep(1);
        }
        return false;
    }


    private static void postH5JobByCityByPlaywright(String cityCode) {

        Page page = PlaywrightUtil.getPageObject(PlaywrightUtil.DeviceType.MOBILE);

        for (String keyword : h5Config.getKeywords()) {
            String searchUrl = getH5SearchUrl(cityCode, keyword);
            log.info("查询url:{}", searchUrl);

            try {
                log.info("开始投递，页面url：{}", searchUrl);
                // 使用PlaywrightUtil获取移动设备页面并导航
                page.navigate(searchUrl);

                // 点击立即沟通，建立chat窗口
                if (isH5JobsPresent(page)) {
                    int previousCount = 0;
                    int retry = 0;
                    // 向下滚动到底部
                    while (true) {
                        // 当前页面中 class="item" 的 li 元素数量
                        int currentCount = (int) page.evaluate("document.querySelectorAll('li.item').length");

                        // 滚动到底部
                        // 滚动到比页面高度更大的值，确保触发加载
                        page.evaluate("window.scrollTo(0, document.documentElement.scrollHeight + 100)");
                        page.waitForTimeout(10000); // 等待数据加载

                        // 检查数量是否变化
                        if (currentCount == previousCount) {
                            retry++;
                            log.info("第{}次下拉重试", retry);
                            if (retry >= 2) {
                                log.info("尝试2次下拉后无新增岗位，退出");
                                break; // 连续两次未加载新数据，认为加载完毕
                            }
                        } else {
                            retry = 0; // 重置尝试次数
                        }

                        previousCount = currentCount;

                        if (config.getDebugger()) {
                            break;
                        }
                    }
                    log.info("已加载全部岗位，总数量: " + previousCount);
                }

                // chat页面进行消息沟通
                h5ResumeSubmission(keyword, page);
            } catch (Exception e) {
                log.error("使用Playwright处理页面时出错: {}", e.getMessage(), e);
            }

        }

    }

    private static String getH5SearchUrl(String cityCode, String keyword) {
        // 经验
        List<String> experience = h5Config.getExperience();
        // 学历
        List<String> degree = h5Config.getDegree();
        // 薪资
        String salary = h5Config.getSalary();
        // 规模
        List<String> scale = h5Config.getScale();

        String searchUrl = baseUrl;

        log.info("cityCode:{}", cityCode);
        log.info("experience:{}", experience);
        log.info("degree:{}", degree);
        log.info("salary:{}", salary);
        if (!H5BossEnum.CityCode.NULL.equals(cityCode)) {
            searchUrl = searchUrl + "/" + cityCode + "/";
        }

        Set<String> ydeSet = new LinkedHashSet<>();
        if (!experience.isEmpty()) {
            if (!H5BossEnum.Salary.NULL.equals(salary)) {
                ydeSet.add(salary);
            }
        }

        if (!degree.isEmpty()) {
            String degreeStr = degree.stream().findFirst().get();
            if (!H5BossEnum.Degree.NULL.equals(degreeStr)) {
                ydeSet.add(degreeStr);
            }
        }
        if (!experience.isEmpty()) {
            String experienceStr = experience.stream().findFirst().get();
            if (!H5BossEnum.Experience.NULL.equals(experienceStr)) {
                ydeSet.add(experienceStr);
            }
        }

        if (!scale.isEmpty()) {
            String scaleStr = scale.stream().findFirst().get();
            if (!H5BossEnum.Scale.NULL.equals(scaleStr)) {
                ydeSet.add(scaleStr);
            }
        }


        String yde = ydeSet.stream().collect(Collectors.joining("-"));
        log.info("yde:{}", yde);
        if (StringUtils.hasLength(yde)) {
            if (!searchUrl.endsWith("/")) {
                searchUrl = searchUrl + "/" + yde + "/";
            } else {
                searchUrl = searchUrl + yde + "/";
            }
        }

        searchUrl = searchUrl + "?query=" + keyword;
        searchUrl = searchUrl + "&ka=sel-salary-" + salary.split("_")[1];
        return searchUrl;
    }


    private static boolean isH5JobsPresent(Page page) {
        try {
            page.waitForSelector("li.item", new Page.WaitForSelectorOptions().setTimeout(40000));
            return true;
        } catch (Exception e) {
            log.warn("页面上没有找到职位列表: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 判断薪资是否不符合期望
     * @param salaryText 薪资文本
     * @return 是否不符合期望
     */
    private static boolean isSalaryNotExpected(String salaryText) {
        try {
            // 解析薪资范围
            Integer[] range = parseSalaryRange(salaryText);
            if (range == null || range.length == 0) {
                return false;
            }
            
            // 获取期望薪资
            List<Integer> expectedSalary = config.getExpectedSalary();
            if (expectedSalary == null || expectedSalary.isEmpty()) {
                return false;
            }
            
            // 如果只有一个值，认为是最低期望薪资
            if (expectedSalary.size() == 1) {
                int minExpect = expectedSalary.get(0);
                // 如果实际薪资上限低于期望下限，不符合要求
                return range[range.length - 1] < minExpect;
            }
            
            // 如果有两个值，认为是期望薪资范围
            if (expectedSalary.size() >= 2) {
                int minExpect = expectedSalary.get(0);
                int maxExpect = expectedSalary.get(1);
                // 检查薪资范围是否有交集
                if (range.length >= 2) {
                    int minActual = range[0];
                    int maxActual = range[range.length - 1];
                    // 没有交集则不符合要求
                    return maxActual < minExpect || minActual > maxExpect;
                } else if (range.length == 1) {
                    // 单值薪资与期望范围比较
                    return range[0] < minExpect || range[0] > maxExpect;
                }
            }
            
            return false;
        } catch (Exception e) {
            log.error("薪资检查异常: {}", e.getMessage());
            return false;
        }
    }

    // === 新增：黑名单过滤逻辑 ===
    static boolean isInBlackList(List<BlackItem> list, String name, String type, String jobName) {
        String typeKey = type.equals("公司") ? "companies" : type.equals("岗位") ? "jobs" : "recruiters";
        for (BlackItem item : list) {
            if (name != null && name.contains(item.name)) {
                // 首次命中记录 addTime 并持久化
                if (item.days != null && item.addTime == null) {
                    item.addTime = System.currentTimeMillis();
                    Map<String, Object> map = blacklistTimeData.getOrDefault(typeKey, new HashMap<>());
                    Map<String, Object> v = new HashMap<>();
                    v.put("addTime", item.addTime);
                    v.put("days", item.days);
                    map.put(item.name, v);
                    blacklistTimeData.put(typeKey, map);
                    saveBlacklistTime();
                }
                if (item.isExpired()) continue;
                long remain = item.remainDays();
                log.info("已过滤：{}黑名单命中【{}】，剩余有效天数：{}，岗位【{}】", type, item.name, remain == Long.MAX_VALUE ? "永久" : remain + "天", jobName);
                return true;
            }
        }
        return false;
    }

    static boolean isInBlackCompanyRecruiterList(List<BlackCompanyRecruiterItem> list, String company, String recruiter, String jobName) {
        String typeKey = "companyRecruiters";
        for (BlackCompanyRecruiterItem item : list) {
            if (company != null && recruiter != null && company.equals(item.company) && recruiter.equals(item.recruiter)) {
                // 首次命中记录 addTime 并持久化
                if (item.days != null && item.addTime == null) {
                    item.addTime = System.currentTimeMillis();
                    Map<String, Object> map = blacklistTimeData.getOrDefault(typeKey, new HashMap<>());
                    Map<String, Object> v = new HashMap<>();
                    v.put("addTime", item.addTime);
                    v.put("days", item.days);
                    map.put(item.company + "|" + item.recruiter, v);
                    blacklistTimeData.put(typeKey, map);
                    saveBlacklistTime();
                }
                if (item.isExpired()) continue;
                long remain = item.remainDays();
                log.info("已过滤：公司+招聘者黑名单命中【{}|{}】，剩余有效天数：{}，岗位【{}】", item.company, item.recruiter, remain == Long.MAX_VALUE ? "永久" : remain + "天", jobName);
                return true;
            }
        }
        return false;
    }

    // 新增方法
    static void addToBlackCompanyRecruiterBlacklist(String company, String recruiter, int days) {
        // 先查重
        for (BlackCompanyRecruiterItem item : blackCompanyRecruiterItems) {
            if (company.equals(item.company) && recruiter.equals(item.recruiter)) {
                return;
            }
        }
        BlackCompanyRecruiterItem newItem = new BlackCompanyRecruiterItem(company, recruiter, days, System.currentTimeMillis());
        blackCompanyRecruiterItems.add(newItem);
        // 持久化到blacklist_time.json
        Map<String, Object> map = blacklistTimeData.getOrDefault("companyRecruiters", new HashMap<>());
        Map<String, Object> v = new HashMap<>();
        v.put("addTime", newItem.addTime);
        v.put("days", newItem.days);
        map.put(company + "|" + recruiter, v);
        blacklistTimeData.put("companyRecruiters", map);
        saveBlacklistTime();
        log.info("自动加入公司+招聘者黑名单：【{}|{}】，有效期{}天", company, recruiter, days);
    }

}
