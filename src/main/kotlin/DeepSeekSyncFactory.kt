package com.kldo

import androidx.compose.runtime.LaunchedEffect
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab



class DeepSeekSyncFactory: ToolWindowFactory {
    override fun  shouldBeAvailable(project: Project) = true
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setAnchor(ToolWindowAnchor.RIGHT ,null)

        // 1. 网页端聊天
        toolWindow.addComposeTab("DeepSeek Web Chat", focusOnClickInside = true) {
            LaunchedEffect(Unit) {
                // initial data loading
            }
            DeepSeekWebPreview("https://chat.deepseek.com/")
        }

        // 2. API查看和充值，以及查看自己的APIKEY
        toolWindow.addComposeTab("DeepSeek API Platform", focusOnClickInside = true) {
            LaunchedEffect(Unit) {
                // initial data loading
            }
            DeepSeekWebPreview("https://platform.deepseek.com/")
        }

        // 4. 刷新网页
        toolWindow.setTitleActions(listOf(
            object : AnAction("刷新当前网页", "刷新 DeepSeek 网页", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) {
                    DeepSeekBrowserHolder.getAllBrowsers().values
                        .find { it?.component?.isShowing == true }
                        ?.cefBrowser?.reload()
                }
            }
        ))

    }
}


