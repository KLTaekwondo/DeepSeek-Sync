package Service;

import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.application.ApplicationManager;

/**
 * 线程管理器服务 - 用于持久化存储当前线程ID
 */
@Service
@State(
        name = "DeepSeekThreadManager",
        storages = @Storage("deepseek-settings.xml")
)
public final class ThreadManagerService implements PersistentStateComponent<ThreadManagerService.State> {

    private State myState = new State();

    /**
     * 存储的数据结构
     */
    public static class State {
        public String lastThreadId = null;  // 上次使用的线程ID
    }

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    /**
     * 获取上次使用的线程ID
     */
    public String getLastThreadId() {
        return myState.lastThreadId;
    }

    /**
     * 保存当前线程ID
     */
    public void saveLastThreadId(String threadId) {
        myState.lastThreadId = threadId;
    }

    /**
     * 清除保存的ID
     */
    public void clear() {
        myState.lastThreadId = null;
    }

    /**
     * 获取服务实例（插件任何地方通过这个方法调用）
     */
    public static ThreadManagerService getInstance() {
        return ApplicationManager.getApplication().getService(ThreadManagerService.class);
    }
}