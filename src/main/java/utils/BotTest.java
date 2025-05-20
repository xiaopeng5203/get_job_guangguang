package utils;

import ai.AiFilter;
import java.util.HashMap;
import java.util.Map;

public class BotTest {
    public static void main(String[] args) {
        // 构造一个Job对象
        Job job = new Job();
        job.setCompanyName("字节跳动");
        job.setJobName("高级Java开发");
        job.setJobArea("北京");
        job.setSalary("40-60K");
        job.setRecruiter("张三 | 字节跳动 | HRBP");

        // 1. 测试投递详情推送
        Bot.sendBarkJobDetail(job, "您好，我在分布式系统有丰富经验，非常适合贵司岗位……");

        // 2. 测试每日统计推送
        Map<String, Integer> platformMap = new HashMap<>();
        platformMap.put("Boss直聘", 5);
        platformMap.put("拉勾网", 2);
        platformMap.put("猎聘", 1);
        platformMap.put("智联招聘", 0);
        platformMap.put("前程无忧", 0);
        Bot.sendBarkDailyStat(8, 7, 1, 87.5, platformMap, 20);

        // 3. 测试异常报警推送
        Bot.sendBarkAlert("Boss直聘登录失效", "Cookie过期，请重新登录");

        // 4. 测试黑名单变更推送
        Bot.sendBarkBlacklistChange("字节跳动 | 张三", "公司+招聘者", "7天", "投递反馈\"不合适\"");

        // 5. 测试AI分析推送
        AiFilter filter = new AiFilter(true, "您的经验与岗位要求高度契合，建议积极沟通。");
        Bot.sendBarkAIMatch(job, filter);
    }
} 