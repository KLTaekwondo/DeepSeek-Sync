package com.kldo

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.MyMessageBundle"

/**
 * 国际化字典访问类。
 *
 * 用法：MyMessageBundle.message("tab.chat")
 * 自动根据 IDE 语言设置返回中文或英文字符串。
 */
object MyMessageBundle : DynamicBundle(BUNDLE) {

    @Nls
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)
}
