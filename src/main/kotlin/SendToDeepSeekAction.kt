package com.kldo

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.project.DumbAware
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * 右键菜单：将选中的代码直接填入 DeepSeek 聊天输入框
 *
 * 流程：选中代码 → 右键/快捷键 → 侧栏打开 + 聊天输入框自动填入代码
 * 剪贴板也同时写入，以防 JS 注入失败
 */
class SendToDeepSeekAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText ?: return

        // 1. 始终写入剪贴板（兜底方案）
        Toolkit.getDefaultToolkit().systemClipboard
            .setContents(StringSelection(selectedText), null)

        // 2. 尝试 JS 注入到 DeepSeek 聊天页面
        val browser = DeepSeekBrowserHolder.getOrCreateBrowser(CHAT_URL)
        if (browser != null) {
            browser.cefBrowser.executeJavaScript(
                buildInjectionScript(selectedText),
                CHAT_URL, 0
            )
        }

        // 3. 打开侧边栏并切换到聊天标签页（第一个 tab 始终是聊天）
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(TOOL_WINDOW_ID) ?: return
        toolWindow.activate(null, true)
        toolWindow.contentManager.getContents()
            .firstOrNull()
            ?.let { toolWindow.contentManager.setSelectedContent(it) }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible =
            project != null &&
            editor != null &&
            editor.selectionModel.hasSelection()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    companion object {
        private const val CHAT_URL = "https://chat.deepseek.com/"
        private const val TOOL_WINDOW_ID = "DeepSeek-Sync/深度同步"

        /**
         * 生成 JS 注入脚本，将代码填入 DeepSeek 聊天的输入框
         *
         * DeepSeek 用的 React，所以必须用 native setter 绕过 React 的受控组件
         */
        private fun buildInjectionScript(code: String): String {
            // 对代码进行 JS 字符串转义（防注入 / 换行 / 特殊字符）
            val escaped = code
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")

            return """
            (() => {
                const CODE = '$escaped';
                let tries = 0;
                const maxTries = 30;  // 最多等 15 秒（500ms × 30）

                const fillInput = () => {
                    // DeepSeek 可能用 textarea 或 contenteditable div
                    const input = document.querySelector('textarea')
                              || document.querySelector('[contenteditable="true"]')
                              || document.querySelector('.ds-input');
                    if (!input) {
                        if (++tries < maxTries) {
                            setTimeout(fillInput, 500);
                        }
                        return;
                    }

                    try {
                        // React 受控组件：用 native setter 绕过 React 的 value 接管
                        const nativeSetter = Object.getOwnPropertyDescriptor(
                            window.HTMLTextAreaElement.prototype, 'value'
                        );
                        if (nativeSetter) {
                            nativeSetter.set.call(input, CODE);
                        } else {
                            input.textContent = CODE;  // contenteditable 兜底
                        }

                        // 触发 React 的 input 事件让框架感知变化
                        input.dispatchEvent(new Event('input', { bubbles: true }));

                        // 光标移到末尾
                        if (input.selectionStart !== undefined) {
                            input.selectionStart = input.selectionEnd = CODE.length;
                        }
                        input.focus();
                    } catch(e) {
                        console.error('DeepSeek-Sync: injection failed', e);
                    }
                };

                if (document.readyState === 'complete') {
                    fillInput();
                } else {
                    window.addEventListener('load', fillInput);
                }
            })();
            """.trimIndent()
        }
    }
}
