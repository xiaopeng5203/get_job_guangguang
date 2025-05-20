package job51;

import lombok.SneakyThrows;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JobUtils;
import utils.SeleniumUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;

import static utils.Bot.sendMessageByTime;
import static utils.Constant.*;
import static utils.JobUtils.formatDuration;

import java.io.File;
import java.util.Arrays;

/**
 * @author loks666
 * 项目链接: <a href="https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 * 前程无忧自动投递简历
 */
public class Job51 {
    private static final Logger log = LoggerFactory.getLogger(Job51.class);

    static Integer page = 1;
    static Integer maxPage = 50;
    static String cookiePath = "./src/main/java/job51/cookie.json";
    static String homeUrl = "https://www.51job.com";
    static String loginUrl = "https://login.51job.com/login.php?lang=c&url=https://www.51job.com/&qrlogin=2";
    static String baseUrl = "https://we.51job.com/pc/search?";
    static List<String> resultList = new ArrayList<>();
    static Job51Config config = Job51Config.init();
    static Date startDate;
    static String blacklistTimePath = "src/main/resources/blacklist_time.json";
    static Map<String, Map<String, Object>> blacklistTimeData = new HashMap<>();
    static List<BlackItem> blackCompanyItems = new ArrayList<>();
    static List<BlackItem> blackJobItems = new ArrayList<>();
    static List<BlackItem> blackRecruiterItems = new ArrayList<>();

    public static class BlackItem {
        public String name;
        public Integer days;
        public Long addTime;
        public BlackItem(String name, Integer days, Long addTime) {
            this.name = name;
            this.days = days;
            this.addTime = addTime;
        }
        public BlackItem(String name) { this(name, null, null); }
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

    static void loadBlackItems() {
        loadBlacklistTime();
        List<?> companies = config.getManualBlackCompanies();
        List<?> jobs = config.getManualBlackJobs();
        List<?> recruiters = config.getManualBlackRecruiters();
        blackCompanyItems = parseBlackListWithTime(companies, "companies");
        blackJobItems = parseBlackListWithTime(jobs, "jobs");
        blackRecruiterItems = parseBlackListWithTime(recruiters, "recruiters");
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

    static boolean isInBlackList(List<BlackItem> list, String name, String type, String jobName) {
        String typeKey = type.equals("公司") ? "companies" : type.equals("岗位") ? "jobs" : "recruiters";
        for (BlackItem item : list) {
            if (name != null && name.contains(item.name)) {
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

    public static void main(String[] args) {
        String searchUrl = getSearchUrl();
        SeleniumUtil.initDriver();
        startDate = new Date();
        Login();
        config.getKeywords().forEach(keyword -> resume(searchUrl + "&keyword=" + keyword));
        printResult();
    }

    private static void printResult() {
        String message = String.format("\n51job投递完成，共投递%d个简历，用时%s", resultList.size(), formatDuration(startDate, new Date()));
        log.info(message);
        sendMessageByTime(message);
        resultList.clear();
        CHROME_DRIVER.close();
        CHROME_DRIVER.quit();
    }

    private static String getSearchUrl() {
        return baseUrl +
                JobUtils.appendListParam("jobArea", config.getJobArea()) +
                JobUtils.appendListParam("salary", config.getSalary());
    }

    private static void Login() {
        CHROME_DRIVER.get(homeUrl);
        if (SeleniumUtil.isCookieValid(cookiePath)) {
            SeleniumUtil.loadCookie(cookiePath);
            CHROME_DRIVER.navigate().refresh();
            SeleniumUtil.sleep(1);
        }
        if (isLoginRequired()) {
            log.error("cookie失效，尝试扫码登录...");
            scanLogin();
        }
    }

    private static boolean isLoginRequired() {
        try {
            String text = CHROME_DRIVER.findElement(By.xpath("//p[@class=\"tit\"]")).getText();
            return text != null && text.contains("登录");
        } catch (Exception e) {
            log.info("cookie有效，已登录...");
            return false;
        }
    }

    @SneakyThrows
    private static void resume(String url) {
        CHROME_DRIVER.get(url);
        SeleniumUtil.sleep(1);

        // 再次判断是否登录
        WebElement login = WAIT.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[contains(@class, 'uname')]")));
        if (login != null && isNotNullOrEmpty(login.getText()) && login.getText().contains("登录")) {
            login.click();
            WAIT.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//i[contains(@class, 'passIcon')]"))).click();
            log.info("请扫码登录...");
            WAIT.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(@class, 'joblist')]")));
            SeleniumUtil.saveCookie(cookiePath);
        }

        //由于51更新，每投递一页之前，停止10秒
        SeleniumUtil.sleep(10);

        int i = 0;
        try {
            CHROME_DRIVER.findElements(By.className("ss")).get(i).click();
        } catch (Exception e) {
            findAnomaly();
        }
        for (int j = page; j <= maxPage; j++) {
            while (true) {
                try {
                    WebElement mytxt = WAIT.until(ExpectedConditions.visibilityOfElementLocated(By.id("jump_page")));
                    SeleniumUtil.sleep(5);
                    mytxt.click();
                    mytxt.clear();
                    mytxt.sendKeys(String.valueOf(j));
                    WAIT.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#app > div > div.post > div > div > div.j_result > div > div:nth-child(2) > div > div.bottom-page > div > div > span.jumpPage"))).click();
                    ACTIONS.keyDown(Keys.CONTROL).sendKeys(Keys.HOME).keyUp(Keys.CONTROL).perform();
                    log.info("第 {} 页", j);
                    break;
                } catch (Exception e) {
                    log.error("mytxt.clear()可能异常...");
                    SeleniumUtil.sleep(1);
                    findAnomaly();
                    CHROME_DRIVER.navigate().refresh();
                }
            }
            postCurrentJob();
        }
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isBlank();
    }

    public static boolean isNotNullOrEmpty(String str) {
        return !isNullOrEmpty(str);
    }


    @SneakyThrows
    private static void postCurrentJob() {
        SeleniumUtil.sleep(1);
        // 选择所有岗位，批量投递
        List<WebElement> checkboxes = CHROME_DRIVER.findElements(By.cssSelector("div.ick"));
        if (checkboxes.isEmpty()) {
            return;
        }
        List<WebElement> titles = CHROME_DRIVER.findElements(By.cssSelector("[class*='jname text-cut']"));
        List<WebElement> companies = CHROME_DRIVER.findElements(By.cssSelector("[class*='cname text-cut']"));
        JavascriptExecutor executor = CHROME_DRIVER;
        for (int i = 0; i < checkboxes.size(); i++) {
            WebElement checkbox = checkboxes.get(i);
            executor.executeScript("arguments[0].click();", checkbox);
            String title = titles.get(i).getText();
            String company = companies.get(i).getText();
            resultList.add(company + " | " + title);
            log.info("选中:{} | {} 职位", company, title);
        }
        SeleniumUtil.sleep(1);
        ACTIONS.keyDown(Keys.CONTROL).sendKeys(Keys.HOME).keyUp(Keys.CONTROL).perform();
        boolean success = false;
        while (!success) {
            try {
                // 查询按钮是否存在
                WebElement parent = CHROME_DRIVER.findElement(By.cssSelector("div.tabs_in"));
                List<WebElement> button = parent.findElements(By.cssSelector("button.p_but"));
                // 如果按钮存在，则点击
                if (button != null && !button.isEmpty()) {
                    SeleniumUtil.sleep(1);
                    button.get(1).click();
                    success = true;
                }
            } catch (ElementClickInterceptedException e) {
                log.error("失败，1s后重试..");
                SeleniumUtil.sleep(1);
            }
        }

        try {
            SeleniumUtil.sleep(3);
            String text = CHROME_DRIVER.findElement(By.xpath("//div[@class='successContent']")).getText();
            if (text.contains("快来扫码下载~")) {
                //关闭弹窗
                CHROME_DRIVER.findElement(By.cssSelector("[class*='van-icon van-icon-cross van-popup__close-icon van-popup__close-icon--top-right']")).click();
            }
        } catch (Exception ignored) {
            log.info("未找到投递成功弹窗！可能为单独投递申请弹窗！");
        }
        String particularly = null;
        try {
            particularly = CHROME_DRIVER.findElement(By.xpath("//div[@class='el-dialog__body']/span")).getText();
        } catch (Exception ignored) {
        }
        if (particularly != null && particularly.contains("需要到企业招聘平台单独申请")) {
            //关闭弹窗
            CHROME_DRIVER.findElement(By.cssSelector("#app > div > div.post > div > div > div.j_result > div > div:nth-child(2) > div > div:nth-child(2) > div:nth-child(2) > div > div.el-dialog__header > button > i")).click();
            log.info("关闭单独投递申请弹窗成功！");
        }
    }

    private static void findAnomaly() {
        try {
            String verify = CHROME_DRIVER.findElement(By.xpath("//p[@class='waf-nc-title']")).getText();
            if (verify.contains("验证")) {
                //关闭弹窗
                log.error("出现访问验证了！程序退出...");
                printResult();
                CHROME_DRIVER.close();
                CHROME_DRIVER.quit();
            }
        } catch (Exception ignored) {
            log.info("未出现访问验证，继续运行...");
        }
    }

    private static void scanLogin() {
        log.info("等待扫码登陆..");
        CHROME_DRIVER.get(loginUrl);
        WAIT.until(ExpectedConditions.presenceOfElementLocated(By.id("hasresume")));
        SeleniumUtil.saveCookie(cookiePath);
    }

}
