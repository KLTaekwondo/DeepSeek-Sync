package com.kldo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import org.cef.handler.CefLoadHandlerAdapter
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

    // 页面加载完成后自动聚焦聊天输入框
    DisposableEffect(browser, autoFocus) {
        if (browser == null || !autoFocus) return@DisposableEffect onDispose {}

        val loadHandler = object : CefLoadHandlerAdapter() {
            override fun onLoadingStateChange(
                _browser: org.cef.browser.CefBrowser,
                isLoading: Boolean,
                _canGoBack: Boolean,
                _canGoForward: Boolean
            ) {
                if (!isLoading) {
                    _browser.executeJavaScript(FOCUS_SCRIPT, url, 0)
                }
            }
        }
        browser.jbCefClient.addLoadHandler(loadHandler, browser.cefBrowser)
        onDispose {
            browser.jbCefClient.removeLoadHandler(loadHandler, browser.cefBrowser)
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

/** 聚焦聊天输入框的 JS 脚本（轮询等待 React 渲染完成） */
private const val FOCUS_SCRIPT = """
(function(){
    var t=0,i=setInterval(function(){
        var e=document.querySelector('textarea');
        if(e){e.focus();clearInterval(i)}
        else if(++t>20)clearInterval(i)
    },500)
})();
"""
