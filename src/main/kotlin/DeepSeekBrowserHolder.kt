package com.kldo

import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest

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
     * 固定标签页导航拦截：
     * - 弹窗 / 新窗口 → 改发到自定义浏览器加载（onBeforePopup）
     * - 普通链接点击（同页面导航）→ 也改发到自定义浏览器加载（onBeforeBrowse）
     * - 拦截后自动切到自定义标签页
     */
    private fun installRedirectHandler(browser: JBCefBrowser) {
        // 弹窗拦截：target="_blank" / window.open()
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

        // 同页面链接点击拦截
        browser.getJBCefClient().addRequestHandler(
            object : CefRequestHandlerAdapter() {
                override fun onBeforeBrowse(
                    browser: org.cef.browser.CefBrowser,
                    frame: CefFrame,
                    request: org.cef.network.CefRequest,
                    userGesture: Boolean,
                    isRedirect: Boolean
                ): Boolean {
                    // 只拦截用户主动点击的（userGesture=true）主框架导航
                    if (frame.isMain && userGesture) {
                        val url = request.url
                        getOrCreateCustomBrowser()?.loadURL(url)
                        onRedirectToCustom?.invoke(url)
                        return true // 取消本次导航，改发到自定义浏览器
                    }
                    return false // 放行首次加载 / iframe / JS 内部跳转
                }
            },
            browser.cefBrowser
        )
    }

    /**
     * 自定义标签页弹窗拦截：在当前浏览器加载。
     * 并设置 Chrome User-Agent 以兼容 B 站等网站。
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

        // 所有请求伪装成 Chrome User-Agent（B 站等网站需此才能正常返回数据）
        browser.getJBCefClient().addRequestHandler(
            object : CefRequestHandlerAdapter() {
                override fun getResourceRequestHandler(
                    browser: org.cef.browser.CefBrowser,
                    frame: CefFrame,
                    request: CefRequest,
                    isNavigation: Boolean,
                    isDownload: Boolean,
                    requestInitiator: String?,
                    disableDefaultHandling: BoolRef
                ): CefResourceRequestHandlerAdapter? {
                    return object : CefResourceRequestHandlerAdapter() {
                        override fun onBeforeResourceLoad(
                            browser: org.cef.browser.CefBrowser,
                            frame: CefFrame,
                            request: CefRequest
                        ): Boolean {
                            request.setHeaderByName(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                        "Chrome/126.0.0.0 Safari/537.36",
                                true // overwrite existing
                            )
                            return false // continue the request
                        }
                    }
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