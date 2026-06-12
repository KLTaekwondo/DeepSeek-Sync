package com.kldo

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * 将文本注入到 DeepSeek 聊天输入框的共享工具。
 *
 * 流程：
 * 1. 写入系统剪贴板（兜底）
 * 2. JS 注入到 JCEF 浏览器的聊天输入框
 * 3. 激活侧边栏并切换到聊天标签页
 */
object DeepSeekInjector {

    private const val CHAT_URL = "https://chat.deepseek.com/"
    private const val TOOL_WINDOW_ID = "DeepSeek-Sync/深度同步"

    /**
     * 将文本注入到 DeepSeek 聊天输入框并激活侧边栏。
     */
    fun inject(text: String, project: Project) {
        // 1. 始终写入剪贴板（兜底）
        Toolkit.getDefaultToolkit().systemClipboard
            .setContents(StringSelection(text), null)

        // 2. JS 注入到 DeepSeek 聊天页面
        val browser = DeepSeekBrowserHolder.getOrCreateBrowser(CHAT_URL)
        browser?.cefBrowser?.executeJavaScript(
            buildInjectionScript(text), CHAT_URL, 0
        )

        // 3. 打开侧边栏并切换到聊天标签页
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(TOOL_WINDOW_ID) ?: return
        toolWindow.activate(null, true)
        toolWindow.contentManager.contents
            .firstOrNull()
            ?.let { toolWindow.contentManager.setSelectedContent(it) }
    }

    /**
     * 生成 JS 注入脚本，将文本填入 DeepSeek 聊天的输入框。
     *
     * DeepSeek 用的 React，所以必须用 native setter 绕过 React 的受控组件。
     */
    fun buildInjectionScript(text: String): String {
        val escaped = text
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
