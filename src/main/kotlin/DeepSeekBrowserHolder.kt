package com.kldo

import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser

object DeepSeekBrowserHolder {
    private val instances = mutableMapOf<String, JBCefBrowser?>()
    private const val CUSTOM_KEY = "__custom_browser__"

    /** 按 URL 缓存获取/创建浏览器（用于固定 tab） */
    fun getOrCreateBrowser(url: String): JBCefBrowser? {
        return instances.getOrPut(url) {
            if (JBCefApp.isSupported()) {
                JBCefBrowser().apply { loadURL(url) }
            } else null
        }
    }

    /** 创建自定义浏览器（不按 URL 缓存，支持动态导航） */
    fun getOrCreateCustomBrowser(initialUrl: String = "about:blank"): JBCefBrowser? {
        return instances.getOrPut(CUSTOM_KEY) {
            if (JBCefApp.isSupported()) {
                JBCefBrowser().apply { loadURL(initialUrl) }
            } else null
        }
    }

    @JvmStatic
    fun getAllBrowsers(): Map<String, JBCefBrowser?> {
        return instances.toMap()
    }
}