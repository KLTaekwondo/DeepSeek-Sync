package com.kldo

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware

/**
 * 右键菜单 → 发送到 DeepSeek → 设置。
 * 打开 Settings → Tools → DeepSeek-Sync。
 */
class OpenSettingsAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(
            e.project,
            DeepSeekSyncPromptConfigurable::class.java
        )
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}
