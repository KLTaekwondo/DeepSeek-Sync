package com.kldo

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

/**
 * 终端 / 编辑器右键菜单：选中错误文本 → 让 DeepSeek 分析原因和修复方案。
 *
 * 从标准 Editor 或终端组件树中获取选中文本，加 prompt 前缀后注入聊天框。
 */
class ExplainTerminalErrorAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 方法一：从标准 Editor 获取选中文本（普通编辑器场景）
        var selectedText = e.getData(CommonDataKeys.EDITOR)
            ?.selectionModel?.selectedText

        // 方法二：终端场景 — ToolWindow 组件树反射
        if (selectedText.isNullOrBlank()) {
            selectedText = getTerminalSelectedText(project)
        }

        if (selectedText.isNullOrBlank()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("DeepSeek-Sync")
                .createNotification(
                    MyMessageBundle.message("action.explain.error.no.selection"),
                    NotificationType.WARNING
                ).notify(project)
            return
        }

        val prompt = MyMessageBundle.message("action.explain.error.prompt")
        val fullText = "$prompt\n\n$selectedText"

        DeepSeekInjector.inject(fullText, project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    companion object {
        /**
         * 通过终端 ToolWindow 的组件树反射获取选中文本。
         *
         * 因去掉了 com.intellij.terminal 编译依赖，不能直接引用终端 API，
         * 改为走 ToolWindow → 组件树 → 反射找 EditorComponentImpl.getEditor() 路径。
         */
        private fun getTerminalSelectedText(project: Project): String? {
            val toolWindow = ToolWindowManager.getInstance(project)
                .getToolWindow("Terminal") ?: return null
            val content = toolWindow.contentManager.getContent(0) ?: return null
            return findSelectedText(content.component)
        }

        /** 在组件树中递归查找 Editor 的选中文本 */
        private fun findSelectedText(comp: java.awt.Component): String? {
            // 1) getEditor() → SelectionModel → getSelectedText()
            try {
                val m = comp::class.java.getMethod("getEditor")
                val editor = m.invoke(comp)
                val selModel = editor::class.java.getMethod("getSelectionModel").invoke(editor)
                val text = selModel::class.java.getMethod("getSelectedText").invoke(selModel) as? String
                if (!text.isNullOrBlank()) return text
            } catch (_: NoSuchMethodException) { }
              catch (_: Exception) { }

            // 2) 直接 getSelectedText()
            try {
                val text = comp::class.java.getMethod("getSelectedText").invoke(comp) as? String
                if (!text.isNullOrBlank()) return text
            } catch (_: NoSuchMethodException) { }
              catch (_: Exception) { }

            // 3) 递归子组件
            if (comp is java.awt.Container) {
                for (child in comp.components) {
                    findSelectedText(child)?.let { return it }
                }
            }
            return null
        }
    }
}
