package com.kldo

import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter

object DeepSeekBrowserHolder {
    private val instances = mutableMapOf<String, JBCefBrowser?>()
    private const val CUSTOM_KEY = "__custom_browser__"

    /** 按 URL 缓存获取/创建浏览器（用于固定 tab） */
    fun getOrCreateBrowser(url: String): JBCefBrowser? {
        return instances.getOrPut(url) {
            if (JBCefApp.isSupported()) {
                JBCefBrowser(url)
            } else null
        }
    }

    /** 创建自定义浏览器（不按 URL 缓存，支持动态导航） */
    fun getOrCreateCustomBrowser(initialUrl: String = "about:blank"): JBCefBrowser? {
        return instances.getOrPut(CUSTOM_KEY) {
            if (JBCefApp.isSupported()) {
                JBCefBrowser(initialUrl).also { installPopupHandler(it) }
            } else null
        }
    }

    /**
     * 拦截链接弹窗，改为在当前浏览器中加载。
     * 防止 JCEF 默认的 "Server with Chromium Embedded Framework" 弹窗。
     */
    private fun installPopupHandler(browser: JBCefBrowser) {
        browser.getJBCefClient().addLifeSpanHandler(
            object : CefLifeSpanHandlerAdapter() {
                override fun onBeforePopup(
                    browser: org.cef.browser.CefBrowser,
                    frame: CefFrame,
                    targetUrl: String,
                    targetFrameName: String
                ): Boolean {
                    browser.loadURL(targetUrl)
                    return true // cancel popup, load in current browser
                }
            },
            browser.cefBrowser
        )
    }

    @JvmStatic
    fun getAllBrowsers(): Map<String, JBCefBrowser?> {
        return instances.toMap()
    }
}