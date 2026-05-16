package com.kldo

import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser

object DeepSeekBrowserHolder {
    private val instances = mutableMapOf<String, JBCefBrowser?>()

    fun getOrCreateBrowser(url: String): JBCefBrowser? {
        return instances.getOrPut(url) {
            if (JBCefApp.isSupported()) {
                JBCefBrowser().apply { loadURL(url) }
            } else null
        }
    }

    @JvmStatic
    fun getAllBrowsers(): Map<String, JBCefBrowser?> {
        return instances.toMap()
    }
}