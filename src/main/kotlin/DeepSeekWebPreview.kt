package com.kldo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import kotlinx.coroutines.delay
import org.jetbrains.jewel.ui.component.Text

/**
 * 嵌入式 DeepSeek 网页视图。
 *
 * @param url 要加载的网址
 * @param autoFocus 页面加载后是否自动聚焦聊天输入框（仅 chat.deepseek.com 生效）
 */
@Composable
fun DeepSeekWebPreview(url: String, autoFocus: Boolean = false) {
    val browser = remember {
        DeepSeekBrowserHolder.getOrCreateBrowser(url)
    }

    // 页面加载后自动聚焦聊天输入框（延时注入 JS，等待页面 + React 渲染完成）
    LaunchedEffect(browser, autoFocus) {
        if (browser == null || !autoFocus) return@LaunchedEffect

        // 分多次注入：页面没加载好 JS 内部会轮询，多次注入保底
        for (ms in listOf(800L, 2000L, 4000L)) {
            delay(ms)
            browser.cefBrowser.executeJavaScript(FOCUS_SCRIPT, url, 0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (browser == null) {
            Text("当前环境不支持 JCEF，无法显示网页")
        } else {
            SwingPanel(
                modifier = Modifier.fillMaxSize().weight(1f),
                factory = { browser.component }
            )
        }
    }
}

/**
 * 聚焦聊天输入框的 JS 脚本。
 * - 轮询等待 React 渲染出输入框
 * - 同时支持 <textarea> 和 contenteditable div
 */
private const val FOCUS_SCRIPT = """
(function(){
    var t=0,i=setInterval(function(){
        var e=document.querySelector('textarea')||document.querySelector('[contenteditable="true"]');
        if(e){e.focus();e.scrollIntoView();clearInterval(i)}
        else if(++t>15)clearInterval(i)
    },500)
})();
"""
