package Logic;

import java.util.List;
import Service.ThreadManagerService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

public class ThreadManager {
    private DeepSeekRuntimeClient client ;
    private ThreadManagerService settings;
    private String currentThreadId;
    private List<String> threadIds;
    private boolean newlyCreated = false;

    public ThreadManager(DeepSeekRuntimeClient dsc) throws Exception {
        this.client = dsc;
        this.settings = ThreadManagerService.getInstance();

        String savedId = settings.getLastThreadId();
        this.threadIds = client.getThreadList();

        if (savedId != null && threadIds.contains(savedId)) {
            this.currentThreadId = savedId;
            this.newlyCreated = false;
        } else if (threadIds != null && !threadIds.isEmpty()) {
            this.currentThreadId = threadIds.get(0);
            this.newlyCreated = false;
        } else {
            // 没有可用线程，创建一个新的
            String projectPath = getCurrentProjectPath();
            this.currentThreadId = client.createThread(projectPath, true, true);
            this.threadIds.add(this.currentThreadId);
            this.newlyCreated = true;
        }

        if (this.currentThreadId != null) {
            settings.saveLastThreadId(this.currentThreadId);
        }
    }

    // 创建新线程
    public String createThread(String workspace, boolean allowShell, boolean autoApprove) throws Exception {
        // 1. 创建线程
        String newId = client.createThread(workspace, allowShell, autoApprove);

        // 2. 添加到本地列表
        if (threadIds == null) {
            threadIds = new java.util.ArrayList<>();
        }
        threadIds.add(newId);

        // 3. 设置为当前线程
        this.currentThreadId = newId;
        settings.saveLastThreadId(newId);
        return newId;
    }

    // 切换到指定线程
    public void switchThread(String switchId) throws Exception {
        if (!threadIds.contains(switchId)) {
            throw new Exception("线程不存在: " + switchId);
        }
        this.currentThreadId = switchId;
        settings.saveLastThreadId(switchId);
    }

    // 发送消息到当前线程
    public void sendMessage(String prompt, DeepSeekRuntimeClient.StreamCallback callback) throws Exception {
        if (currentThreadId == null) {
            throw new Exception("没有可用的线程，请先创建线程");
        }
        client.sendMessageStream(currentThreadId, prompt, callback);
    }

    public String getCurrentThreadId() {
        return currentThreadId;
    }

    public List<String> getThreadIds() {
        return threadIds;
    }

    public boolean isNewlyCreated() { return newlyCreated; }

    private String getCurrentProjectPath() {
        try {
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            if (projects.length > 0) {
                String path = projects[0].getBasePath();
                if (path != null && !path.isEmpty()) {
                    return path;
                }
            }
        } catch (Exception e) {
            System.err.println("获取项目路径失败: " + e.getMessage());
        }
        return System.getProperty("user.home");
    }
}
