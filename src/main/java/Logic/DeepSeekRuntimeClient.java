package Logic;

import com.google.gson.*;
import com.intellij.openapi.project.Project;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;
import okhttp3.sse.EventSourceListener;
import com.intellij.openapi.project.ProjectManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DeepSeekRuntimeClient {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final Gson gson;
    private int lastSeq = 0;
    private EventSource currentEventSource;
    private final OkHttpClient okHttpClient;

    // 有参构造函数，用于初始化客户端
    public DeepSeekRuntimeClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)  // 永不超时，保持连接
                .build();
    }

    // 无参构造函数，用于初始化客户端
    public DeepSeekRuntimeClient() throws Exception {
        this("http://127.0.0.1:7878");

        // 先检查服务是否已经在运行
        if (isServiceRunning()) {
            System.out.println("DeepSeek Runtime API 服务已在运行");
            return;
        }

        // 服务没运行，启动它
        String rootCommand = DeepSeekTuiCheck.Check().getCommandPath();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(rootCommand, "serve", "--http", "--port", "7878", "--insecure");
        processBuilder.redirectErrorStream(true);
        processBuilder.start();

        // 等待服务启动（最多等 5 秒）
        for (int i = 0; i < 50; i++) {
            Thread.sleep(100);
            if (isServiceRunning()) {
                System.out.println("DeepSeek Runtime API 服务器已启动");
                return;
            }
        }
        throw new Exception("DeepSeek Runtime API 服务器启动超时");
    }

    // 检查服务是否在运行
    private boolean isServiceRunning() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(java.time.Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // 创建线程，返回最新的线程ID
    public String createThread(String workspace, boolean allowShell, boolean autoApprove) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("workspace", workspace);
        body.addProperty("allow_shell", allowShell);
        body.addProperty("auto_approve", autoApprove);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/threads"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
        return result.get("id").getAsString();
    }

    // 获取所有线程ID
    public List<String> getThreadList() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/threads"))
                    .timeout(java.time.Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String jsonString = response.body();

            JsonArray threadsArray = JsonParser.parseString(jsonString).getAsJsonArray();
            List<String> threadIds = new ArrayList<>();
            for (int i = 0; i < threadsArray.size(); i++) {
                JsonObject thread = threadsArray.get(i).getAsJsonObject();
                String id = thread.get("id").getAsString();
                threadIds.add(id);
            }
            return threadIds;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // 发送消息到指定线程
    public void sendMessageStream(String threadId, String prompt, StreamCallback callback) {
        // 1. 创建 Turn
        JsonObject body = new JsonObject();
        body.addProperty("prompt", prompt);
        System.out.println("📤 sendMessageStream 被调用, threadId=" + threadId + ", prompt=" + prompt);

        HttpRequest turnRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/threads/" + threadId + "/turns"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        // 2. 发送创建 Turn 的请求，成功后连接 SSE
        httpClient.sendAsync(turnRequest, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        // 关闭旧连接
                        if (currentEventSource != null) {
                            currentEventSource.cancel();
                        }
                        // 使用 OKHttp 连接 SSE
                        connectEventStreamWithOkHttp(threadId, callback);
                    } else {
                        callback.onError("创建 Turn 失败: " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    callback.onError("发送消息失败: " + e.getMessage());
                    return null;
                });
    }

    // 使用 OKHttp 连接 SSE（推荐）
    private void connectEventStreamWithOkHttp(String threadId, StreamCallback callback) {
        String url = baseUrl + "/v1/threads/" + threadId + "/events";
        if (lastSeq > 0) {
            url += "?since_seq=" + lastSeq;
        }

        System.out.println("🔗 连接 SSE (OKHttp): " + url);

        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "text/event-stream")
                .get()
                .build();

        currentEventSource = EventSources.createFactory(okHttpClient)
                .newEventSource(request, new EventSourceListener() {
                    private boolean isNormalClosed = false;

                    @Override
                    public void onOpen(EventSource eventSource, Response response) {
                        System.out.println("📡 SSE 连接已打开");
                        isNormalClosed = false;
                    }

                    @Override
                    public void onEvent(EventSource eventSource, String id, String type, String data) {
                        if (data == null || data.isEmpty()) return;

                        try {
                            JsonObject event = JsonParser.parseString(data).getAsJsonObject();

                            if (event.has("seq")) {
                                lastSeq = event.get("seq").getAsInt();
                            }

                            String eventType = event.get("event").getAsString();

                            if ("item.delta".equals(eventType) && event.has("payload")) {
                                JsonObject payload = event.getAsJsonObject("payload");
                                if (payload.has("delta")) {
                                    String delta = payload.get("delta").getAsString();
                                    String kind = payload.has("kind") ? payload.get("kind").getAsString() : "";

                                    if ("agent_reasoning".equals(kind)) {
                                        callback.onThinking(delta);
                                    } else {
                                        callback.onAnswer(delta);
                                    }
                                }
                            } else if ("turn.completed".equals(eventType)) {
                                // 标记正常关闭，不要触发 onError
                                isNormalClosed = true;
                                callback.onComplete();
                            } else if ("item.failed".equals(eventType)) {
                                callback.onError("处理失败");
                            }
                        } catch (Exception e) {
                            callback.onError("解析事件失败: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onClosed(EventSource eventSource) {
                        System.out.println("🔒 SSE 连接已关闭");
                    }

                    @Override
                    public void onFailure(EventSource eventSource, Throwable t, Response response) {
                        // 如果是正常关闭（收到了 turn.completed），不报错
                        if (isNormalClosed) {
                            System.out.println("✅ SSE 连接正常关闭");
                            return;
                        }
                        // 真正的错误才报
                        System.err.println("❌ SSE 连接失败: " + t.getMessage());
                        callback.onError("SSE 连接失败: " + t.getMessage());
                    }
                });
    }

    // 旧的 connectEventStream 保留但不再使用（可以删除）
    // private void connectEventStream(...) { ... }

    // =================流式回调接口=====================
    public interface StreamCallback {
        void onAnswer(String text);
        void onThinking(String text);
        void onComplete();
        void onError(String error);
    }


}