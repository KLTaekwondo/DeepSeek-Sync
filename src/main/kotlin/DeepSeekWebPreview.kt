package com.kldo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.delay
import org.jetbrains.jewel.ui.component.Text

/**
 * 嵌入式 DeepSeek 网页视图。
 *
 * @param url 要加载的网址
 * @param autoFocus 页面加载后是否自动聚焦聊天输入框
 * @param syncTheme 是否跟随 IDE 主题切换网页亮/暗色
 */
@Composable
fun DeepSeekWebPreview(url: String, autoFocus: Boolean = false, syncTheme: Boolean = false) {
    val browser = remember {
        DeepSeekBrowserHolder.getOrCreateBrowser(url)
    }

    // 自动聚焦
    LaunchedEffect(browser, autoFocus) {
        if (browser == null || !autoFocus) return@LaunchedEffect
        for (ms in listOf(800L, 2000L, 4000L)) {
            delay(ms)
            browser.cefBrowser.executeJavaScript(FOCUS_SCRIPT, url, 0)
        }
    }

    // 主题同步
    LaunchedEffect(browser, syncTheme) {
        if (browser == null || !syncTheme) return@LaunchedEffect
        val isDark = UIUtil.isUnderDarcula()
        val script = buildThemeScript(isDark)
        for (ms in listOf(500L, 2000L, 4000L)) {
            delay(ms)
            browser.cefBrowser.executeJavaScript(script, url, 0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (browser == null) {
            Text(MyMessageBundle.message("ui.jcef.not.supported"))
        } else {
            SwingPanel(
                modifier = Modifier.fillMaxSize().weight(1f),
                factory = { browser.component }
            )
        }
    }
}

// ── JS 脚本 ──

/** 聚焦输入框 */
private const val FOCUS_SCRIPT = """
(function(){
    var t=0,i=setInterval(function(){
        var e=document.querySelector('textarea')||document.querySelector('[contenteditable="true"]');
        if(e){e.focus();e.scrollIntoView();clearInterval(i)}
        else if(++t>15)clearInterval(i)
    },500)
})();
"""

/** 根据 IDE 主题生成网页主题切换脚本 */
private fun buildThemeScript(isDark: Boolean): String {
    val theme = if (isDark) "dark" else "light"
    return """
(function(){
    var theme = '$theme';
    var d = document.documentElement;
    var apply = function() {
        d.setAttribute('data-theme', theme);
        d.setAttribute('data-color-mode', theme);
        d.classList.remove('dark','light');
        d.classList.add(theme);
        try { localStorage.setItem('theme', theme); } catch(e) {}
        /* 部分站点监听系统主题，覆写 matchMedia */
        try {
            Object.defineProperty(window, 'matchMedia', {
                value: function(q) {
                    return q.includes('color-scheme')
                        ? { matches: theme==='dark', media: q, onchange:null, addListener:function(){}, removeListener:function(){}, addEventListener:function(){}, removeEventListener:function(){}, dispatchEvent:function(){} }
                        : window._origMatchMedia ? window._origMatchMedia(q) : { matches:false }
                },
                configurable: true
            });
        } catch(e) {}
    };
    apply();
    setTimeout(apply, 1000);
    setTimeout(apply, 3000);
})();
"""
}
