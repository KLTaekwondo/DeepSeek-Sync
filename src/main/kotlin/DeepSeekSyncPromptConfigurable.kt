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
        main.add(JLabel(MyMessageBundle.message("settings.title")),
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
            panel.border = BorderFactory.createTitledBorder(
                MyMessageBundle.message("settings.prompt.number", i + 1))

            val c = GridBagConstraints()
            // Label → field
            c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST
            c.insets = Insets(2, 5, 2, 5)
            panel.add(JLabel(MyMessageBundle.message("settings.label")), c)

            c.gridx = 1; c.gridy = 0; c.fill = GridBagConstraints.HORIZONTAL
            c.weightx = 1.0
            panel.add(sf.labelField, c)

            // Template → textarea
            c.gridx = 0; c.gridy = 1; c.fill = GridBagConstraints.NONE
            c.weightx = 0.0; c.anchor = GridBagConstraints.NORTHWEST
            panel.add(JLabel(MyMessageBundle.message("settings.template")), c)

            c.gridx = 1; c.gridy = 1; c.fill = GridBagConstraints.BOTH
            c.weightx = 1.0; c.weighty = 1.0
            panel.add(JScrollPane(sf.templateArea), c)

            // 添加槽位到主面板
            main.add(panel, gb.apply { gridy = row++; fill = GridBagConstraints.BOTH;
                weighty = if (i == slots.lastIndex) 1.0 else 0.0; insets = Insets(3, 8, 3, 8) })
        }

        // 变量提示
        main.add(JLabel(MyMessageBundle.message("settings.variables.hint")),
            gb.apply { gridy = row++; fill = GridBagConstraints.HORIZONTAL; weighty = 0.0; insets = Insets(4, 8, 4, 8) })

        // 重置按钮
        main.add(JButton(MyMessageBundle.message("settings.reset")).also { btn ->
            btn.addActionListener { resetToDefaults() }
        }, gb.apply { gridy = row++; insets = Insets(8, 8, 8, 8) })

        // 维护期告示
        main.add(JLabel(MyMessageBundle.message("settings.maintenance.notice")).also {
            it.foreground = javax.swing.UIManager.getColor("Label.disabledForeground")
                ?: java.awt.Color.GRAY
            it.font = it.font.deriveFont(it.font.size * 0.9f)
        }, gb.apply { gridy = row; insets = Insets(2, 8, 8, 8) })

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
