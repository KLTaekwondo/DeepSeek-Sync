package com.kldo

import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser

object DeepSeekBrowserHolder {
    @Volatile
    private var instance : JBCefBrowser? = null // 浏览器实例

    // 获取或创建浏览器实例
    fun getOnCreateBrowser() : JBCefBrowser?{
        // 先检查实例是否存在
        if(instance != null) return instance

        // 再检查一次，确保线程安全
        return synchronized(this) {
            // 确保实例在创建时是唯一的
            if(instance != null) return instance

            // 创建浏览器实例,指向 DeepSeek Chat
            if(JBCefApp.isSupported()) {
                JBCefBrowser().apply {
                    loadURL("https://chat.deepseek.com/")
                }.also { instance = it }
            }else null
        }
    }
}