package Logic;

import Entity.EnvironmentResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DeepSeekTuiCheck {

    // 私有构造函数，防止外部实例化
    private DeepSeekTuiCheck() {}

    // 直接用的命令名，不需要硬编码完整路径了
    private static final String CLI_COMMAND = "deepseek-tui";
    private static EnvironmentResult cachedResult;
    /**
     * 自动查找命令的完整路径
     * Windows 用 where，Linux/Mac 用 which
     */
    public static String resolveCommandPath() {
        try {
            // 检查是否为 Windows 系统
            String os = System.getProperty("os.name").toLowerCase();
            boolean isWindows = os.contains("win");

            // 🔑 Windows 找 .cmd，Linux/Mac 找无后缀的
            String searchTarget = isWindows ? CLI_COMMAND + ".cmd" : CLI_COMMAND;
            String findCommand = isWindows ? "where" : "which";

            // 尝试运行查找命令
            ProcessBuilder pb = new ProcessBuilder(findCommand, searchTarget);
            Process process = pb.start();

            // 读取查找命令的输出
            String encoding = isWindows ? "GBK" : "UTF-8";
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), encoding)
            );

            // 读取查找命令的输出，假设只有一行
            String path = reader.readLine();
            reader.close();
            process.waitFor();

            if (path != null && !path.isEmpty()) {
                path = path.trim();
                return path;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 检测 deepseek-tui 是否可用
     */
    public static EnvironmentResult Check() {
        if(cachedResult != null) return cachedResult;
        try {
            // 1. 先找完整路径
            String commandPath = resolveCommandPath();
            if (commandPath == null) {
                cachedResult = EnvironmentResult.builder()
                        .CheckResult(false)
                        .message("ERROR: DeepSeek TUI Agent not found in PATH environment, please install it first" +
                                "\n" + "ERROR: try npm install deepseek-tui -g to install it or " +
                                "visit https://github.com/Hmbown/DeepSeek-TUI.git for more info")
                        .build();
                return cachedResult;
            }

            // 2. 构建命令（用完整路径 + 参数）
            List<String> commandList = new ArrayList<>();
            commandList.add(commandPath);  // 完整路径，已自动处理转义
            commandList.add("--version");

            ProcessBuilder pb = new ProcessBuilder(commandList);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 3. 读输出
            String os = System.getProperty("os.name").toLowerCase();
            String encoding = os.contains("win") ? "GBK" : "UTF-8";
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), encoding)
            );

            // 4. 检查输出
            StringBuilder versionOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                versionOutput.append(line).append("\n");
            }
            reader.close();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                cachedResult = EnvironmentResult.builder()
                        .CheckResult(true)
                        .commandPath(commandPath)
                        .message("SUCCESS: DeepSeek TUI Agent is ready\nVersion: " + versionOutput.toString().trim())
                        .build();
                return cachedResult;
            }

            cachedResult = EnvironmentResult.builder()
                    .CheckResult(false)
                    .message("ERROR: DeepSeek TUI Agent check failed. Exit code: " + exitCode)
                    .build();
            return cachedResult;

        } catch (Exception e) {
            cachedResult = EnvironmentResult.builder()
                    .CheckResult(false)
                    .message("Check Exception: " + e.getMessage()).build();
            return cachedResult;
        }
    }

    /**
     * 测试入口
     */
    public static void main(String[] args) {
        cachedResult = Check();
        System.out.println("Check Result: " + cachedResult);
    }
}