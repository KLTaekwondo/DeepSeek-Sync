package com.kldo

import Entity.EnvironmentResult
import Logic.DeepSeekRuntimeClient
import Logic.DeepSeekTuiCheck
import Logic.ThreadManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.intellij.ui.JBColor
import kotlinx.coroutines.*
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JScrollPane
import javax.swing.JTextArea
import org.jetbrains.jewel.ui.component.Text

@Composable
fun AgentChatPanelPreview() {
    val chatHistory = ChatState.chatHistory
    val listState = rememberLazyListState()
    var envResult by remember { mutableStateOf<EnvironmentResult?>(null) }
    var isReady by remember { mutableStateOf(false) }
    var hasChecked by remember { mutableStateOf(false) }
    var isReceiving by remember { mutableStateOf(false) }
    var showThreadDropdown by remember { mutableStateOf(false) }

    // 客户端实例
    var runtimeClient by remember { mutableStateOf<DeepSeekRuntimeClient?>(null) }
    var currentThreadId by remember { mutableStateOf<String?>(null) }

    // 线程管理器
    var threadManager by remember { mutableStateOf<ThreadManager?>(null) }
    var threadIds by remember { mutableStateOf<List<String>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    // 自动滚动到底部
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    // 初始化环境和服务
    LaunchedEffect(Unit) {
        envResult = DeepSeekTuiCheck.Check()
        isReady = envResult?.isCheckResult ?: false
        hasChecked = true

        if (isReady && chatHistory.isEmpty()) {
            try {
                runtimeClient = DeepSeekRuntimeClient()

                runtimeClient?.let { client ->
                    threadManager = ThreadManager(client)
                    threadManager?.let { tm ->
                        currentThreadId = tm.currentThreadId
                        threadIds = tm.threadIds

                        if (tm.isNewlyCreated) {
                            chatHistory.add("System: 已创建新线程")
                        } else {
                            chatHistory.add("System: 已恢复上次对话")
                        }
                    }
                }
            } catch (e: Exception) {
                chatHistory.add("System: 连接失败 - ${e.message}")
                isReady = false
            }
        }
    }

    if (!hasChecked) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("正在检测环境...")
        }
        return
    }

    if (!isReady) {
        ErrorPanel("环境检测失败，请检查配置 ${envResult?.message ?: ""}")
        return
    }

    // 切换线程
    fun switchThread(threadId: String) {
        threadManager?.switchThread(threadId)
        currentThreadId = threadId
        chatHistory.clear()
        chatHistory.add("System: 已切换到线程 ${threadId.take(8)}...")
    }

    // 创建新线程
    fun createNewThread() {
        try {
            val workspace = System.getProperty("user.home")
            val newId = threadManager?.createThread(workspace, true, true)
            if (newId != null) {
                threadIds = threadManager?.threadIds ?: emptyList()
                currentThreadId = newId
                chatHistory.clear()
                chatHistory.add("System: 已创建新线程 ${newId.take(8)}...")
            }
        } catch (e: Exception) {
            chatHistory.add("System: 创建线程失败 - ${e.message}")
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || isReceiving) return

        chatHistory.add("You: $text")
        isReceiving = true

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val client = runtimeClient ?: return@launch
                val threadId = currentThreadId ?: return@launch

                var thinkingBuffer = StringBuilder()
                var hasThinking = false
                var hasAddedAnswerPlaceholder = false

                client.sendMessageStream(threadId, text, object : DeepSeekRuntimeClient.StreamCallback {
                    override fun onAnswer(text: String) {
                        CoroutineScope(Dispatchers.Main).launch {
                            if (!hasAddedAnswerPlaceholder) {
                                hasAddedAnswerPlaceholder = true
                                chatHistory.add("DeepSeek: ")
                            }
                            val lastIndex = chatHistory.lastIndex
                            if (lastIndex >= 0 && chatHistory[lastIndex].startsWith("DeepSeek: ")) {
                                val current = chatHistory[lastIndex].removePrefix("DeepSeek: ")
                                chatHistory[lastIndex] = "DeepSeek: $current$text"
                            }
                        }
                    }

                    override fun onThinking(text: String) {
                        if (!hasThinking) {
                            hasThinking = true
                            thinkingBuffer = StringBuilder()
                            CoroutineScope(Dispatchers.Main).launch {
                                chatHistory.add("💭 思考中...")
                            }
                        }
                        thinkingBuffer.append(text)
                    }

                    override fun onComplete() {
                        CoroutineScope(Dispatchers.Main).launch {
                            isReceiving = false
                            hasAddedAnswerPlaceholder = false

                            if (chatHistory.isNotEmpty() && chatHistory.last() == "DeepSeek: ") {
                                chatHistory.removeAt(chatHistory.lastIndex)
                            }

                            if (hasThinking && thinkingBuffer.isNotEmpty()) {
                                val lastThinkingIndex = chatHistory.lastIndexOf("💭 思考中...")
                                if (lastThinkingIndex >= 0) {
                                    chatHistory[lastThinkingIndex] = "💭 ${thinkingBuffer.toString()}"
                                }
                            }
                        }
                    }

                    override fun onError(error: String) {
                        CoroutineScope(Dispatchers.Main).launch {
                            isReceiving = false
                            hasAddedAnswerPlaceholder = false
                            chatHistory.add("❌ 错误: $error")
                        }
                    }
                })
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isReceiving = false
                    chatHistory.add("❌ 错误: ${e.message}")
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

        // 线程选择栏
        ThreadSelector(
            currentThreadId = currentThreadId,
            threadIds = threadIds,
            showDropdown = showThreadDropdown,
            onToggleDropdown = { showThreadDropdown = !showThreadDropdown },
            onSwitchThread = { threadId ->
                switchThread(threadId)
                showThreadDropdown = false
            },
            onCreateNewThread = { createNewThread() }
        )

        // 聊天记录
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState
        ) {
            items(chatHistory) { msg ->
                ChatMessage(msg)
            }
        }

        // 输入栏
        MessageInputBar(
            onSendMessage = { text ->
                sendMessage(text)
            },
            isEnabled = !isReceiving
        )
    }
}

// 线程选择器组件
@Composable
fun ThreadSelector(
    currentThreadId: String?,
    threadIds: List<String>,
    showDropdown: Boolean,
    onToggleDropdown: () -> Unit,
    onSwitchThread: (String) -> Unit,
    onCreateNewThread: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                modifier = Modifier
                    .background(Color(0xFF2D2D2D), RoundedCornerShape(8.dp))
                    .clickable { onToggleDropdown() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💬 ${currentThreadId?.take(8) ?: "无"}...",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    text = if (showDropdown) " ▲" else " ▼",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }

            if (showDropdown) {
                Popup(
                    alignment = Alignment.TopStart,
                    onDismissRequest = onToggleDropdown,
                    properties = PopupProperties(focusable = true)
                ) {
                    Column(
                        modifier = Modifier
                            .width(200.dp)
                            .heightIn(max = 300.dp)
                            .background(Color(0xFF2D2D2D), RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            if (threadIds.isEmpty()) {
                                item {
                                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                        Text("暂无线程", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                items(threadIds) { threadId ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSwitchThread(threadId) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = threadId.take(12),
                                            color = if (threadId == currentThreadId) Color(0xFF4A90D9) else Color.White,
                                            fontSize = 12.sp
                                        )
                                        if (threadId == currentThreadId) {
                                            Text("✓", color = Color(0xFF4A90D9), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF555555)))

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onCreateNewThread() }.padding(12.dp)
                        ) {
                            Text("➕ 新建线程", color = Color(0xFF4A90D9), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Text(
            text = "+",
            color = Color(0xFF4A90D9),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onCreateNewThread() }.padding(8.dp)
        )
    }
}

// 聊天消息组件
@Composable
fun ChatMessage(msg: String) {
    when {
        msg.startsWith("You:") -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = msg.substringAfter("You: "),
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(Color(0xFF2563EB), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .widthIn(max = 280.dp)
                )
            }
        }

        msg.startsWith("System:") || msg.startsWith("Checking Result:") -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = msg,
                    color = if (msg.contains("成功")) Color(0xFF10B981) else Color(0xFFF59E0B),
                    fontSize = 11.sp
                )
            }
        }

        msg.startsWith("DeepSeek:") -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = msg.substringAfter("DeepSeek: "),
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(Color(0xFF374151), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .widthIn(max = 280.dp)
                )
            }
        }

        msg.startsWith("💭") -> {
            Text(
                text = msg,
                color = Color(0xFF9CA3AF),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
            )
        }

        msg.startsWith("❌") -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = msg, color = Color(0xFFEF4444), fontSize = 12.sp)
            }
        }

        else -> {
            Text(
                text = msg,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.background(Color(0xFF374151), RoundedCornerShape(8.dp)).padding(8.dp)
            )
        }
    }
}

// 输入栏组件
@Composable
fun MessageInputBar(
    onSendMessage: (String) -> Unit,
    isEnabled: Boolean
) {
    val inputArea = remember {
        JTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
            rows = 3
            font = java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14)
            foreground = JBColor.WHITE
            background = JBColor.DARK_GRAY
            caretColor = JBColor.GRAY
        }
    }
    val scrollPane = remember { JScrollPane(inputArea) }.apply {
        preferredSize = Dimension(400, 60)
        maximumSize = Dimension(Integer.MAX_VALUE, 120)
    }

    // 键盘监听
    LaunchedEffect(Unit) {
        inputArea.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown) {
                    e.consume()
                    val text = inputArea.text.trim()
                    if (text.isNotEmpty() && isEnabled) {
                        onSendMessage(text)
                        inputArea.text = ""
                    }
                }
            }
        })
    }

    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SwingPanel(
            modifier = Modifier.weight(1f).height(80.dp),
            factory = { scrollPane }
        )

        Text(
            text = if (isEnabled) "Send" else "⏳",
            color = Color(0xFF4A90D9),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable(enabled = isEnabled) {
                    val text = inputArea.text.trim()
                    if (text.isNotEmpty()) {
                        onSendMessage(text)
                        inputArea.text = ""
                    }
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}


object ChatState {
    val chatHistory = mutableStateListOf<String>()
}