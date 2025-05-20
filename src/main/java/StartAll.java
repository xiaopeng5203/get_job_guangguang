import boss.BossConfig;
import boss.H5BossConfig;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.FileInputStream;
import java.util.Map;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StartAll {
    // 存储所有子进程的引用
    private static final List<Process> childProcesses = new ArrayList<>();

    public static void main(String[] args) {
        try {
            System.out.println("正在执行 Boss 任务...");
            executeTask("boss.Boss");
            System.out.println("Boss 任务已完成，完成时间: " + java.time.LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("Boss 任务执行过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 使用独立进程运行指定的类
     *
     * @param className 要执行的类名
     * @throws Exception 如果发生错误
     */
    private static void executeTask(String className) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
            "java", "-cp", System.getProperty("java.class.path"), className
        );
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException(className + " 执行失败，退出代码: " + exitCode);
        }
    }
}
