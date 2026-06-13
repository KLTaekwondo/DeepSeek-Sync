package com.kldo

import com.intellij.openapi.options.Configurable
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class DeepSeekSyncPromptConfigurable : Configurable {

    private val fields = mutableListOf<SlotFields>()

    private class SlotFields(val index: Int) {
        val labelField = JTextField()
        val templateArea = JTextArea(5, 45).apply {
            lineWrap = true
            wrapStyleWord = true
        }
    }

    override fun createComponent(): JComponent {
        fields.clear()

        val main = JPanel(GridBagLayout())
        val gb = GridBagConstraints().apply {
            gridx = 0; fill = GridBagConstraints.HORIZONTAL; weightx = 1.0
        }
        var row = 0

        // 标题
        main.add(JLabel("Customize AI prompt templates — {selection} will be replaced with the selected code."),
            gb.apply { gridy = row++; insets = Insets(4, 8, 4, 8) })

        // 分隔线
        main.add(JSeparator(),
            gb.apply { gridy = row++; insets = Insets(6, 8, 6, 8) })

        // 5 个槽位
        val slots = DeepSeekSyncPromptSettings.getSlots()
        slots.forEachIndexed { i, slot ->
            val sf = SlotFields(i).also { fields.add(it) }
            sf.labelField.text = slot.label
            sf.templateArea.text = slot.template

            val panel = JPanel(GridBagLayout())
            panel.border = BorderFactory.createTitledBorder("Prompt #${i + 1}")

            val c = GridBagConstraints()
            // Label → field
            c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST
            c.insets = Insets(2, 5, 2, 5)
            panel.add(JLabel("Label:"), c)

            c.gridx = 1; c.gridy = 0; c.fill = GridBagConstraints.HORIZONTAL
            c.weightx = 1.0
            panel.add(sf.labelField, c)

            // Template → textarea
            c.gridx = 0; c.gridy = 1; c.fill = GridBagConstraints.NONE
            c.weightx = 0.0; c.anchor = GridBagConstraints.NORTHWEST
            panel.add(JLabel("Template:"), c)

            c.gridx = 1; c.gridy = 1; c.fill = GridBagConstraints.BOTH
            c.weightx = 1.0; c.weighty = 1.0
            panel.add(JScrollPane(sf.templateArea), c)

            // 添加槽位到主面板
            main.add(panel, gb.apply { gridy = row++; fill = GridBagConstraints.BOTH;
                weighty = if (i == slots.lastIndex) 1.0 else 0.0; insets = Insets(3, 8, 3, 8) })
        }

        // 变量提示
        main.add(JLabel("<html>Available variables:<br>" +
                "&nbsp;&nbsp;<b>{selection}</b> — selected code<br>" +
                "&nbsp;&nbsp;<b>{file}</b> — file name<br>" +
                "&nbsp;&nbsp;<b>{class}</b> — enclosing class name<br>" +
                "&nbsp;&nbsp;<b>{method}</b> — enclosing method name<br>" +
                "&nbsp;&nbsp;<b>{package}</b> — package name<br>" +
                "&nbsp;&nbsp;<b>{imports}</b> — file imports</html>"),
            gb.apply { gridy = row++; fill = GridBagConstraints.HORIZONTAL; weighty = 0.0; insets = Insets(4, 8, 4, 8) })

        // 重置按钮
        main.add(JButton("Reset to Defaults").also { btn ->
            btn.addActionListener { resetToDefaults() }
        }, gb.apply { gridy = row; insets = Insets(8, 8, 8, 8) })

        return JScrollPane(main)
    }

    override fun isModified(): Boolean {
        val current = DeepSeekSyncPromptSettings.getSlots()
        return fields.withIndex().any { (i, f) ->
            val slot = current.getOrElse(i) { PromptSlot("", "") }
            f.labelField.text != slot.label || f.templateArea.text != slot.template
        }
    }

    override fun apply() {
        DeepSeekSyncPromptSettings.setSlots(
            fields.map { PromptSlot(it.labelField.text, it.templateArea.text) }
        )
    }

    override fun reset() {
        val current = DeepSeekSyncPromptSettings.getSlots()
        fields.forEachIndexed { i, f ->
            val slot = current.getOrElse(i) { PromptSlot("", "") }
            f.labelField.text = slot.label
            f.templateArea.text = slot.template
        }
    }

    override fun getDisplayName(): String = "DeepSeek-Sync"

    private fun resetToDefaults() {
        DeepSeekSyncPromptSettings.resetDefaults()
        val defaults = DeepSeekSyncPromptSettings.getSlots()
        fields.forEachIndexed { i, f ->
            val slot = defaults.getOrElse(i) { PromptSlot("", "") }
            f.labelField.text = slot.label
            f.templateArea.text = slot.template
        }
    }
}
