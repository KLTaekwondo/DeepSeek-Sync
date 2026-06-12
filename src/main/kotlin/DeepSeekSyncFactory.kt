package com.kldo

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab
import javax.swing.SwingUtilities

/**
 * 侧边栏工具窗口工厂。
 *
 * 实现 ToolWindowFactory 必然继承 isApplicable / isDoNotActivateOnStart 等
 * 过时接口方法，这是平台级约束，所有插件都如此。JetBrains 移除前会提供迁移路径。
 */
class DeepSeekSyncFactory : ToolWindowFactory {

    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setAnchor(ToolWindowAnchor.RIGHT, null)

        // 聊天标签页（打开后自动聚焦输入框）
        toolWindow.addComposeTab(MyMessageBundle.message("tab.chat"), focusOnClickInside = true) {
            DeepSeekWebPreview("https://chat.deepseek.com/", autoFocus = true)
        }

        // 控制台标签页
        toolWindow.addComposeTab(MyMessageBundle.message("tab.platform"), focusOnClickInside = true) {
            DeepSeekWebPreview("https://platform.deepseek.com/")
        }

        // 自定义浏览
        toolWindow.addComposeTab(MyMessageBundle.message("tab.custom"), focusOnClickInside = true) {
            CustomBrowserTab()
        }

        // 固定标签页点链接 → 自动切到自定义标签页
        DeepSeekBrowserHolder.onRedirectToCustom = { _ ->
            SwingUtilities.invokeLater {
                val contents = toolWindow.contentManager.contents
                if (contents.size >= 3) {
                    toolWindow.contentManager.setSelectedContent(contents[2])
                }
            }
        }

        // 刷新按钮
        toolWindow.setTitleActions(listOf(
            object : AnAction(
                MyMessageBundle.message("action.refresh.text"),
                MyMessageBundle.message("action.refresh.description"),
                AllIcons.Actions.Refresh
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    DeepSeekBrowserHolder.getAllBrowsers().values
                        .find { it?.component?.isShowing == true }
                        ?.cefBrowser?.reload()
                }

                override fun getActionUpdateThread(): ActionUpdateThread {
                    return ActionUpdateThread.EDT
                }
            }
        ))
    }
}
