package com.kldo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.JBColor
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.jetbrains.jewel.ui.component.Text
import java.awt.event.ActionListener
import javax.swing.JTextField
import javax.swing.SwingUtilities

/** PropertiesComponent Key：保存的自定义 URL */
private const val PROP_CUSTOM_URL = "DeepSeekSync.customUrl"

/**
 * 可自定义 URL 的浏览器标签页。
 *
 * 用户可以在地址栏输入任意网址，按回车或点击「前往」加载。
 * 默认打开 DeepSeek 聊天页，URL 会持久化保存。
 */
@Composable
fun CustomBrowserTab() {
    // 从 PropertiesComponent 读取上次保存的 URL
    val savedUrl = remember {
        PropertiesComponent.getInstance()
            .getValue(PROP_CUSTOM_URL, "https://chat.deepseek.com/")
    }

    // Swing JTextField（先创建，后续再设监听）
    val urlField = remember {
        JTextField(savedUrl).apply {
            // Darcula 下默认白底白字，设黑字保证可读
            if (!JBColor.isBright()) {
                foreground = java.awt.Color(0x00, 0x00, 0x00)
            }
        }
    }

    val browser = remember {
        DeepSeekBrowserHolder.getOrCreateCustomBrowser(savedUrl)
    }

    // 当前实际 URL（Compose 可观察状态，随导航刷新）
    var currentUrl by remember { mutableStateOf(savedUrl) }

    // 安装浏览器回调：URL 同步 + 回车导航
    LaunchedEffect(Unit) {
        // 页面主框架加载完成后同步 URL 到地址栏
        browser?.getJBCefClient()?.addLoadHandler(
            object : CefLoadHandlerAdapter() {
                override fun onLoadEnd(
                    browser: org.cef.browser.CefBrowser,
                    frame: CefFrame,
                    httpStatusCode: Int
                ) {
                    // 只在主框架加载完成时更新地址栏
                    if (frame.isMain) {
                        SwingUtilities.invokeLater {
                            urlField.text = frame.url
                            currentUrl = frame.url
                        }
                    }
                }
            },
            browser.cefBrowser
        )

        // 回车导航
        urlField.addActionListener(ActionListener {
            navigate(urlField.text, browser)
        })
    }

    // 根据主题取色
    val panelBg = remember { jbColor(0xF5F5F5, 0x00_00_00) }  // 亮灰 / 纯黑
    val btnBorder = remember { jbColor(0xD0D0D0, 0x80_80_80) } // 浅灰 / 灰色
    val bookmarkBg = remember { jbColor(0xFF_FF_FF, 0x1E_1E_1E) } // 白 / 暗灰
    val bookmarkHover = remember { jbColor(0xE8_E8_E8, 0x33_33_33) } // 悬停色

    // ── 收藏夹状态 ──
    var bookmarks by remember { mutableStateOf(loadBookmarks()) }
    var bookmarksOpen by remember { mutableStateOf(false) }
    // 当前 URL 是否已收藏
    val isBookmarked = bookmarks.any { it.url == currentUrl }

    // 收藏 / 取消收藏
    fun toggleBookmark() {
        val current = currentUrl.trim()
        if (current.isBlank()) return
        if (isBookmarked) {
            bookmarks = bookmarks.filterNot { it.url == current }.toMutableList()
        } else {
            if (bookmarks.size >= 15) {
                com.intellij.notification.NotificationGroupManager.getInstance()
                    .getNotificationGroup("DeepSeek-Sync")
                    .createNotification(
                        MyMessageBundle.message("ui.custom.bookmark.full"),
                        com.intellij.notification.NotificationType.WARNING
                    )
                    .notify(com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull())
                return
            }
            val name = displayNameForUrl(current)
            bookmarks = (bookmarks + Bookmark(name, current)).toMutableList()
        }
        saveBookmarks(bookmarks)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 地址栏（含后退/前进 + URL 输入框 + 前往） ──
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(panelBg)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ◀ 后退
            NavButton(
                text = "◀",
                enabled = browser != null,
                onClick = { browser?.cefBrowser?.goBack() }
            )

            // ▶ 前进
            NavButton(
                text = "▶",
                enabled = browser != null,
                onClick = { browser?.cefBrowser?.goForward() }
            )

            Spacer(Modifier.width(4.dp))

            SwingPanel(
                modifier = Modifier.weight(1f).heightIn(min = 28.dp),
                factory = { urlField }
            )

            Spacer(Modifier.width(6.dp))

            // 前往按钮
            Box(
                modifier = Modifier
                    .clickable(enabled = browser != null) { navigate(urlField.text, browser) }
                    .border(1.dp, btnBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(MyMessageBundle.message("ui.custom.url.go"))
            }

            // ↗ 在系统浏览器打开（JCEF 无法播放视频等场景）
            Box(
                modifier = Modifier
                    .clickable(enabled = browser != null) {
                        val url = urlField.text.trim().let {
                            if (it.startsWith("http://") || it.startsWith("https://")) it
                            else "https://$it"
                        }
                        BrowserUtil.browse(url)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("↗")
            }

            // ★ 收藏当前 URL
            Box(
                modifier = Modifier
                    .clickable(enabled = browser != null) { toggleBookmark() }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isBookmarked) "★" else "☆",
                    fontSize = 14.sp
                )
            }

            // ▼ / ▲ 折叠收藏栏
            Box(
                modifier = Modifier
                    .clickable(enabled = browser != null) { bookmarksOpen = !bookmarksOpen }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (bookmarksOpen) "▲" else "▼", fontSize = 10.sp)
            }
        }

        // ── 收藏夹面板（可折叠） ──
        AnimatedVisibility(
            visible = bookmarksOpen && bookmarks.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(bookmarkBg)
                    .border(1.dp, btnBorder.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                bookmarks.forEach { bm ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable {
                                urlField.text = bm.url
                                navigate(bm.url, browser)
                                bookmarksOpen = false
                            }
                            .background(bookmarkHover.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 名称
                        Text(
                            bm.name,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        // URL
                        Text(
                            bm.url,
                            fontSize = 10.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // 删除
                        Box(
                            modifier = Modifier
                                .clickable {
                                    bookmarks = bookmarks.filterNot { it.url == bm.url }.toMutableList()
                                    saveBookmarks(bookmarks)
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // ── 浏览器内容 ──
        if (browser == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(MyMessageBundle.message("ui.jcef.not.supported"))
            }
        } else {
            SwingPanel(
                modifier = Modifier.fillMaxSize().weight(1f),
                factory = { browser.component }
            )
        }
    }
}

/** 带悬停高亮动效的导航按钮 */
@Composable
private fun NavButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor by animateColorAsState(
        targetValue = when {
            !isHovered -> Color.Transparent
            !JBColor.isBright() -> Color(0x30_FFFFFF) // 暗色：浅色遮罩
            else -> Color(0x18_000000)                     // 亮色：深色遮罩
        },
        animationSpec = tween(durationMillis = 200),
        label = "navBg"
    )

    Box(
        modifier = Modifier
            .hoverable(interactionSource, enabled)
            .clickable(enabled = enabled) { onClick.invoke() }
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, textAlign = TextAlign.Center)
    }
}

/** 导航到指定 URL（自动补全 https://），并持久化保存 */
private fun navigate(url: String, browser: com.intellij.ui.jcef.JBCefBrowser?) {
    val normalized = url.trim().let {
        when {
            it.isBlank() -> return
            it.startsWith("http://") || it.startsWith("https://") -> it
            else -> "https://$it"
        }
    }
    // 保存到全局持久化，重启 IDE 后自动恢复
    PropertiesComponent.getInstance().setValue(PROP_CUSTOM_URL, normalized)
    browser?.loadURL(normalized)
    // navigate 是外部触发的，调用方需自己更新 currentUrl
}

/** 根据 IDE 亮/暗主题返回对应 Compose Color */
private fun jbColor(light: Int, dark: Int): Color {
    val rgb = if (!JBColor.isBright()) dark else light
    val c = java.awt.Color(rgb)
    return Color(c.red / 255f, c.green / 255f, c.blue / 255f)
}

// ── 收藏夹 ──

private data class Bookmark(val name: String, val url: String)

private const val PROP_BOOKMARKS = "DeepSeekSync.bookmarks"
private val gson = Gson()

/** 从 PropertiesComponent 加载收藏列表 */
private fun loadBookmarks(): MutableList<Bookmark> {
    val json = PropertiesComponent.getInstance().getValue(PROP_BOOKMARKS, "[]") ?: "[]"
    return try {
        val type = object : TypeToken<List<Bookmark>>() {}.type
        gson.fromJson<List<Bookmark>>(json, type).toMutableList()
    } catch (_: Exception) { mutableListOf() }
}

/** 持久化保存收藏列表 */
private fun saveBookmarks(bookmarks: List<Bookmark>) {
    PropertiesComponent.getInstance().setValue(PROP_BOOKMARKS, gson.toJson(bookmarks))
}

/** 从 URL 提取一个可读的名称 */
private fun displayNameForUrl(url: String): String {
    val clean = url.removePrefix("https://").removePrefix("http://").trimEnd('/')
    // 取主域名作为默认名
    val parts = clean.split('/')
    val domain = parts.first().removePrefix("www.")
    return if (parts.size > 1 && parts[1].isNotBlank()) {
        // 取路径第一段做后缀
        "${domain} /${parts[1].take(20)}"
    } else domain
}
