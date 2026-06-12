package com.kldo

import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter

object DeepSeekBrowserHolder {
    private val instances = mutableMapOf<String, JBCefBrowser?>()
    private const val CUSTOM_KEY = "__custom_browser__"

    /**
     * 固定标签页点击链接时回调。
     * 用于通知 DeepSeekSyncFactory 自动切到自定义标签页。
     */
    var onRedirectToCustom: ((url: String) -> Unit)? = null

    /** 按 URL 缓存获取/创建浏览器（用于固定 tab） */
    fun getOrCreateBrowser(url: String): JBCefBrowser? {
        return instances.getOrPut(url) {
            if (JBCefApp.isSupported()) {
                JBCefBrowser(url).also { installRedirectHandler(it) }
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
     * 固定标签页弹窗拦截：URL 改发到自定义浏览器加载 + 自动切标签。
     */
    private fun installRedirectHandler(browser: JBCefBrowser) {
        browser.getJBCefClient().addLifeSpanHandler(
            object : CefLifeSpanHandlerAdapter() {
                override fun onBeforePopup(
                    browser: org.cef.browser.CefBrowser,
                    frame: CefFrame,
                    targetUrl: String,
                    targetFrameName: String
                ): Boolean {
                    getOrCreateCustomBrowser()?.loadURL(targetUrl)
                    onRedirectToCustom?.invoke(targetUrl)
                    return true // cancel popup
                }
            },
            browser.cefBrowser
        )
    }

    /**
     * 自定义标签页弹窗拦截：在当前浏览器加载。
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