package com.kldo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.intellij.util.ui.UIUtil
import org.jetbrains.jewel.ui.component.Text
import java.awt.event.ActionListener
import javax.swing.JTextField

/**
 * 可自定义 URL 的浏览器标签页。
 *
 * 用户可以在地址栏输入任意网址，按回车或点击「前往」加载。
 * 默认打开 DeepSeek 聊天页。
 */
@Composable
fun CustomBrowserTab() {
    val browser = remember {
        DeepSeekBrowserHolder.getOrCreateCustomBrowser("https://chat.deepseek.com/")
    }

    // Swing JTextField
    val urlField = remember {
        JTextField("https://chat.deepseek.com/").apply {
            addActionListener(ActionListener {
                navigate(text, browser)
            })
            // Darcula 下默认白底白字，设黑字保证可读
            if (UIUtil.isUnderDarcula()) {
                foreground = java.awt.Color(0x00, 0x00, 0x00)
            }
        }
    }

    // 根据主题取色
    val panelBg = remember { jbColor(0xF5F5F5, 0x00_00_00) }  // 亮灰 / 纯黑
    val btnBorder = remember { jbColor(0xD0D0D0, 0x80_80_80) } // 浅灰 / 灰色

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 地址栏 ──
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(panelBg)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SwingPanel(
                modifier = Modifier.weight(1f).heightIn(min = 28.dp),
                factory = { urlField }
            )

            Spacer(Modifier.width(6.dp))

            // 前往按钮（点击区域）
            Box(
                modifier = Modifier
                    .clickable { navigate(urlField.text, browser) }
                    .border(1.dp, btnBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text("前往")
            }
        }

        // ── 浏览器内容 ──
        if (browser == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("当前环境不支持 JCEF，无法显示网页")
            }
        } else {
            SwingPanel(
                modifier = Modifier.fillMaxSize().weight(1f),
                factory = { browser.component }
            )
        }
    }
}

/** 导航到指定 URL（自动补全 https://） */
private fun navigate(url: String, browser: com.intellij.ui.jcef.JBCefBrowser?) {
    val normalized = url.trim().let {
        when {
            it.isBlank() -> return
            it.startsWith("http://") || it.startsWith("https://") -> it
            else -> "https://$it"
        }
    }
    browser?.loadURL(normalized)
}

/** 根据 IDE 亮/暗主题返回对应 Compose Color */
private fun jbColor(light: Int, dark: Int): Color {
    val rgb = if (UIUtil.isUnderDarcula()) dark else light
    val c = java.awt.Color(rgb)
    return Color(c.red / 255f, c.green / 255f, c.blue / 255f)
}
