package com.kldo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab
import javax.swing.SwingUtilities

/**
 * 侧边栏工具窗口工厂。
 * 无 tab 栏，通过标题栏图标按钮切换页面。
 */
class DeepSeekSyncFactory : ToolWindowFactory {

    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setAnchor(ToolWindowAnchor.RIGHT, null)

        // 共享状态：当前选中页
        var selectedTab by mutableStateOf(0)

        // 重定向
        DeepSeekBrowserHolder.onRedirectToCustom = { _ ->
            SwingUtilities.invokeLater { selectedTab = 2 }
        }
        DeepSeekBrowserHolder.onSwitchToChat = {
            SwingUtilities.invokeLater { selectedTab = 0 }
        }

        // 单个 content（无 tab 栏）
        toolWindow.addComposeTab("", focusOnClickInside = true) {
            when (selectedTab) {
                0 -> DeepSeekWebPreview("https://chat.deepseek.com/", autoFocus = true)
                1 -> DeepSeekWebPreview("https://platform.deepseek.com/")
                2 -> CustomBrowserTab()
            }
        }

        // 图标按钮
        val chatIcon = IconLoader.getIcon("/META-INF/tabChat.svg", javaClass)
        val platformIcon = IconLoader.getIcon("/META-INF/tabPlatform.svg", javaClass)
        val globeIcon = IconLoader.getIcon("/META-INF/tabGlobe.svg", javaClass)

        toolWindow.setTitleActions(listOf(
            object : AnAction(
                MyMessageBundle.message("tab.chat"),
                MyMessageBundle.message("tab.chat"),
                chatIcon
            ) {
                override fun actionPerformed(e: AnActionEvent) { selectedTab = 0 }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            },
            object : AnAction(
                MyMessageBundle.message("tab.platform"),
                MyMessageBundle.message("tab.platform"),
                platformIcon
            ) {
                override fun actionPerformed(e: AnActionEvent) { selectedTab = 1 }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            },
            object : AnAction(
                MyMessageBundle.message("tab.custom"),
                MyMessageBundle.message("tab.custom"),
                globeIcon
            ) {
                override fun actionPerformed(e: AnActionEvent) { selectedTab = 2 }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            },
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
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            }
        ))
    }
}
