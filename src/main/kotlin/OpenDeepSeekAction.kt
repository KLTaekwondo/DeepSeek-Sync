package com.kldo

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

/**
 * 全局快捷键切换 DeepSeek 侧栏（唤出 / 关闭）。
 *
 * 与 SendToDeepSeekAction 不同（需要选中代码 + 复制），
 * 这个 action 单纯切换侧栏显隐并默认聚焦聊天标签页。
 */
class OpenDeepSeekAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(TOOL_WINDOW_ID) ?: return

        if (toolWindow.isActive) {
            toolWindow.hide()
        } else {
            toolWindow.activate(null, true)
            toolWindow.contentManager.contents
                .firstOrNull()
                ?.let { toolWindow.contentManager.setSelectedContent(it) }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    companion object {
        private const val TOOL_WINDOW_ID = "DeepSeek-Sync/深度同步"
    }
}
