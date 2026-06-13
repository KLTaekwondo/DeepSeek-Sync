package com.kldo

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware

/**
 * 右键菜单「发送到 DeepSeek ▶」子菜单各项 AI 操作。
 *
 * 通过 Action ID 区分不同的 prompt，无需为每个功能创建单独类。
 */
class DeepSeekAIAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText ?: return

        val actionId = ActionManager.getInstance().getId(this)
        val promptKey = PROMPT_MAP[actionId] ?: "action.send.code.prompt"
        val prompt = MyMessageBundle.message(promptKey)

        DeepSeekInjector.inject("$prompt\n\n$selectedText", project)
    }

    override fun update(e: AnActionEvent) {
        val actionId = ActionManager.getInstance().getId(this)

        // 从字典设置本地化文本
        val bundleKey = TEXT_MAP[actionId]
        if (bundleKey != null) {
            e.presentation.text = MyMessageBundle.message("$bundleKey.text")
            e.presentation.description = MyMessageBundle.message("$bundleKey.description")
        }

        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible =
            e.project != null &&
            editor != null &&
            editor.selectionModel.hasSelection()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    companion object {
        /** Action ID → 字典 key 前缀（.text / .description 拼接） */
        private val TEXT_MAP = mapOf(
            "DeepSeekSync.ExplainCode"  to "action.send.explain",
            "DeepSeekSync.ImproveCode"  to "action.send.improve",
            "DeepSeekSync.ReviewCode"   to "action.send.review",
            "DeepSeekSync.TestCode"     to "action.send.test",
            "DeepSeekSync.CommentCode"  to "action.send.comment",
        )

        /** Action ID → prompt key */
        private val PROMPT_MAP = mapOf(
            "DeepSeekSync.ExplainCode"  to "action.send.explain.prompt",
            "DeepSeekSync.ImproveCode"  to "action.send.improve.prompt",
            "DeepSeekSync.ReviewCode"   to "action.send.review.prompt",
            "DeepSeekSync.TestCode"     to "action.send.test.prompt",
            "DeepSeekSync.CommentCode"  to "action.send.comment.prompt",
        )
    }
}
