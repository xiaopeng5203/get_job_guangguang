package utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * @author loks666
 * 项目链接: <a href="https://github.com/loks666/get_jobs">https://github.com/loks666/get_jobs</a>
 */
@Slf4j
public class Bot {
    private static final Logger log = LoggerFactory.getLogger(Bot.class);

    private static final String HOOK_URL;
    private static boolean isSend;
    private static String barkUrl;
    private static boolean isBarkSend;

    static {
        // 加载环境变量
        Dotenv dotenv = Dotenv
                .configure()
                .directory(ProjectRootResolver.rootPath+"/src/main/resources")
                .load();
        HOOK_URL = dotenv.get("HOOK_URL");

        // 使用 Jackson 加载 config.yaml 配置
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            HashMap<String, Object> config = mapper.readValue(new File(ProjectRootResolver.rootPath+"/src/main/resources/config.yaml"), new TypeReference<HashMap<String, Object>>() {
            });
            log.info("YAML 配置内容: {}", config);

            // 获取 bot 配置
            HashMap<String, Object> botConfig = safeCast(config.get("bot"), HashMap.class);
            if (botConfig != null && botConfig.get("is_send") != null) {
                isSend = Boolean.TRUE.equals(safeCast(botConfig.get("is_send"), Boolean.class));
            } else {
                log.warn("配置文件中缺少 'bot.is_send' 键或值为空，不发送消息。");
                isSend = false;
            }
            if (botConfig != null && botConfig.get("is_bark_send") != null) {
                isBarkSend = Boolean.TRUE.equals(safeCast(botConfig.get("is_bark_send"), Boolean.class));
            } else {
                log.warn("配置文件中缺少 'bot.is_bark_send' 键或值为空，不发送消息。");
                isBarkSend = false;
            }
            if (botConfig != null && botConfig.get("bark_url") != null) {
                barkUrl = botConfig.get("bark_url").toString();
            } else {
                log.warn("配置文件中缺少 'bot.bark_url' 键或值为空，不发送消息。");
            }
        } catch (IOException e) {
            log.error("读取 config.yaml 异常：{}", e.getMessage());
            isSend = false;
            isBarkSend = false;
        }
    }

    public static void sendBark(String message) {
        if (!isBarkSend) {
            return;
        }
        try {
            String url = barkUrl + java.net.URLEncoder.encode(message, "UTF-8");
            String response = Request.get(url).execute().returnContent().asString();
            log.info("消息推送成功: {}", response);
        } catch (Exception e) {
            log.error("消息推送失败: {}", e.getMessage());
        }
    }

    public static void sendMessageByTime(String message) {
        if (!isSend) {
            sendBark(message);
            return;
        }
        // 格式化当前时间
        String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        String formattedMessage = String.format("%s %s", currentTime, message);
        sendMessage(formattedMessage);
        sendBark(formattedMessage);
    }

    public static void sendMessage(String message) {
        if (!isSend) {
            sendBark(message);
            return;
        }
        // 发送HTTP请求
        try {
            String response = Request.post(HOOK_URL)
                    .bodyString("{\"msgtype\": \"text\", \"text\": {\"content\": \"" + message + "\"}}",
                            org.apache.hc.core5.http.ContentType.APPLICATION_JSON)
                    .execute()
                    .returnContent()
                    .asString();
            log.info("消息推送成功: {}", response);
        } catch (Exception e) {
            log.error("消息推送失败: {}", e.getMessage());
        }
        sendBark(message);
    }

    public static void main(String[] args) {
        sendMessageByTime("企业微信推送测试消息...");
    }

    /**
     * 通用的安全类型转换方法，避免未检查的类型转换警告
     *
     * @param obj   要转换的对象
     * @param clazz 目标类型的 Class 对象
     * @param <T>   目标类型
     * @return 如果对象类型匹配，则返回转换后的对象，否则返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T safeCast(Object obj, Class<T> clazz) {
        if (clazz.isInstance(obj)) {
            return (T) obj;
        } else {
            return null;
        }
    }

    /**
     * Bark推送：投递岗位详情（含AI打招呼语）
     */
    public static void sendBarkJobDetail(utils.Job job, String aiSayHi) {
        String msg = String.format(
            "【投递成功】\n公司：%s\n岗位：%s\n城市：%s\n薪资：%s\n招聘者：%s\nAI打招呼语：%s\n投递时间：%s",
            job.getCompanyName(),
            job.getJobName(),
            job.getJobArea(),
            job.getSalary(),
            job.getRecruiter(),
            aiSayHi,
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );
        sendBark(msg);
    }

    /**
     * Bark推送：每日/每周投递统计
     */
    public static void sendBarkDailyStat(int total, int success, int fail, double rate, java.util.Map<String, Integer> platformMap, int weekTotal) {
        String msg = String.format(
            "【投递统计】\n日期：%s\n总投递岗位数：%d\n成功投递：%d\n失败投递：%d\n命中率：%.2f%%\n平台分布：Boss直聘%d，拉勾%d，猎聘%d，智联%d，51Job%d\n本周累计：%d",
            new SimpleDateFormat("yyyy-MM-dd").format(new Date()),
            total, success, fail, rate,
            platformMap.getOrDefault("Boss直聘", 0),
            platformMap.getOrDefault("拉勾网", 0),
            platformMap.getOrDefault("猎聘", 0),
            platformMap.getOrDefault("智联招聘", 0),
            platformMap.getOrDefault("前程无忧", 0),
            weekTotal
        );
        sendBark(msg);
    }

    /**
     * Bark推送：异常与报警
     */
    public static void sendBarkAlert(String type, String detail) {
        String msg = String.format(
            "【异常报警】\n类型：%s\n详情：%s\n时间：%s\n请及时处理！",
            type, detail, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );
        sendBark(msg);
    }

    /**
     * Bark推送：黑名单变更
     */
    public static void sendBarkBlacklistChange(String who, String type, String period, String reason) {
        String msg = String.format(
            "【黑名单变更】\n已拉黑：%s\n类型：%s\n有效期：%s\n原因：%s\n时间：%s",
            who, type, period, reason, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );
        sendBark(msg);
    }

    /**
     * Bark推送：岗位匹配度与AI分析结论
     */
    public static void sendBarkAIMatch(utils.Job job, ai.AiFilter filter) {
        String msg = String.format(
            "【岗位匹配分析】\n公司：%s\n岗位：%s\n匹配度：%s\nAI分析结论：%s\nAI打招呼语：%s",
            job.getCompanyName(),
            job.getJobName(),
            filter.isMatch() ? "高度匹配" : "不匹配",
            filter.getResult(),
            filter.getResult()
        );
        sendBark(msg);
    }
}
