package com.kldo

import com.intellij.ide.util.PropertiesComponent

/**
 * 5 个自定义 Prompt 槽位的设置存储。
 *
 * 读写 PropertiesComponent，无需注册 service，启动零开销。
 * 默认值从 MyMessageBundle 读取，与现有行为一致。
 */
data class PromptSlot(
    val label: String,
    val template: String
)

object DeepSeekSyncPromptSettings {

    private const val PREFIX = "DeepSeekSync.prompt"
    const val SLOT_COUNT = 5

    private fun key(index: Int, field: String) = "$PREFIX.$index.$field"

    fun getSlots(): List<PromptSlot> {
        val props = PropertiesComponent.getInstance()
        return (0 until SLOT_COUNT).map { i ->
            PromptSlot(
                label = props.getValue(key(i, "label")) ?: defaultLabel(i),
                template = props.getValue(key(i, "template")) ?: defaultTemplate(i)
            )
        }
    }

    fun setSlots(slots: List<PromptSlot>) {
        val props = PropertiesComponent.getInstance()
        slots.forEachIndexed { i, slot ->
            props.setValue(key(i, "label"), slot.label)
            props.setValue(key(i, "template"), slot.template)
        }
    }

    fun resetDefaults() {
        val props = PropertiesComponent.getInstance()
        (0 until SLOT_COUNT).forEach { i ->
            props.setValue(key(i, "label"), defaultLabel(i))
            props.setValue(key(i, "template"), defaultTemplate(i))
        }
    }

    /** 替换模板中的变量 */
    fun processTemplate(
        template: String,
        selection: String,
        file: String? = null,
        lang: String? = null,
        className: String? = null,
        methodName: String? = null,
        packageName: String? = null,
        imports: String? = null
    ): String {
        var result = template
        result = result.replace("{selection}", selection)
        if (file != null) result = result.replace("{file}", file)
        if (lang != null) result = result.replace("{lang}", lang)
        result = result.replace("{class}", className ?: "")
        result = result.replace("{method}", methodName ?: "")
        result = result.replace("{package}", packageName ?: "")
        result = result.replace("{imports}", imports ?: "")
        return result
    }

    // ── 默认值 ──

    private fun defaultLabel(index: Int): String = when (index) {
        0 -> "📖 " + MyMessageBundle.message("action.DeepSeekSync.ExplainCode.text").removePrefix("📖 ").trim()
        1 -> "✨ " + MyMessageBundle.message("action.DeepSeekSync.ImproveCode.text").removePrefix("✨ ").trim()
        2 -> "🔍 " + MyMessageBundle.message("action.DeepSeekSync.ReviewCode.text").removePrefix("🔍 ").trim()
        3 -> "🧪 " + MyMessageBundle.message("action.DeepSeekSync.TestCode.text").removePrefix("🧪 ").trim()
        4 -> "💬 " + MyMessageBundle.message("action.DeepSeekSync.CommentCode.text").removePrefix("💬 ").trim()
        else -> ""
    }

    private fun defaultTemplate(index: Int): String {
        val prompt = MyMessageBundle.message(
            when (index) {
                0 -> "action.send.explain.prompt"
                1 -> "action.send.improve.prompt"
                2 -> "action.send.review.prompt"
                3 -> "action.send.test.prompt"
                4 -> "action.send.comment.prompt"
                else -> ""
            }
        )
        return """{file} | {class}.{method} | {package}
{imports}

$prompt

{selection}""".trimIndent()
    }
}
