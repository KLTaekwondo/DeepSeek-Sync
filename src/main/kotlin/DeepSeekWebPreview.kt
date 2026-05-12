package com.kldo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.ui.component.Text

@Composable
@Preview
fun DeepSeekWebPreview() {
    // 先用 remember 保存浏览器实例，保证生命周期可控
    val browser = remember {
        DeepSeekBrowserHolder.getOnCreateBrowser()
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