package com.kldo

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement

/**
 * 右键菜单「发送到 DeepSeek ▶」子菜单各项 AI 操作。
 *
 * 通过 Action ID 区分不同的 prompt slot，用户可在 Settings 中自定义模板。
 */
class DeepSeekAIAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText ?: return

        val actionId = ActionManager.getInstance().getId(this) ?: return
        val slot = slotFor(actionId)

        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)?.name

        // ── PSI 上下文（反射，不依赖 Java 模块） ──
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val psiElement = psiFile?.findElementAt(editor.selectionModel.selectionStart)
        val (className, methodName, packageName) = extractPsiContext(psiElement)
        val importsText = psiFile?.text?.let { text ->
            val lines = text.lines().map { it.trim() }
                .filter { it.startsWith("import ") }
            if (lines.isEmpty()) null else lines.joinToString("\n") { it.removeSuffix(";") }
        }

        val prompt = DeepSeekSyncPromptSettings.processTemplate(
            template = slot.template,
            selection = selectedText,
            file = file,
            className = className,
            methodName = methodName,
            packageName = packageName,
            imports = importsText
        )
        DeepSeekInjector.inject(prompt, project)
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible =
            e.project != null &&
            editor != null &&
            editor.selectionModel.hasSelection()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    /**
     * 通过反射调用 PsiTreeUtil / PsiClass / PsiMethod，
     * 避免对 [com.intellij.modules.java] 的编译期依赖。
     * 在不支持 Java PSI 的 IDE 中返回 null，不会崩溃。
     */
    private fun extractPsiContext(element: PsiElement?): PsiContext {
        if (element == null) return PsiContext()
        return try {
            val psiTreeUtil = Class.forName("com.intellij.psi.util.PsiTreeUtil")
            val getParent = psiTreeUtil.getMethod("getParentOfType", PsiElement::class.java, Class::class.java)

            val psiClassClass = Class.forName("com.intellij.psi.PsiClass")
            val psiClass = getParent.invoke(null, element, psiClassClass)
            val nameMethod = psiClassClass.getMethod("getName")
            val qualifiedNameMethod = psiClassClass.getMethod("getQualifiedName")

            val cn = nameMethod.invoke(psiClass) as? String?
            val qn = qualifiedNameMethod.invoke(psiClass) as? String?
            val pkg = qn?.let { q ->
                val dot = q.lastIndexOf('.')
                if (dot > 0) q.substring(0, dot) else ""
            }?.ifBlank { null }

            val psiMethodClass = Class.forName("com.intellij.psi.PsiMethod")
            val psiMethod = getParent.invoke(null, element, psiMethodClass)
            val mn = psiMethod?.let { nameMethod.invoke(it) as? String }

            PsiContext(className = cn, methodName = mn, packageName = pkg)
        } catch (_: Exception) {
            PsiContext()
        }
    }

    private data class PsiContext(
        val className: String? = null,
        val methodName: String? = null,
        val packageName: String? = null
    )

    companion object {
        /** Action ID → slot index */
        private val SLOT_INDEX = mapOf(
            "DeepSeekSync.ExplainCode"  to 0,
            "DeepSeekSync.ImproveCode"  to 1,
            "DeepSeekSync.ReviewCode"   to 2,
            "DeepSeekSync.TestCode"     to 3,
            "DeepSeekSync.CommentCode"  to 4,
        )

        private fun slotFor(actionId: String): PromptSlot {
            val slots = DeepSeekSyncPromptSettings.getSlots()
            val index = SLOT_INDEX[actionId] ?: 0
            return slots.getOrElse(index) { slots.first() }
        }
    }
}
