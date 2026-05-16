package Logic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DeepSeekCLI {

    /**
     * 流式提问，每读到一行就回调一次
     * @param question 用户的问题
     * @param onLine 每读一行就调用这个回调
     */
    public static void askStream(String question, Consumer<String> onLine) throws Exception {
        String commandPath = DeepSeekTuiCheck.resolveCommandPath();
        if (commandPath == null) {
            onLine.accept("Error: deepseek-tui not found");
            return;
        }

        List<String> commandList = new ArrayList<>();
        commandList.add(commandPath);
        commandList.add("exec");
        commandList.add(question);

        ProcessBuilder pb = new ProcessBuilder(commandList);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String os = System.getProperty("os.name").toLowerCase();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        );

        String line;
        while ((line = reader.readLine()) != null) {
            onLine.accept(line);  // 每读一行就回调给 UI
        }
        reader.close();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            onLine.accept("Error: exited with code " + exitCode);
        }
    }
}